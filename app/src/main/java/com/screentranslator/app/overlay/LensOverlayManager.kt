package com.screentranslator.app.overlay

import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.view.Gravity
import android.view.WindowManager
import com.screentranslator.app.data.model.TranslatedBlock

/**
 * Google Lens benzeri in-place çeviri katmanını yöneten sınıf.
 * Status Bar, Navigation Bar ve Kamera Çentiği (Cutout) boşluklarını yok sayarak
 * ekranın tam 0,0 noktasına oturur; piksel kaymasını önler.
 */
class LensOverlayManager(private val context: Context) {

    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private var overlayView: LensOverlayView? = null

    /**
     * Çevrilen blokları ekran üzerinde tam yerlerinde sıfır kayma ile gösterir.
     */
    fun show(blocks: List<TranslatedBlock>, captureWidth: Int, captureHeight: Int) {
        hide()

        if (blocks.isEmpty()) return

        val view = LensOverlayView(context, blocks, captureWidth, captureHeight) {
            hide()
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 0
            y = 0

            // Android 9+ Kamera çentiği alanını da kapsa (Pencerenin aşağı ötelenmesini engeller)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
        }

        try {
            windowManager.addView(view, params)
            overlayView = view
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Lens görünümünü kapatır.
     */
    fun hide() {
        try {
            overlayView?.let {
                if (it.isAttachedToWindow) {
                    windowManager.removeView(it)
                }
            }
            overlayView = null
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun isShowing(): Boolean = overlayView != null
}
