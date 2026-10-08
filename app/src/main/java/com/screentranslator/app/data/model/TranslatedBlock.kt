package com.screentranslator.app.data.model

import android.graphics.Rect

/**
 * Ekrandaki bir metin bloğunun koordinatlarını ve çevirisini tutan model.
 */
data class TranslatedBlock(
    val originalText: String,
    val translatedText: String,
    val boundingBox: Rect
)
