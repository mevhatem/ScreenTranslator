package com.screentranslator.app.overlay

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import com.screentranslator.app.data.model.TranslatedBlock
import kotlin.math.max
import kotlin.math.min

/**
 * Google Lens in-place çeviri katmanı.
 * Özellikler:
 * - İki parmakla kıstırarak yakınlaştırma (Pinch-to-Zoom: 1x .. 4x).
 * - Yakınlaştırıldığında parmakla kaydırma (Pan / Drag).
 * - Çift dokunarak hızlı yakınlaştırma / sıfırlama (Double Tap).
 * - Sağ üstte şık Kapat (X) butonu.
 */
@SuppressLint("ViewConstructor")
class LensOverlayView(
    context: Context,
    private val blocks: List<TranslatedBlock>,
    private val captureWidth: Int,
    private val captureHeight: Int,
    private val onDismiss: () -> Unit
) : View(context) {

    // Arka plan derinlik efekti
    private val dimPaint = Paint().apply {
        color = Color.parseColor("#0D000000")
        style = Paint.Style.FILL
    }

    // Blok arka planı: %75 yarı saydam modern koyu kart
    private val blockBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#C4121722")
        style = Paint.Style.FILL
    }

    // İnce şık kenarlık
    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#334CAF50")
        style = Paint.Style.STROKE
        strokeWidth = 1.5f
    }

    // Çeviri metni boyası
    private val textPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#FFFFFF")
        isFakeBoldText = true
    }

    // Kapatma butonu boyaları
    private val closeBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#CC1E2430")
        style = Paint.Style.FILL
    }
    private val closeIconPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        strokeWidth = 4f
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }

    // Zoom ve Pan Değişkenleri
    private var scaleFactor = 1.0f
    private var focusX = 0f
    private var focusY = 0f
    private var translationX = 0f
    private var translationY = 0f
    private var lastTouchX = 0f
    private var lastTouchY = 0f
    private var activePointerId = MotionEvent.INVALID_POINTER_ID

    private val closeButtonRect = RectF()

    // İki parmakla yakınlaştırma algılayıcı
    private val scaleDetector = ScaleGestureDetector(context, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScale(detector: ScaleGestureDetector): Boolean {
            scaleFactor *= detector.scaleFactor
            scaleFactor = max(1.0f, min(scaleFactor, 4.0f))
            focusX = detector.focusX
            focusY = detector.focusY
            invalidate()
            return true
        }
    })

    // Çift tıklama algılayıcı
    private val gestureDetector = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
        override fun onDoubleTap(e: MotionEvent): Boolean {
            if (scaleFactor > 1.2f) {
                scaleFactor = 1.0f
                translationX = 0f
                translationY = 0f
            } else {
                scaleFactor = 2.2f
                focusX = e.x
                focusY = e.y
            }
            invalidate()
            return true
        }
    })

    init {
        isClickable = true
        isFocusable = true
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        canvas.save()

        // Zoom ve Pan Dönüşümleri
        if (scaleFactor > 1.0f) {
            canvas.translate(translationX, translationY)
            canvas.scale(scaleFactor, scaleFactor, focusX, focusY)
        }

        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), dimPaint)

        val scaleX = if (captureWidth > 0) width.toFloat() / captureWidth.toFloat() else 1f
        val scaleY = if (captureHeight > 0) height.toFloat() / captureHeight.toFloat() else 1f

        for (block in blocks) {
            val box = block.boundingBox
            if (box.width() <= 0 || box.height() <= 0) continue

            val left = box.left * scaleX
            val top = box.top * scaleY
            val right = box.right * scaleX
            val bottom = box.bottom * scaleY

            val rectF = RectF(
                (left - 3f).coerceAtLeast(0f),
                (top - 2f).coerceAtLeast(0f),
                (right + 3f).coerceAtMost(width.toFloat()),
                (bottom + 2f).coerceAtMost(height.toFloat())
            )

            canvas.drawRoundRect(rectF, 8f, 8f, blockBgPaint)
            canvas.drawRoundRect(rectF, 8f, 8f, borderPaint)

            drawTextInsideRect(canvas, block.translatedText, rectF)
        }

        canvas.restore()

        // Sağ üst köşedeki Kapatma (X) Butonunu Çiz (Zoom'dan bağımsız sabit)
        drawCloseButton(canvas)
    }

    private fun drawCloseButton(canvas: Canvas) {
        val size = 96f
        val margin = 48f
        val right = width - margin
        val top = margin + 40f
        val left = right - size
        val bottom = top + size

        closeButtonRect.set(left, top, right, bottom)
        canvas.drawRoundRect(closeButtonRect, size / 2f, size / 2f, closeBgPaint)

        val pad = 30f
        canvas.drawLine(left + pad, top + pad, right - pad, bottom - pad, closeIconPaint)
        canvas.drawLine(right - pad, top + pad, left + pad, bottom - pad, closeIconPaint)
    }

    private fun drawTextInsideRect(canvas: Canvas, text: String, rect: RectF) {
        val targetWidth = (rect.width() - 8).toInt().coerceAtLeast(16)
        val targetHeight = (rect.height() - 2).toInt().coerceAtLeast(12)

        var testSize = (rect.height() * 0.76f).coerceIn(9f, 22f)
        textPaint.textSize = testSize

        var staticLayout = StaticLayout.Builder.obtain(
            text,
            0,
            text.length,
            textPaint,
            targetWidth
        )
            .setAlignment(Layout.Alignment.ALIGN_NORMAL)
            .setLineSpacing(0f, 1.0f)
            .setIncludePad(false)
            .build()

        while (staticLayout.height > targetHeight && testSize > 7.0f) {
            testSize -= 0.5f
            textPaint.textSize = testSize
            staticLayout = StaticLayout.Builder.obtain(
                text,
                0,
                text.length,
                textPaint,
                targetWidth
            )
                .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                .setLineSpacing(0f, 1.0f)
                .setIncludePad(false)
                .build()
        }

        canvas.save()
        val dy = rect.top + ((rect.height() - staticLayout.height) / 2f).coerceAtLeast(0f)
        canvas.translate(rect.left + 4f, dy)
        staticLayout.draw(canvas)
        canvas.restore()
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        scaleDetector.onTouchEvent(event)
        gestureDetector.onTouchEvent(event)

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                // Kapatma butonuna tıklandı mı?
                if (closeButtonRect.contains(event.x, event.y)) {
                    onDismiss()
                    return true
                }
                lastTouchX = event.x
                lastTouchY = event.y
                activePointerId = event.getPointerId(0)
            }

            MotionEvent.ACTION_MOVE -> {
                val pointerIndex = event.findPointerIndex(activePointerId)
                if (pointerIndex != -1 && scaleFactor > 1.0f) {
                    val x = event.getX(pointerIndex)
                    val y = event.getY(pointerIndex)
                    val dx = x - lastTouchX
                    val dy = y - lastTouchY

                    translationX += dx
                    translationY += dy

                    lastTouchX = x
                    lastTouchY = y
                    invalidate()
                }
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                activePointerId = MotionEvent.INVALID_POINTER_ID
                // Eğer hiç zoom yapılmamışsa ve boş alana tek tıklandıysa kapat
                if (scaleFactor <= 1.05f && !closeButtonRect.contains(event.x, event.y)) {
                    onDismiss()
                }
            }

            MotionEvent.ACTION_POINTER_UP -> {
                val pointerIndex = event.actionIndex
                val pointerId = event.getPointerId(pointerIndex)
                if (pointerId == activePointerId) {
                    val newPointerIndex = if (pointerIndex == 0) 1 else 0
                    lastTouchX = event.getX(newPointerIndex)
                    lastTouchY = event.getY(newPointerIndex)
                    activePointerId = event.getPointerId(newPointerIndex)
                }
            }
        }

        return true
    }
}
