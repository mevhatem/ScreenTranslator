package com.screentranslator.app.engine

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * Google Translate'in yüksek kaliteli bulut motorunu kullanan ücretsiz çeviri servisi.
 * Yapay zeka destekli, doğal dil ve argo/teknoloji terimlerini (Gemini, Antigravity vb.)
 * doğru anlayan çeviri sağlar.
 */
class OnlineTranslatorEngine {

    companion object {
        private const val TAG = "OnlineTranslatorEngine"
    }

    /**
     * Verilen metni internet üzerinden yüksek kaliteyle Türkçeye çevirir.
     */
    suspend fun translate(text: String, sourceLang: String = "auto", targetLang: String = "tr"): String? =
        withContext(Dispatchers.IO) {
            if (text.isBlank()) return@withContext ""

            var connection: HttpURLConnection? = null
            try {
                val encodedText = URLEncoder.encode(text, "UTF-8")
                val urlString = "https://translate.googleapis.com/translate_a/single?client=gtx&sl=$sourceLang&tl=$targetLang&dt=t&q=$encodedText"
                val url = URL(urlString)

                connection = (url.openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    connectTimeout = 4000
                    readTimeout = 4000
                    setRequestProperty("User-Agent", "Mozilla/5.0")
                }

                val responseCode = connection.responseCode
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    val reader = BufferedReader(InputStreamReader(connection.inputStream))
                    val response = StringBuilder()
                    var line: String?
                    while (reader.readLine().also { line = it } != null) {
                        response.append(line)
                    }
                    reader.close()

                    // JSON formatı: [[["Çevrilmiş Metin","Orijinal Metin",null,null,10]],null,"en",...]
                    val jsonArray = JSONArray(response.toString())
                    val sentencesArray = jsonArray.getJSONArray(0)
                    val resultBuilder = StringBuilder()

                    for (i in 0 until sentencesArray.length()) {
                        val sentence = sentencesArray.getJSONArray(i)
                        resultBuilder.append(sentence.getString(0))
                    }

                    val translated = resultBuilder.toString().trim()
                    Log.d(TAG, "Online çeviri başarılı: $translated")
                    translated
                } else {
                    Log.w(TAG, "HTTP Hatası: $responseCode")
                    null
                }
            } catch (e: Exception) {
                Log.w(TAG, "Online çeviri yapılamadı (internet yok veya zaman aşımı): ${e.message}")
                null
            } finally {
                connection?.disconnect()
            }
        }
}
