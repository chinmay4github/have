package com.roadwise.adas

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.util.Log
import android.util.Range
import android.util.Size
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.camera2.interop.Camera2CameraInfo
import androidx.camera.camera2.interop.ExperimentalCamera2Interop
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import java.util.concurrent.Executors

private val Background = Color(0xFF08131B)
private val PanelColor = Color(0xFF101F29)
private val PanelBorder = Color(0xFF223640)
private val Accent = Color(0xFF73E2BE)
private val Foreground = Color(0xFFEAF3F5)
private val Muted = Color(0xFF98AAB4)
private val Danger = Color(0xFFFF766E)
private val Amber = Color(0xFFFFC66D)

private data class MusicUiState(
    val title: String = "No audio selected",
    val hasTrack: Boolean = false,
    val isPlaying: Boolean = false,
    val status: String = "Choose an audio file while parked.",
)

class MainActivity : ComponentActivity() {
    private var driveState by mutableStateOf(DriveUiState())
    private var musicState by mutableStateOf(MusicUiState())
    private var cameraPermissionGranted by mutableStateOf(false)
    private var locationPermissionGranted by mutableStateOf(false)
    private var pendingStart = false

    private lateinit var driveController: DriveAssistController
    private var exoPlayer: ExoPlayer? = null

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) {
        refreshPermissions()
        if (pendingStart) {
            pendingStart = false
            if (cameraPermissionGranted) {
                beginMonitoring()
            } else if (::driveController.isInitialized) {
                driveController.updateCameraStatus("Camera permission is required to start")
            }
        }
    }

    private val audioPicker = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null && !driveState.isRunning) loadAudio(uri)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = android.graphics.Color.rgb(8, 19, 27)
        window.navigationBarColor = android.graphics.Color.rgb(8, 19, 27)
        refreshPermissions()
        driveController = DriveAssistController(this) { next -> driveState = next }

        setContent {
            RoadwiseApp(
                state = driveState,
                music = musicState,
                cameraPermissionGranted = cameraPermissionGranted,
                locationPermissionGranted = locationPermissionGranted,
                onStartMonitoring = ::requestStart,
                onStopMonitoring = ::stopMonitoring,
                onChooseAudio = { audioPicker.launch(arrayOf("audio/*")) },
                onToggleAudio = ::toggleAudio,
                onAnalyzeFrame = driveController::analyzeFrame,
                onCameraStatus = driveController::updateCameraStatus,
            )
        }
    }

    override fun onResume() {
        super.onResume()
        refreshPermissions()
    }

    override fun onStop() {
        if (::driveController.isInitialized && driveState.isRunning) {
            driveController.stopMonitoring()
        }
        exoPlayer?.pause()
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        super.onStop()
    }

    override fun onDestroy() {
        if (::driveController.isInitialized) driveController.release()
        exoPlayer?.release()
        exoPlayer = null
        super.onDestroy()
    }

    private fun refreshPermissions() {
        cameraPermissionGranted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.CAMERA,
        ) == PackageManager.PERMISSION_GRANTED
        locationPermissionGranted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION,
        ) == PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_COARSE_LOCATION,
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestStart() {
        refreshPermissions()
        val missing = buildList {
            if (!cameraPermissionGranted) add(Manifest.permission.CAMERA)
            if (!locationPermissionGranted) {
                add(Manifest.permission.ACCESS_FINE_LOCATION)
                add(Manifest.permission.ACCESS_COARSE_LOCATION)
            }
        }
        if (missing.isNotEmpty()) {
            pendingStart = true
            permissionLauncher.launch(missing.toTypedArray())
        } else {
            beginMonitoring()
        }
    }

    private fun beginMonitoring() {
        if (!cameraPermissionGranted) return
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        driveController.startMonitoring()
    }

    private fun stopMonitoring() {
        driveController.stopMonitoring()
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    private fun loadAudio(uri: Uri) {
        try {
            contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        } catch (_: SecurityException) {
            // The provider may return a temporary-only URI; playback can still work this session.
        }
        val title = queryDisplayName(uri) ?: uri.lastPathSegment?.substringAfterLast('/') ?: "Selected audio"
        val player = player()
        player.setMediaItem(MediaItem.fromUri(uri))
        player.prepare()
        player.play()
        musicState = MusicUiState(
            title = title,
            hasTrack = true,
            isPlaying = true,
            status = "Audio focus enabled · spoken alerts take priority.",
        )
    }

    private fun queryDisplayName(uri: Uri): String? {
        return try {
            contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) cursor.getString(0) else null
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun player(): ExoPlayer {
        exoPlayer?.let { return it }
        return ExoPlayer.Builder(this).build().also { created ->
            created.setAudioAttributes(
                androidx.media3.common.AudioAttributes.Builder()
                    .setUsage(androidx.media3.common.C.USAGE_MEDIA)
                    .setContentType(androidx.media3.common.C.AUDIO_CONTENT_TYPE_MUSIC)
                    .build(),
                true,
            )
            created.addListener(object : Player.Listener {
                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    musicState = musicState.copy(
                        isPlaying = isPlaying,
                        status = if (isPlaying) {
                            "Audio focus enabled · spoken alerts take priority."
                        } else if (musicState.hasTrack) {
                            "Paused · spoken alerts remain available."
                        } else {
                            musicState.status
                        },
                    )
                }

                override fun onPlayerError(error: PlaybackException) {
                    musicState = musicState.copy(isPlaying = false, status = "This audio file could not be played.")
                }
            })
            exoPlayer = created
        }
    }

    private fun toggleAudio() {
        val player = exoPlayer ?: return
        if (player.isPlaying) player.pause() else player.play()
    }
}

