package com.screentranslator.app.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.screentranslator.app.R
import com.screentranslator.app.engine.MlKitEngine
import com.screentranslator.app.engine.ScreenCaptureEngine
import com.screentranslator.app.overlay.FloatingBubbleManager
import com.screentranslator.app.overlay.FloatingResultCardManager
import com.screentranslator.app.overlay.LensOverlayManager
import com.screentranslator.app.ui.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Android 14+ Foreground MediaProjection standartlarına tam uyumlu
 * Kayan Baloncuk, Google Lens In-Place Ekran Çevirmeni Servisi.
 */
class BubbleService : Service() {

    companion object {
        const val EXTRA_RESULT_CODE = "extra_result_code"
        const val EXTRA_RESULT_DATA = "extra_result_data"
        const val ACTION_STOP_SERVICE = "action_stop_service"

        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "screen_translator_channel"
    }

    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.Main + serviceJob)
    private val mainHandler = Handler(Looper.getMainLooper())

    private var mediaProjection: MediaProjection? = null
    private var screenCaptureEngine: ScreenCaptureEngine? = null
    private lateinit var mlKitEngine: MlKitEngine
    private lateinit var bubbleManager: FloatingBubbleManager
    private lateinit var resultCardManager: FloatingResultCardManager
    private lateinit var lensOverlayManager: LensOverlayManager

    private var isProcessing = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()

        mlKitEngine = MlKitEngine()
        resultCardManager = FloatingResultCardManager(this)
        lensOverlayManager = LensOverlayManager(this)

        bubbleManager = FloatingBubbleManager(this) {
            handleBubbleClick()
        }

        // Çeviri modelini arka planda önceden hazırlamaya başla
        serviceScope.launch {
            mlKitEngine.ensureModelDownloaded()
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent == null) {
            stopSelf()
            return START_NOT_STICKY
        }

        if (intent.action == ACTION_STOP_SERVICE) {
            stopSelf()
            return START_NOT_STICKY
        }

        val resultCode = intent.getIntExtra(EXTRA_RESULT_CODE, 0)
        @Suppress("DEPRECATION")
        val resultData = intent.getParcelableExtra<Intent>(EXTRA_RESULT_DATA)

        if (resultCode == 0 || resultData == null) {
            stopSelf()
            return START_NOT_STICKY
        }

        // 1. Android 14 Kuralı: MediaProjection başlatılmadan ÖNCE Foreground Service çağrılmalıdır!
        startForegroundWithMediaProjection()

        // 2. MediaProjection başlatma ve Callback ekleme
        initMediaProjection(resultCode, resultData)

        // 3. Ekrana balonu yerleştir
        bubbleManager.show()

        return START_STICKY
    }

    /**
     * Android 14+ zorunlu Foreground bildirimini mediaProjection tipiyle başlatır.
     */
    private fun startForegroundWithMediaProjection() {
        val notification = createNotification()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ServiceCompat.startForeground(
                this,
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun initMediaProjection(resultCode: Int, resultData: Intent) {
        val mediaProjectionManager =
            getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager

        val projection = mediaProjectionManager.getMediaProjection(resultCode, resultData)
        if (projection == null) {
            stopSelf()
            return
        }

        // Android 14 MediaProjection callback zorunluluğu
        projection.registerCallback(object : MediaProjection.Callback() {
            override fun onStop() {
                super.onStop()
                mediaProjection = null
                stopSelf()
            }
        }, mainHandler)

        mediaProjection = projection
        screenCaptureEngine = ScreenCaptureEngine(this, projection)
    }

    /**
     * Baloncuğa tıklandığında:
     * Eğer Lens overlay ekrandaysa kapatır, değilse yeni ekran yakalayıp yerinde çevirir.
     */
    private fun handleBubbleClick() {
        // Eğer Lens görünümü zaten ekrandaysa, baloncuğa tekrar dokunulduğunda kapat
        if (lensOverlayManager.isShowing()) {
            lensOverlayManager.hide()
            return
        }

        if (isProcessing) return
        isProcessing = true

        serviceScope.launch {
            try {
                // 1. Baloncuğu gizle ki ekran görüntüsünde kendi ikonu çıkmasın
                bubbleManager.hide()

                // Pencerenin ekran buffer'ından kaybolması için kısa gecikme
                delay(120)

                // 2. Ekran görüntüsü al
                val bitmap = screenCaptureEngine?.captureScreen()

                // 3. Baloncuğu geri göster ve yükleniyor animasyonunu aç
                bubbleManager.show()
                bubbleManager.setLoading(true)

                if (bitmap != null) {
                    val bWidth = bitmap.width
                    val bHeight = bitmap.height

                    // 4. ML Kit OCR ve Blok Bazlı Çeviri pipeline'ını işlet
                    val (result, blocks) = mlKitEngine.processImageWithBlocks(bitmap)

                    // Bellek temizliği
                    if (!bitmap.isRecycled) {
                        bitmap.recycle()
                    }

                    withContext(Dispatchers.Main) {
                        bubbleManager.setLoading(false)

                        if (blocks.isNotEmpty()) {
                            // Google Lens gibi: Çevirileri tam orijinal metinlerin üzerine sıfır kayma ile çiz
                            lensOverlayManager.show(blocks, bWidth, bHeight)
                        } else {
                            // Blok bulunamadıysa veya hata varsa kartı göster
                            resultCardManager.showResult(result)
                        }
                    }
                } else {
                    bubbleManager.setLoading(false)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                bubbleManager.setLoading(false)
                bubbleManager.show()
            } finally {
                isProcessing = false
            }
        }
    }

    private fun createNotification(): Notification {
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingOpenApp = PendingIntent.getActivity(
            this,
            0,
            openAppIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val stopIntent = Intent(this, BubbleService::class.java).apply {
            action = ACTION_STOP_SERVICE
        }
        val pendingStop = PendingIntent.getService(
            this,
            1,
            stopIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.notification_title))
            .setContentText(getString(R.string.notification_text))
            .setSmallIcon(R.drawable.ic_translate_bubble)
            .setContentIntent(pendingOpenApp)
            .addAction(R.drawable.ic_close, "Servisi Durdur", pendingStop)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.notification_channel_desc)
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()

        lensOverlayManager.hide()
        bubbleManager.remove()
        resultCardManager.remove()

        screenCaptureEngine?.release()
        screenCaptureEngine = null

        mlKitEngine.close()

        stopForeground(STOP_FOREGROUND_REMOVE)
    }
}
