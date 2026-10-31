package com.retrocam.app.settings

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class ColorProfile(val title: String, val description: String) {
    SONY_CCD_1999("Sony CCD 1999", "Warm punchy reds, golden skin, milky lifted blacks"),
    PANASONIC_MINIDV("Panasonic MiniDV 2002", "Clean highlights, cyan/teal shadows, Y2K digital feel"),
    JVC_HI8("JVC Hi8 1994", "Saturated analogue tape, warm amber tones, softened detail"),
    BW_SECURITY("B&W Surveillance", "High-contrast monochrome CCD sensor with grain"),
    CYBER_Y2K("Cyber Y2K", "Slight magenta shift, neon glow, saturated cyber look")
}

enum class FpsCadence(val label: String, val fpsRange: Pair<Int, Int>, val description: String) {
    LP_15("15 FPS (LP Stutter)", Pair(15, 15), "Heavy vintage stutter & motion lag"),
    SUPER8_18("18 FPS (Super 8)", Pair(18, 18), "Dreamy analog film cadence"),
    MINIDV_24("24 FPS (MiniDV Indie)", Pair(20, 24), "Iconic camcorder shutter lag & blur"),
    CCD_30("30 FPS (Standard CCD)", Pair(30, 30), "Standard vintage video rate"),
    SMOOTH_60("60 FPS (Modern)", Pair(30, 60), "Butter-smooth smartphone rate")
}

enum class OsdColor(val label: String, val colorHex: Long) {
    PHOSPHOR_AMBER("Phosphor Amber", 0xFFFFB326),
    ELECTRIC_GREEN("Electric Green", 0xFF00FF66),
    CYBER_CYAN("Cyber Cyan", 0xFF00E5FF),
    VCR_WHITE("VCR White", 0xFFEEEEEE),
    NEON_RED("Neon Red", 0xFFFF2A2A)
}

data class AppSettingsState(
    // Aspect & Distortion
    val squishFactor: Float = 1.35f,
    val barrelDistortion: Float = 0.0f, // Fisheye / Century optics death lens
    val letterboxMatte: Boolean = true,
    val vignetteIntensity: Float = 0.35f,

    // Motion & Sensor Lag
    val fpsCadence: FpsCadence = FpsCadence.MINIDV_24,
    val rawSensorNoise: Boolean = true, // Disable Android computational denoise
    val softOpticalEdges: Boolean = true, // Disable Android edge over-sharpening

    // Noise & Artifacts
    val chromaNoiseIntensity: Float = 0.65f, // Multi-colored RGB noise specks
    val lumaGrainIntensity: Float = 0.50f, // Micro-grain
    val crtScanlineOpacity: Float = 0.40f,
    val crtScanlineDensity: Int = 75,
    val bloomIntensity: Float = 0.45f,
    val chromaticAberration: Float = 0.50f,
    val tapeJitterEnabled: Boolean = true,
    val tapeWrinkleGlitch: Boolean = true,

    // Color Science
    val colorProfile: ColorProfile = ColorProfile.SONY_CCD_1999,

    // OSD / HUD Settings
    val osdColor: OsdColor = OsdColor.PHOSPHOR_AMBER,
    val osdCustomYear: Int? = 1999, // null = real-time
    val osdCustomLabel: String = "CCD 640x",
    val showBatteryMeter: Boolean = true,
    val showTapeCounter: Boolean = true,
    val showFocusReticle: Boolean = true,
    val showDateStamp: Boolean = true,
    val burnOsdIntoPhotos: Boolean = true,
    val burnOsdIntoVideos: Boolean = true,

    // Audio & Haptics
    val lowPassAudioFilter: Boolean = true,
    val camcorderTapeHum: Boolean = false,
    val tactileHaptics: Boolean = true
)

