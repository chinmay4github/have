package com.roadwise.adas

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.media.AudioAttributes
import android.os.Bundle
import android.os.Looper
import android.os.SystemClock
import android.speech.tts.TextToSpeech
import android.util.Log
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import androidx.camera.core.ImageProxy
import java.util.Locale
import java.util.concurrent.atomic.AtomicLong
import kotlin.math.max
import kotlin.math.sqrt

private const val SPEED_ALERT_KMH = 80f
private const val SPEED_REMINDER_INTERVAL_MS = 20_000L
private const val OCR_SAMPLE_INTERVAL_MS = 250L

data class DriveUiState(
    val isRunning: Boolean = false,
    val speedKmh: Float? = null,
    val isOverSpeed: Boolean = false,
    val gpsStatus: String = "GPS inactive",
    val cameraStatus: String = "Camera paused",
    val ocrStatus: String = "OCR paused",
    val lastSignTitle: String = "No sign read yet",
    val lastSignDetail: String = "Start monitoring to read road-sign text.",
)

/** Foreground-only controller for GPS speed alerts, CameraX OCR, and spoken cues. */
class DriveAssistController(
    context: Context,
    private val onStateChanged: (DriveUiState) -> Unit,
    private val onSafetyEvent: (String) -> Unit,
) : LocationListener, SensorEventListener {
    private val appContext = context.applicationContext
    private val mainHandler = android.os.Handler(Looper.getMainLooper())
    private val locationManager = appContext.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    private val sensorManager = appContext.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val gravityEstimate = FloatArray(3)
    private var gravityInitialized = false
    private var lastSafetyEventAt = 0L
    private val textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    private val stateLock = Any()
    private var uiState = DriveUiState()
    private val lastAnalysisAt = AtomicLong(0L)
    @Volatile private var lastValidLocationAt = 0L
    @Volatile private var filteredSpeedKmh: Float? = null
    @Volatile private var wasOverSpeed = false
    @Volatile private var lastSpeedWarningAt = 0L
    @Volatile private var lastSpokenSignKey = ""
    @Volatile private var lastSpokenSignAt = 0L
    @Volatile private var ttsReady = false
    @Volatile private var pendingSpeech: String? = null

    private val clearStaleSpeed = Runnable {
        if (SystemClock.elapsedRealtime() - lastValidLocationAt >= 6_000L) {
            filteredSpeedKmh = null
            wasOverSpeed = false
            updateState {
                it.copy(
                    speedKmh = null,
                    isOverSpeed = false,
                    gpsStatus = "GPS signal lost · speed unavailable",
                )
            }
        }
    }

    private val textToSpeech = TextToSpeech(appContext) { status ->
        if (status == TextToSpeech.SUCCESS) {
            try {
                val english = Locale.US
                if (textToSpeech.isLanguageAvailable(english) >= TextToSpeech.LANG_AVAILABLE) {
                    textToSpeech.language = english
                }
                textToSpeech.setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ASSISTANCE_NAVIGATION_GUIDANCE)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build(),
                )
                ttsReady = true
                pendingSpeech?.let { queued ->
                    pendingSpeech = null
                    speak(queued, urgent = false)
                }
            } catch (exception: Exception) {
                Log.w("RoadwiseADAS", "Unable to configure speech output", exception)
            }
        }
    }

    fun startMonitoring() {
        synchronized(stateLock) {
            if (uiState.isRunning) return
        }
        filteredSpeedKmh = null
        wasOverSpeed = false
        lastSpeedWarningAt = 0L
        updateState {
            it.copy(
                isRunning = true,
                gpsStatus = "Acquiring GPS fix…",
                cameraStatus = "Opening rear camera…",
                ocrStatus = "On-device sign reader starting",
            )
        }
        gravityInitialized = false
        lastSafetyEventAt = 0L
        sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)?.let { accelerometer ->
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_GAME, mainHandler)
        }

        val fineGranted = ContextCompat.checkSelfPermission(
            appContext,
            Manifest.permission.ACCESS_FINE_LOCATION,
        ) == PackageManager.PERMISSION_GRANTED
        val coarseGranted = ContextCompat.checkSelfPermission(
            appContext,
            Manifest.permission.ACCESS_COARSE_LOCATION,
        ) == PackageManager.PERMISSION_GRANTED

        if (!fineGranted && !coarseGranted) {
            updateState { it.copy(gpsStatus = "Allow location for GPS speed alerts") }
            return
        }

        try {
            if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                updateState { it.copy(gpsStatus = "Turn on GPS for speed alerts") }
                return
            }
            // Updates are registered only while this foreground screen is actively monitoring.
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1_000L, 0f, this)
            updateState {
                it.copy(
                    gpsStatus = if (fineGranted) "Acquiring precise GPS" else "Approximate location · speed may be limited",
                )
            }
        } catch (securityException: SecurityException) {
            updateState { it.copy(gpsStatus = "Location permission unavailable") }
        } catch (exception: Exception) {
            updateState { it.copy(gpsStatus = "GPS unavailable on this device") }
            Log.w("RoadwiseADAS", "Could not start GPS updates", exception)
        }
    }

    fun stopMonitoring() {
        sensorManager.unregisterListener(this)
        gravityInitialized = false
        try {
            locationManager.removeUpdates(this)
        } catch (_: SecurityException) {
            // Permission may have been revoked while the app was open.
        }
        filteredSpeedKmh = null
        wasOverSpeed = false
        lastSpeedWarningAt = 0L
        lastValidLocationAt = 0L
        mainHandler.removeCallbacks(clearStaleSpeed)
        pendingSpeech = null
        if (ttsReady) {
            try {
                textToSpeech.stop()
            } catch (_: Exception) {
                // Speech can be unavailable on devices without a configured TTS engine.
            }
        }
        updateState {
            it.copy(
                isRunning = false,
                speedKmh = null,
                isOverSpeed = false,
                gpsStatus = "GPS inactive",
                cameraStatus = "Camera paused",
                ocrStatus = "OCR paused",
            )
        }
    }

    fun updateCameraStatus(status: String) {
        updateState { it.copy(cameraStatus = status) }
    }

    fun analyzeFrame(imageProxy: ImageProxy) {
        val now = SystemClock.elapsedRealtime()
        val previous = lastAnalysisAt.get()
        if (now - previous < OCR_SAMPLE_INTERVAL_MS || !lastAnalysisAt.compareAndSet(previous, now)) {
            imageProxy.close()
            return
        }

        val mediaImage = imageProxy.image
        if (mediaImage == null) {
            imageProxy.close()
            return
        }

        try {
            val inputImage = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
            textRecognizer.process(inputImage)
                .addOnSuccessListener { visionText -> onRecognizedText(visionText.text) }
                .addOnFailureListener { exception ->
                    Log.w("RoadwiseADAS", "On-device text recognition failed", exception)
                }
                .addOnCompleteListener {
                    imageProxy.close()
                }
        } catch (exception: Exception) {
            imageProxy.close()
            Log.w("RoadwiseADAS", "Unable to analyze camera frame", exception)
        }
    }

    override fun onLocationChanged(location: Location) {
        val running = synchronized(stateLock) { uiState.isRunning }
        if (!running) return

        if (!location.hasSpeed()) {
            updateState { it.copy(gpsStatus = "Waiting for GPS speed data") }
            return
        }
        if (location.hasAccuracy() && (!location.accuracy.isFinite() || location.accuracy > 35f)) {
            updateState { it.copy(gpsStatus = "Weak GPS accuracy · speed may be unreliable") }
            return
        }
        if (location.hasSpeedAccuracy() && location.speedAccuracyMetersPerSecond > 5f) {
            updateState { it.copy(gpsStatus = "Weak GPS speed fix · use caution") }
            return
        }

        val rawSpeedKmh = max(0f, location.speed * 3.6f)
        if (!rawSpeedKmh.isFinite() || rawSpeedKmh > 250f) return
        val previous = filteredSpeedKmh
        val previousLocationAt = lastValidLocationAt
        // Smooth ordinary GPS jitter while still reacting promptly to sustained speed changes.
        val smoothed = if (previous == null) rawSpeedKmh else previous * 0.55f + rawSpeedKmh * 0.45f
        val now = SystemClock.elapsedRealtime()
        val elapsedSincePrevious = if (previousLocationAt > 0L) now - previousLocationAt else Long.MAX_VALUE
        val rapidSlowdown = previous != null &&
            elapsedSincePrevious in 500L..2_500L &&
            previous - smoothed >= 15f
        filteredSpeedKmh = smoothed
        val overSpeed = smoothed > SPEED_ALERT_KMH
        lastValidLocationAt = now
        mainHandler.removeCallbacks(clearStaleSpeed)
        mainHandler.postDelayed(clearStaleSpeed, 6_000L)
        val shouldWarn = overSpeed && (!wasOverSpeed || now - lastSpeedWarningAt >= SPEED_REMINDER_INTERVAL_MS)
        wasOverSpeed = overSpeed
        if (shouldWarn) lastSpeedWarningAt = now

        updateState {
            it.copy(
                speedKmh = smoothed,
                isOverSpeed = overSpeed,
                gpsStatus = if (location.hasAccuracy()) {
                    "GPS fix · ±${location.accuracy.toInt()} m"
                } else {
                    "GPS speed active"
                },
            )
        }
        if (shouldWarn) {
            speak("Your speed is above 80 kilometers per hour. Please drive carefully.", urgent = true)
        }
        if (rapidSlowdown) reportSafetyEvent("Rapid slowdown")
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type != Sensor.TYPE_ACCELEROMETER) return
        val running = synchronized(stateLock) { uiState.isRunning }
        if (!running) return

        val values = event.values
        if (!gravityInitialized) {
            for (index in 0..2) gravityEstimate[index] = values[index]
            gravityInitialized = true
            return
        }

        val alpha = 0.8f
        val dx = values[0] - gravityEstimate[0]
        val dy = values[1] - gravityEstimate[1]
        val dz = values[2] - gravityEstimate[2]
        for (index in 0..2) gravityEstimate[index] = alpha * gravityEstimate[index] + (1f - alpha) * values[index]
        val linearAcceleration = sqrt(dx * dx + dy * dy + dz * dz)
        if (linearAcceleration >= 5f) reportSafetyEvent("Sudden motion")
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit

    private fun reportSafetyEvent(reason: String) {
        val now = SystemClock.elapsedRealtime()
        if (now - lastSafetyEventAt < 20_000L) return
        lastSafetyEventAt = now
        onSafetyEvent(reason)
    }

    @Deprecated("Deprecated by Android; retained for compatibility with LocationListener.")
    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) = Unit

    override fun onProviderEnabled(provider: String) {
        if (provider == LocationManager.GPS_PROVIDER) {
            updateState { it.copy(gpsStatus = "GPS reacquiring fix…") }
        }
    }

    override fun onProviderDisabled(provider: String) {
        if (provider == LocationManager.GPS_PROVIDER) {
            updateState { it.copy(gpsStatus = "Turn on GPS for speed alerts") }
        }
    }

    fun release() {
        stopMonitoring()
        textRecognizer.close()
        try {
            textToSpeech.stop()
            textToSpeech.shutdown()
        } catch (_: Exception) {
            // Text-to-speech engine may already be shutting down.
        }
    }

    private fun onRecognizedText(text: String) {
        if (text.isBlank() || !synchronized(stateLock) { uiState.isRunning }) return
        val cue = RoadSignInterpreter.interpret(text)
        if (cue == null) {
            val alreadyScanning = synchronized(stateLock) {
                uiState.ocrStatus == "Scanning · no clear road-sign text"
            }
            if (!alreadyScanning) {
                updateState { it.copy(ocrStatus = "Scanning · no clear road-sign text") }
            }
            return
        }

        val now = SystemClock.elapsedRealtime()
        val shouldSpeak = cue.key != lastSpokenSignKey || now - lastSpokenSignAt >= 15_000L
        if (shouldSpeak) {
            lastSpokenSignKey = cue.key
            lastSpokenSignAt = now
            speak(cue.spokenText, urgent = false)
        }
        val cueChanged = synchronized(stateLock) {
            uiState.lastSignTitle != cue.title ||
                uiState.lastSignDetail != cue.detail ||
                uiState.ocrStatus != "On-device OCR · verify sign visually"
        }
        if (cueChanged) {
            updateState {
                it.copy(
                    ocrStatus = "On-device OCR · verify sign visually",
                    lastSignTitle = cue.title,
                    lastSignDetail = cue.detail,
                )
            }
        }
    }

    private fun speak(text: String, urgent: Boolean) {
        if (!ttsReady) {
            pendingSpeech = text
            return
        }
        try {
            textToSpeech.speak(
                text,
                if (urgent) TextToSpeech.QUEUE_FLUSH else TextToSpeech.QUEUE_ADD,
                Bundle(),
                "roadwise-${SystemClock.elapsedRealtime()}",
            )
        } catch (exception: Exception) {
            Log.w("RoadwiseADAS", "Speech output failed", exception)
        }
    }

    private fun updateState(transform: (DriveUiState) -> DriveUiState) {
        val next = synchronized(stateLock) {
            uiState = transform(uiState)
            uiState
        }
        mainHandler.post { onStateChanged(next) }
    }
}
