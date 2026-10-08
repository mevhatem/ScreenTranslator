package com.screentranslator.app.engine

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.Image
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.DisplayMetrics
import android.util.Log
import android.view.Display
import android.view.WindowManager
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

/**
 * Ekran görüntüsü yakalama motoru.
 * Donanımın gerçek 1:1 piksel matrisini (getRealMetrics) kullanarak bulanıklık ve
 * çözünürlük kaybını (downscale) sıfıra indirir; en küçük yazıları bile jilet gibi yakalar.
 */
class ScreenCaptureEngine(
    private val context: Context,
    private val mediaProjection: MediaProjection
) {
    companion object {
        private const val TAG = "ScreenCaptureEngine"
    }

    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val mainHandler = Handler(Looper.getMainLooper())

    private var virtualDisplay: VirtualDisplay? = null
    private var imageReader: ImageReader? = null
    private var width: Int = 0
    private var height: Int = 0
    private var density: Int = 0

    @Volatile
    private var lastValidBitmap: Bitmap? = null

    @Volatile
    private var pendingDeferred: CompletableDeferred<Bitmap?>? = null

    init {
        initVirtualDisplay()
    }

    @SuppressLint("WrongConstant")
    @Suppress("DEPRECATION")
    private fun initVirtualDisplay() {
        // Gerçek donanım ekran çözünürlüğünü ve DPI'ını al (Asla downscale olmaması için)
        val realMetrics = DisplayMetrics()
        val display: Display? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                context.display
            } catch (e: Exception) {
                windowManager.defaultDisplay
            }
        } else {
            windowManager.defaultDisplay
        }

        display?.getRealMetrics(realMetrics) ?: windowManager.defaultDisplay.getRealMetrics(realMetrics)

        width = realMetrics.widthPixels
        height = realMetrics.heightPixels
        density = realMetrics.densityDpi

        Log.d(TAG, "Donanım Gerçek Ekran Çözünürlüğü: ${width}x${height}, DPI: $density")

        // 1:1 tam donanım çözünürlüğünde ImageReader
        val reader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 3)
        imageReader = reader

        reader.setOnImageAvailableListener({ ir ->
            var image: Image? = null
            try {
                image = ir.acquireLatestImage()
                if (image != null) {
                    val bitmap = convertImageToBitmap(image)
                    if (bitmap != null) {
                        val old = lastValidBitmap
                        lastValidBitmap = bitmap
                        if (old != null && !old.isRecycled && old != bitmap) {
                            old.recycle()
                        }

                        val deferred = pendingDeferred
                        if (deferred != null && !deferred.isCompleted) {
                            pendingDeferred = null
                            deferred.complete(bitmap.copy(Bitmap.Config.ARGB_8888, false))
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Image işleme hatası: ${e.message}")
            } finally {
                image?.close()
            }
        }, mainHandler)

        virtualDisplay = mediaProjection.createVirtualDisplay(
            "ScreenTranslatorVirtualDisplay",
            width,
            height,
            density,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            reader.surface,
            null,
            mainHandler
        )

        Log.d(TAG, "1:1 Netlikte VirtualDisplay kuruldu.")
    }

    private fun convertImageToBitmap(image: Image): Bitmap? {
        return try {
            val plane = image.planes[0]
            val buffer = plane.buffer
            val pixelStride = plane.pixelStride
            val rowStride = plane.rowStride
            val rowPadding = rowStride - pixelStride * width

            buffer.rewind()

            val bitmapWidth = width + (rowPadding / pixelStride)
            val tempBitmap = Bitmap.createBitmap(
                bitmapWidth,
                height,
                Bitmap.Config.ARGB_8888
            )
            tempBitmap.copyPixelsFromBuffer(buffer)

            val cleanBitmap = if (bitmapWidth != width) {
                val cropped = Bitmap.createBitmap(tempBitmap, 0, 0, width, height)
                tempBitmap.recycle()
                cropped
            } else {
                tempBitmap
            }

            cleanBitmap
        } catch (e: Exception) {
            Log.e(TAG, "convertImageToBitmap hatası: ${e.message}")
            null
        }
    }

    suspend fun captureScreen(): Bitmap? = withContext(Dispatchers.Default) {
        val deferred = CompletableDeferred<Bitmap?>()
        pendingDeferred = deferred

        for (i in 1..6) {
            if (deferred.isCompleted) break
            delay(40)
        }

        if (deferred.isCompleted) {
            return@withContext deferred.await()
        }

        pendingDeferred = null

        val fallback = lastValidBitmap
        if (fallback != null && !fallback.isRecycled) {
            Log.d(TAG, "Hazır tam netlikteki kare kullanıldı.")
            return@withContext fallback.copy(Bitmap.Config.ARGB_8888, false)
        }

        Log.w(TAG, "Kare bulunamadı.")
        null
    }

    fun release() {
        try {
            pendingDeferred?.cancel()
            pendingDeferred = null
            lastValidBitmap?.recycle()
            lastValidBitmap = null
            virtualDisplay?.release()
            virtualDisplay = null
            imageReader?.close()
            imageReader = null
            mediaProjection.stop()
        } catch (e: Exception) {
            Log.e(TAG, "Release hatası: ${e.message}")
        }
    }
}
