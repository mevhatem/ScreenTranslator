<div align="center">

# 🌐 Screen Translator
### Google Lens-Style In-Place Screen Translator for Android

A modern, fast, completely free and open-source Android screen translation tool.  
Translates foreign text on your screen in real time by placing elegant, translucent in-place cards directly over the original text.

[![Platform](https://img.shields.io/badge/Platform-Android%2014%2B%20(API%2034)-3DDC84.svg?style=for-the-badge&logo=android)](https://developer.android.com)
[![Kotlin](https://img.shields.io/badge/Language-Kotlin%202.0-7F52FF.svg?style=for-the-badge&logo=kotlin)](https://kotlinlang.org)
[![Compose](https://img.shields.io/badge/UI-Jetpack%20Compose-4285F4.svg?style=for-the-badge&logo=jetpackcompose)](https://developer.android.com/jetpack/compose)
[![ML Kit](https://img.shields.io/badge/ML%20Kit-On--Device%20OCR-EA4335.svg?style=for-the-badge&logo=google)](https://developers.google.com/ml-kit)
[![License](https://img.shields.io/badge/License-MIT-F4B400.svg?style=for-the-badge)](LICENSE)

</div>

---

## 🌟 Key Features

* 🎯 **Google Lens-Style In-Place Overlay:** Translates text right where it is on the screen (`boundingBox`). Semi-transparent frosted-glass cards appear directly over the original text with accurate translations.
* 🔍 **Pinch-to-Zoom & Pan:** Magnify small menus, tiny buttons, or labels from **1x up to 4x** using standard two-finger pinch gestures, and freely drag/pan across the screen.
* ⚡ **Double-Tap Quick Zoom:** Double-tap anywhere to instantly zoom into 2.2x or reset back to default.
* 🧠 **Smart Hybrid Translation Engine:**
  * **Online (Cloud AI - Primary):** Automatically routes full sentences through Google Cloud Translation, maintaining conversational context and technical terminology without awkward literal translations.
  * **Offline (On-Device ML Kit - Fallback):** When no internet connection is available, silently falls back to on-device ML Kit models for uninterrupted offline translation.
* 📐 **Pixel-Perfect Alignment:** Zero coordinate drift. Compensates for Status Bar, Navigation Bar, and camera display cutouts (notches) so translation overlays sit precisely on target pixels.
* 🚀 **Zero-Timeout Frame Capture:** Maintains a hot, 1:1 hardware buffer in memory. Works instantaneously (0ms) even on completely static screens where no frame animations are happening.
* 📱 **Android 14 (API 34) Ready:** Fully compliant with Android 14 `mediaProjection` Foreground Service requirements and system alert window permissions.

---

## 🛠️ Architecture & Tech Stack

Engineered following **Clean Architecture** and modular programming principles:

```
app/src/main/java/com/screentranslator/app/
├── data/model/
│   ├── TranslatedBlock.kt         # Coordinate-aware translated block model
│   └── TranslationResult.kt       # Unified translation result model
├── engine/
│   ├── ScreenCaptureEngine.kt     # MediaProjection, VirtualDisplay & 1:1 hardware resolution
│   ├── MlKitEngine.kt             # Context-preserving OCR & hybrid translation coordinator
│   └── OnlineTranslatorEngine.kt  # High-quality cloud translation engine
├── overlay/
│   ├── FloatingBubbleManager.kt   # Draggable floating action bubble
│   ├── LensOverlayView.kt         # Custom Canvas (Pinch-to-zoom, Pan, In-place cards)
│   ├── LensOverlayManager.kt      # WindowManager full-screen overlay layer
│   └── FloatingResultCardManager.kt # Dismissable bottom result card
├── service/
│   └── BubbleService.kt           # Foreground Service (MediaProjection)
└── ui/
    └── MainActivity.kt            # Modern Jetpack Compose dashboard & permission flow
```

---

## 🚀 Getting Started

### Prerequisites
* Android Studio (Ladybug / Koala / Hedgehog or newer)
* JDK 17
* Android SDK 34 (Android 14)

### Clone & Build
```bash
git clone https://github.com/mevhatem/ScreenTranslator.git
cd ScreenTranslator
./gradlew assembleDebug
```

Built APK location:
```
app/build/outputs/apk/debug/app-debug.apk
```

---

## 📲 How to Use

1. Launch the app and grant the **Display over other apps** permission.
2. Tap **"Start Translation Bubble"** and confirm the screen capture prompt.
3. Switch to any app, game, or web page containing foreign text.
4. Tap the **floating blue bubble**:
   * Translated cards will overlay directly onto original texts.
   * Pinch with two fingers to zoom into small or dense fonts.
   * Tap the **(X)** button in the top-right corner or tap any empty area to dismiss.

---

## 📄 License

This project is licensed under the [MIT License](LICENSE). Feel free to use, modify, and distribute.

<div align="center">
Maintained with ❤️ by <b><a href="https://github.com/mevhatem">@mevhatem</a></b>
</div>
