name: Build APK

# Builds automatically on every push to main, and can also be triggered
# manually from the Actions tab (useful if you just want a fresh build
# without pushing a new change).
on:
  push:
    branches: [ main ]
  workflow_dispatch:

jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4

      # Piper's voice MODEL is too large for a normal browser file upload
      # (GitHub's repo-upload limit is 25MB; the model is ~65MB) — so only
      # voice.onnx lives as a GitHub Release asset and gets pulled down
      # here, right before the build. voice.onnx.json is small (a few KB
      # of settings) and is committed directly in the repo as normal —
      # nothing to fetch for that one.
      - name: Download Piper voice model
        env:
          GITHUB_TOKEN: ${{ secrets.GITHUB_TOKEN }}
        run: |
          mkdir -p app/src/main/assets/piper
          gh release download piper-voice --repo ${{ github.repository }} \
            --pattern "voice.onnx" \
            --dir app/src/main/assets/piper --clobber

      - name: Set up JDK 17
        uses: actions/setup-java@v4
        with:
          distribution: 'temurin'
          java-version: '17'

      - name: Set up Android SDK
        uses: android-actions/setup-android@v3

      - name: Set up Gradle
        uses: gradle/actions/setup-gradle@v3
        with:
          gradle-version: '8.5'

      - name: Build debug APK
        run: gradle assembleDebug --no-daemon

      # Shows up under the finished run, in the "Artifacts" section near
      # the bottom of the page — downloadable straight from a phone browser.
      - name: Upload APK
        uses: actions/upload-artifact@v4
        with:
          name: nexus-debug-apk
          path: app/build/outputs/apk/debug/app-debug.apk
