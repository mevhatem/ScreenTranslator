package com.screentranslator.app.data.model

/**
 * OCR ve çeviri işlem sonucunu temsil eden veri modeli.
 */
data class TranslationResult(
    val originalText: String,
    val translatedText: String,
    val isSuccess: Boolean = true,
    val errorMessage: String? = null
)
