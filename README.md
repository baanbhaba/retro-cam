# RetroCam 📹✨

> A tactile, authentic 90s vintage camcorder and retro camera app for Android powered by Jetpack Compose and real-time OpenGL ES shaders.

[![GitHub Release](https://img.shields.io/github/v/release/baanbhaba/retro-cam?style=for-the-badge&logo=github&color=FF6B00)](https://github.com/baanbhaba/retro-cam/releases)
[![Android](https://img.shields.io/badge/Platform-Android_8.0+-3DDC84?style=for-the-badge&logo=android&logoColor=white)](https://android.com)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.0-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![OpenGL ES](https://img.shields.io/badge/OpenGL_ES-2.0%2F3.0-5586A4?style=for-the-badge&logo=opengl&logoColor=white)](https://www.khronos.org/opengles/)

---

## 🌟 Highlights

* **Authentic Vintage Aesthetics**: Faithfully recreates the iconic look of 90s camcorders, VHS tapes, Hi8, and early digital CCD sensors.
* **Tactile Camcorder Interface**:
  * Clean, spacious 4:3 viewfinder framed by rounded retro bezels.
  * Dedicated **Camera Gear Selector** modal for vintage presets.
  * Real-time **Tap-to-Focus** reticle with responsive touch exposure slider ($-2.0\text{ EV}$ to $+2.0\text{ EV}$).
  * Tactile **W / T** optical & digital zoom rocker with haptic feedback.
  * Floating minimal retro HUD: `STBY`/`REC` badge, dynamic timecode, anamorphic squish indicators, and authentic neon date stamping (`OCT 31 1999`).
* **Real-Time GLSL Shader Pipeline**:
  * VHS head-switching noise, tape flutter, tracking lines, and signal roll.
  * CRT phosphor mask, aperture grille, and scanlines.
  * Chromatic aberration, barrel distortion, and lens vignetting.
  * Organic film grain and blooming highlights.
  * Sensor quantization, color banding, and Bayer pattern color bleed.
* **Deep Retro Settings Drawer**:
  * Categorized into 6 tabs: **OPTICS**, **SENSOR**, **ARTIFACTS**, **COLOR**, **OSD**, and **AUDIO**.
  * Customizable date stamping, tape degradation severity, audio hiss, and tape motor hum.
* **Built-in Live Preview**:
  * Interactive HTML5/WebGL preview environment located in `preview/index.html` with full webcam integration.

---

## 📸 Camera Presets

| Preset | Era | Visual Characteristics |
| :--- | :--- | :--- |
| **Sony Handycam DCR-VX1000** | 1995 | MiniDV 3CCD color fidelity, interlaced scanlines, high saturation |
| **JVC GR-C1 (Back to the Future)** | 1984 | Warm analog VHS color bleed, heavy tracking noise, tape grain |
| **Panasonic NV-R33 VHS-C** | 1991 | Soft chroma resolution, noticeable RF noise, classic timestamp |
| **Canon Canovision Hi8** | 1998 | Rich contrast, mild tape flutter, broadcast-style color tuning |
| **Game Boy Camera** | 1998 | 4-level monochrome Bayer matrix dithering, 128x112 lo-fi sensor |
| **Cyber-shot DSC-P1** | 2000 | Early digital CCD bloom, 3.3MP sensor quantization, vintage JPEG artifacts |

---

## 🚀 Installation & Downloads

### Pre-built APK
Download the latest signed release directly from the [Releases](https://github.com/baanbhaba/retro-cam/releases) page or pick up `RetroCam.apk` directly from the repository root.

```bash
# Install via ADB
adb install -r RetroCam.apk
```

---

## 🛠️ Building from Source

### Prerequisites
* JDK 17 or higher
* Android SDK (API 34 / Android 14)
* Android NDK (optional for custom shader optimizations)

### Build Steps

```bash
# Clone the repository
git clone https://github.com/baanbhaba/retro-cam.git
cd retro-cam

# Build Release APK
./gradlew assembleRelease

# The resulting APK will be at:
# app/build/outputs/apk/release/app-release.apk
```

---

## 🖥️ Live Browser Preview

To test the shaders, camera selector, and UI controls in real-time on desktop/Linux without an Android device:

```bash
cd preview
python3 -m http.server 8765
```
Open `http://localhost:8765` in Google Chrome or any WebGL2-compatible browser with webcam access.

---

## 📂 Project Structure

```
retro-cam/
├── app/
│   ├── src/main/java/com/retrocam/app/
│   │   ├── camera/          # CameraX controllers & tap-to-focus pipeline
│   │   ├── gl/              # OpenGL ES shaders, textures & surface renderers
│   │   ├── model/           # Camera presets & retro effect state
│   │   └── ui/              # Compose UI: RetroCamUi, SettingsDialog, CameraSelector
│   └── build.gradle.kts     # Android application build configuration
├── preview/
│   └── index.html           # Desktop WebGL & HTML5 live test runner
├── RetroCam.apk             # Standalone release APK
└── build.gradle.kts         # Root Gradle build file
```

---

## 📜 License

Licensed under the [Apache License 2.0](LICENSE).