@Composable
private fun RoadwiseApp(
    state: DriveUiState,
    music: MusicUiState,
    cameraPermissionGranted: Boolean,
    locationPermissionGranted: Boolean,
    onStartMonitoring: () -> Unit,
    onStopMonitoring: () -> Unit,
    onChooseAudio: () -> Unit,
    onToggleAudio: () -> Unit,
    onAnalyzeFrame: (ImageProxy) -> Unit,
    onCameraStatus: (String) -> Unit,
) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Accent,
            onPrimary = Background,
            background = Background,
            surface = PanelColor,
            onSurface = Foreground,
            error = Danger,
        ),
    ) {
        Surface(modifier = Modifier.fillMaxSize(), color = Background) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 18.dp)
                    .padding(top = 14.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(15.dp),
            ) {
                AppHeader(isRunning = state.isRunning)
                SpeedPanel(state = state)
                MonitoringButton(
                    isRunning = state.isRunning,
                    hasCameraPermission = cameraPermissionGranted,
                    hasLocationPermission = locationPermissionGranted,
                    onClick = if (state.isRunning) onStopMonitoring else onStartMonitoring,
                )
                CameraPanel(
                    state = state,
                    cameraPermissionGranted = cameraPermissionGranted,
                    onAnalyzeFrame = onAnalyzeFrame,
                    onCameraStatus = onCameraStatus,
                )
                SignPanel(state = state)
                AudioPanel(
                    music = music,
                    isMonitoring = state.isRunning,
                    onChooseAudio = onChooseAudio,
                    onToggleAudio = onToggleAudio,
                )
                SafetyNote()
                Text(
                    text = "ROADWISE · DRIVER ASSIST PROTOTYPE",
                    modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
                    color = Muted.copy(alpha = 0.7f),
                    fontSize = 10.sp,
                    letterSpacing = 1.5.sp,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Composable
private fun AppHeader(isRunning: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Surface(shape = RoundedCornerShape(15.dp), color = Accent.copy(alpha = 0.13f)) {
            Box(modifier = Modifier.size(48.dp), contentAlignment = Alignment.Center) {
                Text("R", color = Accent, fontSize = 25.sp, fontWeight = FontWeight.Black)
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                "ROADWISE",
                color = Accent,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.4.sp,
            )
            Text("Drive assist", color = Foreground, fontSize = 21.sp, fontWeight = FontWeight.SemiBold)
        }
        StatusPill(text = if (isRunning) "ACTIVE" else "STANDBY", active = isRunning)
    }
}

@Composable
private fun StatusPill(text: String, active: Boolean) {
    val tint = if (active) Accent else Muted
    Surface(
        shape = RoundedCornerShape(50),
        color = tint.copy(alpha = 0.11f),
        border = BorderStroke(1.dp, tint.copy(alpha = 0.24f)),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.size(6.dp).clip(CircleShape).background(tint))
            Spacer(Modifier.width(6.dp))
            Text(text, color = tint, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
        }
    }
}

