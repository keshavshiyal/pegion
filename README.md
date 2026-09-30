<div align="center">

<img src="art/logo.png" alt="Pegion Logo" width="130" />

# Pegion
### Always delivers.

**A native, blazingly fast, and privacy-first Android download manager.**  
*Accelerated multi-segment engine, instant pause/resume, live URL probing, and zero telemetry.*

---

[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)
[![Platform](https://img.shields.io/badge/Platform-Android%208.0%2B%20(API%2026%2B)-3DDC84.svg?logo=android&logoColor=white)](https://android.com)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.2-7F52FF.svg?logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![Compose](https://img.shields.io/badge/Jetpack%20Compose-M3%20Expressive-4285F4.svg?logo=jetpackcompose&logoColor=white)](https://developer.android.com/jetpack/compose)
[![Build Status](https://img.shields.io/badge/CI%2FCD-Passing-brightgreen.svg?logo=githubactions&logoColor=white)](https://github.com/keshavshiyal/pegion/actions)
[![Telemetry](https://img.shields.io/badge/Telemetry-Zero%20Tracking-success.svg?logo=shield&logoColor=white)](#-privacy--security-first)

[**Features**](#-features) • [**Comparison**](#-how-pegion-compares) • [**Architecture**](#-architecture--tech-stack) • [**Download APK**](#-getting-started) • [**Build from Source**](#-building-from-source) • [**Roadmap**](#-roadmap)

</div>

---

## 🕊️ Why Pegion?

Most Android download managers today are cluttered with full-screen ads, battery-draining background trackers, and outdated user interfaces. 

**Pegion is different.** Inspired by the historic carrier pigeons that delivered vital communications across continents with steadfast reliability, Pegion is designed to be:

- ⚡ **Blazing Fast:** Multi-segment parallel acceleration downloads files up to 10× faster by splitting files into concurrent byte chunks.
- 🛡️ **100% Private & Ad-Free:** Zero telemetry, zero analytics SDKs, zero advertising. No tracking of your downloads or network requests.
- 🔋 **System Respectful:** Built with modern Android architecture (Coroutines, Room, Foreground Services with `DATA_SYNC`) to minimize CPU, memory, and battery consumption.
- 🎨 **Modern & Beautiful:** Designed from the ground up using Jetpack Compose and Material 3 Expressive guidelines, complete with true AMOLED pitch-black theming.

---

## ✨ Features

### 🚀 Turbo Multi-Segment Engine
- **Parallel Chunking:** Automatically divides downloads into 4 to 8 parallel byte ranges for servers supporting HTTP Range headers, maximizing your bandwidth.
- **Smart Single-Stream Fallback:** Gracefully degrades to single-stream downloads when servers do not support partial content (`206 Partial Content`).
- **Per-Segment Resiliency:** Each chunk handles its own retries with exponential backoff; if a single connection drops, only that chunk retries rather than the entire file.

### 🔍 Live URL Probing & Previews
- **Instant Server Inspection:** When you paste a URL, Pegion probes the server in the background to detect the real filename, total byte size, MIME type, and resume capability before you tap Download.
- **Interactive Segment Visualizer:** Live segmented progress bar showing real-time progress for each thread with expandable chunk range diagnostics.

### ⏱️ Resilient Resume & Persistence
- **Pause & Resume Anytime:** Resume interrupted downloads seamlessly across airplane mode transitions, network changes (Wi-Fi to Mobile), or device restarts.
- **Boot Recovery:** Automatically re-enqueues unfinished downloads after device reboots via `RECEIVE_BOOT_COMPLETED`.
- **Atomic Database Flush:** Room SQLite database updates are decoupled and throttled, ensuring zero IO bottlenecks and preventing database lock contention.

### 🎛️ Bandwidth & Queue Management
- **Configurable Concurrency:** Set active concurrent downloads from 1 to 5 to suit your connection.
- **Speed Limits:** Apply global bandwidth caps or fine-tune limits per individual download.
- **Network & Power Rules:** 
  - *Wi-Fi Only Mode* to protect mobile data plans.
  - *Charging Only Mode* to preserve battery on the go.
  - *Auto-Pause* on network disconnect.

### 🛡️ Privacy & Verification
- **Cryptographic Checksum Verification:** Verify downloaded file integrity against expected hashes (**SHA-256**, **MD5**, **SHA-1**) to detect corruption or tampering.
- **Scoped Storage & MediaStore:** Automatically indexes completed files with `MediaScannerConnection` and supports atomic `MediaStore.Downloads` publishing (`IS_PENDING`), ensuring files appear immediately in your Files and Gallery apps.

### 📲 System Ecosystem Integration
- **Quick Settings Tile:** Pull down your Android notification tray and tap the **Pegion Add** tile to immediately trigger a download from your clipboard.
- **Seamless Share Sheet Overlay:** Share links directly from Chrome, Firefox, YouTube, or Reddit to Pegion's lightweight translucent bottom sheet without losing your place in your active app.
- **Smart Clipboard Detection:** Automatic banner detection when a valid HTTP/HTTPS link is copied to the clipboard.

### 🎨 Material 3 Expressive UI
- **Four Distinct Themes:** System Default, Crisp Light, Deep Space Dark, and Pure Pitch AMOLED Black (0% RGB black for OLED battery savings).
- **Live Bézier Speed Charts:** Smooth real-time telemetry visualizing transfer rates and accurate ETA calculations.
- **Batch Download Dialog:** Paste multiple URLs separated by newlines to bulk-queue items with one tap.

---

## 📊 How Pegion Compares

| Feature | 🕊️ Pegion | Default Browser DM | ADM / 1DM |
| :--- | :---: | :---: | :---: |
| **Ads & Trackers** | ❌ **Zero (Clean & Private)** | ❌ None | ⚠️ Full-Screen Ads & Trackers |
| **License** | ✅ **Apache-2.0 (Open Source)** | ⚠️ Closed / Vendor | ❌ Proprietary Closed Source |
| **Multi-Segment Turbo** | ✅ **4–8 Parallel Threads** | ❌ Single Stream | ✅ Multi-Segment |
| **Live URL Header Probing** | ✅ **Automatic on Paste** | ❌ No | ⚠️ Manual Only |
| **Visual Chunk Inspector** | ✅ **Segment Block Visualizer**| ❌ No | ⚠️ Basic |
| **Checksum Verification** | ✅ **SHA-256 / MD5 / SHA-1** | ❌ No | ⚠️ Rare / Complex |
| **Quick Settings Tile** | ✅ **Yes (Clipboard Auto-detect)**| ❌ No | ❌ No |
| **Translucent Share Sheet** | ✅ **Overlay Bottom Sheet** | ❌ Switches Apps | ⚠️ Clunky Overlay |
| **Pure AMOLED Dark Mode**| ✅ **True Black (0% RGB)** | ⚠️ Dark Grey | ⚠️ Paid Feature |
| **Tech Stack** | ✅ **100% Jetpack Compose M3** | ❌ Legacy Views | ❌ Legacy Views |

---

## 🏗️ Architecture & Tech Stack

Pegion is engineered following the **Modern Android Development (MAD)** standards and Clean Architecture principles:

```
┌─────────────────────────────────────────────────────────────┐
│                      Jetpack Compose UI                     │
│  (M3 Material Palette, Expressive Motion, Bézier Charts)   │
└──────────────────────────────┬──────────────────────────────┘
                               │ Observes UI State (StateFlow)
┌──────────────────────────────▼──────────────────────────────┐
│                    ViewModel Layer                          │
│  (HomeViewModel, DetailsViewModel, SettingsViewModel)       │
└──────────────────────────────┬──────────────────────────────┘
                               │ Requests actions & queries
┌──────────────────────────────▼──────────────────────────────┐
│                   Repository Layer                          │
│        (DownloadRepository, PreferencesRepository)          │
└──────────────┬───────────────────────────────┬──────────────┘
               │                               │
┌──────────────▼──────────────┐ ┌──────────────▼──────────────┐
│   Local Persistence (Room)  │ │      DataStore (Prefs)      │
│  - Downloads SQLite Table   │ │  - Theme, Concurrency,      │
│  - Segments SQLite Table    │ │  - Speed Limits & Policies  │
└─────────────────────────────┘ └─────────────────────────────┘
               ▲
               │ Database synchronization & progress flush
┌──────────────┴──────────────────────────────────────────────┐
│               DownloadEngine (Core Turbo Engine)            │
│  - OkHttp 4 with HTTP/2 connection pooling & Keep-Alive     │
│  - Parallel segment coroutines with per-chunk retry logic    │
│  - SpeedLimiter (Thread-safe token bucket via Mutex)        │
│  - Network & Battery state monitors                         │
│  - MediaScanner & MediaStore.Downloads (Scoped Storage)     │
│  - DownloadForegroundService (DATA_SYNC notifications)      │
└─────────────────────────────────────────────────────────────┘
```

### Technology Highlights
- **UI Toolkit:** [Jetpack Compose](https://developer.android.com/jetpack/compose) with Material 3 Expressive components.
- **Core Language:** [Kotlin 2.2](https://kotlinlang.org) with structured Concurrency & Coroutines.
- **Reactive Data:** Kotlin `StateFlow` and `SharedFlow` for non-blocking reactive data streams.
- **Networking:** [OkHttp 4](https://square.github.io/okhttp/) with custom buffer allocation, range header manipulation, and probe dispatching.
- **Local Database:** [Room SQLite](https://developer.android.com/training/data-storage/room) with multi-table segment persistence.
- **Preferences:** [Jetpack DataStore](https://developer.android.com/topic/libraries/architecture/datastore) (Preferences).
- **Background Execution:** Android `ForegroundService` with `FOREGROUND_SERVICE_TYPE_DATA_SYNC` and `WorkManager`.

---

## 📥 Getting Started

### Download the Latest APK
Pre-compiled APK packages are available directly under the [**Releases**](https://github.com/keshavshiyal/pegion/releases) tab.

1. Download `pegion-release-apk.zip` (or debug build).
2. Extract and open the `.apk` on your device.
3. If prompted, allow installation from your browser or file manager.
4. Open Pegion, allow notification permissions (required for progress controls on Android 13+), and start downloading!

---

## 🛠️ Building from Source

### Prerequisites
- **JDK 17** (Temurin, Zulu, or OpenJDK)
- **Android SDK** with Platform API 36
- **Gradle 8.11+** (included via Gradle Wrapper)

### Clone & Compile

```bash
# 1. Clone the repository
git clone https://github.com/keshavshiyal/pegion.git
cd pegion

# 2. Setup environment configuration
cp .env.example .env

# 3. Build Debug APK
# Linux / macOS:
./gradlew assembleDebug

# Windows PowerShell:
.\gradlew.bat assembleDebug

# 4. Run Unit Tests
./gradlew testDebugUnitTest
```

The resulting APK will be located at:
`app/build/outputs/apk/debug/app-debug.apk`

---

## 🗺️ Roadmap

- [x] **P0:** Project restructuring, licensing, code formatting, and automated CI/CD APK generation.
- [x] **P1:** Turbo parallel multi-segment download engine with per-chunk retry and thread-safe speed limiter.
- [x] **P2:** Material 3 expressive modal bottom sheet with live URL probing, server range inspection, and chunk visualizer.
- [x] **P3:** Scoped Storage integration, `MediaStore.Downloads` indexing, Quick Settings tile, and transparent Share Target overlay.
- [ ] **P4 (Future):** Torrent / Magnet link download support.
- [ ] **P4 (Future):** Desktop / Browser companion extension for remote link dispatching.
- [ ] **P4 (Future):** F-Droid and IzzyOnDroid store packaging.

---

## 🤝 Contributing

Contributions make open-source software incredible! Any improvements, bug reports, or feature suggestions are welcome.

1. Fork the Project: `https://github.com/keshavshiyal/pegion`
2. Create your Feature Branch: `git checkout -b feat/YourFeature`
3. Commit your Changes: `git commit -m 'feat: Add YourFeature'`
4. Push to the Branch: `git push origin feat/YourFeature`
5. Open a Pull Request.

Please check [CONTRIBUTING.md](CONTRIBUTING.md) for details on code style and testing guidelines.

---

## 📄 License

Pegion is licensed under the **Apache License 2.0**. See the [LICENSE](LICENSE) file for more information.

---

<div align="center">
  <sub>Crafted with precision & passion by <a href="https://github.com/keshavshiyal">Keshav Shiyal</a>.</sub>
</div>
