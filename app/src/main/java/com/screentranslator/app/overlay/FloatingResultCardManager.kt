package com.screentranslator.app.overlay

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.PixelFormat
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import com.screentranslator.app.R
import com.screentranslator.app.data.model.TranslationResult

/**
 * Çeviri sonucunu ekranda şık bir floating card ile gösteren yönetici.
 */
class FloatingResultCardManager(private val context: Context) {

    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager

    private var cardView: View? = null
    private var params: WindowManager.LayoutParams? = null

    @SuppressLint("InflateParams")
    fun showResult(result: TranslationResult) {
        if (cardView == null) {
            val inflater = LayoutInflater.from(context)
            cardView = inflater.inflate(R.layout.layout_result_card, null)

            params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                        WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
                y = 60 // Ekranın altından boşluk
            }

            try {
                windowManager.addView(cardView, params)
            } catch (e: Exception) {
                e.printStackTrace()
                return
            }
        }

        cardView?.let { view ->
            val tvTranslated = view.findViewById<TextView>(R.id.tvTranslatedText)
            val tvOriginal = view.findViewById<TextView>(R.id.tvOriginalText)
            val btnClose = view.findViewById<ImageButton>(R.id.btnCloseCard)
            val btnCopy = view.findViewById<ImageButton>(R.id.btnCopyResult)

            if (result.isSuccess) {
                tvTranslated.text = result.translatedText
                tvOriginal.text = result.originalText
            } else {
                tvTranslated.text = result.errorMessage ?: "Çeviri başarısız oldu."
                tvOriginal.text = result.originalText.ifEmpty { "Metin algılanamadı." }
            }

            btnClose.setOnClickListener {
                hide()
            }

            btnCopy.setOnClickListener {
                val clipText = if (result.isSuccess) result.translatedText else result.originalText
                if (clipText.isNotBlank()) {
                    val clip = ClipData.newPlainText("Çeviri Sonucu", clipText)
                    clipboardManager.setPrimaryClip(clip)
                    Toast.makeText(context, "Metin panoya kopyalandı", Toast.LENGTH_SHORT).show()
                }
            }

            view.visibility = View.VISIBLE
        }
    }

    /**
     * Sonuç kartını gizler.
     */
    fun hide() {
        cardView?.visibility = View.GONE
    }

    /**
     * Sonuç kartını tamamen bellekten ve pencereden siler.
     */
    fun remove() {
        try {
            cardView?.let {
                if (it.isAttachedToWindow) {
                    windowManager.removeView(it)
                }
            }
            cardView = null
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
