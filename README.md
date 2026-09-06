# 🎬 StreamIMDb TV for Android TV

[![Android TV](https://img.shields.io/badge/Platform-Android%20TV-00897B?logo=android&logoColor=white)](https://github.com/hmonowar32/StreamIMDB)
[![Kotlin](https://img.shields.io/badge/Language-Kotlin-7F52FF?logo=kotlin&logoColor=white)](https://kotlinlang.org/)
[![Jetpack Compose](https://img.shields.io/badge/UI-Jetpack%20Compose%20M3-4285F4?logo=jetpackcompose&logoColor=white)](https://developer.android.com/jetpack/compose)
[![Direct APK Download](https://img.shields.io/badge/Download-StreamIMDb--TV.apk-E50914?logo=android&logoColor=white)](#-app-download-links)
[![License](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)

A high-performance **Android TV** application engineered specifically for streaming movies and TV shows from StreamIMDb on big screens with TV remote D-Pad spatial navigation, voice search, virtual cursor, ad shielding, and companion smartphone remote control.

---

## 📲 App Download Links

### 1. Direct APK Download (Android TV & Fire TV)
- **[⬇️ Download StreamIMDb-TV.apk (Latest Build)](https://ais-pre-dsjfsdvyzuxx6yaf63yfwt-560512199949.asia-southeast1.run.app/StreamIMDb-TV.apk)**
- **[📦 GitHub Releases (Latest APK)](https://github.com/hmonowar32/StreamIMDB/releases/latest/download/StreamIMDb-TV.apk)**
- **Local Repository File**: [`public/StreamIMDb-TV.apk`](public/StreamIMDb-TV.apk) or [`app/build/outputs/apk/debug/app-debug.apk`](app/build/outputs/apk/debug/app-debug.apk)

### 2. Live Web Preview & Streaming Emulator
- **[▶️ Open Live Web Stream & Emulator](https://ais-pre-dsjfsdvyzuxx6yaf63yfwt-560512199949.asia-southeast1.run.app)**

---

## 📺 How to Install on Android TV / Google TV / Fire TV

### Method 1: Downloader App on Android TV (Easiest)
1. Install the free **Downloader by AFTVnews** app from the Google Play Store on your Android TV.
2. Open Downloader and enter the direct download link:
   ```text
   https://ais-pre-dsjfsdvyzuxx6yaf63yfwt-560512199949.asia-southeast1.run.app/StreamIMDb-TV.apk
   ```
3. Once downloaded, select **Install** when prompted.

---

### Method 2: ADB Wireless Sideload (Fastest for Developers)
1. Enable **Developer Options** and **Network Debugging** on your Android TV.
2. Note your TV's IP address (e.g. `192.168.1.100`).
3. Run the following commands from your computer:
   ```bash
   # Connect to TV
   adb connect 192.168.1.100:5555

   # Install the pre-built APK
   adb install -r public/StreamIMDb-TV.apk
   ```

---

### Method 3: USB Flash Drive Sideload
1. Download [`StreamIMDb-TV.apk`](public/StreamIMDb-TV.apk) onto a USB flash drive.
2. Plug the USB drive into your Android TV or TV Box.
3. Open any file manager (e.g., *FX File Explorer* or *File Commander*) on your TV and click `StreamIMDb-TV.apk` to install.

---

### Method 4: Built-in Phone Companion Local Server
1. Launch StreamIMDb on your TV.
2. Click the **Phone Remote** icon in the TV top header.
3. Open the displayed URL (`http://<tv-ip>:8088`) in your phone or PC browser.
4. Click **📲 Download TV App (StreamIMDb-TV.apk)** to download the APK directly over your home Wi-Fi network.

---

## ✨ Features & Capabilities

- 🎤 **Android TV Voice Search**: Press the microphone button on your TV remote or tap the HUD mic button to search for movie titles using speech recognition.
- 🎯 **Dual Navigation Modes**:
  - **D-Pad Spatial Mode**: Optimized for standard TV remote arrows with smart DOM focus highlighting.
  - **Virtual Mouse Pointer Mode**: Floating on-screen cursor for interacting with web players and buttons that require precise clicking.
- 📱 **Mobile Phone Web Remote**: Built-in HTTP server (`:8088`) and low-latency WebSocket server (`:8089`) allowing any phone on your local Wi-Fi to act as a responsive touch controller, trackpad, and keyboard.
- 🔊 **TV Audio & Volume Controls**: Adjust TV volume, mute/unmute, and fast-forward or rewind video from the phone companion or remote shortcuts.
- 🛡️ **Built-in Ad & Pop-up Shield**: WebResourceRequest filter that blocks intrusive advertising pop-ups, redirects, and third-party tracking scripts before they load.
- 🎬 **4K Fullscreen Video Player**: Integrated `WebChromeClient` with custom video view holder for smooth 4K/1080p HTML5 playback.
- 📑 **Watch History & Bookmarks**: Keep track of watched movies and save favorites locally using Room & DataStore persistence.
- ⚡ **Android 12+ SplashScreen API**: Instant branded cinema splash launch screen.

---

## 🛠️ Building from Source

```bash
# Clone the repository
git clone https://github.com/hmonowar32/StreamIMDB.git
cd StreamIMDB

# Build the Debug APK
gradle assembleDebug

# The compiled APK is generated at:
# app/build/outputs/apk/debug/app-debug.apk
```

---

## 📄 License
This project is open-source under the [MIT License](LICENSE).
