# 🚀 Gallant Nexus Android

A cutting-edge Android application that brings the Gallant ecosystem to your mobile device. Built with Kotlin and featuring advanced voice synthesis capabilities using Piper ONNX models.

## ✨ Features

- **Voice Synthesis** - Piper ONNX-based voice synthesis with multiple voice options
- **Ecosystem Integration** - Direct connection to the Gallant ecosystem
- **Native Performance** - Built entirely in Kotlin for optimal speed
- **Modern UI** - Material Design 3 compliant interface
- **Offline Capable** - Download voices for offline use
- **Multi-Voice Support** - Choose from various voice personalities
- **Fast Inference** - Optimized ONNX runtime for smooth speech
- **User-Friendly** - Intuitive navigation and controls

## 🏗️ Tech Stack

- **Language:** Kotlin
- **Platform:** Android (API 21+)
- **Build System:** Gradle
- **CI/CD:** GitHub Actions (build-apk.yml)
- **Voice Engine:** Piper ONNX Models
- **Runtime:** ONNX Runtime for Android
- **Architecture:** MVVM (recommended)

## 📋 System Requirements

- **Minimum SDK:** Android 5.0 (API 21)
- **Target SDK:** Android 14+ (API 34+)
- **RAM:** 2GB minimum (4GB+ recommended)
- **Storage:** 500MB+ for app + voice models
- **Processor:** ARM or x86

## 🎤 Voice Models

This project uses **Piper** - a high-quality, on-device text-to-speech engine.

### Available Voice Models
- Multiple voices with different personalities
- Stored in `app/src/main/assets/piper/`
- Download from [Release Pages](https://github.com/fav-pixel/Gallant-nexus-android/releases)
- Each model: ~65MB
- Supports multiple languages

## 🚀 Quick Start

### Prerequisites
- Android Studio (2023.1+)
- Android SDK 21+
- Kotlin 1.8+
- Gradle 8.0+
- Git

### Installation

```bash
# Clone the repository
git clone https://github.com/fav-pixel/Gallant-nexus-android.git

# Navigate to directory
cd Gallant-nexus-android

# Open in Android Studio
# File → Open → Select this directory
```

### Setting Up Voice Files

1. **Download voice models** from [Releases](https://github.com/fav-pixel/Gallant-nexus-android/releases)
2. **Create directory:**
   ```bash
   mkdir -p app/src/main/assets/piper/
   ```
3. **Place voice files** in `app/src/main/assets/piper/`
4. **Verify structure:**
   ```
   app/src/main/assets/piper/
   ├── voice.onnx
   └── voice_config.json
   ```

### Building

```bash
# Debug APK
./gradlew assembleDebug

# Release APK (requires signing)
./gradlew assembleRelease

# Install on device
./gradlew installDebug
```

### Running on Emulator/Device

```bash
# Run directly
./gradlew run

# Or use Android Studio
# Device → Select device → Run
```

## 📁 Project Structure

```
Gallant-nexus-android/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/
│   │   │   │   └── com/gallant/nexus/
│   │   │   │       ├── MainActivity.kt
│   │   │   │       ├── ui/
│   │   │   │       ├── viewmodel/
│   │   │   │       └── repository/
│   │   │   ├── res/
│   │   │   │   ├── layout/
│   │   │   │   ├── drawable/
│   │   │   │   └── values/
│   │   │   └── assets/
│   │   │       └── piper/        # Voice models
│   │   ├── test/
│   │   └── androidTest/
│   ├── build.gradle.kts
│   └── proguard-rules.pro
├── .github/
│   └── workflows/
│       └── build-apk.yml         # Automated APK building
├── gradle/
├── build.gradle.kts
├── settings.gradle.kts
└── README.md
```

## 🎮 Core Components

### MainActivity
- App entry point
- Handles initialization
- Manages navigation

### Voice Engine
- Piper ONNX initialization
- Model loading
- Text-to-speech conversion
- Audio output handling

### UI Screens
- Home screen
- Voice selection
- Text input
- Playback controls
- Settings

### Data Layer
- Local storage (preferences)
- Model management
- Cache handling

## 🔄 CI/CD Pipeline

### GitHub Actions Workflow
Located in `.github/workflows/build-apk.yml`:

```yaml
- Triggers on: push, pull_request
- Runs: Gradle build
- Produces: Debug & Release APKs
- Artifacts: Available for download
```

### Building via Actions
1. Push to repository
2. GitHub Actions automatically builds
3. APKs available in workflow artifacts
4. Download and test on devices

## 🎤 Piper Integration Guide

### Initialize Piper
```kotlin
val piperEngine = PiperEngine(context)
piperEngine.initialize("path/to/model.onnx")
```

### Synthesize Speech
```kotlin
val audio = piperEngine.synthesize("Hello, world!")
audioPlayer.play(audio)
```

### Load Custom Voice
```kotlin
piperEngine.loadVoice(
    modelPath = "assets/piper/custom_voice.onnx",
    configPath = "assets/piper/custom_voice_config.json"
)
```

## 📊 Performance Optimization

### Memory Management
- Stream audio instead of loading all
- Lazy load voice models
- Cache frequently used voices

### Battery Optimization
- Use efficient ONNX operations
- Minimize audio processing
- Background task management

### File Size
- Modular voice models (download on demand)
- ProGuard obfuscation and shrinking
- Asset compression

## 🧪 Testing

### Unit Tests
```bash
./gradlew test
```

### Instrumented Tests
```bash
./gradlew connectedAndroidTest
```

### Manual Testing
- Test on multiple Android versions
- Test with different voice models
- Test offline functionality
- Test memory usage

## 🔐 Security

- Sensitive data in encrypted shared preferences
- API key management via BuildConfig
- ProGuard code obfuscation
- Secure asset loading

## 📦 Dependencies

Key libraries (see `build.gradle.kts`):
- Kotlin Coroutines
- Android Jetpack (ViewModel, LiveData)
- Material Design 3
- ONNX Runtime
- Audio framework

## 🤝 Contributing

Contributions welcome! Please:
1. Fork the repository
2. Create a feature branch
3. Make meaningful commits
4. Add tests for new features
5. Submit a Pull Request

### Code Style
- Follow Kotlin conventions
- Use proper naming
- Add documentation
- Keep files focused

## 📝 License

This project is part of the Gallant ecosystem.

## 👤 Author

**fav-pixel** - [GitHub Profile](https://github.com/fav-pixel)

## 📞 Support & Issues

For bugs, features, or questions:
- [GitHub Issues](https://github.com/fav-pixel/Gallant-nexus-android/issues)
- Check existing issues first
- Provide device/Android version info
- Include error logs

## 🚀 Roadmap

- [ ] Multi-language support
- [ ] Voice cloning capabilities
- [ ] Real-time speech recognition
- [ ] Audio effects
- [ ] Cloud sync
- [ ] Widget support
- [ ] Accessibility features
- [ ] Performance dashboard

## 💡 Tips & Tricks

### First Run
1. App downloads voice model on first launch
2. ~2 minutes for initial setup
3. Subsequent uses are instant
4. Works offline after download

### Battery Saving
- Use shorter texts
- Disable auto-play
- Close app when done
- Clear cache periodically

### Better Voice Quality
- Adjust speech rate in settings
- Use appropriate voices for content
- Test different models
- Check audio output quality

---

**Last Updated:** August 2026
**Status:** Active Development 🚀
**Platform:** Android 5.0+
**Latest Release:** [View Releases](https://github.com/fav-pixel/Gallant-nexus-android/releases)
