package com.screentranslator.app.data.model

import android.graphics.Rect

/**
 * Ekrandaki bir metin bloğunun koordinatlarını, çevirisini ve
 * 1:1 orijinal yazı tipi boyutunu (font size) tutan model.
 */
data class TranslatedBlock(
    val originalText: String,
    val translatedText: String,
    val boundingBox: Rect,
    val originalFontSize: Float
)
