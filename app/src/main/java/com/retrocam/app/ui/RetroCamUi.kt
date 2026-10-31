package com.retrocam.app.ui

import android.net.Uri
import android.util.Range
import androidx.camera.view.PreviewView
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import coil.compose.AsyncImage
import com.retrocam.app.camera.RetroCamManager
import com.retrocam.app.camera.RetroPhotoProcessor
import com.retrocam.app.settings.*
import kotlinx.coroutines.delay
import java.util.*
import kotlin.math.roundToInt

enum class CaptureMode {
    PHOTO,
    VIDEO
}

enum class CamcorderPreset(
    val title: String,
    val subtitle: String,
    val specs: String,
    val colorProfile: ColorProfile,
    val squishFactor: Float,
    val bloomIntensity: Float,
    val scanlines: Float,
    val grain: Float,
    val osdColor: OsdColor,
    val vintageYear: Int?
) {
    SONY_90S(
        "SONY CCD-TRV65 (1999)",
        "Warm punchy reds, golden skin tones, milky lifted blacks",
        "CCD 640x • PHOSPHOR AMBER",
        ColorProfile.SONY_CCD_1999,
        1.35f,
        0.45f,
        0.35f,
        0.55f,
        OsdColor.PHOSPHOR_AMBER,
        1999
    ),
    MINI_DV(
        "PANASONIC NV-GS400 (2002)",
        "Clean highlights, cyan/teal shadows, Y2K digital camcorder feel",
        "MINIDV 720x480 • CYBER CYAN",
        ColorProfile.PANASONIC_MINIDV,
        1.25f,
        0.25f,
        0.20f,
        0.40f,
        OsdColor.CYBER_CYAN,
        2002
    ),
    HI8_TAPE(
        "JVC GR-AXM10 HI-8 (1994)",
        "Saturated analogue tape, warm amber tones, softened detail",
        "ANALOGUE TAPE • VINTAGE AMBER",
        ColorProfile.JVC_HI8,
        1.35f,
        0.55f,
        0.50f,
        0.65f,
        OsdColor.PHOSPHOR_AMBER,
        1994
    ),
    SKATE_DV(
        "SONY DCR-VX1000 (1998)",
        "High-contrast anamorphic street look, 90s skate video standard",
        "ANAMORPHIC SQUEEZE • ELECTRIC GREEN",
        ColorProfile.PANASONIC_MINIDV,
        1.48f,
        0.40f,
        0.30f,
        0.65f,
        OsdColor.ELECTRIC_GREEN,
        1998
    ),
    NIGHTSHOT(
        "HANDYCAM NIGHTSHOT (2000)",
        "0-Lux infrared night-vision simulation with phosphor green glow",
        "0-LUX IR • NEON GREEN",
        ColorProfile.CYBER_Y2K,
        1.30f,
        0.65f,
        0.45f,
        0.75f,
        OsdColor.ELECTRIC_GREEN,
        2000
    ),
    BW_CCTV(
        "SECURITY CCD MONITOR (1995)",
        "High-contrast monochrome surveillance sensor with heavy grain",
        "B&W 4:3 • VCR WHITE",
        ColorProfile.BW_SECURITY,
        1.33f,
        0.20f,
        0.60f,
        0.80f,
        OsdColor.VCR_WHITE,
        1995
    )
}

