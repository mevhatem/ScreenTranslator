package com.screentranslator.app.engine

import android.graphics.Bitmap
import android.graphics.Rect
import android.util.Log
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.Translator
import com.google.mlkit.nl.translate.TranslatorOptions
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.screentranslator.app.data.model.TranslatedBlock
import com.screentranslator.app.data.model.TranslationResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

/**
 * Akıllı Doğal Dil OCR ve Hibrit Çeviri Motoru:
 * - Paragrafları ve cümleleri ortadan bölmez! Cümle bütünlüğünü koruyarak
 *   Google Translate Cloud motoruna iletir (kusursuz doğal Türkçe).
 * - Orijinal metinlerin gerçek yazı tipi boyutlarını (font size) piksel piksel hesaplar.
 */
class MlKitEngine(
    sourceLang: String = TranslateLanguage.ENGLISH,
    targetLang: String = TranslateLanguage.TURKISH
) {
    companion object {
        private const val TAG = "MlKitEngine"
    }

    private val textRecognizer: TextRecognizer =
        TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    private val translatorOptions = TranslatorOptions.Builder()
        .setSourceLanguage(sourceLang)
        .setTargetLanguage(targetLang)
        .build()

    private val offlineTranslator: Translator = Translation.getClient(translatorOptions)
    private val onlineTranslator = OnlineTranslatorEngine()

    @Volatile
    private var isOfflineModelDownloaded = false

    var preferOnline: Boolean = true

    suspend fun ensureModelDownloaded(): Boolean = withContext(Dispatchers.IO) {
        try {
            val conditions = DownloadConditions.Builder().build()
            offlineTranslator.downloadModelIfNeeded(conditions).await()
            isOfflineModelDownloaded = true
            true
        } catch (e: Exception) {
            Log.e(TAG, "Offline model indirme hatası: ${e.message}", e)
            false
        }
    }

    private suspend fun smartTranslate(text: String): String {
        if (preferOnline) {
            val onlineResult = onlineTranslator.translate(text)
            if (!onlineResult.isNullOrBlank()) {
                return onlineResult
            }
        }

        if (!isOfflineModelDownloaded) {
            ensureModelDownloaded()
        }
        return try {
            offlineTranslator.translate(text).await()
        } catch (e: Exception) {
            text
        }
    }

    /**
     * RAM üzerindeki Bitmap'ten cümle bütünlüğünü ve 1:1 orijinal yazı tipi boyutlarını koruyarak çevirir.
     */
    suspend fun processImageWithBlocks(bitmap: Bitmap): Pair<TranslationResult, List<TranslatedBlock>> =
        withContext(Dispatchers.Default) {
            try {
                val image = InputImage.fromBitmap(bitmap, 0)
                val visionText = textRecognizer.process(image).await()
                val fullRecognizedText = visionText.text.trim()

                Log.d(TAG, "OCR bitti. Toplam blok: ${visionText.textBlocks.size}")

                if (fullRecognizedText.isEmpty() || visionText.textBlocks.isEmpty()) {
                    return@withContext Pair(
                        TranslationResult(
                            originalText = "",
                            translatedText = "",
                            isSuccess = false,
                            errorMessage = "Ekranda okunabilir bir metin bulunamadı."
                        ),
                        emptyList()
                    )
                }

                // (text, boundingBox, originalFontSize)
                val targetUnits = mutableListOf<Triple<String, Rect, Float>>()

                for (block in visionText.textBlocks) {
                    val lines = block.lines
                    if (lines.isEmpty()) continue

                    val box = block.boundingBox ?: continue

                    if (lines.size > 1) {
                        // Çok satırlı paragraf: Satırları boşlukla birleştir
                        val fullParagraphText = lines.joinToString(" ") { it.text.trim() }.trim()
                        if (fullParagraphText.length >= 2 && fullParagraphText.any { it.isLetter() }) {
                            // Ortalama satır yüksekliğinden gerçek yazı boyutu hesapla
                            val heights = lines.mapNotNull { it.boundingBox?.height()?.toFloat() }
                            val avgHeight = if (heights.isNotEmpty()) heights.average().toFloat() else (box.height().toFloat() / lines.size)
                            val fontSize = avgHeight * 0.72f
                            targetUnits.add(Triple(fullParagraphText, box, fontSize))
                        }
                    } else {
                        // Tek satırlık menü, buton, kullanıcı adı
                        val line = lines[0]
                        val lineBox = line.boundingBox ?: box
                        val text = line.text.trim()
                        if (text.length >= 2 && text.any { it.isLetter() }) {
                            val fontSize = lineBox.height().toFloat() * 0.72f
                            targetUnits.add(Triple(text, lineBox, fontSize))
                        }
                    }
                }

                Log.d(TAG, "Bağlam ve font boyutlu çevrilecek birim sayısı: ${targetUnits.size}")

                // Paralel Çeviri
                val deferredUnits = targetUnits.map { (text, box, fontSize) ->
                    async(Dispatchers.IO) {
                        try {
                            val translated = smartTranslate(text)
                            TranslatedBlock(
                                originalText = text,
                                translatedText = translated,
                                boundingBox = box,
                                originalFontSize = fontSize
                            )
                        } catch (e: Exception) {
                            Log.e(TAG, "Çeviri hatası: ${e.message}")
                            null
                        }
                    }
                }

                val translatedBlocks = deferredUnits.awaitAll().filterNotNull()
                val fullTranslatedText = translatedBlocks.joinToString("\n\n") { it.translatedText }

                val result = TranslationResult(
                    originalText = fullRecognizedText,
                    translatedText = fullTranslatedText,
                    isSuccess = true
                )

                Pair(result, translatedBlocks)
            } catch (e: Exception) {
                Log.e(TAG, "processImageWithBlocks hatası: ${e.message}", e)
                Pair(
                    TranslationResult(
                        originalText = "",
                        translatedText = "",
                        isSuccess = false,
                        errorMessage = "Hata oluştu: ${e.localizedMessage}"
                    ),
                    emptyList()
                )
            }
        }

    fun close() {
        try {
            textRecognizer.close()
            offlineTranslator.close()
        } catch (e: Exception) {
            Log.e(TAG, "Kapatma hatası: ${e.message}", e)
        }
    }
}
