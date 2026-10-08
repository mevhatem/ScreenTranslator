package com.screentranslator.app.ui

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.screentranslator.app.service.BubbleService

class MainActivity : ComponentActivity() {

    private var hasOverlayPermission by mutableStateOf(false)
    private var hasNotificationPermission by mutableStateOf(false)
    private var isServiceRunning by mutableStateOf(false)

    private val overlayPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        checkPermissions()
        if (Settings.canDrawOverlays(this)) {
            Toast.makeText(this, "Üstte görüntüleme izni verildi.", Toast.LENGTH_SHORT).show()
        }
    }

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasNotificationPermission = isGranted
    }

    private val mediaProjectionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            startBubbleService(result.resultCode, result.data!!)
        } else {
            Toast.makeText(this, "Ekran yakalama izni verilmedi.", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        checkPermissions()

        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color(0xFF0F131C)
                ) {
                    ModernDashboard(
                        hasOverlay = hasOverlayPermission,
                        hasNotification = hasNotificationPermission,
                        isServiceRunning = isServiceRunning,
                        onRequestOverlay = { requestOverlayPermission() },
                        onRequestNotification = { requestNotificationPermission() },
                        onStartService = { requestScreenCapture() },
                        onStopService = { stopBubbleService() }
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        checkPermissions()
    }

    private fun checkPermissions() {
        hasOverlayPermission = Settings.canDrawOverlays(this)
        hasNotificationPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    private fun requestOverlayPermission() {
        if (!Settings.canDrawOverlays(this)) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            overlayPermissionLauncher.launch(intent)
        }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    private fun requestScreenCapture() {
        if (!hasOverlayPermission) {
            Toast.makeText(this, "Lütfen önce üstte gösterme iznini açın.", Toast.LENGTH_LONG).show()
            requestOverlayPermission()
            return
        }

        val projectionManager =
            getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        mediaProjectionLauncher.launch(projectionManager.createScreenCaptureIntent())
    }

    private fun startBubbleService(resultCode: Int, data: Intent) {
        val serviceIntent = Intent(this, BubbleService::class.java).apply {
            putExtra(BubbleService.EXTRA_RESULT_CODE, resultCode)
            putExtra(BubbleService.EXTRA_RESULT_DATA, data)
        }
        ContextCompat.startForegroundService(this, serviceIntent)
        isServiceRunning = true
        Toast.makeText(this, "Çeviri baloncuğu aktif edildi!", Toast.LENGTH_SHORT).show()
    }

    private fun stopBubbleService() {
        val stopIntent = Intent(this, BubbleService::class.java).apply {
            action = BubbleService.ACTION_STOP_SERVICE
        }
        startService(stopIntent)
        isServiceRunning = false
        Toast.makeText(this, "Servis durduruldu.", Toast.LENGTH_SHORT).show()
    }
}

@Composable
fun ModernDashboard(
    hasOverlay: Boolean,
    hasNotification: Boolean,
    isServiceRunning: Boolean,
    onRequestOverlay: () -> Unit,
    onRequestNotification: () -> Unit,
    onStartService: () -> Unit,
    onStopService: () -> Unit
) {
    val scrollState = rememberScrollState()

    val statusBg by animateColorAsState(
        targetValue = if (isServiceRunning) Color(0xFF1B5E20) else Color(0xFF263238),
        animationSpec = tween(400),
        label = "statusBg"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 24.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Logo & Başlık
        Box(
            modifier = Modifier
                .size(80.dp)
                .background(
                    Brush.radialGradient(
                        listOf(Color(0xFF2979FF), Color(0xFF1565C0))
                    ),
                    CircleShape
                )
                .border(2.dp, Color(0x40FFFFFF), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Translate,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(42.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Screen Translator",
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )

        Text(
            text = "Google Lens Tarzı Yerinde Ekran Çevirmeni",
            fontSize = 13.sp,
            color = Color(0xFF90A4AE),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 4.dp)
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Canlı Durum Rozeti (Badge)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .background(statusBg, RoundedCornerShape(20.dp))
                .padding(horizontal = 16.dp, vertical = 6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(if (isServiceRunning) Color(0xFF69F0AE) else Color(0xFF90A4AE), CircleShape)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (isServiceRunning) "Servis Aktif & Hazır" else "Servis Kapalı",
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White
            )
        }

        Spacer(modifier = Modifier.height(28.dp))

        // İzin Kartı
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF161B26))
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "Gerekli İzinler",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(14.dp))

                PermissionItem(
                    title = "Diğer Uygulamaların Üzerinde Gösterme",
                    desc = "Kayan baloncuk ve Lens çeviri katmanı için zorunludur.",
                    isGranted = hasOverlay,
                    onGrant = onRequestOverlay
                )

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    Spacer(modifier = Modifier.height(12.dp))
                    PermissionItem(
                        title = "Bildirim İzni (Android 13+)",
                        desc = "Servisin arka planda kesintisiz çalışması için.",
                        isGranted = hasNotification,
                        onGrant = onRequestNotification
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Özellikler ve Kullanım İpuçları Kartı
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF161B26))
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "Özellikler & İpuçları",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(14.dp))

                FeatureRow(
                    icon = Icons.Default.Layers,
                    title = "In-Place Yerinde Çeviri",
                    desc = "Google Lens gibi ekrandaki orijinal metinlerin tam üzerine çeviri kutuları yerleşir."
                )
                Spacer(modifier = Modifier.height(10.dp))
                FeatureRow(
                    icon = Icons.Default.ZoomIn,
                    title = "İki Parmakla Yakınlaştırma (Pinch-to-Zoom)",
                    desc = "Küçük metinleri iki parmağınızla 4 kata kadar büyüterek rahatça okuyabilirsiniz."
                )
                Spacer(modifier = Modifier.height(10.dp))
                FeatureRow(
                    icon = Icons.Default.Info,
                    title = "Akıllı Hibrit Motor",
                    desc = "İnternet varken yapay zeka bulut motoru, internetsizken on-device ML Kit devrededir."
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Başlat / Durdur Butonu
        if (!isServiceRunning) {
            Button(
                onClick = onStartService,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF2979FF)
                )
            ) {
                Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Çevirici Baloncuğu Başlat",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        } else {
            Button(
                onClick = onStopService,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFD32F2F)
                )
            ) {
                Icon(imageVector = Icons.Default.Stop, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Servisi Durdur",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
fun PermissionItem(
    title: String,
    desc: String,
    isGranted: Boolean,
    onGrant: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Icon(
            imageVector = if (isGranted) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
            contentDescription = null,
            tint = if (isGranted) Color(0xFF4CAF50) else Color(0xFFE53935),
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White
            )
            Text(
                text = desc,
                fontSize = 11.sp,
                color = Color(0xFF78909C)
            )
        }
        if (!isGranted) {
            Spacer(modifier = Modifier.width(8.dp))
            OutlinedButton(
                onClick = onGrant,
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(text = "İzin Ver", fontSize = 12.sp)
            }
        }
    }
}

@Composable
fun FeatureRow(icon: ImageVector, title: String, desc: String) {
    Row(verticalAlignment = Alignment.Top) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color(0xFF2979FF),
            modifier = Modifier
                .size(20.dp)
                .padding(top = 2.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Column {
            Text(
                text = title,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFFECEFF1)
            )
            Text(
                text = desc,
                fontSize = 11.sp,
                color = Color(0xFF78909C)
            )
        }
    }
}
