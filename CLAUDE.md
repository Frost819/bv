# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

BV is a third-party Bilibili Android TV application built with Jetpack Compose. It targets Android 5.0+ (minSdk 21, compileSdk/targetSdk 36) and is optimized for TV (Leanback) environments. The app is a fork with custom UI/UX enhancements over the original `aaa1115910/bv` repo.

**Language**: Kotlin (2.1.21), **Java target**: JDK 21, **Build system**: Gradle with Kotlin DSL and version catalogs.

## Build Commands

```bash
# Debug build (default flavor, universal APK)
./gradlew assembleDefaultDebug

# Release build
./gradlew assembleDefaultRelease

# Lite build (excludes VLC native libs, smaller APK)
./gradlew assembleLiteDebug

# Run unit tests
./gradlew test

# Run a single test class
./gradlew :app:test --tests "dev.aaa1115910.bv.GithubApiTest"

# Clean build
./gradlew clean
```

**Note**: Firebase/Google Services integration is conditional — builds work without `google-services.json`, but Firebase features (Crashlytics, Analytics) will be disabled.

**Note**: Release builds require `signing.properties` at the project root pointing to keystore files.

## Module Architecture

```
app/              → Main Android TV app (Compose UI, ViewModels, Room DB, Koin DI)
bili-api/          → Bilibili API client (Ktor HTTP, gRPC, JSoup scraping) — Kotlin JVM library
bili-api-grpc/     → Protocol Buffer definitions and generated gRPC stubs
bili-subtitle/     → Subtitle parsing library — Kotlin JVM library
bv-player/         → Custom video player UI component (Media3 + Compose) — Android library
libs/              → Pre-compiled native libraries (AV1, FFmpeg, VLC, Media3 container) — git submodule
buildSrc/          → Build configuration (AppConfiguration.kt for version/SDK, ProtobufConfiguration.kt)
```

**Dependency flow**: `app` → `bili-api`, `bili-subtitle`, `bv-player`; `bili-api` → `bili-api-grpc`

## Key Architecture Patterns

- **MVVM**: ViewModels in `app/src/main/kotlin/dev/aaa1115910/bv/viewmodel/`, screens in `screen/`
- **Dependency Injection**: Koin with KSP annotations (`@Single`, `@Factory`, etc.)
- **Database**: Room with entities in `entity/`, DAOs in `dao/`, schemas exported to `app/schemas/`
- **Networking**: Ktor client (OkHttp engine) for HTTP; gRPC for some Bilibili APIs; JSoup for web scraping
- **Video playback**: Dual-engine support — Media3 (ExoPlayer) and VLC, with custom decoders (AV1, FFmpeg)
- **TV navigation**: Custom focus management via `FocusGroup`, D-pad handling, Leanback integration

## Build Variants

**Flavors** (`channel` dimension):
- `default` — Full build with all features including VLC
- `lite` — Excludes VLC native libraries for smaller APK size

**Build types**: `debug`, `release`, `r8Test` (R8 minification testing), `alpha`

## Version Management

Version code is derived from git commit count (`git rev-list --count HEAD`). Version name format: `{major}.{minor}.{patch}.r{versionCode}.{shortCommitHash}`. Configured in `buildSrc/src/main/kotlin/AppConfiguration.kt`.

The `applicationId` is `dev.frost819.bv` (changed from original `dev.aaa1115910.bv` to bypass Xiaomi TV blocking). The namespace remains `dev.aaa1115910.bv`.

## Dependency Management

Dependencies are managed via Gradle version catalogs:
- `gradle/gradle.versions.toml` — Kotlin, AGP, Firebase, third-party libs
- `gradle/androidx.versions.toml` — AndroidX libraries (Compose, Media3, Room, etc.)

## Important Conventions

- Code and comments are a mix of Chinese and English
- ProGuard rules are in `app/proguard-rules.pro` — must be updated when adding libraries that use reflection
- Compose compiler stability config is in `app/compose_compiler_config.conf`
- APK naming: `BV_{versionCode}_{versionName}.{buildType}_{flavor}_{abi}.apk`
