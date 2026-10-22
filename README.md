<<<<<<< HEAD
<div align="center">

<img width="1200" height="475" alt="GHBanner" src="https://github.com/user-attachments/assets/0aa67016-6eaf-458a-adb2-6e31a0763ed6" />

  <h1>Built with AI Studio</h2>

  <p>The fastest path from prompt to production with Gemini.</p>

  <a href="https://aistudio.google.com/apps">Start building</a>

</div>
=======
# Lens (عدسة) - Native Android Camera Application

A native Android camera app that reproduces the identity, screens, behavior, and animations of `lens-camera.html`.

## Key Features

- **Full-Screen Live Camera Viewfinder**: Real-time CameraX preview with pinch-to-zoom (1.0x to 4.0x) and zoom slider pill.
- **Real ColorMatrix Filters**: 7 photo filters (Original, Mono, Sepia, Vivid, Cool, Warm, Fade) mathematically rendered on preview and baked into the captured JPEG image files.
- **Dual Visual Styles**:
  - **iOS Look**: Corner radius 12dp, large bold 34sp titles, inset grouped lists, segmented controls, pill toggle switches (51x31dp, green `#34c759` when on), spring animations, bottom-sheet dialogs.
  - **Material 3 Look**: Corner radius 24dp, 28sp titles, Material 3 motion (scale and fade), centered dialogs.
- **Bilingual & Instant RTL Mirroring**: Full English and Arabic (`العربية`) translations with instant dynamic layout direction mirroring.
- **Hardware Integration**:
  - Hardware torch on back camera; full-screen white flash on front camera.
  - Front camera preview is mirrored and saved photo matches preview.
  - Crop-to-preview: the saved photo is cropped to exactly what the preview shows, including current zoom level.
  - Shutter sound: synthesized 1100 Hz square wave audio pulse matching the web AudioContext oscillator.
  - Multi-interval self-timer (Off, 3s, 10s) with animated countdown overlay.
- **Photo Storage & MediaStore**:
  - Saved under `Pictures/Lens` in Android MediaStore and indexed in a local Room database (`PhotoEntity`).
  - Single and bulk download to system `Downloads` folder.
  - System share sheet integration.
  - Edge-to-edge photo viewer with swipe navigation between photos.
- **Custom Adaptive Launcher Icon**: Faithful recreation of SVG symbol `#lg` with diagonal dark gradient, amber ring `#ffb703`, and 6 rotated aperture blades.

## How to Build and Run

1. Open the project root in **Android Studio** (Ladybug / Iguana or later).
2. Allow Gradle to sync dependencies (minSdk 26, targetSdk 36, Kotlin 2.2).
3. Connect an Android device or launch an Android Emulator with Camera support.
4. Select the `app` run configuration and click **Run** (or `Shift + F10`).
5. Grant Camera permission on initial launch to start capturing photos.
>>>>>>> 26bc9ef (Initial commit: Lens Android camera application with CI/CD APK workflow)
