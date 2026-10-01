package com.roadwise.adas

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.ImageProxy
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
import androidx.compose.foundation.layout.widthIn
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
    private var dashcamState by mutableStateOf(DashcamUiState())
    private var musicState by mutableStateOf(MusicUiState())
    private var routeStatus by mutableStateOf("Route guidance opens in your maps app.")
    private var cameraPermissionGranted by mutableStateOf(false)
    private var locationPermissionGranted by mutableStateOf(false)
    private var pendingStart = false

    private lateinit var driveController: DriveAssistController
    private lateinit var dashcamController: DashcamCameraController
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
        dashcamController = DashcamCameraController(this) { next -> dashcamState = next }
        driveController = DriveAssistController(
            context = this,
            onStateChanged = { next -> driveState = next },
            onSafetyEvent = dashcamController::captureSafetyEvent,
        )

        setContent {
            RoadwiseApp(
                state = driveState,
                dashcam = dashcamState,
                music = musicState,
                routeStatus = routeStatus,
                cameraPermissionGranted = cameraPermissionGranted,
                locationPermissionGranted = locationPermissionGranted,
                onStartMonitoring = ::requestStart,
                onStopMonitoring = ::stopMonitoring,
                onChooseAudio = { audioPicker.launch(arrayOf("audio/*")) },
                onToggleAudio = ::toggleAudio,
                onToggleEventCapture = { dashcamController.setEventRecordingArmed(!dashcamState.isArmed) },
                onToggleManualRecording = dashcamController::toggleManualRecording,
                onOpenRoute = ::openRoute,
                onAnalyzeFrame = driveController::analyzeFrame,
                cameraController = dashcamController,
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
        if (::dashcamController.isInitialized) dashcamController.stopForBackground()
        exoPlayer?.pause()
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        super.onStop()
    }

    override fun onDestroy() {
        if (::dashcamController.isInitialized) dashcamController.release()
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
        dashcamController.stopForBackground()
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    private fun openRoute(destination: String) {
        val query = destination.trim()
        if (query.isBlank()) {
            routeStatus = "Enter a destination first."
            return
        }
        if (driveState.isRunning) {
            routeStatus = "Stop monitoring before handing off to navigation."
            return
        }

        val navigationIntent = Intent(
            Intent.ACTION_VIEW,
            Uri.parse("google.navigation:q=${Uri.encode(query)}&mode=d"),
        ).setPackage("com.google.android.apps.maps")
        try {
            startActivity(navigationIntent)
            routeStatus = "Route opened in Google Maps. Roadwise remains paused during navigation."
        } catch (_: Exception) {
            try {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=${Uri.encode(query)}")))
                routeStatus = "Route opened in your maps app. Roadwise remains paused during navigation."
            } catch (_: Exception) {
                routeStatus = "No map app found. Install a maps app, then try again."
            }
        }
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
    dashcam: DashcamUiState,
    music: MusicUiState,
    routeStatus: String,
    cameraPermissionGranted: Boolean,
    locationPermissionGranted: Boolean,
    onStartMonitoring: () -> Unit,
    onStopMonitoring: () -> Unit,
    onChooseAudio: () -> Unit,
    onToggleAudio: () -> Unit,
    onToggleEventCapture: () -> Unit,
    onToggleManualRecording: () -> Unit,
    onOpenRoute: (String) -> Unit,
    onAnalyzeFrame: (ImageProxy) -> Unit,
    cameraController: DashcamCameraController,
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
                MonitoringButton(
                    isRunning = state.isRunning,
                    hasCameraPermission = cameraPermissionGranted,
                    hasLocationPermission = locationPermissionGranted,
                    onClick = if (state.isRunning) onStopMonitoring else onStartMonitoring,
                )
                CameraPanel(
                    state = state,
                    dashcam = dashcam,
                    cameraPermissionGranted = cameraPermissionGranted,
                    onAnalyzeFrame = onAnalyzeFrame,
                    cameraController = cameraController,
                    onToggleEventCapture = onToggleEventCapture,
                    onToggleManualRecording = onToggleManualRecording,
                )
                SpeedPanel(state = state)
                SignPanel(state = state)
                NavigationPanel(
                    isMonitoring = state.isRunning,
                    routeStatus = routeStatus,
                    onOpenRoute = onOpenRoute,
                )
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
    dashcam: DashcamUiState,
    cameraPermissionGranted: Boolean,
    onAnalyzeFrame: (ImageProxy) -> Unit,
    cameraController: DashcamCameraController,
    onToggleEventCapture: () -> Unit,
    onToggleManualRecording: () -> Unit,
) {
    Panel(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = CircleShape, color = Accent.copy(alpha = 0.12f)) {
                Box(Modifier.size(34.dp), contentAlignment = Alignment.Center) {
                    Icon(Icons.Filled.CameraAlt, contentDescription = null, tint = Accent, modifier = Modifier.size(17.dp))
                }
            }
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("ROAD CAMERA", color = Foreground, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                Text("Live sign reader · local dashcam", color = Muted, fontSize = 10.sp)
            }
            if (state.isRunning && dashcam.isRecording) {
                CameraBadge(text = "REC ${formatClock(dashcam.recordingSeconds)}", tint = Danger)
            }
        }

        Spacer(Modifier.height(12.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(246.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(Color(0xFF071016)),
        ) {
            if (state.isRunning && cameraPermissionGranted) {
                CameraLiveView(
                    cameraController = cameraController,
                    onAnalyzeFrame = onAnalyzeFrame,
                    modifier = Modifier.fillMaxSize(),
                )
                Row(
                    modifier = Modifier.fillMaxWidth().align(Alignment.TopStart).padding(11.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CameraBadge(text = "LIVE", tint = Accent)
                    Spacer(Modifier.weight(1f))
                    CameraBadge(
                        text = if (dashcam.isArmed) "EVENTS ARMED" else "EVENTS OFF",
                        tint = if (dashcam.isArmed) Amber else Muted,
                    )
                }
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth()
                        .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.88f))))
                        .padding(horizontal = 12.dp, vertical = 11.dp),
                    verticalAlignment = Alignment.Bottom,
                ) {
                    SpeedometerOverlay(speedKmh = state.speedKmh)
                    Spacer(Modifier.weight(1f))
                    Column(horizontalAlignment = Alignment.End, modifier = Modifier.widthIn(max = 150.dp)) {
                        Text(
                            text = state.ocrStatus,
                            color = Foreground,
                            fontSize = 9.sp,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.End,
                        )
                        Spacer(Modifier.height(5.dp))
                        CameraBadge(text = "GPS · ${state.gpsStatus.take(24)}", tint = Accent)
                    }
                }
            } else {
                Column(
                    modifier = Modifier.align(Alignment.Center).padding(horizontal = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(Icons.Filled.CameraAlt, contentDescription = null, tint = Accent.copy(alpha = 0.75f), modifier = Modifier.size(29.dp))
                    Text(
                        text = if (cameraPermissionGranted) "Dashcam is paused" else "Camera permission needed",
                        color = Foreground,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = if (cameraPermissionGranted) "Start drive assist to open the road camera." else "Allow camera access to use sign reading and dashcam features.",
                        color = Muted,
                        fontSize = 10.sp,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }

        Spacer(Modifier.height(9.dp))
        Text(
            dashcam.status,
            color = when {
                dashcam.isRecording -> Danger
                dashcam.isArmed -> Amber
                else -> Muted
            },
            fontSize = 10.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(3.dp))
        Text(dashcam.cameraStatus, color = if (state.isRunning) Accent else Muted, fontSize = 9.sp)
        if (dashcam.lastEvent != null) {
            Spacer(Modifier.height(3.dp))
            Text("Last event · ${dashcam.lastEvent}", color = Amber, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Spacer(Modifier.height(11.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(9.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(
                onClick = onToggleEventCapture,
                enabled = state.isRunning && cameraPermissionGranted && dashcam.cameraStatus.startsWith("Live"),
                modifier = Modifier.weight(1f).height(46.dp),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, if (dashcam.isArmed) Amber else PanelBorder),
            ) {
                Text(
                    if (dashcam.isArmed) "Events armed" else "Arm event clips",
                    color = if (dashcam.isArmed) Amber else Muted,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                )
            }
            Button(
                onClick = onToggleManualRecording,
                enabled = state.isRunning && cameraPermissionGranted && dashcam.cameraStatus.startsWith("Live"),
                modifier = Modifier.weight(1f).height(46.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (dashcam.isRecording) Color(0xFF512628) else Color(0xFF25353D),
                    contentColor = if (dashcam.isRecording) Color(0xFFFFA19B) else Foreground,
                ),
            ) {
                Text(
                    if (dashcam.isManualRecording) "Stop recording" else "Record now",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = when {
                !state.isRunning -> "Start monitoring to enable dashcam controls."
                dashcam.isArmed -> "Sudden movement or rapid GPS slowdown saves a 20-second local clip."
                else -> "Event recording is opt-in. Clips begin after a trigger; no pre-event footage is buffered."
            },
            color = Muted,
            fontSize = 9.sp,
            lineHeight = 13.sp,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            "CameraX requests up to 120 FPS where supported; video may use a lower device-supported rate. OCR samples ~4 FPS. The GPS speed overlay is live-only, not burned into the silent video.",
            color = Muted.copy(alpha = 0.82f),
            fontSize = 9.sp,
            lineHeight = 13.sp,
        )
    }
}

@Composable
private fun SpeedometerOverlay(speedKmh: Float?) {
    Surface(
        color = Color(0xE607131B),
        shape = RoundedCornerShape(15.dp),
        border = BorderStroke(1.dp, Accent.copy(alpha = 0.35f)),
    ) {
        Column(Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
            Text("GPS SPEED", color = Muted, fontSize = 8.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp)
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    speedKmh?.toInt()?.toString() ?: "—",
                    color = if (speedKmh != null && speedKmh > 80f) Danger else Foreground,
                    fontSize = 27.sp,
                    lineHeight = 28.sp,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.width(5.dp))
                Text("km/h", color = Muted, fontSize = 10.sp, modifier = Modifier.padding(bottom = 3.dp))
            }
        }
    }
}

private fun formatClock(seconds: Int): String {
    val minutes = seconds / 60
    val remainder = seconds % 60
    return "%02d:%02d".format(Locale.US, minutes, remainder)
}

@Composable
private fun CameraBadge(text: String, tint: Color) {
    Surface(
        shape = RoundedCornerShape(50),
        color = Color(0xCC07131B),
        border = BorderStroke(1.dp, tint.copy(alpha = 0.34f)),
    ) {
        Text(
            text,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
            color = tint,
            fontSize = 8.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.7.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun CameraLiveView(
    cameraController: DashcamCameraController,
    onAnalyzeFrame: (ImageProxy) -> Unit,
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

    androidx.compose.runtime.DisposableEffect(cameraController, lifecycleOwner, previewView) {
        cameraController.bindCamera(lifecycleOwner, previewView, onAnalyzeFrame)
        onDispose { cameraController.unbindCamera() }
    }

    AndroidView(factory = { previewView }, modifier = modifier)
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