@Composable
private fun Panel(
    modifier: Modifier = Modifier,
    color: Color = PanelColor,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(23.dp),
        color = color,
        border = BorderStroke(1.dp, PanelBorder),
    ) {
        Column(Modifier.padding(17.dp), content = content)
    }
}

@Composable
private fun SpeedPanel(state: DriveUiState) {
    val danger = state.isOverSpeed
    Panel(
        modifier = Modifier.fillMaxWidth(),
        color = if (danger) Color(0xFF241A1D) else PanelColor,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.weight(1f)) {
                Text("CURRENT SPEED", color = Muted, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.7.sp)
                Spacer(Modifier.height(5.dp))
                Text(
                    state.gpsStatus,
                    color = if (danger) Danger else Muted,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Icon(Icons.Filled.LocationOn, contentDescription = "GPS status", tint = if (danger) Danger else Accent, modifier = Modifier.size(20.dp))
        }

        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = state.speedKmh?.toInt()?.toString() ?: "—",
                color = if (danger) Danger else Foreground,
                fontSize = 66.sp,
                lineHeight = 68.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.SansSerif,
            )
            Spacer(Modifier.width(10.dp))
            Text("km/h", modifier = Modifier.padding(bottom = 10.dp), color = Muted, fontSize = 15.sp, fontWeight = FontWeight.Medium)
            Spacer(Modifier.weight(1f))
            Column(horizontalAlignment = Alignment.End, modifier = Modifier.padding(bottom = 9.dp)) {
                Text("VOICE ALERT", color = Muted, fontSize = 9.sp, letterSpacing = 1.2.sp)
                Text("> 80 km/h", color = if (danger) Danger else Accent, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
        }

        val progress = ((state.speedKmh ?: 0f) / 120f).coerceIn(0f, 1f)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(7.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0xFF263842)),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(progress)
                    .background(if (danger) Danger else Accent),
            )
        }
        Spacer(Modifier.height(7.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("0", color = Muted, fontSize = 10.sp)
            Text("80 km/h alert point", color = Muted, fontSize = 10.sp)
            Text("120", color = Muted, fontSize = 10.sp)
        }

        if (danger) {
            Spacer(Modifier.height(12.dp))
            Surface(color = Danger.copy(alpha = 0.13f), shape = RoundedCornerShape(13.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Filled.Warning, contentDescription = null, tint = Danger, modifier = Modifier.size(17.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Above 80 km/h · please drive carefully", color = Danger, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun MonitoringButton(
    isRunning: Boolean,
    hasCameraPermission: Boolean,
    hasLocationPermission: Boolean,
    onClick: () -> Unit,
) {
    Column {
        Button(
            onClick = onClick,
            modifier = Modifier.fillMaxWidth().height(55.dp),
            shape = RoundedCornerShape(17.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isRunning) Color(0xFF26343B) else Accent,
                contentColor = if (isRunning) Foreground else Background,
            ),
        ) {
            Icon(
                if (isRunning) Icons.Filled.Stop else Icons.Filled.PlayArrow,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                when {
                    isRunning -> "Stop monitoring"
                    !hasCameraPermission -> "Allow access & start"
                    else -> "Start drive assist"
                },
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = when {
                isRunning -> "Monitoring stops automatically when this screen leaves the foreground."
                !hasCameraPermission -> "Camera is required · location enables the 80 km/h alert."
                !hasLocationPermission -> "Camera sign reading can run; allow location for speed alerts."
                else -> "Camera and GPS are used only while monitoring is active."
            },
            modifier = Modifier.fillMaxWidth(),
            color = Muted,
            fontSize = 10.sp,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun CameraPanel(
    state: DriveUiState,
    cameraPermissionGranted: Boolean,
    onAnalyzeFrame: (ImageProxy) -> Unit,
    onCameraStatus: (String) -> Unit,
) {
    Panel(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = CircleShape, color = Accent.copy(alpha = 0.12f)) {
                Box(Modifier.size(34.dp), contentAlignment = Alignment.Center) {
                    Icon(Icons.Filled.CameraAlt, contentDescription = null, tint = Accent, modifier = Modifier.size(17.dp))
                }
            }
            Spacer(Modifier.width(10.dp))
            Column {
                Text("CAMERA SIGN READER", color = Foreground, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.8.sp)
                Text("Rear camera · on-device text recognition", color = Muted, fontSize = 10.sp)
            }
        }

        Spacer(Modifier.height(13.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(214.dp)
                .clip(RoundedCornerShape(17.dp))
                .background(Color(0xFF071016)),
        ) {
            if (state.isRunning && cameraPermissionGranted) {
                CameraLiveView(
                    onAnalyzeFrame = onAnalyzeFrame,
                    onCameraStatus = onCameraStatus,
                    modifier = Modifier.fillMaxSize(),
                )
                Column(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(11.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    CameraBadge(text = "LIVE", tint = Accent)
                    CameraBadge(text = "LOCAL OCR", tint = Foreground)
                }
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth()
                        .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.82f))))
                        .padding(horizontal = 12.dp, vertical = 11.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(Modifier.size(6.dp).clip(CircleShape).background(Accent))
                    Spacer(Modifier.width(7.dp))
                    Text(
                        text = state.ocrStatus,
                        color = Foreground,
                        fontSize = 10.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            } else {
                Column(
                    modifier = Modifier.align(Alignment.Center).padding(horizontal = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(Icons.Filled.CameraAlt, contentDescription = null, tint = Accent.copy(alpha = 0.75f), modifier = Modifier.size(28.dp))
                    Text(
                        text = when {
                            !cameraPermissionGranted -> "Camera permission needed"
                            else -> "Camera paused"
                        },
                        color = Foreground,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = if (cameraPermissionGranted) "Start monitoring to turn on the rear camera." else "Allow camera access when you are ready to begin.",
                        color = Muted,
                        fontSize = 10.sp,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        Text(
            "CameraX requests up to 120 FPS when supported; OCR samples about 4 frames/sec. Hardware and camera-session limits can reduce the frame rate.",
            color = Muted,
            fontSize = 10.sp,
            lineHeight = 14.sp,
        )
        if (state.isRunning) {
            Spacer(Modifier.height(5.dp))
            Text(state.cameraStatus, color = Accent.copy(alpha = 0.9f), fontSize = 10.sp)
        }
    }
}

@Composable
private fun CameraBadge(text: String, tint: Color) {
    Surface(
        shape = RoundedCornerShape(50),
        color = Color(0xCC07131B),
        border = BorderStroke(1.dp, tint.copy(alpha = 0.34f)),
    ) {
        Text(text, modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp), color = tint, fontSize = 8.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
    }
}

@Composable
private fun CameraLiveView(
    onAnalyzeFrame: (ImageProxy) -> Unit,
    onCameraStatus: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val previewView = remember(context) {
        PreviewView(context).apply {
            scaleType = PreviewView.ScaleType.FILL_CENTER
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
        }
    }

    androidx.compose.runtime.DisposableEffect(lifecycleOwner, previewView) {
        var disposed = false
        var boundProvider: ProcessCameraProvider? = null
        var boundAnalysis: ImageAnalysis? = null
        val analysisExecutor = Executors.newSingleThreadExecutor()
        val mainExecutor = ContextCompat.getMainExecutor(context)
        onCameraStatus("Opening CameraX rear camera…")

        val providerFuture = ProcessCameraProvider.getInstance(context)
        providerFuture.addListener({
            if (disposed) return@addListener
            try {
                val provider = providerFuture.get()
                boundProvider = provider
                val cameraInfo = CameraSelector.DEFAULT_BACK_CAMERA
                    .filter(provider.availableCameraInfos)
                    .firstOrNull()
                val rateChoices = supportedFrameRates(context, cameraInfo)
                val attempts = mutableListOf<Range<Int>?>().apply {
                    addAll(rateChoices)
                    add(null)
                }
                var connected = false

                for (requestedRate in attempts) {
                    if (disposed) break
                    var analysis: ImageAnalysis? = null
                    try {
                        val previewBuilder = Preview.Builder()
                        val analysisBuilder = ImageAnalysis.Builder()
                            .setTargetResolution(Size(1280, 720))
                            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        if (requestedRate != null) {
                            previewBuilder.setTargetFrameRate(requestedRate)
                            analysisBuilder.setTargetFrameRate(requestedRate)
                        }
                        val preview = previewBuilder.build()
                        val builtAnalysis = analysisBuilder.build()
                        analysis = builtAnalysis
                        builtAnalysis.setAnalyzer(analysisExecutor) { imageProxy -> onAnalyzeFrame(imageProxy) }
                        preview.setSurfaceProvider(previewView.surfaceProvider)

                        provider.unbindAll()
                        provider.bindToLifecycle(
                            lifecycleOwner,
                            CameraSelector.DEFAULT_BACK_CAMERA,
                            preview,
                            builtAnalysis,
                        )
                        boundAnalysis = builtAnalysis
                        val rateLabel = when {
                            requestedRate == null -> "Live · device-default FPS · OCR samples ~4 FPS"
                            requestedRate.upper >= 120 -> "Live · 120 FPS requested · OCR samples ~4 FPS"
                            else -> "Live · up to ${requestedRate.upper} FPS requested · OCR samples ~4 FPS"
                        }
                        onCameraStatus(rateLabel)
                        connected = true
                        break
                    } catch (exception: Exception) {
                        analysis?.clearAnalyzer()
                        provider.unbindAll()
                        Log.w("RoadwiseADAS", "CameraX could not bind at requested frame range $requestedRate", exception)
                    }
                }
                if (!connected && !disposed) {
                    onCameraStatus("Camera could not start · close other camera apps and retry")
                }
            } catch (exception: Exception) {
                Log.e("RoadwiseADAS", "Unable to initialize CameraX", exception)
                if (!disposed) onCameraStatus("Camera unavailable · check camera permission")
            }
        }, mainExecutor)

        onDispose {
            disposed = true
            boundAnalysis?.clearAnalyzer()
            boundProvider?.unbindAll()
            analysisExecutor.shutdownNow()
            onCameraStatus("Camera paused")
        }
    }

    AndroidView(factory = { previewView }, modifier = modifier)
}

@OptIn(ExperimentalCamera2Interop::class)
private fun supportedFrameRates(
    context: Context,
    cameraInfo: androidx.camera.core.CameraInfo?,
): List<Range<Int>> {
    if (cameraInfo == null) return emptyList()
    return try {
        val cameraId = Camera2CameraInfo.from(cameraInfo).cameraId
        val manager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        val ranges = manager.getCameraCharacteristics(cameraId)
            .get(CameraCharacteristics.CONTROL_AE_AVAILABLE_TARGET_FPS_RANGES)
            ?.toList()
            .orEmpty()
        val atOrBelow120 = ranges
            .filter { it.upper <= 120 }
            .sortedWith(compareByDescending<Range<Int>> { it.upper }.thenByDescending { it.lower })
        atOrBelow120.distinctBy { it.lower to it.upper }
    } catch (exception: Exception) {
        Log.w("RoadwiseADAS", "Unable to read camera FPS capabilities", exception)
        emptyList()
    }
}

@Composable
private fun SignPanel(state: DriveUiState) {
    Panel(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.Top) {
            Surface(shape = CircleShape, color = Amber.copy(alpha = 0.12f)) {
                Box(Modifier.size(36.dp), contentAlignment = Alignment.Center) {
                    Text("↗", color = Amber, fontSize = 19.sp, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(Modifier.width(11.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("LAST SIGN READ", color = Muted, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
                Spacer(Modifier.height(5.dp))
                Text(
                    state.lastSignTitle,
                    color = Foreground,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(4.dp))
                Text(state.lastSignDetail, color = Muted, fontSize = 10.sp, lineHeight = 14.sp)
            }
        }
        Spacer(Modifier.height(12.dp))
        Text(
            if (state.isRunning) state.ocrStatus else "Sign reading is off until monitoring starts.",
            color = Accent.copy(alpha = 0.9f),
            fontSize = 10.sp,
        )
    }
}

@Composable
private fun AudioPanel(
    music: MusicUiState,
    isMonitoring: Boolean,
    onChooseAudio: () -> Unit,
    onToggleAudio: () -> Unit,
) {
    Panel(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = CircleShape, color = Accent.copy(alpha = 0.12f)) {
                Box(Modifier.size(36.dp), contentAlignment = Alignment.Center) {
                    Icon(Icons.Filled.MusicNote, contentDescription = null, tint = Accent, modifier = Modifier.size(19.dp))
                }
            }
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("CABIN AUDIO", color = Foreground, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.8.sp)
                Text("Local music player", color = Muted, fontSize = 10.sp)
            }
            IconButton(onClick = onToggleAudio, enabled = music.hasTrack) {
                Surface(
                    shape = CircleShape,
                    color = if (music.hasTrack) Accent else Color(0xFF25353D),
                ) {
                    Box(Modifier.size(42.dp), contentAlignment = Alignment.Center) {
                        Icon(
                            if (music.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                            contentDescription = if (music.isPlaying) "Pause music" else "Play music",
                            tint = if (music.hasTrack) Background else Muted,
                            modifier = Modifier.size(23.dp),
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(9.dp))
        Text(
            music.title,
            color = Foreground,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(4.dp))
        Text(music.status, color = Muted, fontSize = 10.sp)
        Spacer(Modifier.height(12.dp))
        OutlinedButton(
            onClick = onChooseAudio,
            enabled = !isMonitoring,
            modifier = Modifier.fillMaxWidth().height(44.dp),
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, if (isMonitoring) PanelBorder else Accent.copy(alpha = 0.55f)),
        ) {
            Text(
                if (isMonitoring) "Select audio while parked" else if (music.hasTrack) "Change audio" else "Choose audio",
                color = if (isMonitoring) Muted else Accent,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
private fun SafetyNote() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = Color(0xFF1B1C18),
        border = BorderStroke(1.dp, Amber.copy(alpha = 0.23f)),
    ) {
        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.Top) {
            Icon(Icons.Filled.Warning, contentDescription = null, tint = Amber, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(9.dp))
            Column {
                Text("DRIVER AID ONLY · NOT CERTIFIED ADAS", color = Amber, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
                Spacer(Modifier.height(5.dp))
                Text(
                    "OCR can miss or misread signs and GPS speed can be wrong. Mount the phone before driving; never handle it while moving. Keep watching the road and obey posted signs.",
                    color = Color(0xFFC5C8BB),
                    fontSize = 10.sp,
                    lineHeight = 15.sp,
                )
            }
        }
    }
}
