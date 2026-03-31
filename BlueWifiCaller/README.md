# 📡 BlueWifi Caller

> **Make voice calls over Bluetooth or WiFi Direct — no internet required.**  
> Perfect for crowded places, bike rides, warehouses, events, or anywhere cellular/internet is unavailable.

---

## 📋 Table of Contents

1. [What It Does](#what-it-does)
2. [Features](#features)
3. [Requirements](#requirements)
4. [Project Structure](#project-structure)
5. [Build on Web (No Android Studio)](#build-on-web-no-android-studio)
   - [Option A: GitHub Actions (Recommended)](#option-a-github-actions-recommended)
   - [Option B: Gitpod](#option-b-gitpod)
   - [Option C: Replit](#option-c-replit)
   - [Option D: Google Cloud Shell](#option-d-google-cloud-shell)
6. [Install the APK](#install-the-apk)
7. [How to Use](#how-to-use)
8. [Architecture](#architecture)
9. [Permissions Explained](#permissions-explained)
10. [Troubleshooting](#troubleshooting)
11. [Known Limitations](#known-limitations)

---

## What It Does

BlueWifi Caller lets two Android devices call each other using:
- **Bluetooth RFCOMM (SPP)** — up to ~10 metres, ideal for bike/helmet use  
- **WiFi Direct (P2P)** — up to ~100 metres, better for crowded venues

No SIM card, no cellular data, no Wi-Fi router, no internet connection is needed at any point.  
Voice is streamed as raw 16 kHz PCM audio over the peer-to-peer socket in real time.

---

## Features

| Feature | Detail |
|---|---|
| Bluetooth calling | RFCOMM SPP with auto-server + client roles |
| WiFi Direct calling | P2P with automatic Group Owner / Client negotiation |
| Real-time voice | 16 kHz Mono PCM, AudioRecord → socket → AudioTrack |
| Incoming call UI | Full-screen incoming call with accept / decline |
| In-call controls | Mute mic, toggle speakerphone, call duration timer |
| Foreground service | Keeps call alive when app is backgrounded |
| Device discovery | Animated radar scanner with live peer list |
| Device naming | Customisable name visible to the other person |
| Android 12+ | Uses new `BLUETOOTH_SCAN`, `BLUETOOTH_CONNECT`, `NEARBY_WIFI_DEVICES` permissions |
| Material 3 UI | Jetpack Compose with dark electric-blue theme |

---

## Requirements

| Item | Version |
|---|---|
| Android | **12 (API 31)** or higher |
| Kotlin | 1.9.10 |
| Gradle | 8.2 |
| Compile SDK | 34 |
| Java | 17 |

Both devices must have the app installed. They need Bluetooth or WiFi radios (any modern Android phone qualifies).

---

## Project Structure

```
BlueWifiCaller/
├── build.gradle                        ← Root Gradle config
├── settings.gradle                     ← Module settings
├── gradle/wrapper/
│   └── gradle-wrapper.properties       ← Gradle 8.2 wrapper
└── app/
    ├── build.gradle                    ← App dependencies
    ├── proguard-rules.pro
    └── src/main/
        ├── AndroidManifest.xml         ← Permissions + service declaration
        ├── java/com/bluewificaller/
        │   ├── BlueWifiCallerApp.kt    ← Application class, notification channels
        │   ├── MainActivity.kt         ← Entry point, runtime permission requests
        │   ├── di/
        │   │   └── AppModule.kt        ← Hilt dependency injection
        │   ├── model/
        │   │   └── Models.kt           ← Data classes: Peer, CallSession, Message
        │   ├── service/
        │   │   ├── BluetoothConnectionManager.kt  ← BT RFCOMM server+client
        │   │   ├── WifiDirectManager.kt           ← WiFi P2P + TCP socket
        │   │   ├── AudioEngine.kt                 ← AudioRecord + AudioTrack
        │   │   └── CallService.kt                 ← Foreground service, call logic
        │   ├── viewmodel/
        │   │   └── MainViewModel.kt    ← Bridges UI ↔ Service
        │   └── ui/
        │       ├── Navigation.kt       ← NavHost + auto-navigate on call state
        │       ├── theme/
        │       │   └── Theme.kt        ← Material 3 dark theme, colors, typography
        │       └── screens/
        │           ├── HomeScreen.kt       ← Landing page, connection type chooser
        │           ├── PeersScreen.kt      ← Device scanner with radar animation
        │           ├── InCallScreen.kt     ← Active call UI
        │           ├── IncomingCallScreen.kt ← Incoming call ring UI
        │           └── SettingsScreen.kt   ← Device name, about, how-it-works
        └── res/
            ├── drawable/
            │   ├── ic_call_notification.xml
            │   └── ic_launcher_foreground.xml
            ├── mipmap-*/ic_launcher*.xml
            └── values/
                ├── colors.xml
                ├── strings.xml
                └── themes.xml
```

---

## Build on Web (No Android Studio)

### Option A: GitHub Actions (Recommended)

This is the easiest zero-setup method. GitHub builds the APK in the cloud for free.

#### Step 1 — Create a GitHub repository

1. Go to [github.com](https://github.com) and sign in (create a free account if needed).
2. Click **"New repository"** → name it `BlueWifiCaller` → set to **Public** → click **Create repository**.

#### Step 2 — Upload the project files

**Method 1 — GitHub Web UI (drag & drop):**
1. On the repo page click **"uploading an existing file"**.
2. Drag the entire `BlueWifiCaller/` folder into the browser window.
3. Click **"Commit changes"**.

**Method 2 — Git CLI (if available on your machine or in cloud shell):**
```bash
git init
git remote add origin https://github.com/YOUR_USERNAME/BlueWifiCaller.git
git add .
git commit -m "Initial commit"
git push -u origin main
```

#### Step 3 — Add the GitHub Actions workflow

Create the file `.github/workflows/build.yml` in your repo (you can do this in the GitHub web editor):

```yaml
name: Build APK

on:
  push:
    branches: [ main ]
  workflow_dispatch:      # ← allows manual trigger from the UI

jobs:
  build:
    runs-on: ubuntu-latest

    steps:
      - name: Checkout code
        uses: actions/checkout@v3

      - name: Set up JDK 17
        uses: actions/setup-java@v3
        with:
          java-version: '17'
          distribution: 'temurin'

      - name: Cache Gradle packages
        uses: actions/cache@v3
        with:
          path: |
            ~/.gradle/caches
            ~/.gradle/wrapper
          key: ${{ runner.os }}-gradle-${{ hashFiles('**/*.gradle*', '**/gradle-wrapper.properties') }}

      - name: Grant execute permission for gradlew
        run: chmod +x gradlew

      - name: Build Debug APK
        run: ./gradlew assembleDebug --stacktrace

      - name: Upload APK
        uses: actions/upload-artifact@v3
        with:
          name: BlueWifiCaller-debug
          path: app/build/outputs/apk/debug/app-debug.apk
```

#### Step 4 — Trigger the build

1. Go to your repo → **Actions** tab.
2. Select **"Build APK"** → click **"Run workflow"** → **"Run workflow"** (green button).
3. Wait ~5–8 minutes for the build to complete.

#### Step 5 — Download the APK

1. Click on the finished workflow run.
2. Scroll down to **Artifacts** → click **"BlueWifiCaller-debug"**.
3. A `.zip` is downloaded — extract it to get `app-debug.apk`.

---

### Option B: Gitpod

Gitpod gives you a full VS Code IDE in the browser with a terminal.

1. Go to [gitpod.io](https://gitpod.io) and sign in with GitHub.
2. Open your GitHub repo URL prefixed with `https://gitpod.io/#` — e.g.:
   ```
   https://gitpod.io/#https://github.com/YOUR_USERNAME/BlueWifiCaller
   ```
3. Once the workspace loads, open the terminal and run:
   ```bash
   # Install Android SDK
   sudo apt-get update && sudo apt-get install -y wget unzip
   wget https://dl.google.com/android/repository/commandlinetools-linux-10406996_latest.zip
   unzip commandlinetools-linux-10406996_latest.zip -d android-sdk
   export ANDROID_HOME=$PWD/android-sdk
   export PATH=$PATH:$ANDROID_HOME/cmdline-tools/bin
   
   # Accept licenses
   yes | sdkmanager --licenses --sdk_root=$ANDROID_HOME
   sdkmanager "platforms;android-34" "build-tools;34.0.0" --sdk_root=$ANDROID_HOME
   
   # Build
   chmod +x gradlew
   ./gradlew assembleDebug
   ```
4. The APK is at `app/build/outputs/apk/debug/app-debug.apk`.
5. Right-click → **Download** to save to your computer.

---

### Option C: Replit

1. Go to [replit.com](https://replit.com) → **Create Repl** → choose **Bash** template.
2. In the Shell tab run:
   ```bash
   # Clone your repo
   git clone https://github.com/YOUR_USERNAME/BlueWifiCaller.git
   cd BlueWifiCaller
   
   # Install Java 17
   curl -s "https://get.sdkman.io" | bash
   source "$HOME/.sdkman/bin/sdkman-init.sh"
   sdk install java 17.0.9-tem
   
   # Install Android SDK
   wget -q https://dl.google.com/android/repository/commandlinetools-linux-10406996_latest.zip
   unzip -q commandlinetools-linux-10406996_latest.zip -d $HOME/android-sdk
   export ANDROID_HOME=$HOME/android-sdk
   export PATH=$PATH:$ANDROID_HOME/cmdline-tools/bin
   yes | sdkmanager --licenses --sdk_root=$ANDROID_HOME
   sdkmanager "platforms;android-34" "build-tools;34.0.0" --sdk_root=$ANDROID_HOME
   
   # Build
   chmod +x gradlew
   ./gradlew assembleDebug
   ```
3. Download from `app/build/outputs/apk/debug/app-debug.apk`.

---

### Option D: Google Cloud Shell

Google Cloud Shell is free and has Java pre-installed.

1. Go to [shell.cloud.google.com](https://shell.cloud.google.com).
2. Run these commands:
   ```bash
   # Clone your repo
   git clone https://github.com/YOUR_USERNAME/BlueWifiCaller.git
   cd BlueWifiCaller
   
   # Check Java (should be 11+; upgrade to 17 if needed)
   java -version
   
   # If Java < 17:
   sudo apt-get install -y openjdk-17-jdk
   export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64
   
   # Install Android build tools
   wget -q https://dl.google.com/android/repository/commandlinetools-linux-10406996_latest.zip
   unzip -q commandlinetools-linux-10406996_latest.zip -d $HOME/android-sdk
   export ANDROID_HOME=$HOME/android-sdk
   export PATH=$PATH:$ANDROID_HOME/cmdline-tools/bin
   yes | sdkmanager --licenses --sdk_root=$ANDROID_HOME
   sdkmanager "platforms;android-34" "build-tools;34.0.0" --sdk_root=$ANDROID_HOME
   
   chmod +x gradlew
   ./gradlew assembleDebug
   ```
3. In the Cloud Shell editor, right-click the APK at `app/build/outputs/apk/debug/app-debug.apk` → **Download**.

---

## Install the APK

### Enable Unknown Sources

1. On your Android phone go to **Settings → Security** (or **Privacy**).
2. Enable **"Install unknown apps"** — or when you tap the APK file, Android 8+ will prompt you to allow installs from Files/Chrome.

### Transfer and Install

**Via USB:**
```bash
adb install app/build/outputs/apk/debug/app-debug.apk
```

**Via Google Drive / Email:**
1. Upload `app-debug.apk` to Google Drive or send it to yourself via email/WhatsApp.
2. Open it on the phone → tap **Install**.

**Via USB file transfer:**
1. Connect phone by USB in file transfer mode.
2. Copy the APK to the phone's Downloads folder.
3. Open a file manager → navigate to Downloads → tap the APK → Install.

---

## How to Use

### First-Time Setup

1. Open the app → grant all permissions when prompted (Bluetooth, WiFi, Microphone, Location, Notifications).
2. Go to **Settings** → enter a device name (e.g. "Ravi's Bike") → tap **Save Name**.

### Making a Call

**Device A (caller):**
1. On the Home screen choose **Bluetooth** or **WiFi Direct**.
2. Tap **"Find Nearby People"**.
3. Tap **"Start Scan"** — nearby devices running the app appear in the list.
4. Tap the green ☎ call button next to the person you want to call.

**Device B (callee):**
1. The app must be open (or running as a foreground service in the background).
2. An incoming call screen appears — tap **Accept** (green) or **Decline** (red).

**During the call:**
- 🎤 Tap the mic button to mute/unmute.
- 🔊 Tap the speaker button to switch between earpiece and speakerphone.
- 📵 Tap the red hang-up button to end the call.

---

## Architecture

```
┌─────────────────────────────────────────────────────────┐
│                     UI Layer (Compose)                   │
│  HomeScreen  PeersScreen  InCallScreen  IncomingScreen   │
└───────────────────────┬─────────────────────────────────┘
                        │ StateFlow / SharedFlow
┌───────────────────────▼─────────────────────────────────┐
│                   MainViewModel (Hilt)                    │
│         Bridges UI ↔ CallService via ServiceConnection   │
└───────────────────────┬─────────────────────────────────┘
                        │ bound service
┌───────────────────────▼─────────────────────────────────┐
│                CallService (Foreground)                   │
│  Manages call lifecycle, audio routing, signalling msgs  │
└──────┬────────────────┬──────────────────────────────────┘
       │                │
┌──────▼──────┐  ┌──────▼───────────┐  ┌────────────────┐
│ Bluetooth   │  │  WifiDirect      │  │  AudioEngine   │
│ Manager     │  │  Manager         │  │  (Capture +    │
│ (RFCOMM)    │  │  (P2P + TCP)     │  │   Playback)    │
└─────────────┘  └──────────────────┘  └────────────────┘
```

### Signalling Flow

```
Caller                          Callee
  │                               │
  │──BT/WiFi connect()──────────► │
  │                               │  (server accepts socket)
  │──CALL_REQUEST ───────────────►│
  │                               │  (incoming call UI shown)
  │◄──CALL_ACCEPT ────────────── │
  │                               │
  │══ AUDIO_CHUNK (base64 PCM) ══►│
  │◄═ AUDIO_CHUNK (base64 PCM) ══ │
  │        (real-time voice)      │
  │                               │
  │──CALL_END ───────────────────►│
  │                               │  (call terminated)
```

### Audio Pipeline

```
Microphone
    ↓
AudioRecord (16kHz, Mono, PCM16)
    ↓
Base64 encode
    ↓
Message(type=AUDIO_CHUNK, payload=base64)
    ↓
JSON + newline delimiter
    ↓
RFCOMM socket / TCP socket
    ↓  (other device)
Message received, Base64 decode
    ↓
AudioTrack.write() (16kHz, Mono, PCM16)
    ↓
Earpiece / Speaker
```

---

## Permissions Explained

| Permission | Why It's Needed |
|---|---|
| `BLUETOOTH_SCAN` | Discover nearby Bluetooth devices (Android 12+) |
| `BLUETOOTH_CONNECT` | Connect to and communicate with Bluetooth devices |
| `BLUETOOTH_ADVERTISE` | Make this device discoverable |
| `ACCESS_FINE_LOCATION` | Required by Android for WiFi Direct peer discovery |
| `NEARBY_WIFI_DEVICES` | Android 13+ permission for WiFi scanning without full location |
| `RECORD_AUDIO` | Capture microphone audio for voice calls |
| `MODIFY_AUDIO_SETTINGS` | Switch between earpiece and speakerphone |
| `FOREGROUND_SERVICE` | Keep the call alive when the app is backgrounded |
| `POST_NOTIFICATIONS` | Show incoming call and in-call notification |
| `WAKE_LOCK` | Keep CPU awake during an active call |

---

## Troubleshooting

### Can't find the other device
- Both devices must have the **same connection type** selected (both Bluetooth OR both WiFi Direct).
- Ensure both devices have the app **open and foreground** while scanning.
- For Bluetooth: make sure Bluetooth is ON. Android 12+ no longer auto-enables BT; the user must enable it manually in Quick Settings.
- For WiFi Direct: ensure WiFi is ON (you don't need to be connected to a router, just have WiFi enabled).
- Try tapping **Start Scan** again — discovery has a 30-second timeout on Android.

### Call connects but no audio
- Check that microphone permission is granted: **Settings → Apps → BlueWifiCaller → Permissions → Microphone**.
- Make sure neither device is in Do Not Disturb mode.
- Try toggling the speaker button in-call.

### App crashes on launch
- Ensure you're running Android 12 (API 31) or higher.
- Reinstall the APK after fully uninstalling any previous version.

### Build fails on GitHub Actions
- Check the Actions log for the error.
- Common fix: the `gradlew` file may not have execute permission — the workflow already runs `chmod +x gradlew` but ensure the file is committed to git with the right permissions:
  ```bash
  git update-index --chmod=+x gradlew
  git commit -m "Fix gradlew permissions"
  ```

### "Bluetooth permission denied" in logs
- Android 12+ requires explicit runtime permission for `BLUETOOTH_SCAN` and `BLUETOOTH_CONNECT`.
- The app requests these on startup, but if you denied them, go to **Settings → Apps → BlueWifiCaller → Permissions** and grant them.

---

## Known Limitations

| Limitation | Reason |
|---|---|
| Audio uses base64-encoded JSON messages | Simpler cross-platform signalling; adds ~33% overhead vs raw bytes |
| No echo cancellation | Would require Android AudioEffect / WebRTC; planned for v2 |
| One-to-one calls only | Group calling requires a mesh relay; out of scope for v1 |
| Bluetooth range ~10m | Physical limitation of classic BT; BLE would be lower latency but more complex |
| WiFi Direct on some devices requires location permission | Android OS requirement, cannot be bypassed |
| App must be open or in foreground service for incoming calls | Background app restrictions on Android 12+ |

---

## License

MIT — free to use, modify, and distribute.
