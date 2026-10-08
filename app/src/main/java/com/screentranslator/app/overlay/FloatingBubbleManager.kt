package com.screentranslator.app.overlay

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.PixelFormat
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.WindowManager
import android.widget.ImageView
import android.widget.ProgressBar
import com.screentranslator.app.R
import kotlin.math.abs

/**
 * Ekranda sürüklenebilen ve tıklanabilen yüzen baloncuk yöneticisi.
 */
class FloatingBubbleManager(
    private val context: Context,
    private val onBubbleClick: () -> Unit
) {
    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private var bubbleView: View? = null
    private var params: WindowManager.LayoutParams? = null

    private var initialX = 0
    private var initialY = 0
    private var initialTouchX = 0f
    private var initialTouchY = 0f
    private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop

    @SuppressLint("InflateParams", "ClickableViewAccessibility")
    fun show() {
        if (bubbleView != null) {
            bubbleView?.visibility = View.VISIBLE
            return
        }

        val inflater = LayoutInflater.from(context)
        bubbleView = inflater.inflate(R.layout.layout_floating_bubble, null)

        params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 40
            y = 200
        }

        bubbleView?.setOnTouchListener { _, event ->
            val p = params ?: return@setOnTouchListener false
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = p.x
                    initialY = p.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    true
                }

                MotionEvent.ACTION_MOVE -> {
                    p.x = initialX + (event.rawX - initialTouchX).toInt()
                    p.y = initialY + (event.rawY - initialTouchY).toInt()
                    bubbleView?.let { windowManager.updateViewLayout(it, p) }
                    true
                }

                MotionEvent.ACTION_UP -> {
                    val diffX = abs(event.rawX - initialTouchX)
                    val diffY = abs(event.rawY - initialTouchY)
                    // Kullanıcı sürüklemediyse, tıklama olarak algıla
                    if (diffX < touchSlop && diffY < touchSlop) {
                        onBubbleClick()
                    }
                    true
                }

                else -> false
            }
        }

        try {
            windowManager.addView(bubbleView, params)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Baloncuğu ekran görüntüsü alma anında geçici olarak gizler.
     */
    fun hide() {
        bubbleView?.visibility = View.GONE
    }

    /**
     * Baloncuk üzerinde yükleniyor göstergesini ayarlar.
     */
    fun setLoading(isLoading: Boolean) {
        bubbleView?.let { view ->
            val progressBar = view.findViewById<ProgressBar>(R.id.progressBarBubble)
            val imgBubble = view.findViewById<ImageView>(R.id.imgBubble)

            if (isLoading) {
                progressBar.visibility = View.VISIBLE
                imgBubble.alpha = 0.3f
            } else {
                progressBar.visibility = View.GONE
                imgBubble.alpha = 1.0f
            }
        }
    }

    /**
     * Baloncuğu ekrandan ve WindowManager'dan tamamen kaldırır.
     */
    fun remove() {
        try {
            bubbleView?.let {
                if (it.isAttachedToWindow) {
                    windowManager.removeView(it)
                }
            }
            bubbleView = null
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