@Composable
fun RetroCamScreen(
    camManager: RetroCamManager,
    settingsManager: SettingsManager,
    modifier: Modifier = Modifier
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val haptic = LocalHapticFeedback.current

    val settings by settingsManager.settings.collectAsState()

    // Camera & Recording State
    val isRecording by camManager.isRecording.collectAsState()
    val recordingDurationSec by camManager.recordingDurationSec.collectAsState()
    val zoomRatio by camManager.zoomRatio.collectAsState()
    val isTorchOn by camManager.isTorchOn.collectAsState()
    val latestUri by camManager.latestCapturedUri.collectAsState()

    var previewViewRef by remember { mutableStateOf<PreviewView?>(null) }
    var captureMode by remember { mutableStateOf(CaptureMode.PHOTO) }
    var showFlashEffect by remember { mutableStateOf(false) }
    var isExposureLocked by remember { mutableStateOf(false) }

    // Tap to focus & exposure / contrast state
    var focusPoint by remember { mutableStateOf<Offset?>(null) }
    var exposureCompensationIndex by remember { mutableIntStateOf(0) }
    var showFocusReticle by remember { mutableStateOf(false) }
    var lastInteractionTime by remember { mutableLongStateOf(0L) }

    // Active preset
    var activePreset by remember { mutableStateOf<CamcorderPreset?>(CamcorderPreset.SONY_90S) }

    // Modals
    var showFullSettingsDialog by remember { mutableStateOf(false) }
    var showTapeStatusModal by remember { mutableStateOf(false) }
    var showDateCustomizerModal by remember { mutableStateOf(false) }
    var showAspectQuickModal by remember { mutableStateOf(false) }
    var showMediaViewerModal by remember { mutableStateOf(false) }
    var showCameraSelectorModal by remember { mutableStateOf(false) }

    // Auto-dismiss focus reticle after 3.5s of inactivity
    LaunchedEffect(lastInteractionTime) {
        if (showFocusReticle && lastInteractionTime > 0L) {
            delay(3500)
            showFocusReticle = false
        }
    }

    // Sync settings to OpenGL video recorder shader pipeline
    LaunchedEffect(settings) {
        camManager.updateEffectSettings(settings)
    }

    // Live clock ticker
    var currentTimeString by remember { mutableStateOf("") }
    LaunchedEffect(settings.osdCustomYear) {
        while (true) {
            currentTimeString = formatOsdDate(settings.osdCustomYear)
            delay(1000)
        }
    }

    // Shutter flash effect
    LaunchedEffect(showFlashEffect) {
        if (showFlashEffect) {
            delay(120)
            showFlashEffect = false
        }
    }

    // Root Chassis
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0D0E12))
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // 1. TOP MINIMALIST BAR
            CleanTopBar(
                osdColor = settings.osdColor,
                isTorchOn = isTorchOn,
                onOpenTapeStatus = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    showTapeStatusModal = true
                },
                onToggleTorch = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    camManager.toggleTorch()
                },
                onFlipCamera = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    previewViewRef?.let { pv -> camManager.toggleCamera(lifecycleOwner, pv) }
                },
                onOpenSettings = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    showFullSettingsDialog = true
                }
            )

            // 2. VIEWFINDER SCREEN
            Box(
                modifier = Modifier
                    .weight(1.0f)
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 4.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.Black)
                    .border(1.dp, Color(0xFF1E1F28), RoundedCornerShape(20.dp))
                    .pointerInput(Unit) {
                        detectTapGestures { tapOffset ->
                            focusPoint = tapOffset
                            showFocusReticle = true
                            lastInteractionTime = System.currentTimeMillis()
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            previewViewRef?.let { pv ->
                                camManager.tapToFocus(pv, tapOffset.x, tapOffset.y)
                            }
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                // CameraX Preview
                AndroidView(
                    factory = { ctx ->
                        PreviewView(ctx).apply {
                            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                            scaleType = PreviewView.ScaleType.FIT_CENTER
                            previewViewRef = this
                            camManager.bindCamera(lifecycleOwner, this)
                        }
                    },
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            scaleY = (1.0f / settings.squishFactor).coerceIn(0.55f, 1.0f)
                            scaleX = 1.04f
                        }
                )

                // Letterbox Mattes
                if (settings.letterboxMatte) {
                    val matteHeight = ((settings.squishFactor - 1.0f) * 75f).dp
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(matteHeight.coerceAtLeast(24.dp))
                                .background(Color(0xFA060608))
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(matteHeight.coerceAtLeast(24.dp))
                                .background(Color(0xFA060608))
                        )
                    }
                }

                // Overlays
                if (settings.crtScanlineOpacity > 0.05f) {
                    ScanlineOverlay(opacity = settings.crtScanlineOpacity)
                }
                LiveCamcorderNoiseOverlay(
                    enabled = true,
                    intensity = settings.chromaNoiseIntensity
                )

                if (isRecording) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .border(3.dp, Color(0xFFFF2E2E), RoundedCornerShape(20.dp))
                    )
                }

                if (showFlashEffect) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.White)
                    )
                }

                // TAP TO FOCUS & EXPOSURE SLIDER
                if (showFocusReticle && focusPoint != null) {
                    CleanFocusAndExposureSlider(
                        point = focusPoint!!,
                        exposureCompensationIndex = exposureCompensationIndex,
                        exposureStep = camManager.getExposureStep(),
                        exposureRange = camManager.getExposureRange(),
                        phosphorColor = Color(settings.osdColor.colorHex),
                        isExposureLocked = isExposureLocked,
                        onExposureChange = { newIdx ->
                            exposureCompensationIndex = newIdx
                            camManager.setExposureIndex(newIdx)
                            lastInteractionTime = System.currentTimeMillis()
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        },
                        onReticleClick = {
                            isExposureLocked = !isExposureLocked
                            lastInteractionTime = System.currentTimeMillis()
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        }
                    )
                }

                // FLOATING MINIMAL HUD
                FloatingMinimalOsd(
                    isRecording = isRecording,
                    recordingSec = recordingDurationSec,
                    squishFactor = settings.squishFactor,
                    presetTitle = activePreset?.title?.split(" ")?.getOrNull(0) ?: "SONY",
                    dateString = currentTimeString,
                    osdColor = Color(settings.osdColor.colorHex),
                    onTapRecStatus = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        if (captureMode == CaptureMode.VIDEO) {
                            if (isRecording) camManager.stopVideoRecording() else camManager.startVideoRecording {}
                        } else {
                            captureMode = CaptureMode.VIDEO
                        }
                    },
                    onTapAspect = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        showAspectQuickModal = true
                    },
                    onTapDate = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        showDateCustomizerModal = true
                    },
                    onTapPresetBadge = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        showCameraSelectorModal = true
                    }
                )
            }

            // 3. SPACIOUS ERGONOMIC CONTROLS PANEL (No middle strip crowding the screen!)
            CleanControlsPanel(
                captureMode = captureMode,
                isRecording = isRecording,
                zoomRatio = zoomRatio,
                squishFactor = settings.squishFactor,
                year = settings.osdCustomYear,
                activePreset = activePreset,
                latestUri = latestUri,
                onModeChange = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    captureMode = it
                },
                onOpenCamSelector = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    showCameraSelectorModal = true
                },
                onToggleAspect = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    showAspectQuickModal = true
                },
                onToggleYear = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    showDateCustomizerModal = true
                },
                onZoomIn = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    camManager.stepZoom(true)
                },
                onZoomOut = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    camManager.stepZoom(false)
                },
                onOpenGallery = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    showMediaViewerModal = true
                },
                onShutterClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    if (captureMode == CaptureMode.PHOTO) {
                        showFlashEffect = true
                        val effectConfig = RetroPhotoProcessor.EffectConfig(
                            squishFactor = settings.squishFactor,
                            bloomEnabled = settings.bloomIntensity > 0.05f,
                            chromaticAberration = settings.chromaticAberration > 0.05f,
                            scanlinesEnabled = settings.crtScanlineOpacity > 0.05f,
                            grainIntensity = settings.lumaGrainIntensity * 0.16f + 0.04f,
                            chromaNoiseEnabled = settings.chromaNoiseIntensity > 0.05f,
                            colorGrade = settings.colorProfile.ordinal,
                            customDateText = if (settings.showDateStamp) currentTimeString else null
                        )
                        camManager.takePhoto(effectConfig) {}
                    } else {
                        if (isRecording) {
                            camManager.stopVideoRecording()
                        } else {
                            camManager.startVideoRecording {}
                        }
                    }
                }
            )
        }

        // MODALS
        if (showCameraSelectorModal) {
            RetroCameraSelectorModal(
                activePreset = activePreset,
                onSelectPreset = { preset ->
                    activePreset = preset
                    settingsManager.update { s ->
                        s.copy(
                            colorProfile = preset.colorProfile,
                            squishFactor = preset.squishFactor,
                            bloomIntensity = preset.bloomIntensity,
                            crtScanlineOpacity = preset.scanlines,
                            lumaGrainIntensity = preset.grain,
                            osdColor = preset.osdColor,
                            osdCustomYear = preset.vintageYear
                        )
                    }
                    showCameraSelectorModal = false
                },
                onDismiss = { showCameraSelectorModal = false }
            )
        }
        if (showFullSettingsDialog) {
            RetroSettingsScreen(settingsManager = settingsManager) {
                showFullSettingsDialog = false
            }
        }
        if (showTapeStatusModal) {
            RetroTapeStatusModal { showTapeStatusModal = false }
        }
        if (showDateCustomizerModal) {
            RetroDateCustomizerModal(settingsManager = settingsManager) {
                showDateCustomizerModal = false
            }
        }
        if (showAspectQuickModal) {
            RetroAspectQuickModal(settingsManager = settingsManager) {
                showAspectQuickModal = false
            }
        }
        if (showMediaViewerModal) {
            RetroMediaViewerModal(latestUri = latestUri) {
                showMediaViewerModal = false
            }
        }
    }
}

