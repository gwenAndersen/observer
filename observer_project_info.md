# Observer Project Information

This document outlines the compilation mechanism and key configuration details for the `observer` Android project, intended to inform an AI agent.

## 1. Compilation Mechanism

The `observer` project is a standard Android Gradle project. It is compiled using the Gradle wrapper script (`gradlew`).

To build a debug version of the application, execute the following command from within the `observer/` directory:

```bash
./gradlew assembleDebug
```

Alternatively, for a full build including tests and other checks, use:

```bash
./gradlew build
```

The compiled APK file will typically be found in `observer/app/build/outputs/apk/debug/`.

## 2. Java, Gradle, and Android Configuration

Here are the key configuration details for the project:

*   **Android Gradle Plugin (AGP) Version:** `8.12.1`
    *   (Found in top-level `observer/build.gradle`)
*   **Gradle Version:** `8.13`
    *   (Found in `observer/gradle/wrapper/gradle-wrapper.properties`)
*   **Kotlin Gradle Plugin Version:** `2.0.0`
    *   (Found in top-level `observer/build.gradle`)
*   **Compile SDK Version:** `34` (Android 14)
    *   (Found in `observer/app/build.gradle`)
*   **Min SDK Version:** `26`
    *   (Found in `observer/app/build.gradle`)
*   **Target SDK Version:** `34` (Android 14)
    *   (Found in `observer/app/build.gradle`)
*   **Java Compatibility:** `Java 8`
    *   (`JavaVersion.VERSION_1_8` for source and target compatibility, and `jvmTarget = '1.8'`)
    *   (Found in `observer/app/build.gradle`)

This configuration indicates a modern Android project using Kotlin and Jetpack Compose, built with Gradle 8.13 and AGP 8.12.1, targeting Android 14 (API 34) and compatible with Java 8.

## 3. Android Asset Packaging Tool (AAPT2)

The "Android Asset Packaging Tool" (`aapt` or `aapt2`) is a crucial component of the Android build system. It is responsible for compiling application resources (like XML layouts, drawables, and AndroidManifest.xml) into a binary format and packaging them into the final APK.

*   **Automatic Management:** `aapt2` is not directly configured but is automatically managed by the Android Gradle Plugin (AGP).
*   **Version Dependency:** The specific version of `aapt2` used is determined by the AGP version. For this project, AGP `8.12.1` implicitly provides and utilizes a compatible `aapt2` version.
*   **Default Configuration:** The `observer/app/build.gradle` file does not contain any custom `aaptOptions` blocks. Therefore, `aapt2` operates with its default settings as provided by AGP `8.12.1`.