# 🌐 Android Screen Translator (Google Lens Tarzı Yerinde Ekran Çevirmeni)

Modern, tamamen ücretsiz, **Google Lens** benzeri yerinde (in-place) ekran çevirisi yapan açık kaynaklı bir Android uygulamasıdır. Ekranda gezinirken yabancı dildeki metinlerin üzerine şık yarı saydam kartlar yerleştirerek anında Türkçe çevirisini gösterir.

![Android 14+](https://img.shields.io/badge/Platform-Android%2014%2B%20(API%2034)-brightgreen.svg)
![Kotlin](https://img.shields.io/badge/Language-Kotlin%202.0-blue.svg)
![Jetpack Compose](https://img.shields.io/badge/UI-Jetpack%20Compose%20%26%20Custom%20Canvas-4285F4.svg)
![ML Kit](https://img.shields.io/badge/OCR-Google%20ML%20Kit-orange.svg)
![License](https://img.shields.io/badge/License-MIT-purple.svg)

---

## ✨ Özellikler

* 🎯 **Google Lens Tarzı In-Place (Yerinde) Çeviri:** Algılanan metinlerin tam koordinatlarına (`boundingBox`) şık yarı saydam buzlu cam kartlar yerleştirilir; orijinal metnin tam üstünde Türkçe karşılığı görünür.
* 🔍 **İki Parmakla Yakınlaştırma (Pinch-to-Zoom & Pan):** Ekranda küçük butonlar veya menüler olduğunda iki parmağınızla **1x ile 4x arasında** istediğiniz kadar büyütebilir, ekranda kaydırarak dolaşabilirsiniz.
* ⚡ **Çift Dokunma (Double-Tap):** Çift tıklayarak anında 2.2x büyütebilir veya sıfırlayabilirsiniz.
* 🧠 **Akıllı Hibrit Çeviri Motoru:**
  * **İnternet Varsa (Öncelikli):** Google'ın ana bulut yapay zeka çeviri motorunu kullanarak bağlamı, argoyu ve teknoloji terimlerini (Gemini, Opus vb.) bozmadan akıcı Türkçe çevirir.
  * **İnternet Yoksa (Yedek):** Google ML Kit On-Device modeli devreye girerek tamamen internetsiz çeviriye devam eder.
* 💎 **Sıfır Kayma & 1:1 Donanım Netliği:** Durum çubuğu (Status Bar), navigasyon çubuğu ve kamera deliği (Display Cutout) kaymaları yok sayılır; metinler milimetrik olarak orijinal yazının üstüne oturur.
* 🚀 **Statik Ekran Koruması:** Ekranda hareket olmasa bile hazır tutulan son net kare anında 0 milisaniyede yakalanır; zaman aşımı veya boş dönme problemi yaşanmaz.
* 📱 **Android 14 (API 34) Uyumlu:** Güncel Foreground Service `mediaProjection` politikalarına ve izinlerine %100 uyumludur.

---

## 🛠️ Teknik Mimari

Proje, **Clean Architecture** prensiplerine uygun olarak modüler bir şekilde inşa edilmiştir:

```
app/src/main/java/com/screentranslator/app/
├── data/model/
│   ├── TranslatedBlock.kt         # Koordinatlı çevrilmiş blok modeli
│   └── TranslationResult.kt       # Genel çeviri sonuç modeli
├── engine/
│   ├── ScreenCaptureEngine.kt     # MediaProjection, VirtualDisplay & Stride çözümü
│   ├── MlKitEngine.kt             # OCR, paragraf birleştirme & çeviri koordinatörü
│   └── OnlineTranslatorEngine.kt  # Yüksek kaliteli Google Translate bulut motoru
├── overlay/
│   ├── FloatingBubbleManager.kt   # Sürüklenebilir kayan baloncuk yöneticisi
│   ├── LensOverlayView.kt         # Custom Canvas (Pinch-to-zoom, Pan, In-place kartlar)
│   ├── LensOverlayManager.kt      # WindowManager tam ekran katman yöneticisi
│   └── FloatingResultCardManager.kt # Alternatif alt sonuç kartı
├── service/
│   └── BubbleService.kt           # Foreground Service (MediaProjection)
└── ui/
    └── MainActivity.kt            # Jetpack Compose Modern Dashboard & İzin Akışı
```

---

## 🚀 Kurulum ve Derleme

### Gereksinimler
* Android Studio Ladybug / Koala / Hedgehog
* JDK 17
* Android SDK 34 (Android 14)

### Terminalden Derleme:
```bash
git clone https://github.com/KULLANICI_ADINIZ/ScreenTranslator.git
cd ScreenTranslator
./gradlew assembleDebug
```
Üretilen APK konumu:
`app/build/outputs/apk/debug/app-debug.apk`

---

## 📄 Lisans

Bu proje [MIT Lisansı](LICENSE) kapsamında açık kaynak olarak lisanslanmıştır.
