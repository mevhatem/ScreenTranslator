<div align="center">

# 🌐 Screen Translator
### Google Lens Tarzı In-Place (Yerinde) Ekran Çevirmeni

Anlık, modern, tamamen ücretsiz ve açık kaynaklı Android ekran çeviri uygulaması.  
Ekranda gördüğünüz yabancı dildeki metinlerin tam üzerine şık yarı saydam kartlar yerleştirerek anında Türkçe çevirisini gösterir.

[![Platform](https://img.shields.io/badge/Platform-Android%2014%2B%20(API%2034)-3DDC84.svg?style=for-the-badge&logo=android)](https://developer.android.com)
[![Kotlin](https://img.shields.io/badge/Language-Kotlin%202.0-7F52FF.svg?style=for-the-badge&logo=kotlin)](https://kotlinlang.org)
[![Compose](https://img.shields.io/badge/UI-Jetpack%20Compose-4285F4.svg?style=for-the-badge&logo=jetpackcompose)](https://developer.android.com/jetpack/compose)
[![ML Kit](https://img.shields.io/badge/ML%20Kit-On--Device%20OCR-EA4335.svg?style=for-the-badge&logo=google)](https://developers.google.com/ml-kit)
[![License](https://img.shields.io/badge/License-MIT-F4B400.svg?style=for-the-badge)](LICENSE)

</div>

---

## 🌟 Öne Çıkan Özellikler

* 🎯 **Google Lens Tarzı In-Place Çeviri:** Algılanan metinlerin tam koordinatlarına (`boundingBox`) yarı saydam buzlu cam kartlar yerleştirilir. Orijinal metnin tam üzerinde Türkçe karşılığı görünür.
* 🔍 **İki Parmakla Yakınlaştırma (Pinch-to-Zoom & Pan):** Küçük menüler, butonlar veya ince yazılar olduğunda ekranı iki parmağınızla **1x ile 4x arasında** büyüterek rahatça okuyabilirsiniz.
* ⚡ **Çift Dokunma (Double-Tap):** Hızlıca 2.2x yakınlaşmak veya normal boyuta dönmek için ekrana çift dokunmanız yeterlidir.
* 🧠 **Akıllı Hibrit Çeviri Motoru:**
  * **İnternet Varsa (Öncelikli):** Google'ın ana bulut yapay zeka çeviri motorunu kullanarak bağlamı, argoyu ve teknoloji terimlerini (*Gemini, Opus, Antigravity* vb.) bozmadan akıcı ve doğal Türkçe çevirir.
  * **İnternet Yoksa (Yedek):** Google ML Kit On-Device modeli devreye girerek tamamen internetsiz (çevrimdışı) çalışmaya devam eder.
* 📐 **Sıfır Piksel Kayması (Pixel-Perfect Alignment):** Durum çubuğu (Status Bar), navigasyon çubuğu ve kamera deliği (Display Cutout) kaymaları telafi edilir; kutular yazının üzerine milimetrik oturur.
* 🚀 **Statik Ekran Koruması:** Ekran tamamen hareketsiz olsa dahi hazır tutulan net kare anında **0 milisaniyede** yakalanır; zaman aşımı yaşanmaz.
* 📱 **Android 14 (API 34) Standartları:** Güncel Foreground Service `mediaProjection` politikalarına tam uyumludur.

---

## 🛠️ Mimari & Modüler Tasarım

Temiz kod ve **Clean Architecture** prensiplerine uygun mimari:

```
app/src/main/java/com/screentranslator/app/
├── data/model/
│   ├── TranslatedBlock.kt         # Koordinatlı çevrilmiş blok modeli
│   └── TranslationResult.kt       # Genel çeviri sonuç modeli
├── engine/
│   ├── ScreenCaptureEngine.kt     # MediaProjection, VirtualDisplay & 1:1 donanım netliği
│   ├── MlKitEngine.kt             # Paragraf bütünlüğü korumalı OCR & çeviri koordinatörü
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

## 🚀 Kurulum ve Çalıştırma

### Gereksinimler
* Android Studio (Ladybug / Koala / Hedgehog veya üzeri)
* JDK 17
* Android SDK 34 (Android 14)

### Projeyi Klonlama ve Derleme:
```bash
git clone https://github.com/mevhatem/ScreenTranslator.git
cd ScreenTranslator
./gradlew assembleDebug
```

Üretilen APK dosyası:
```
app/build/outputs/apk/debug/app-debug.apk
```

---

## 📲 Kullanım

1. Uygulamayı açın ve **"Diğer Uygulamaların Üzerinde Gösterme"** iznini verin.
2. **"Çevirici Baloncuğu Başlat"** butonuna basın ve ekran yakalama onayını verin.
3. Yabancı dildeki herhangi bir uygulamaya veya web sayfasına geçin.
4. Ekranda duran mavi baloncuğa **tek dokunun**:
   * Orijinal yazıların üzerine doğrudan Türkçe çevirileri yerleşecektir.
   * Küçük yazıları iki parmağınızla açarak büyütebilirsiniz.
   * Çıkmak için sağ üstteki **(X)** butonuna veya ekranda boş bir yere dokunmanız yeterlidir.

---

## 📄 Lisans

Bu proje [MIT Lisansı](LICENSE) ile lisanslanmıştır. Dilediğiniz gibi kullanabilir, katkıda bulunabilir ve geliştirebilirsiniz.

<div align="center">
Geliştirici: <b><a href="https://github.com/mevhatem">@mevhatem</a></b>
</div>
