package com.roadwise.adas

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.provider.MediaStore
import android.util.Log
import android.util.Range
import android.util.Size
import androidx.camera.camera2.interop.Camera2CameraInfo
import androidx.camera.camera2.interop.ExperimentalCamera2Interop
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.FallbackStrategy
import androidx.camera.video.FileOutputOptions
import androidx.camera.video.MediaStoreOutputOptions
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

private const val EVENT_CLIP_LENGTH_MS = 20_000L

/** State for the in-app live dashcam, recording and event-trigger controls. */
data class DashcamUiState(
    val cameraStatus: String = "Camera paused",
    val isArmed: Boolean = false,
    val isRecording: Boolean = false,
    val isManualRecording: Boolean = false,
    val recordingSeconds: Int = 0,
    val status: String = "Dashcam off",
    val lastEvent: String? = null,
    val savedClipCount: Int = 0,
)

/**
 * CameraX preview + OCR + video capture session. Event clips are opt-in and start
 * after an event is detected; no footage is retained before the trigger.
 */
class DashcamCameraController(
    context: Context,
    private val onStateChanged: (DashcamUiState) -> Unit,
) {
    private enum class RecordingMode { MANUAL, EVENT }

    private val appContext = context.applicationContext
    private val mainHandler = Handler(Looper.getMainLooper())
    private val mainExecutor = ContextCompat.getMainExecutor(appContext)
    private var uiState = DashcamUiState()
    private var bindingGeneration = 0
    private var boundProvider: ProcessCameraProvider? = null
    private var boundAnalysis: ImageAnalysis? = null
    private var analysisExecutor: ExecutorService? = null
    private var videoCapture: VideoCapture<Recorder>? = null
    private var recorder: Recorder? = null
    private var activeRecording: Recording? = null
    private var recordingMode: RecordingMode? = null
    private var recordingStartedAt = 0L
    private var stopRequested = false

    private val recordingTicker = object : Runnable {
        override fun run() {
            if (activeRecording == null) return
            val seconds = ((SystemClock.elapsedRealtime() - recordingStartedAt) / 1_000L).toInt()
            updateState { it.copy(recordingSeconds = seconds) }
            mainHandler.postDelayed(this, 1_000L)
        }
    }

    private val stopEventClip = Runnable {
        if (recordingMode == RecordingMode.EVENT && activeRecording != null) {
            stopActiveRecording()
        }
    }

    @OptIn(ExperimentalCamera2Interop::class)
    fun bindCamera(
        lifecycleOwner: LifecycleOwner,
        previewView: PreviewView,
        analyzer: (ImageProxy) -> Unit,
    ) {
        unbindCamera()
        val generation = bindingGeneration
        val executor = Executors.newSingleThreadExecutor()
        analysisExecutor = executor
        updateState { it.copy(cameraStatus = "Opening rear camera…") }

        val providerFuture = ProcessCameraProvider.getInstance(appContext)
        providerFuture.addListener({
            if (generation != bindingGeneration) return@addListener
            try {
                val provider = providerFuture.get()
                val cameraInfo = CameraSelector.DEFAULT_BACK_CAMERA
                    .filter(provider.availableCameraInfos)
                    .firstOrNull()
                val candidates = supportedFrameRates(cameraInfo)
                val attempts = mutableListOf<Range<Int>?>().apply {
                    addAll(candidates)
                    add(null)
                }
                var bound = false

                for (requestedRate in attempts) {
                    if (generation != bindingGeneration) break
                    var analysis: ImageAnalysis? = null
                    try {
                        val previewBuilder = Preview.Builder()
                        val analysisBuilder = ImageAnalysis.Builder()
                            .setTargetResolution(Size(1280, 720))
                            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        if (requestedRate != null) {
                            // CameraX exposes target frame-rate hints on Preview; video/analysis
                            // may still run at a lower rate to satisfy the camera's session limits.
                            previewBuilder.setTargetFrameRate(requestedRate)
                        }

                        val preview = previewBuilder.build()
                        val imageAnalysis = analysisBuilder.build()
                        analysis = imageAnalysis
                        imageAnalysis.setAnalyzer(executor) { proxy -> analyzer(proxy) }
                        preview.setSurfaceProvider(previewView.surfaceProvider)

                        val recorder = Recorder.Builder()
                            .setQualitySelector(
                                QualitySelector.from(
                                    Quality.HD,
                                    FallbackStrategy.lowerQualityOrHigherThan(Quality.SD),
                                ),
                            )
                            .build()
                        val video = VideoCapture.withOutput(recorder)

                        provider.unbindAll()
                        provider.bindToLifecycle(
                            lifecycleOwner,
                            CameraSelector.DEFAULT_BACK_CAMERA,
                            preview,
                            imageAnalysis,
                            video,
                        )

                        boundProvider = provider
                        boundAnalysis = imageAnalysis
                        videoCapture = video
                        this@DashcamCameraController.recorder = recorder
                        val fpsLabel = when {
                            requestedRate == null -> "Live · dashcam ready · device-default FPS"
                            requestedRate.upper >= 120 -> "Live · 120 FPS requested · video session may use less"
                            else -> "Live · up to ${requestedRate.upper} FPS requested"
                        }
                        updateState {
                            it.copy(
                                cameraStatus = fpsLabel,
                                status = if (it.isArmed) "Event capture armed" else "Dashcam ready · recording is off",
                            )
                        }
                        bound = true
                        break
                    } catch (exception: Exception) {
                        analysis?.clearAnalyzer()
                        provider.unbindAll()
                        Log.w("RoadwiseADAS", "CameraX could not bind at frame range $requestedRate", exception)
                    }
                }
                if (!bound && generation == bindingGeneration) {
                    updateState {
                        it.copy(cameraStatus = "Camera unavailable · close other camera apps and retry")
                    }
                }
            } catch (exception: Exception) {
                Log.e("RoadwiseADAS", "Unable to initialize dashcam CameraX session", exception)
                if (generation == bindingGeneration) {
                    updateState { it.copy(cameraStatus = "Camera unavailable · check permission") }
                }
            }
        }, mainExecutor)
    }

    fun setEventRecordingArmed(armed: Boolean) {
        runOnMain {
            if (uiState.isArmed == armed) return@runOnMain
            if (!armed && recordingMode == RecordingMode.EVENT) stopActiveRecording()
            updateState {
                it.copy(
                    isArmed = armed,
                    status = when {
                        armed && it.isRecording -> "Recording · event capture armed"
                        armed -> "Event capture armed · waiting for a trigger"
                        it.isRecording -> "Recording manually"
                        else -> "Dashcam ready · recording is off"
                    },
                )
            }
        }
    }

    fun toggleManualRecording() {
        runOnMain {
            when {
                recordingMode == RecordingMode.MANUAL -> stopActiveRecording()
                recordingMode == RecordingMode.EVENT && activeRecording != null -> {
                    mainHandler.removeCallbacks(stopEventClip)
                    recordingMode = RecordingMode.MANUAL
                    updateState {
                        it.copy(
                            isManualRecording = true,
                            status = "Recording manually · tap stop to save",
                        )
                    }
                }
                activeRecording != null -> updateState { it.copy(status = "Saving current clip…") }
                else -> startRecording(RecordingMode.MANUAL, "manual")
            }
        }
    }

    /** Called for a sharp accelerometer impulse or a rapid GPS-speed drop. */
    fun captureSafetyEvent(reason: String) {
        runOnMain {
            updateState { it.copy(lastEvent = reason) }
            when {
                activeRecording != null -> updateState {
                    it.copy(status = "$reason detected during recording · current clip is being saved")
                }
                !uiState.isArmed -> updateState {
                    it.copy(status = "$reason detected · arm event capture to save video")
                }
                else -> startRecording(RecordingMode.EVENT, reason)
            }
        }
    }

    /** Stop capture before the Activity backgrounds; camera recording never runs invisibly. */
    fun stopForBackground() {
        runOnMain {
            updateState { it.copy(isArmed = false) }
            mainHandler.removeCallbacks(stopEventClip)
            if (activeRecording != null) stopActiveRecording()
        }
    }

    fun unbindCamera() {
        bindingGeneration += 1
        if (activeRecording != null) stopActiveRecording()
        boundAnalysis?.clearAnalyzer()
        boundAnalysis = null
        boundProvider?.unbindAll()
        boundProvider = null
        videoCapture = null
        recorder = null
        analysisExecutor?.shutdownNow()
        analysisExecutor = null
        updateState { it.copy(cameraStatus = "Camera paused") }
    }

    fun release() {
        stopForBackground()
        unbindCamera()
    }

    private fun startRecording(mode: RecordingMode, reason: String) {
        val activeRecorder = recorder
        if (videoCapture == null || activeRecorder == null) {
            updateState { it.copy(status = "Camera is not ready · recording unavailable") }
            return
        }

        try {
            val outputOptions = createOutputOptions(mode, reason)
            val pending = activeRecorder
                .prepareRecording(appContext, outputOptions)
                .start(mainExecutor) { event -> onRecordEvent(event, mode, reason) }
            activeRecording = pending
            recordingMode = mode
            recordingStartedAt = SystemClock.elapsedRealtime()
            stopRequested = false
            mainHandler.removeCallbacks(recordingTicker)
            mainHandler.postDelayed(recordingTicker, 1_000L)
            if (mode == RecordingMode.EVENT) {
                mainHandler.removeCallbacks(stopEventClip)
                mainHandler.postDelayed(stopEventClip, EVENT_CLIP_LENGTH_MS)
            }
            updateState {
                it.copy(
                    isRecording = true,
                    isManualRecording = mode == RecordingMode.MANUAL,
                    recordingSeconds = 0,
                    status = if (mode == RecordingMode.EVENT) {
                        "Recording 20-second event clip · $reason"
                    } else {
                        "Recording manually · tap stop to save"
                    },
                )
            }
        } catch (exception: Exception) {
            activeRecording = null
            recordingMode = null
            Log.e("RoadwiseADAS", "Could not start dashcam recording", exception)
            updateState { it.copy(isRecording = false, status = "Recording failed · check storage and camera") }
        }
    }

    private fun onRecordEvent(event: VideoRecordEvent, modeAtStart: RecordingMode, reason: String) {
        when (event) {
            is VideoRecordEvent.Start -> updateState {
                it.copy(
                    isRecording = true,
                    isManualRecording = recordingMode == RecordingMode.MANUAL,
                    status = if (recordingMode == RecordingMode.EVENT) {
                        "Recording event clip · $reason"
                    } else {
                        "Recording manually · tap stop to save"
                    },
                )
            }
            is VideoRecordEvent.Finalize -> {
                mainHandler.removeCallbacks(recordingTicker)
                mainHandler.removeCallbacks(stopEventClip)
                val saved = !event.hasError()
                val currentMode = recordingMode ?: modeAtStart
                activeRecording = null
                recordingMode = null
                stopRequested = false
                updateState {
                    it.copy(
                        isRecording = false,
                        isManualRecording = false,
                        recordingSeconds = 0,
                        savedClipCount = it.savedClipCount + if (saved) 1 else 0,
                        status = when {
                            !saved -> "Clip could not be saved · check free storage"
                            currentMode == RecordingMode.EVENT -> "Event clip saved · ${savedFolderLabel()}"
                            else -> "Dashcam clip saved · ${savedFolderLabel()}"
                        },
                    )
                }
            }
        }
    }

    private fun stopActiveRecording() {
        val current = activeRecording ?: return
        if (stopRequested) return
        stopRequested = true
        mainHandler.removeCallbacks(stopEventClip)
        updateState { it.copy(status = "Finalizing dashcam clip…") }
        try {
            current.stop()
        } catch (exception: Exception) {
            Log.w("RoadwiseADAS", "Could not stop dashcam recording cleanly", exception)
            activeRecording = null
            recordingMode = null
            updateState { it.copy(isRecording = false, isManualRecording = false, status = "Recording stopped") }
        }
    }

    private fun createOutputOptions(mode: RecordingMode, reason: String): androidx.camera.video.OutputOptions {
        val timestamp = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(Date())
        val safeReason = reason.uppercase(Locale.US).replace(Regex("[^A-Z0-9]+"), "_").trim('_').take(28)
        val displayName = if (mode == RecordingMode.EVENT) {
            "Roadwise_Event_${safeReason}_$timestamp.mp4"
        } else {
            "Roadwise_Dashcam_$timestamp.mp4"
        }

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, displayName)
                put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4")
                put(MediaStore.MediaColumns.RELATIVE_PATH, "${Environment.DIRECTORY_MOVIES}/Roadwise")
            }
            MediaStoreOutputOptions.Builder(
                appContext.contentResolver,
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            ).setContentValues(values).build()
        } else {
            val parent = appContext.getExternalFilesDir(Environment.DIRECTORY_MOVIES) ?: appContext.filesDir
            val directory = File(parent, "Roadwise").apply { mkdirs() }
            FileOutputOptions.Builder(File(directory, displayName)).build()
        }
    }

    private fun savedFolderLabel(): String = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        "Movies/Roadwise"
    } else {
        "app-specific Movies/Roadwise"
    }

    @OptIn(ExperimentalCamera2Interop::class)
    private fun supportedFrameRates(cameraInfo: androidx.camera.core.CameraInfo?): List<Range<Int>> {
        if (cameraInfo == null) return emptyList()
        return try {
            val cameraId = Camera2CameraInfo.from(cameraInfo).cameraId
            val manager = appContext.getSystemService(Context.CAMERA_SERVICE) as android.hardware.camera2.CameraManager
            manager.getCameraCharacteristics(cameraId)
                .get(android.hardware.camera2.CameraCharacteristics.CONTROL_AE_AVAILABLE_TARGET_FPS_RANGES)
                ?.toList()
                .orEmpty()
                .filter { it.upper <= 120 }
                .sortedWith(compareByDescending<Range<Int>> { it.upper }.thenByDescending { it.lower })
                .distinctBy { it.lower to it.upper }
        } catch (exception: Exception) {
            Log.w("RoadwiseADAS", "Unable to inspect camera frame-rate ranges", exception)
            emptyList()
        }
    }

    private fun updateState(transform: (DashcamUiState) -> DashcamUiState) {
        val next = synchronized(this) {
            uiState = transform(uiState)
            uiState
        }
        mainHandler.post { onStateChanged(next) }
    }

    private fun runOnMain(action: () -> Unit) {
        if (Looper.myLooper() == Looper.getMainLooper()) action() else mainHandler.post(action)
    }
}
