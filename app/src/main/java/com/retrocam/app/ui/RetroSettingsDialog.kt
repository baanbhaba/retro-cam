package com.retrocam.app.ui

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.retrocam.app.settings.*

enum class SettingsTab(val title: String, val icon: ImageVector) {
    OPTICS("OPTICS", Icons.Default.AspectRatio),
    SENSOR("SENSOR & FPS", Icons.Default.Speed),
    ARTIFACTS("ARTIFACTS", Icons.Default.Grain),
    COLOR("COLOR SCIENCE", Icons.Default.Palette),
    OSD("OSD & HUD", Icons.Default.Subtitles),
    AUDIO("AUDIO & MISC", Icons.Default.Audiotrack)
}

@Composable
fun RetroSettingsDialog(
    settingsManager: SettingsManager,
    onDismiss: () -> Unit
) {
    RetroSettingsScreen(settingsManager = settingsManager, onDismiss = onDismiss)
}

@Composable
fun RetroSettingsScreen(
    settingsManager: SettingsManager,
    onDismiss: () -> Unit
) {
    val settings by settingsManager.settings.collectAsState()
    var selectedTab by remember { mutableStateOf(SettingsTab.OPTICS) }

    // Modern Slide-up Bottom Sheet Modal
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
                .fillMaxHeight(0.88f)
                .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                .background(Color(0xFF101117))
                .border(
                    width = 1.dp,
                    color = Color(0xFF262736),
                    shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
                )
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) { /* consume inner clicks */ }
        ) {
            // DRAG HANDLE BAR
            Box(
                modifier = Modifier
                    .padding(top = 10.dp, bottom = 4.dp)
                    .width(42.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color(0xFF363748))
                    .align(Alignment.CenterHorizontally)
            )

            // HEADER
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFFFB326))
                    )
                    Text(
                        text = "CALIBRATION & SETTINGS",
                        color = Color.White,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        letterSpacing = 0.5.sp
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Reset to Defaults Button
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF1A1B24))
                            .border(1.dp, Color(0xFF2C2D3C), RoundedCornerShape(8.dp))
                            .clickable {
                                settingsManager.update { AppSettingsState() }
                            }
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Text(
                            text = "RESET",
                            color = Color(0xFFAAAAAA),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    // Close Button
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color.LightGray,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // HORIZONTAL TABS CAROUSEL
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SettingsTab.values().forEach { tab ->
                    val isSelected = selectedTab == tab
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) Color(0x26FFB326) else Color(0xFF161720))
                            .border(
                                width = 1.dp,
                                color = if (isSelected) Color(0xFFFFB326) else Color(0xFF262734),
                                shape = RoundedCornerShape(12.dp)
                            )
                            .clickable { selectedTab = tab }
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(
                                imageVector = tab.icon,
                                contentDescription = tab.title,
                                tint = if (isSelected) Color(0xFFFFB326) else Color(0xFF888898),
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = tab.title,
                                color = if (isSelected) Color(0xFFFFB326) else Color(0xFFAAAAAA),
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }

            Divider(color = Color(0xFF1E1F2A), thickness = 1.dp, modifier = Modifier.padding(top = 6.dp))

            // TAB SCROLLABLE BODY
            Column(
                modifier = Modifier
                    .weight(1.0f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                when (selectedTab) {
                    SettingsTab.OPTICS -> RevampedOpticsTab(settings, settingsManager)
                    SettingsTab.SENSOR -> RevampedSensorTab(settings, settingsManager)
                    SettingsTab.ARTIFACTS -> RevampedArtifactsTab(settings, settingsManager)
                    SettingsTab.COLOR -> RevampedColorTab(settings, settingsManager)
                    SettingsTab.OSD -> RevampedOsdTab(settings, settingsManager)
                    SettingsTab.AUDIO -> RevampedAudioTab(settings, settingsManager)
                }
                Spacer(modifier = Modifier.height(24.dp))
            }

            // FOOTER BAR
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF14151C))
                    .border(1.dp, Color(0xFF20212C))
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFB326)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().height(44.dp)
                ) {
                    Text(
                        text = "APPLY & RETURN TO VIEWFINDER",
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
private fun RevampedOpticsTab(settings: AppSettingsState, mgr: SettingsManager) {
    SettingsCard {
        ModernSectionHeader("ANAMORPHIC SQUEEZE & RATIO")

        ModernSliderRow(
            title = "Vertical Squish Ratio",
            value = settings.squishFactor,
            range = 1.0f..1.65f,
            displayValue = String.format("%.2fx", settings.squishFactor),
            description = "Compresses frame vertically to mimic vintage anamorphic skate camcorders"
        ) { v -> mgr.update { s -> s.copy(squishFactor = v) } }

        ModernSliderRow(
            title = "Lens Vignette Falloff",
            value = settings.vignetteIntensity,
            range = 0.0f..0.80f,
            displayValue = String.format("%.0f%%", settings.vignetteIntensity * 100f),
            description = "Darkens perimeter and corners like low-cost 90s zoom barrels"
        ) { v -> mgr.update { s -> s.copy(vignetteIntensity = v) } }

        ModernSwitchRow(
            title = "Cinematic Letterbox Mattes",
            checked = settings.letterboxMatte,
            description = "Top and bottom black matte bars matching classic CRT aspect"
        ) { v -> mgr.update { s -> s.copy(letterboxMatte = v) } }
    }
}

@Composable
private fun RevampedSensorTab(settings: AppSettingsState, mgr: SettingsManager) {
    SettingsCard {
        ModernSectionHeader("SHUTTER CADENCE & FPS RATE")

        Text(
            text = "Select sensor frame rate to simulate vintage motion lag & stutter:",
            color = Color(0xFFAAAAAA),
            fontSize = 11.sp,
            modifier = Modifier.padding(bottom = 6.dp)
        )

        FpsCadence.values().forEach { cadence ->
            val selected = settings.fpsCadence == cadence
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 3.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (selected) Color(0x26FFB326) else Color(0xFF161720))
                    .border(
                        width = 1.dp,
                        color = if (selected) Color(0xFFFFB326) else Color(0xFF262734),
                        shape = RoundedCornerShape(10.dp)
                    )
                    .clickable { mgr.update { s -> s.copy(fpsCadence = cadence) } }
                    .padding(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = cadence.label,
                            color = if (selected) Color(0xFFFFB326) else Color.White,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp
                        )
                        Text(
                            text = cadence.description,
                            color = Color(0xFF888898),
                            fontSize = 10.sp
                        )
                    }
                    if (selected) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Selected",
                            tint = Color(0xFFFFB326),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }

    SettingsCard {
        ModernSectionHeader("HARDWARE CAMERA2 BYPASS")

        ModernSwitchRow(
            title = "Bypass Computational Denoising",
            checked = settings.rawSensorNoise,
            description = "Disables Android noise reduction to preserve raw CCD grain"
        ) { v -> mgr.update { s -> s.copy(rawSensorNoise = v) } }

        ModernSwitchRow(
            title = "Bypass Digital Edge Sharpening",
            checked = settings.softOpticalEdges,
            description = "Removes artificial smartphone sharpening halos for soft vintage optics"
        ) { v -> mgr.update { s -> s.copy(softOpticalEdges = v) } }
    }
}

@Composable
private fun RevampedArtifactsTab(settings: AppSettingsState, mgr: SettingsManager) {
    SettingsCard {
        ModernSectionHeader("CCD SENSOR NOISE & GRAIN")

        ModernSliderRow(
            title = "RGB Chroma Noise (Color Specks)",
            value = settings.chromaNoiseIntensity,
            range = 0.0f..1.0f,
            displayValue = String.format("%.0f%%", settings.chromaNoiseIntensity * 100f),
            description = "Multicolor dancing noise artifacts in deep shadows"
        ) { v -> mgr.update { s -> s.copy(chromaNoiseIntensity = v) } }

        ModernSliderRow(
            title = "Luminance Film Micro-Grain",
            value = settings.lumaGrainIntensity,
            range = 0.0f..1.0f,
            displayValue = String.format("%.0f%%", settings.lumaGrainIntensity * 100f),
            description = "Organic dancing luminance grain layer"
        ) { v -> mgr.update { s -> s.copy(lumaGrainIntensity = v) } }
    }

    SettingsCard {
        ModernSectionHeader("CRT OPTICS & HALATION")

        ModernSliderRow(
            title = "CRT Scanlines Opacity",
            value = settings.crtScanlineOpacity,
            range = 0.0f..0.8f,
            displayValue = String.format("%.0f%%", settings.crtScanlineOpacity * 100f),
            description = "Horizontal analog television scanline modulation"
        ) { v -> mgr.update { s -> s.copy(crtScanlineOpacity = v) } }

        ModernSliderRow(
            title = "Highlight Bloom / Halation",
            value = settings.bloomIntensity,
            range = 0.0f..1.0f,
            displayValue = String.format("%.0f%%", settings.bloomIntensity * 100f),
            description = "Warm amber glow and dreamlike aura around light sources"
        ) { v -> mgr.update { s -> s.copy(bloomIntensity = v) } }

        ModernSliderRow(
            title = "Chromatic Aberration (Prism RGB Split)",
            value = settings.chromaticAberration,
            range = 0.0f..1.0f,
            displayValue = String.format("%.0f%%", settings.chromaticAberration * 100f),
            description = "Radial color fringing on high-contrast edges"
        ) { v -> mgr.update { s -> s.copy(chromaticAberration = v) } }
    }

    SettingsCard {
        ModernSectionHeader("MAGNETIC TAPE GLITCHES")

        ModernSwitchRow(
            title = "Horizontal Tape Jitter Lines",
            checked = settings.tapeJitterEnabled,
            description = "Subtle VHS magnetic track lines"
        ) { v -> mgr.update { s -> s.copy(tapeJitterEnabled = v) } }

        ModernSwitchRow(
            title = "Tape Startup Wrinkle on REC",
            checked = settings.tapeWrinkleGlitch,
            description = "Realistic magnetic tape warp when pressing Record"
        ) { v -> mgr.update { s -> s.copy(tapeWrinkleGlitch = v) } }
    }
}

@Composable
private fun RevampedColorTab(settings: AppSettingsState, mgr: SettingsManager) {
    SettingsCard {
        ModernSectionHeader("COLOR SCIENCE MATRIX")

        ColorProfile.values().forEach { profile ->
            val selected = settings.colorProfile == profile
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 3.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (selected) Color(0x26FFB326) else Color(0xFF161720))
                    .border(
                        width = 1.dp,
                        color = if (selected) Color(0xFFFFB326) else Color(0xFF262734),
                        shape = RoundedCornerShape(10.dp)
                    )
                    .clickable { mgr.update { s -> s.copy(colorProfile = profile) } }
                    .padding(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = profile.title,
                            color = if (selected) Color(0xFFFFB326) else Color.White,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp
                        )
                        Text(
                            text = profile.description,
                            color = Color(0xFF888898),
                            fontSize = 10.sp
                        )
                    }
                    if (selected) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Selected",
                            tint = Color(0xFFFFB326),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RevampedOsdTab(settings: AppSettingsState, mgr: SettingsManager) {
    SettingsCard {
        ModernSectionHeader("PHOSPHOR HUD COLOR")

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            OsdColor.values().forEach { colorOpt ->
                val isSelected = settings.osdColor == colorOpt
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { mgr.update { s -> s.copy(osdColor = colorOpt) } }
                        .padding(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(Color(colorOpt.colorHex))
                            .border(
                                width = if (isSelected) 3.dp else 1.dp,
                                color = if (isSelected) Color.White else Color(0x44FFFFFF),
                                shape = CircleShape
                            )
                    )
                    Text(
                        text = colorOpt.label.split(" ")[0],
                        color = if (isSelected) Color.White else Color(0xFF888898),
                        fontSize = 10.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
    }

    SettingsCard {
        ModernSectionHeader("VINTAGE YEAR STAMP")

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            listOf(1995, 1998, 1999, 2002, null).forEach { year ->
                val isSelected = settings.osdCustomYear == year
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSelected) Color(0xFFFFB326) else Color(0xFF161720))
                        .border(
                            width = 1.dp,
                            color = if (isSelected) Color(0xFFFFB326) else Color(0xFF262734),
                            shape = RoundedCornerShape(8.dp)
                        )
                        .clickable { mgr.update { s -> s.copy(osdCustomYear = year) } }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = year?.toString() ?: "REAL-TIME",
                        color = if (isSelected) Color.Black else Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
    }

    SettingsCard {
        ModernSectionHeader("HUD WIDGET VISIBILITY")

        ModernSwitchRow("Battery Readout [||||]", settings.showBatteryMeter) { v ->
            mgr.update { s -> s.copy(showBatteryMeter = v) }
        }
        ModernSwitchRow("Tape Counter & Timecode", settings.showTapeCounter) { v ->
            mgr.update { s -> s.copy(showTapeCounter = v) }
        }
        ModernSwitchRow("Center Focus Reticle [ + ]", settings.showFocusReticle) { v ->
            mgr.update { s -> s.copy(showFocusReticle = v) }
        }
        ModernSwitchRow("Vintage Date / Time Overlay", settings.showDateStamp) { v ->
            mgr.update { s -> s.copy(showDateStamp = v) }
        }
        ModernSwitchRow("Burn HUD into Saved Photos", settings.burnOsdIntoPhotos) { v ->
            mgr.update { s -> s.copy(burnOsdIntoPhotos = v) }
        }
        ModernSwitchRow("Burn HUD into Saved Videos", settings.burnOsdIntoVideos) { v ->
            mgr.update { s -> s.copy(burnOsdIntoVideos = v) }
        }
    }
}

@Composable
private fun RevampedAudioTab(settings: AppSettingsState, mgr: SettingsManager) {
    SettingsCard {
        ModernSectionHeader("AUDIO & SENSORY HAPTICS")

        ModernSwitchRow(
            title = "Camcorder Low-Pass Audio Filter",
            checked = settings.lowPassAudioFilter,
            description = "Warm low-pass filter cutting modern ultra-crisp highs"
        ) { v -> mgr.update { s -> s.copy(lowPassAudioFilter = v) } }

        ModernSwitchRow(
            title = "Simulated Tape Drive Motor Hum",
            checked = settings.camcorderTapeHum,
            description = "Faint mechanical tape whirr in the background"
        ) { v -> mgr.update { s -> s.copy(camcorderTapeHum = v) } }

        ModernSwitchRow(
            title = "Tactile Button Haptic Feedback",
            checked = settings.tactileHaptics,
            description = "Vibration clicks when triggering physical camcorder buttons"
        ) { v -> mgr.update { s -> s.copy(tactileHaptics = v) } }
    }
}

@Composable
private fun SettingsCard(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF14151D))
            .border(1.dp, Color(0xFF22232E), RoundedCornerShape(16.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        content = content
    )
}

@Composable
private fun ModernSectionHeader(text: String) {
    Text(
        text = text,
        color = Color(0xFFFFB326),
        fontWeight = FontWeight.Bold,
        fontFamily = FontFamily.Monospace,
        fontSize = 11.sp,
        letterSpacing = 0.5.sp,
        modifier = Modifier.padding(bottom = 2.dp)
    )
}

@Composable
private fun ModernSliderRow(
    title: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    displayValue: String,
    description: String? = null,
    onValueChange: (Float) -> Unit
) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(title, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0xFF20212C))
                    .padding(horizontal = 8.dp, vertical = 2.dp)
            ) {
                Text(displayValue, color = Color(0xFFFFB326), fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
            }
        }

        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = range,
            colors = SliderDefaults.colors(
                thumbColor = Color(0xFFFFB326),
                activeTrackColor = Color(0xFFFFB326),
                inactiveTrackColor = Color(0xFF2A2B38)
            ),
            modifier = Modifier.padding(top = 2.dp, bottom = 2.dp)
        )

        if (description != null) {
            Text(description, color = Color(0xFF888898), fontSize = 10.sp)
        }
    }
}

@Composable
private fun ModernSwitchRow(
    title: String,
    checked: Boolean,
    description: String? = null,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(title, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            if (description != null) {
                Text(description, color = Color(0xFF888898), fontSize = 10.sp)
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color(0xFFFFB326),
                checkedTrackColor = Color(0xFF4A3E20),
                uncheckedThumbColor = Color(0xFF888898),
                uncheckedTrackColor = Color(0xFF20212C)
            )
        )
    }
}