class SettingsManager(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("retrocam_settings", Context.MODE_PRIVATE)

    private val _settings = MutableStateFlow(loadSettings())
    val settings: StateFlow<AppSettingsState> = _settings.asStateFlow()

    private fun loadSettings(): AppSettingsState {
        return AppSettingsState(
            squishFactor = prefs.getFloat("squishFactor", 1.35f),
            barrelDistortion = prefs.getFloat("barrelDistortion", 0.0f),
            letterboxMatte = prefs.getBoolean("letterboxMatte", true),
            vignetteIntensity = prefs.getFloat("vignetteIntensity", 0.35f),

            fpsCadence = try {
                FpsCadence.valueOf(prefs.getString("fpsCadence", FpsCadence.MINIDV_24.name) ?: FpsCadence.MINIDV_24.name)
            } catch (e: Exception) { FpsCadence.MINIDV_24 },

            rawSensorNoise = prefs.getBoolean("rawSensorNoise", true),
            softOpticalEdges = prefs.getBoolean("softOpticalEdges", true),

            chromaNoiseIntensity = prefs.getFloat("chromaNoiseIntensity", 0.65f),
            lumaGrainIntensity = prefs.getFloat("lumaGrainIntensity", 0.50f),
            crtScanlineOpacity = prefs.getFloat("crtScanlineOpacity", 0.40f),
            bloomIntensity = prefs.getFloat("bloomIntensity", 0.45f),
            chromaticAberration = prefs.getFloat("chromaticAberration", 0.50f),
            tapeJitterEnabled = prefs.getBoolean("tapeJitterEnabled", true),
            tapeWrinkleGlitch = prefs.getBoolean("tapeWrinkleGlitch", true),

            colorProfile = try {
                ColorProfile.valueOf(prefs.getString("colorProfile", ColorProfile.SONY_CCD_1999.name) ?: ColorProfile.SONY_CCD_1999.name)
            } catch (e: Exception) { ColorProfile.SONY_CCD_1999 },

            osdColor = try {
                OsdColor.valueOf(prefs.getString("osdColor", OsdColor.PHOSPHOR_AMBER.name) ?: OsdColor.PHOSPHOR_AMBER.name)
            } catch (e: Exception) { OsdColor.PHOSPHOR_AMBER },

            osdCustomYear = if (prefs.contains("osdCustomYear")) prefs.getInt("osdCustomYear", 1999).let { if (it == -1) null else it } else 1999,
            osdCustomLabel = prefs.getString("osdCustomLabel", "CCD 640x") ?: "CCD 640x",
            showBatteryMeter = prefs.getBoolean("showBatteryMeter", true),
            showTapeCounter = prefs.getBoolean("showTapeCounter", true),
            showFocusReticle = prefs.getBoolean("showFocusReticle", true),
            showDateStamp = prefs.getBoolean("showDateStamp", true),
            burnOsdIntoPhotos = prefs.getBoolean("burnOsdIntoPhotos", true),
            burnOsdIntoVideos = prefs.getBoolean("burnOsdIntoVideos", true),

            lowPassAudioFilter = prefs.getBoolean("lowPassAudioFilter", true),
            camcorderTapeHum = prefs.getBoolean("camcorderTapeHum", false),
            tactileHaptics = prefs.getBoolean("tactileHaptics", true)
        )
    }

    fun update(transform: (AppSettingsState) -> AppSettingsState) {
        val newSettings = transform(_settings.value)
        _settings.value = newSettings
        saveSettings(newSettings)
    }

    private fun saveSettings(s: AppSettingsState) {
        prefs.edit().apply {
            putFloat("squishFactor", s.squishFactor)
            putFloat("barrelDistortion", s.barrelDistortion)
            putBoolean("letterboxMatte", s.letterboxMatte)
            putFloat("vignetteIntensity", s.vignetteIntensity)
            putString("fpsCadence", s.fpsCadence.name)
            putBoolean("rawSensorNoise", s.rawSensorNoise)
            putBoolean("softOpticalEdges", s.softOpticalEdges)
            putFloat("chromaNoiseIntensity", s.chromaNoiseIntensity)
            putFloat("lumaGrainIntensity", s.lumaGrainIntensity)
            putFloat("crtScanlineOpacity", s.crtScanlineOpacity)
            putFloat("bloomIntensity", s.bloomIntensity)
            putFloat("chromaticAberration", s.chromaticAberration)
            putBoolean("tapeJitterEnabled", s.tapeJitterEnabled)
            putBoolean("tapeWrinkleGlitch", s.tapeWrinkleGlitch)
            putString("colorProfile", s.colorProfile.name)
            putString("osdColor", s.osdColor.name)
            putInt("osdCustomYear", s.osdCustomYear ?: -1)
            putString("osdCustomLabel", s.osdCustomLabel)
            putBoolean("showBatteryMeter", s.showBatteryMeter)
            putBoolean("showTapeCounter", s.showTapeCounter)
            putBoolean("showFocusReticle", s.showFocusReticle)
            putBoolean("showDateStamp", s.showDateStamp)
            putBoolean("burnOsdIntoPhotos", s.burnOsdIntoPhotos)
            putBoolean("burnOsdIntoVideos", s.burnOsdIntoVideos)
            putBoolean("lowPassAudioFilter", s.lowPassAudioFilter)
            putBoolean("camcorderTapeHum", s.camcorderTapeHum)
            putBoolean("tactileHaptics", s.tactileHaptics)
            apply()
        }
    }
}
