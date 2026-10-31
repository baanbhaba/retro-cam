package com.retrocam.app.camera

import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.hardware.camera2.CaptureRequest
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.util.Range
import androidx.annotation.OptIn
import androidx.camera.camera2.interop.Camera2Interop
import androidx.camera.camera2.interop.ExperimentalCamera2Interop
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.*
import androidx.camera.video.VideoCapture
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.retrocam.app.gl.RetroGlRenderer
import com.retrocam.app.settings.AppSettingsState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class RetroCamManager(private val context: Context) {

    companion object {
        private const val TAG = "RetroCamManager"
    }

    private val cameraExecutor: ExecutorService = Executors.newSingleThreadExecutor()
    private var cameraProvider: ProcessCameraProvider? = null
    private var camera: Camera? = null

    val glRenderer: RetroGlRenderer = RetroGlRenderer()
    private val cameraEffect: RetroCameraEffect = RetroCameraEffect(glRenderer)

    private var preview: Preview? = null
    private var imageCapture: ImageCapture? = null
    private var videoCapture: VideoCapture<Recorder>? = null
    private var currentRecording: Recording? = null

    private var lensFacing: Int = CameraSelector.LENS_FACING_BACK

    // UI & Camera State
    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    private val _recordingDurationSec = MutableStateFlow(0)
    val recordingDurationSec: StateFlow<Int> = _recordingDurationSec.asStateFlow()

    private val _zoomRatio = MutableStateFlow(1.0f)
    val zoomRatio: StateFlow<Float> = _zoomRatio.asStateFlow()

    private val _isTorchOn = MutableStateFlow(false)
    val isTorchOn: StateFlow<Boolean> = _isTorchOn.asStateFlow()

    private val _latestCapturedUri = MutableStateFlow<Uri?>(null)
    val latestCapturedUri: StateFlow<Uri?> = _latestCapturedUri.asStateFlow()

    private val _lastPhotoBitmap = MutableStateFlow<Bitmap?>(null)
    val lastPhotoBitmap: StateFlow<Bitmap?> = _lastPhotoBitmap.asStateFlow()

    fun bindCamera(
        lifecycleOwner: LifecycleOwner,
        previewView: PreviewView,
        cameraEffect: CameraEffect? = null
    ) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            try {
                cameraProvider = cameraProviderFuture.get()
                bindCameraUseCases(lifecycleOwner, previewView, cameraEffect)
            } catch (e: Exception) {
                Log.e(TAG, "Use case binding failed", e)
            }
        }, ContextCompat.getMainExecutor(context))
    }

    @OptIn(ExperimentalCamera2Interop::class)
    private fun bindCameraUseCases(
        lifecycleOwner: LifecycleOwner,
        previewView: PreviewView,
        cameraEffect: CameraEffect? = null
    ) {
        val provider = cameraProvider ?: return

        val cameraSelector = CameraSelector.Builder()
            .requireLensFacing(lensFacing)
            .build()

        val isBackCamera = lensFacing == CameraSelector.LENS_FACING_BACK

        // Preview setup
        val previewBuilder = Preview.Builder()
            .setTargetAspectRatio(AspectRatio.RATIO_4_3)

        if (isBackCamera) {
            try {
                val previewExtender = Camera2Interop.Extender(previewBuilder)
                previewExtender.setCaptureRequestOption(
                    CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE,
                    Range(18, 24)
                )
                previewExtender.setCaptureRequestOption(
                    CaptureRequest.NOISE_REDUCTION_MODE,
                    CaptureRequest.NOISE_REDUCTION_MODE_OFF
                )
                previewExtender.setCaptureRequestOption(
                    CaptureRequest.EDGE_MODE,
                    CaptureRequest.EDGE_MODE_OFF
                )
            } catch (e: Exception) {
                Log.w(TAG, "Camera2 interop not applied to preview", e)
            }
        }

        preview = previewBuilder.build().apply {
            setSurfaceProvider(previewView.surfaceProvider)
        }

        // ImageCapture setup
        val imgCaptureBuilder = ImageCapture.Builder()
            .setTargetAspectRatio(AspectRatio.RATIO_4_3)
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)

        if (isBackCamera) {
            try {
                val imgExtender = Camera2Interop.Extender(imgCaptureBuilder)
                imgExtender.setCaptureRequestOption(
                    CaptureRequest.NOISE_REDUCTION_MODE,
                    CaptureRequest.NOISE_REDUCTION_MODE_OFF
                )
                imgExtender.setCaptureRequestOption(
                    CaptureRequest.EDGE_MODE,
                    CaptureRequest.EDGE_MODE_OFF
                )
            } catch (e: Exception) {
                Log.w(TAG, "Camera2 interop not applied to photo capture", e)
            }
        }

        imageCapture = imgCaptureBuilder.build()

        // VideoCapture setup
        val recorder = Recorder.Builder()
            .setQualitySelector(QualitySelector.from(Quality.HD, FallbackStrategy.lowerQualityOrHigherThan(Quality.SD)))
            .setExecutor(cameraExecutor)
            .build()
        videoCapture = VideoCapture.withOutput(recorder)

        try {
            provider.unbindAll()

            var boundCamera: Camera? = null
            if (isBackCamera) {
                try {
                    val useCaseGroup = UseCaseGroup.Builder()
                        .addUseCase(preview!!)
                        .addUseCase(imageCapture!!)
                        .addUseCase(videoCapture!!)
                        .addEffect(this.cameraEffect)
                        .build()
                    boundCamera = provider.bindToLifecycle(lifecycleOwner, cameraSelector, useCaseGroup)
                } catch (e: Exception) {
                    Log.w(TAG, "Binding with CameraEffect failed, falling back to direct video capture", e)
                    boundCamera = provider.bindToLifecycle(
                        lifecycleOwner,
                        cameraSelector,
                        preview!!,
                        imageCapture!!,
                        videoCapture!!
                    )
                }
            } else {
                // Front / Selfie Camera: cleanly bind directly without rear effects
                boundCamera = provider.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    preview!!,
                    imageCapture!!,
                    videoCapture!!
                )
            }

            camera = boundCamera

            // Sync zoom state
            camera?.cameraInfo?.zoomState?.observe(lifecycleOwner) { state ->
                _zoomRatio.value = state.zoomRatio
            }
        } catch (e: Exception) {
            Log.e(TAG, "Binding use cases failed", e)
        }
    }

    fun updateEffectSettings(settings: AppSettingsState) {
        glRenderer.squishFactor = settings.squishFactor
        glRenderer.aspectMode = if (settings.letterboxMatte) 0 else 2
        glRenderer.bloomIntensity = settings.bloomIntensity
        glRenderer.chromaAberration = settings.chromaticAberration * 0.008f
        glRenderer.grainIntensity = settings.lumaGrainIntensity * 0.14f + 0.03f
        glRenderer.scanlineIntensity = settings.crtScanlineOpacity * 0.18f
        glRenderer.colorGrade = settings.colorProfile.ordinal
    }

    fun toggleCamera(lifecycleOwner: LifecycleOwner, previewView: PreviewView, effect: CameraEffect? = null) {
        val nextFacing = if (lensFacing == CameraSelector.LENS_FACING_BACK) {
            CameraSelector.LENS_FACING_FRONT
        } else {
            CameraSelector.LENS_FACING_BACK
        }
        val selector = CameraSelector.Builder().requireLensFacing(nextFacing).build()
        if (cameraProvider?.hasCamera(selector) == true) {
            lensFacing = nextFacing
            _isTorchOn.value = false
            bindCameraUseCases(lifecycleOwner, previewView, effect)
        }
    }

    fun toggleTorch() {
        if (lensFacing == CameraSelector.LENS_FACING_FRONT) return
        val current = _isTorchOn.value
        camera?.cameraControl?.enableTorch(!current)
        _isTorchOn.value = !current
    }

    fun stepZoom(zoomIn: Boolean) {
        val cam = camera ?: return
        val current = _zoomRatio.value
        val target = if (zoomIn) {
            when {
                current < 1.4f -> 1.5f
                current < 1.9f -> 2.0f
                current < 2.9f -> 3.0f
                current < 4.5f -> 5.0f
                else -> current
            }
        } else {
            when {
                current > 4.0f -> 3.0f
                current > 2.5f -> 2.0f
                current > 1.7f -> 1.5f
                current > 1.1f -> 1.0f
                else -> 1.0f
            }
        }
        cam.cameraControl.setZoomRatio(target)
    }

    fun setZoom(ratio: Float) {
        camera?.cameraControl?.setZoomRatio(ratio)
    }

    fun tapToFocus(previewView: PreviewView, x: Float, y: Float, onFocusComplete: ((Boolean) -> Unit)? = null) {
        val cam = camera ?: return
        try {
            val factory = previewView.meteringPointFactory
            val point = factory.createPoint(x.coerceAtLeast(0f), y.coerceAtLeast(0f))
            val action = FocusMeteringAction.Builder(point, FocusMeteringAction.FLAG_AF or FocusMeteringAction.FLAG_AE)
                .setAutoCancelDuration(4, TimeUnit.SECONDS)
                .build()
            val future = cam.cameraControl.startFocusAndMetering(action)
            future.addListener({
                try {
                    val result = future.get()
                    onFocusComplete?.invoke(result.isFocusSuccessful)
                } catch (e: Exception) {
                    onFocusComplete?.invoke(false)
                }
            }, ContextCompat.getMainExecutor(context))
        } catch (e: Exception) {
            Log.e(TAG, "Focus metering failed", e)
            onFocusComplete?.invoke(false)
        }
    }

    fun setExposureIndex(index: Int) {
        val cam = camera ?: return
        try {
            val range = getExposureRange()
            val clamped = index.coerceIn(range.lower, range.upper)
            cam.cameraControl.setExposureCompensationIndex(clamped)
        } catch (e: Exception) {
            Log.e(TAG, "Exposure compensation failed", e)
        }
    }

    fun getExposureRange(): Range<Int> {
        val state = camera?.cameraInfo?.exposureState
        if (state != null && state.isExposureCompensationSupported) {
            val r = state.exposureCompensationRange
            if (r.lower < r.upper) return r
        }
        return Range(-4, 4)
    }

    fun getExposureStep(): Float {
        val step = camera?.cameraInfo?.exposureState?.exposureCompensationStep?.toFloat()
        return if (step != null && step > 0f) step else 0.5f
    }

    fun takePhoto(
        config: RetroPhotoProcessor.EffectConfig,
        onPhotoCaptured: (Uri?) -> Unit
    ) {
        val capture = imageCapture ?: return

        capture.takePicture(
            cameraExecutor,
            object : ImageCapture.OnImageCapturedCallback() {
                override fun onCaptureSuccess(image: ImageProxy) {
                    val bitmap = imageProxyToBitmap(image)
                    image.close()

                    if (bitmap != null) {
                        _lastPhotoBitmap.value = bitmap
                        // Process with retro shaders & stamp
                        CoroutineScope(Dispatchers.IO).launch {
                            val savedUri = RetroPhotoProcessor.processAndSavePhoto(context, bitmap, config)
                            _latestCapturedUri.value = savedUri
                            onPhotoCaptured(savedUri)
                        }
                    } else {
                        onPhotoCaptured(null)
                    }
                }

                override fun onError(exception: ImageCaptureException) {
                    Log.e(TAG, "Photo capture failed", exception)
                    onPhotoCaptured(null)
                }
            }
        )
    }

    @SuppressLint("MissingPermission")
    fun startVideoRecording(onRecordingFinished: (Uri?) -> Unit) {
        val vc = videoCapture ?: return
        if (currentRecording != null) return

        val filename = "RETRO_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())}.mp4"
        val contentValues = ContentValues().apply {
            put(MediaStore.Video.Media.DISPLAY_NAME, filename)
            put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Video.Media.RELATIVE_PATH, Environment.DIRECTORY_MOVIES + "/RetroCam")
            }
        }

        val mediaStoreOutput = MediaStoreOutputOptions.Builder(
            context.contentResolver,
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        ).setContentValues(contentValues).build()

        var pendingRecording = vc.output.prepareRecording(context, mediaStoreOutput)
        val hasAudio = ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.RECORD_AUDIO
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        if (hasAudio) {
            pendingRecording = pendingRecording.withAudioEnabled()
        }

        var durationCounter = 0
        currentRecording = pendingRecording.start(ContextCompat.getMainExecutor(context)) { event ->
            when (event) {
                is VideoRecordEvent.Start -> {
                    _isRecording.value = true
                    _recordingDurationSec.value = 0
                }
                is VideoRecordEvent.Status -> {
                    durationCounter = (event.recordingStats.recordedDurationNanos / 1_000_000_000L).toInt()
                    _recordingDurationSec.value = durationCounter
                }
                is VideoRecordEvent.Finalize -> {
                    _isRecording.value = false
                    _recordingDurationSec.value = 0
                    currentRecording = null
                    val outputUri = event.outputResults.outputUri
                    if (!event.hasError() && outputUri != Uri.EMPTY) {
                        _latestCapturedUri.value = outputUri
                        MediaScannerConnection.scanFile(
                            context,
                            arrayOf(outputUri.toString()),
                            arrayOf("video/mp4"),
                            null
                        )
                        onRecordingFinished(outputUri)
                    } else if (outputUri != Uri.EMPTY) {
                        _latestCapturedUri.value = outputUri
                        onRecordingFinished(outputUri)
                    } else {
                        Log.e(TAG, "Video recording finalized with error: ${event.error}")
                        onRecordingFinished(null)
                    }
                }
            }
        }
    }

    fun stopVideoRecording() {
        currentRecording?.stop()
        currentRecording = null
        _isRecording.value = false
    }

    private fun imageProxyToBitmap(image: ImageProxy): Bitmap? {
        val buffer = image.planes[0].buffer
        val bytes = ByteArray(buffer.remaining())
        buffer.get(bytes)
        val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return null

        val rotation = image.imageInfo.rotationDegrees
        val matrix = Matrix().apply {
            if (rotation != 0) postRotate(rotation.toFloat())
            if (lensFacing == CameraSelector.LENS_FACING_FRONT) {
                // Mirror front camera selfie horizontally
                postScale(-1f, 1f)
            }
        }
        return if (!matrix.isIdentity) {
            Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        } else {
            bitmap
        }
    }

    fun release() {
        cameraExecutor.shutdown()
    }
}
