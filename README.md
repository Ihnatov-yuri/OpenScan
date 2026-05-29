# OpenScan — Document Scanner for Android

[![Android](https://img.shields.io/badge/Platform-Android-brightgreen.svg)](https://developer.android.com/)
[![Kotlin](https://img.shields.io/badge/Language-Kotlin-blue.svg)](https://kotlinlang.org/)
[![Jetpack Compose](https://img.shields.io/badge/UI-Jetpack%20Compose-blue.svg)](https://developer.android.com/jetpack/compose)
[![API](https://img.shields.io/badge/API-24%2B-orange.svg)](https://android-arsenal.com/api?level=24)

A clean, reliable document scanner for Android. It captures pages with the
camera, automatically finds the document edges, corrects the perspective, and
lets you **fix anything that scanned incorrectly** — adjust the crop corners,
rotate, and change the color filter — before saving a multi-page PDF.

Scanning is powered by Google's **ML Kit Document Scanner**, which runs the
capture + edge-detection + editing flow inside Google Play Services. That makes
the in-app code small and robust, and means the app itself needs **no camera
permission**.

## ✨ Features

- 📷 **Smart capture** — automatic edge detection and perspective correction
- 🛠️ **Fix bad scans** — drag the crop corners, rotate, and apply
  color / grayscale / auto-enhance filters in the built-in editor
- 🖼️ **Import from gallery** — scan an existing photo instead of using the camera
- 📄 **Multi-page PDFs** — combine up to 30 pages into one document
- 🗂️ **Library** — saved scans listed with thumbnails, page count, size and date
- 📤 **Share** — send the PDF to any app via the system share sheet
- ⬇️ **Save to Downloads** — export the PDF to the public Downloads folder
- ✏️ **Rename / Delete** — manage your saved documents
- 🔒 **Local first** — documents are stored only in the app's own storage

## 🏗️ Architecture

| Layer | What it does | Key files |
|-------|--------------|-----------|
| Entry | Hosts Compose, launches the scanner, wires callbacks | `MainActivity.kt` |
| Scanner | ML Kit Document Scanner configuration | `scanner/DocumentScanner.kt` |
| Data | Save / list / rename / delete / export documents | `data/DocumentStore.kt`, `data/ScannedDocument.kt` |
| State | `HomeUiState` + storage side effects off the main thread | `ui/HomeViewModel.kt` |
| UI | Home list, empty state, dialogs, document rows | `ui/HomeScreen.kt`, `ui/components/*` |

### Stack
- **UI:** Jetpack Compose + Material 3 (dynamic color on Android 12+)
- **Scanning:** `play-services-mlkit-document-scanner`
- **Images:** Coil for thumbnails
- **Async:** Kotlin Coroutines

## 🚀 Build & Run

Requirements: Android Studio (Koala or newer), JDK 17, Android SDK 34, a device
or emulator **with Google Play Services**.

```bash
# Build a debug APK
./gradlew assembleDebug

# Install on a connected device/emulator
./gradlew installDebug
```

The APK is produced at `app/build/outputs/apk/debug/app-debug.apk`.

> First scan: ML Kit downloads a small scanner module from Google Play the
> first time you open the scanner, so an internet connection is needed once.

## 📋 Permissions

- `WRITE_EXTERNAL_STORAGE` (Android 9 and below only) — used solely for the
  "Save to Downloads" export. On Android 10+ the export uses scoped storage and
  needs no permission. The camera is operated by Google Play Services, so the
  app declares **no `CAMERA` permission** of its own.

## 📁 Project Structure

```
app/src/main/java/com/openscan/scanner/
├── MainActivity.kt
├── scanner/DocumentScanner.kt
├── data/
│   ├── DocumentStore.kt
│   └── ScannedDocument.kt
└── ui/
    ├── HomeScreen.kt
    ├── HomeViewModel.kt
    ├── Dialogs.kt
    ├── Formatters.kt
    ├── IntentUtils.kt
    ├── components/ (DocumentRow, EmptyState)
    └── theme/
```

## 📄 License

See [LICENSE](LICENSE).
