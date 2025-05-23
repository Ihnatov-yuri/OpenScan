# OpenScan - Document Scanner App

[![Android](https://img.shields.io/badge/Platform-Android-brightgreen.svg)](https://developer.android.com/)
[![Kotlin](https://img.shields.io/badge/Language-Kotlin-blue.svg)](https://kotlinlang.org/)
[![Jetpack Compose](https://img.shields.io/badge/UI-Jetpack%20Compose-blue.svg)](https://developer.android.com/jetpack/compose)
[![API](https://img.shields.io/badge/API-24%2B-orange.svg)](https://android-arsenal.com/api?level=24)

A modern, privacy-focused document scanner app for Android built with Jetpack Compose, CameraX, and OpenCV.

## 🚀 Quick Start

### Prerequisites
- Android Studio Arctic Fox or later
- Android SDK 24+ (Android 7.0)
- Gradle 8.11.1+

### Build the App

1. **Verify Build Configuration**:
   ```bash
   ./verify-build.sh
   ```

2. **Build Debug APK**:
   ```bash
   ./build.sh debug
   ```

3. **Install on Device**:
   ```bash
   ./build.sh install
   ```

4. **Build Release APK**:
   ```bash
   ./build.sh release
   ```

## ✨ Features

### 📷 Document Scanning
- **Automatic Document Detection**: Uses OpenCV for intelligent edge detection
- **Perspective Correction**: Automatically straightens skewed documents
- **Real-time Camera Preview**: Live camera view with document overlay
- **High-Quality Capture**: CameraX integration for optimal image quality

### 🎨 Image Processing
- **Color Modes**: Color, Grayscale, and Black & White options
- **Adaptive Enhancement**: CLAHE-based contrast improvement
- **Quality Control**: Adjustable compression for optimal file size

### 📄 PDF Generation
- **Multi-page PDFs**: Combine multiple scans into single documents
- **Professional Quality**: iText library for industry-standard PDFs
- **Size Optimization**: Configurable quality settings

### 🔒 Privacy & Security
- **100% Local Processing**: No data leaves your device
- **No Ads or Tracking**: Completely ad-free experience
- **Secure Storage**: Files stored in app-private directories
- **Safe Sharing**: FileProvider for secure document sharing

## 🏗️ Architecture

### Technology Stack
- **UI Framework**: Jetpack Compose with Material Design 3
- **Camera**: CameraX for modern camera functionality
- **Computer Vision**: OpenCV for document detection
- **PDF Generation**: iText 7 for professional PDF creation
- **Architecture**: MVVM pattern with Compose state management

### Project Structure
```
app/
├── src/main/java/com/openscan/scanner/
│   ├── MainActivity.kt                 # App entry point
│   ├── ui/
│   │   ├── components/                 # Reusable UI components
│   │   ├── navigation/                 # Navigation setup
│   │   ├── screens/                    # Main app screens
│   │   └── theme/                      # Material Design theme
│   ├── utils/                          # Core utilities
│   │   ├── DocumentDetector.kt         # OpenCV document detection
│   │   ├── ImageProcessor.kt           # Image enhancement
│   │   └── PdfGenerator.kt             # PDF creation
│   └── data/
│       └── repository/                 # Data management
└── src/main/res/                       # Android resources
```

## 🔧 Dependencies

### Core Libraries
- **Jetpack Compose**: Modern Android UI toolkit
- **CameraX**: Advanced camera functionality
- **OpenCV**: Computer vision and image processing
- **iText 7**: PDF generation and manipulation
- **Material Design 3**: Modern design system

### Build Configuration
- **Gradle**: 8.11.1
- **Android Gradle Plugin**: 8.7.2
- **Kotlin**: 1.9.20
- **Target SDK**: 34 (Android 14)
- **Min SDK**: 24 (Android 7.0)

## 📱 Screens

### 🏠 Home Screen
- Recent document list
- Quick access to camera
- Document management (share/delete)
- Settings and configuration

### 📷 Camera Screen
- Live camera preview
- Document detection overlay
- Flash control
- Auto-capture when document detected

### 🖼️ Document Review
- Image preview with zoom
- Color mode selection
- Quality adjustments
- Save to PDF functionality

### ⚙️ Settings Screen
- Storage management
- Quality preferences
- App information
- Privacy settings

## 🔧 Memory Management

### Optimizations
- **OpenCV Mat Cleanup**: Proper resource management prevents memory leaks
- **Bitmap Recycling**: Efficient image memory usage
- **Background Processing**: Heavy operations off main thread
- **Stream Management**: Proper file stream handling

### Performance Features
- **Lazy Loading**: Documents loaded on demand
- **Image Compression**: Configurable quality settings
- **Cache Management**: Temporary file cleanup
- **Efficient Navigation**: Compose navigation with proper state management

## 🛡️ Security Features

### Privacy Protection
- **Local-Only Processing**: No network requests
- **No Data Collection**: Zero telemetry or analytics
- **Secure File Storage**: App-private directories
- **Permission Management**: Minimal required permissions

### File Security
- **FileProvider Integration**: Secure sharing mechanism
- **Temporary File Cleanup**: Automatic cache management
- **Access Control**: Proper file permission handling

## 🚀 Build and Deploy

### Development
```bash
# Clean build
./build.sh clean

# Debug build with installation
./build.sh install

# Run tests
./build.sh test

# Code quality checks
./build.sh lint
```

### Release
```bash
# Build release APK
./build.sh release

# Full verification
./build.sh check
```

## 📊 Performance Metrics

### Expected Performance
- **App Size**: ~25-30 MB (debug), ~20-25 MB (release)
- **Memory Usage**: ~50-100 MB during processing
- **Processing Time**: 2-5 seconds for document detection
- **PDF Generation**: 1-3 seconds per page

### System Requirements
- **RAM**: 2GB+ recommended
- **Storage**: 100MB+ for app and documents
- **Camera**: Rear camera with autofocus

## 🤝 Contributing

### Development Setup
1. Clone the repository
2. Open in Android Studio
3. Run `./verify-build.sh` to check configuration
4. Build and test with `./build.sh debug`

### Code Style
- Follow Kotlin coding conventions
- Use Jetpack Compose best practices
- Implement proper error handling
- Add comprehensive documentation

## 📄 License

MIT License - see [LICENSE](LICENSE) file for details.

## 🔍 Assembly Status

✅ **Ready for Production Build**

All dependencies resolved, memory leaks fixed, and functionality verified. See [ASSEMBLY_READY.md](ASSEMBLY_READY.md) for detailed build status.

---

**Built with ❤️ for privacy-conscious users who value local document processing without ads or data collection.** 