@Composable
fun CleanTopBar(
    osdColor: OsdColor,
    isTorchOn: Boolean,
    onOpenTapeStatus: () -> Unit,
    onToggleTorch: () -> Unit,
    onFlipCamera: () -> Unit,
    onOpenSettings: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 20.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.clickable { onOpenTapeStatus() }
        ) {
            Text(
                text = "RETROCAM",
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 1.5.sp
            )
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(0x26FFB326))
                    .border(1.dp, Color(0x4DFFB326), RoundedCornerShape(4.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "DV-60",
                    color = Color(0xFFFFB326),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            IconButton(
                onClick = onToggleTorch,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = if (isTorchOn) Icons.Default.FlashOn else Icons.Default.FlashOff,
                    contentDescription = "Torch",
                    tint = if (isTorchOn) Color(0xFFFFB326) else Color(0xFF8E8E98),
                    modifier = Modifier.size(20.dp)
                )
            }

            IconButton(
                onClick = onFlipCamera,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.FlipCameraAndroid,
                    contentDescription = "Flip",
                    tint = Color(0xFF8E8E98),
                    modifier = Modifier.size(20.dp)
                )
            }

            IconButton(
                onClick = onOpenSettings,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Tune,
                    contentDescription = "Settings",
                    tint = Color(0xFF8E8E98),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
fun FloatingMinimalOsd(
    isRecording: Boolean,
    recordingSec: Int,
    squishFactor: Float,
    presetTitle: String,
    dateString: String,
    osdColor: Color,
    onTapRecStatus: () -> Unit,
    onTapAspect: () -> Unit,
    onTapDate: () -> Unit,
    onTapPresetBadge: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "rec_blink")
    val alphaAnim by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = if (isRecording) 0.2f else 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "blink_alpha"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(14.dp)
    ) {
        // TOP ROW
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(7.dp),
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xA60A0A0E))
                    .border(1.dp, Color(0x1AFFFFFF), RoundedCornerShape(12.dp))
                    .clickable { onTapRecStatus() }
                    .padding(horizontal = 10.dp, vertical = 5.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .graphicsLayer { alpha = if (isRecording) alphaAnim else 1.0f }
                        .background(if (isRecording) Color(0xFFFF2E2E) else Color(0xFF00E676))
                )
                Text(
                    text = if (isRecording) "REC" else "STBY",
                    color = if (isRecording) Color(0xFFFF2E2E) else Color(0xFF00E676),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                val min = recordingSec / 60
                val sec = recordingSec % 60
                Text(
                    text = String.format("%02d:%02d", min, sec),
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xA60A0A0E))
                    .border(1.dp, Color(0x1AFFFFFF), RoundedCornerShape(12.dp))
                    .clickable { onTapAspect() }
                    .padding(horizontal = 10.dp, vertical = 5.dp)
            ) {
                Text(
                    text = String.format("%.2fx SQUISH", squishFactor),
                    color = Color(0xFFAAAAAA),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        // BOTTOM ROW
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xA60A0A0E))
                    .border(1.dp, Color(0x1AFFFFFF), RoundedCornerShape(8.dp))
                    .clickable { onTapDate() }
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = dateString,
                    color = osdColor,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    style = TextStyle(shadow = Shadow(color = Color.Black, offset = Offset(1f, 1f), blurRadius = 3f))
                )
            }

            // Clickable Preset Tag -> Opens Camera Selector Page
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xA60A0A0E))
                    .border(1.dp, Color(0x33FFB326), RoundedCornerShape(8.dp))
                    .clickable { onTapPresetBadge() }
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "📹 $presetTitle ▾",
                    color = Color(0xFFFFB326),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}

