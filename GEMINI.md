# BV Project Context

## Overview
**BV** (formerly "Bug Video") is a third-party Bilibili Android TV application designed for a better viewing experience on large screens. It is built using modern Android development practices, specifically **Jetpack Compose**.

**Key Features:**
- Optimized for Android TV (remote control navigation).
- Jetpack Compose UI (Material 3, TV Foundation).
- Multi-module architecture.
- Support for high-quality video playback (ExoPlayer/Media3, VLC, native decoders).
- Bilibili integration (Login, Home, Partition, Dynamic, Search, Danmaku).

**Note:** The project explicitly mentions it is not supported for use in mainland China, likely a disclaimer, but functionally targets Bilibili services.

## Architecture & Technology Stack

### Core Technologies
- **Language:** Kotlin
- **UI Framework:** Jetpack Compose (using `androidx.compose.tv`, Material 3).
- **Dependency Injection:** Koin (with KSP).
- **Asynchronous Processing:** Kotlin Coroutines & Flow.
- **Networking:** 
    - **Ktor** (CIO & OkHttp engines) for HTTP requests.
    - **gRPC-Kotlin** (Protobuf) for specific API interactions.
- **Image Loading:** Coil.
- **Persistence:** Room Database, DataStore (Preferences).
- **Media Playback:** 
    - AndroidX Media3 (ExoPlayer)
    - LibVLC (via `:libs:libVLC`)
    - Native decoders (AV1, FFmpeg)
    - **Danmaku:** AkDanmaku

### Module Structure
- **`:app`**: The main application module containing the UI code, view models, and feature implementations.
- **`:bili-api`**: Handles Bilibili HTTP/REST API interactions and data models.
- **`:bili-api-grpc`**: Handles Bilibili gRPC API interactions and Protobuf definitions.
- **`:bili-subtitle`**: Logic for parsing and managing subtitles.
- **`:bv-player`**: Encapsulates video player logic (Media3/VLC wrappers).
- **`:libs`**:
    - `:libs:av1Decoder`: Native AV1 decoder wrapper.
    - `:libs:ffmpegDecoder`: Native FFmpeg decoder wrapper.
    - `:libs:libVLC`: LibVLC wrapper.

## Build & Development

### Prerequisites
- **JDK:** Java 17 (Defined in `app/build.gradle.kts`).
- **Android SDK:** Configured in `gradle/libs.versions.toml` (targetSdk, compileSdk).

### Key Commands
- **Build Debug APK:**
  ```bash
  ./gradlew assembleDebug
  ```
- **Build Release APK:**
  ```bash
  ./gradlew assembleRelease
  ```
- **Run Tests:**
  ```bash
  ./gradlew test
  ```
- **Run Lint:**
  ```bash
  ./gradlew lint
  ```

### Build Variants
- **Build Types:** `debug`, `release`, `r8Test`, `alpha`.
- **Product Flavors:** 
    - `lite` (Likely reduced features or size).
    - `default` (Full feature set).

### Configuration
- **Version Catalogs:** Dependencies are managed in `gradle/libs.versions.toml`, `gradle/androidx.versions.toml`, and `gradle/gradle.versions.toml`.
- **Signing:** Configured to look for `signing.properties` in the project root. If missing, it may fall back to debug signing or skip signing for release builds depending on the task.

## Conventions
- **Code Style:** Kotlin coding conventions.
- **UI:** Declarative UI using Jetpack Compose.
- **Navigation:** Likely Compose Navigation or a custom solution adapted for TV focus handling.
- **Logging:** Uses `kotlin-logging` and `slf4j`.
