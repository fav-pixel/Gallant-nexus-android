# Gallant Nexus Android

A lightweight Android host shell for the Gallant ecosystem. The application is built with Kotlin and WebView, with a small native bridge for permissions, downloads, notifications, and file selection.

## Features

- Direct connection to the Gallant ecosystem.
- Responsive ecosystem UI inside a native Android WebView host.
- Native microphone permission handoff for browser-based voice input.
- Native file picker support for PDF, image, code, and other attachments.
- Download handling through Android storage APIs.
- Android notification integration.
- Triskelion-inspired NEXUS loading identity.
- Lightweight runtime with no bundled speech engine or voice model.

## Tech stack

- **Language:** Kotlin
- **Platform:** Android, minimum SDK 24, target SDK 34
- **Build system:** Gradle
- **Host surface:** Android WebView
- **Native bridge:** Kotlin permission, file, download, and notification handlers
- **CI/CD:** GitHub Actions workflow, when configured in the repository
- **Architecture:** Lightweight host shell

## System requirements

- Android Studio 2023.1 or newer.
- Android SDK 34.
- Kotlin 1.8 or newer.
- Gradle 8.0 or newer.
- Android device or emulator running API 24 or newer.
- Approximately 100 MB of free storage for the host application, excluding WebView cache.

## Quick start

```bash
git clone https://github.com/fav-pixel/Gallant-nexus-android.git
cd Gallant-nexus-android
```

Open the project in Android Studio, allow Gradle synchronization to complete, and run the `app` configuration on an Android device or emulator.

## Building

```bash
# Debug APK
./gradlew assembleDebug

# Release APK; signing configuration is required for distribution
./gradlew assembleRelease

# Install the debug APK on a connected device
./gradlew installDebug
```

The archive supplied for this source snapshot does not include a Gradle wrapper. If `./gradlew` is unavailable, open the project in Android Studio or run the build with a locally installed Gradle 8.x distribution.

## Project structure

```text
Gallant-nexus-android/
├── app/
│   └── src/main/
│       ├── java/com/favpixel/nexus/
│       │   ├── MainActivity.kt
│       │   ├── AndroidDownloader.kt
│       │   └── AndroidNotifications.kt
│       ├── res/
│       │   ├── drawable/
│       │   ├── layout/
│       │   └── values/
│       └── assets/                 # Lightweight app assets only
├── build.gradle
├── settings.gradle
└── README.md
```

## Native host bridge

`MainActivity` owns the WebView lifecycle and loading overlay. It also provides the Android-specific capabilities that a WebView cannot perform alone:

- Microphone permission requests for browser `getUserMedia` flows.
- File chooser integration for `<input type="file">`.
- Download routing through Android storage APIs.
- Native notification requests.
- Back-navigation handling and WebView state restoration.

The WebView remains responsible for the ecosystem application surface. No local speech runtime, voice model, or text-to-speech engine is bundled in this package.

## Triskelion loading identity

The Android loading overlay uses `app/src/main/res/drawable/nexus_splash.png`, a compact Triskelion-inspired NEXUS mark with a central gateway core and three radial routes. It is intentionally kept as a small local drawable so the loading state works without a network request.

## Testing

Use Android Studio or Gradle to run unit and instrumented tests when those test targets are present:

```bash
./gradlew test
./gradlew connectedAndroidTest
```

Manual verification should cover WebView loading, the Triskelion loading overlay, back navigation, microphone permission flow, file selection, downloads, and notifications on a physical device or emulator.

## Security notes

The native bridge should expose only the capabilities required by the hosted application. Permission requests must be granted only after checking the requested resource, file callbacks must be cleared on cancellation, and sensitive credentials must remain in the hosted application's secure configuration rather than in Android resources.

## Contributing

Keep Kotlin files focused, document native bridge changes, test on the minimum supported API level, and avoid adding large bundled runtimes to the host shell unless the feature requires them and their device cost has been evaluated.

## License and author

This project is part of the Gallant ecosystem.

**Author:** [fav-pixel](https://github.com/fav-pixel)

**Repository:** [Gallant-nexus-android](https://github.com/fav-pixel/Gallant-nexus-android)

**Last updated:** August 2026