@Composable
fun CleanFocusAndExposureSlider(
    point: Offset,
    exposureCompensationIndex: Int,
    exposureStep: Float,
    exposureRange: Range<Int>,
    phosphorColor: Color,
    isExposureLocked: Boolean,
    onExposureChange: (Int) -> Unit,
    onReticleClick: () -> Unit
) {
    val currentExposure by rememberUpdatedState(exposureCompensationIndex)
    val currentRange by rememberUpdatedState(exposureRange)
    val onExposureChangeUpdated by rememberUpdatedState(onExposureChange)

    var dragAccumulator by remember { mutableFloatStateOf(0f) }

    Box(
        modifier = Modifier
            .offset {
                IntOffset(
                    (point.x - 32.dp.toPx()).roundToInt().coerceAtLeast(8),
                    (point.y - 32.dp.toPx()).roundToInt().coerceAtLeast(8)
                )
            }
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .border(1.5.dp, if (isExposureLocked) phosphorColor else Color(0xCCFFB326), RoundedCornerShape(12.dp))
                    .clickable { onReticleClick() },
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "+",
                        color = phosphorColor,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    if (isExposureLocked) {
                        Text(
                            text = "AE-L",
                            color = phosphorColor,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }

            Column(
                modifier = Modifier
                    .width(34.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xE60D0E12))
                    .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(16.dp))
                    .pointerInput(Unit) {
                        detectVerticalDragGestures { _, dragAmount ->
                            dragAccumulator -= dragAmount
                            val stepThreshold = 20f
                            if (dragAccumulator > stepThreshold) {
                                val steps = (dragAccumulator / stepThreshold).toInt()
                                val next = (currentExposure + steps).coerceIn(currentRange.lower, currentRange.upper)
                                onExposureChangeUpdated(next)
                                dragAccumulator %= stepThreshold
                            } else if (dragAccumulator < -stepThreshold) {
                                val steps = (-dragAccumulator / stepThreshold).toInt()
                                val next = (currentExposure - steps).coerceIn(currentRange.lower, currentRange.upper)
                                onExposureChangeUpdated(next)
                                dragAccumulator %= stepThreshold
                            }
                        }
                    }
                    .padding(vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.WbSunny,
                    contentDescription = "Exposure",
                    tint = phosphorColor,
                    modifier = Modifier.size(14.dp)
                )

                Spacer(modifier = Modifier.height(4.dp))

                Box(
                    modifier = Modifier
                        .width(3.dp)
                        .height(54.dp)
                        .background(Color(0x33FFFFFF), RoundedCornerShape(2.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    val rangeSpan = (currentRange.upper - currentRange.lower).coerceAtLeast(1)
                    val progress = ((currentExposure - currentRange.lower).toFloat() / rangeSpan).coerceIn(0f, 1f)
                    val thumbOffset = ((0.5f - progress) * 44f).dp
                    Box(
                        modifier = Modifier
                            .offset(y = thumbOffset)
                            .size(11.dp)
                            .clip(CircleShape)
                            .background(phosphorColor)
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                val ev = currentExposure * exposureStep
                Text(
                    text = String.format("%+.1f", ev),
                    color = Color.White,
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun CleanControlsPanel(
    captureMode: CaptureMode,
    isRecording: Boolean,
    zoomRatio: Float,
    squishFactor: Float,
    year: Int?,
    activePreset: CamcorderPreset?,
    latestUri: Uri?,
    onModeChange: (CaptureMode) -> Unit,
    onOpenCamSelector: () -> Unit,
    onToggleAspect: () -> Unit,
    onToggleYear: () -> Unit,
    onZoomIn: () -> Unit,
    onZoomOut: () -> Unit,
    onOpenGallery: () -> Unit,
    onShutterClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF0D0E12))
            .navigationBarsPadding()
            .padding(horizontal = 20.dp, vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // QUICK ACTIONS ROW: Camera Selector Button + Aspect Ratio + Year
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val presetShortName = activePreset?.title?.split(" ")?.getOrNull(0) ?: "SONY 90s"
            CleanPillButton(
                text = "📹 $presetShortName ▾",
                active = true,
                isHighlight = true,
                onClick = onOpenCamSelector
            )
            CleanPillButton(
                text = String.format("RATIO: %.2fx", squishFactor),
                active = true,
                isHighlight = false,
                onClick = onToggleAspect
            )
            CleanPillButton(
                text = "YEAR: ${year ?: "REAL"}",
                active = true,
                isHighlight = false,
                onClick = onToggleYear
            )
        }

        // MAIN CONTROL DECK (Zoom Rocker, Large Shutter, Mode + Gallery)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left: Zoom Rocker
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = String.format("%.1fx", zoomRatio),
                    color = Color(0xFF888888),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(24.dp))
                        .background(Color(0xFF14151C))
                        .border(1.dp, Color(0xFF262734), RoundedCornerShape(24.dp))
                        .padding(3.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF1A1B24))
                            .clickable { onZoomOut() },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("W", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }

                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF1A1B24))
                            .clickable { onZoomIn() },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("T", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }

            // Center: 80dp Shutter Button
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF161720))
                    .border(3.dp, Color(0xFF2B2C3A), CircleShape)
                    .clickable { onShutterClick() },
                contentAlignment = Alignment.Center
            ) {
                if (captureMode == CaptureMode.PHOTO) {
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFF0F0F5))
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(if (isRecording) 32.dp else 60.dp)
                            .clip(if (isRecording) RoundedCornerShape(8.dp) else CircleShape)
                            .background(Color(0xFFFF2E2E))
                    )
                }
            }

            // Right: Mode Switch & Gallery
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(18.dp))
                        .background(Color(0xFF14151C))
                        .border(1.dp, Color(0xFF262734), RoundedCornerShape(18.dp))
                        .padding(3.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (captureMode == CaptureMode.PHOTO) Color(0xFFFFB326) else Color.Transparent)
                            .clickable { onModeChange(CaptureMode.PHOTO) }
                            .padding(horizontal = 9.dp, vertical = 5.dp)
                    ) {
                        Text(
                            "PHOTO",
                            color = if (captureMode == CaptureMode.PHOTO) Color.Black else Color.Gray,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (captureMode == CaptureMode.VIDEO) Color(0xFFFFB326) else Color.Transparent)
                            .clickable { onModeChange(CaptureMode.VIDEO) }
                            .padding(horizontal = 9.dp, vertical = 5.dp)
                    ) {
                        Text(
                            "VIDEO",
                            color = if (captureMode == CaptureMode.VIDEO) Color.Black else Color.Gray,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF181922))
                        .border(1.dp, Color(0xFF2B2C3A), RoundedCornerShape(12.dp))
                        .clickable { onOpenGallery() },
                    contentAlignment = Alignment.Center
                ) {
                    if (latestUri != null) {
                        AsyncImage(
                            model = latestUri,
                            contentDescription = "Gallery",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.PhotoLibrary,
                            contentDescription = "Gallery",
                            tint = Color.Gray,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CleanPillButton(
    text: String,
    active: Boolean,
    isHighlight: Boolean = false,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(
                if (isHighlight) Color(0x2BFFB326) else (if (active) Color(0x1AFFFFFF) else Color(0xFF14151C))
            )
            .border(
                1.dp,
                if (isHighlight) Color(0xFFFFB326) else Color(0xFF262734),
                RoundedCornerShape(20.dp)
            )
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 7.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = if (isHighlight) Color(0xFFFFB326) else Color(0xFFCCCCCC),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )
    }
}

@Composable
fun RetroCameraSelectorModal(
    activePreset: CamcorderPreset?,
    onSelectPreset: (CamcorderPreset) -> Unit,
    onDismiss: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xB3000000))
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) { onDismiss() },
        contentAlignment = Alignment.BottomCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
                .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                .background(Color(0xFF101117))
                .border(1.dp, Color(0xFF262736), RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) { /* consume clicks */ }
        ) {
            Box(
                modifier = Modifier
                    .padding(top = 10.dp, bottom = 4.dp)
                    .width(42.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color(0xFF363748))
                    .align(Alignment.CenterHorizontally)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("📹", fontSize = 16.sp)
                    Text(
                        text = "CAMCORDER GEAR CATALOG",
                        color = Color.White,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.LightGray, modifier = Modifier.size(20.dp))
                }
            }

            Text(
                text = "SELECT VINTAGE OPTICAL BODY & SENSOR:",
                color = Color(0xFF888898),
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 10.dp)
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                CamcorderPreset.values().forEach { preset ->
                    val isSelected = activePreset == preset
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (isSelected) Color(0x26FFB326) else Color(0xFF14151D))
                            .border(
                                width = if (isSelected) 1.5.dp else 1.dp,
                                color = if (isSelected) Color(0xFFFFB326) else Color(0xFF22232E),
                                shape = RoundedCornerShape(16.dp)
                            )
                            .clickable { onSelectPreset(preset) }
                            .padding(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = preset.title,
                                    color = if (isSelected) Color(0xFFFFB326) else Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 13.sp
                                )
                                Text(
                                    text = preset.subtitle,
                                    color = Color(0xFF888898),
                                    fontSize = 11.sp,
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                                Box(
                                    modifier = Modifier
                                        .padding(top = 6.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(Color(0xFF1E1F2A))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = preset.specs,
                                        color = Color(0xFFAAAAAA),
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }

                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "Equipped",
                                    tint = Color(0xFFFFB326),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF14151C))
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFB326)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().height(44.dp)
                ) {
                    Text(
                        text = "EQUIP & RETURN TO VIEWFINDER",
                        color = Color.Black,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}

@Composable
fun LiveCamcorderNoiseOverlay(
    enabled: Boolean = true,
    intensity: Float = 0.60f
) {
    if (!enabled || intensity < 0.05f) return

    var noiseTick by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(42)
            noiseTick = (noiseTick + 1) % 60
        }
    }

    Canvas(modifier = Modifier.fillMaxSize()) {
        val random = kotlin.random.Random(noiseTick.toLong() * 997L + 17L)
        val w = size.width
        val h = size.height

        val noiseColors = arrayOf(
            Color(0x38FF3B30),
            Color(0x3832ADE6),
            Color(0x38FFD60A),
            Color(0x40FFFFFF),
            Color(0x30000000)
        )

        val dotCount = (150 * intensity).toInt()
        for (i in 0 until dotCount) {
            val x = random.nextFloat() * w
            val y = random.nextFloat() * h
            val c = noiseColors[random.nextInt(noiseColors.size)]
            val radius = random.nextFloat() * 1.6f + 0.7f
            drawCircle(
                color = c,
                radius = radius,
                center = Offset(x, y)
            )
        }
    }
}

@Composable
fun ScanlineOverlay(opacity: Float = 0.35f) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.SpaceEvenly
    ) {
        repeat(75) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(Color.Black.copy(alpha = opacity * 0.35f))
            )
        }
    }
}

fun formatOsdDate(customYear: Int?): String {
    val now = Calendar.getInstance()
    val monthNames = arrayOf("JAN", "FEB", "MAR", "APR", "MAY", "JUN", "JUL", "AUG", "SEP", "OCT", "NOV", "DEC")
    val month = monthNames[now.get(Calendar.MONTH)]
    val day = String.format("%02d", now.get(Calendar.DAY_OF_MONTH))
    val year = customYear ?: now.get(Calendar.YEAR)
    val hour = now.get(Calendar.HOUR).let { if (it == 0) 12 else it }
    val minute = String.format("%02d", now.get(Calendar.MINUTE))
    val second = String.format("%02d", now.get(Calendar.SECOND))
    val amPm = if (now.get(Calendar.AM_PM) == Calendar.AM) "AM" else "PM"

    return "$month $day $year  $hour:$minute:$second $amPm"
}
