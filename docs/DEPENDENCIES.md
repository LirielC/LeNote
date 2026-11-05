# LeNote Dependencies and Licenses (`v1.0.3`)

This document records the direct dependencies used by **LeNote**, their roles in the application, the versions declared in [`gradle/libs.versions.toml`](../gradle/libs.versions.toml) and [`app/build.gradle.kts`](../app/build.gradle.kts), and the licensing considerations relevant to distribution.

The inventory describes the `full` application build used for the official LeNote APK.

---

## Contents

1. [Licensing Summary](#1-licensing-summary)
2. [Production Dependencies](#2-production-dependencies)
3. [Important Transitive Dependencies](#3-important-transitive-dependencies)
4. [Test-only Dependencies](#4-test-only-dependencies)
5. [Build Plugins](#5-build-plugins)
6. [Verification Notes](#6-verification-notes)

---

## 1. Licensing Summary

LeNote's original source code is distributed under the [MIT License](../LICENSE). Third-party components retain their own licenses and notices.

Most of the Android UI, storage, and inking stack is open source under Apache License 2.0. The production build also includes **Google ML Kit Digital Ink Recognition**, which is governed by Google's ML Kit and API terms rather than being an entirely open-source component.

The ML Kit component is used for on-device handwriting recognition, search indexing, and handwriting beautification. Its language model may be downloaded once for the selected language; recognition then runs locally on the device.

> **Distribution note:** This dependency inventory describes the official LeNote build. It does not claim that the APK is suitable for repositories requiring an entirely FOSS dependency set.

---

## 2. Production Dependencies

Versions below are the direct declarations in the version catalog and app module. Transitive resolution may select newer compatible versions where Gradle dependency constraints require it.

| Component | Maven artifact | Declared version | Role in LeNote | License |
|---|---|---:|---|---|
| Jetpack Ink — Authoring | `androidx.ink:ink-authoring` | `1.0.0` | Low-latency rendering of in-progress strokes through `InProgressStrokesView`. | Apache-2.0 |
| Jetpack Ink — Brush | `androidx.ink:ink-brush` | `1.0.0` | Pressure-sensitive pen and highlighter brush definitions. | Apache-2.0 |
| Jetpack Ink — Geometry | `androidx.ink:ink-geometry` | `1.0.0` | 2D primitives, transforms, bounds, and geometric intersections. | Apache-2.0 |
| Jetpack Ink — Rendering | `androidx.ink:ink-rendering` | `1.0.0` | Vector stroke rendering for the Android canvas and exports. | Apache-2.0 |
| Jetpack Ink — Strokes | `androidx.ink:ink-strokes` | `1.0.0` | Data structures for finalized strokes and input batches. | Apache-2.0 |
| Jetpack Ink — Storage | `androidx.ink:ink-storage` | `1.0.0` | Compact binary serialization of stroke input batches. | Apache-2.0 |
| Jetpack Compose BOM | `androidx.compose:compose-bom` | `2026.03.01` | Keeps Compose modules aligned to one release set. | Apache-2.0 |
| Compose UI and Graphics | `androidx.compose.ui:ui`<br>`androidx.compose.ui:ui-graphics`<br>`androidx.compose.ui:ui-tooling-preview` | BOM-managed | Declarative UI, custom canvas surfaces, previews, and the LeNote design system. | Apache-2.0 |
| Compose Material 3 | `androidx.compose.material3:material3` | BOM-managed | Material components, dialogs, sliders, and surfaces. | Apache-2.0 |
| AndroidX Activity Compose | `androidx.activity:activity-compose` | `1.13.0` | Connects `ComponentActivity`, activity results, and Compose. | Apache-2.0 |
| AndroidX Activity KTX | `androidx.activity:activity-ktx` | `1.13.0` | Kotlin extensions for activity lifecycle and back navigation. | Apache-2.0 |
| AndroidX Core KTX | `androidx.core:core-ktx` | `1.15.0` | Core Android utilities and secure `FileProvider` sharing. | Apache-2.0 |
| AndroidX AppCompat | `androidx.appcompat:appcompat` | `1.7.1` | Compatibility resources and Android themes. | Apache-2.0 |
| Material Components | `com.google.android.material:material` | `1.14.0` | Base Material themes and Android views. | Apache-2.0 |
| Google ML Kit Digital Ink | `com.google.mlkit:digital-ink-recognition` | `19.0.0` | Handwriting recognition for FTS5 search and handwriting-to-font conversion. | Google ML Kit/API terms |

---

## 3. Important Transitive Dependencies

The ML Kit dependency brings additional runtime components into the production APK. The exact resolved graph should be checked with Gradle for a specific build.

| Component family | Purpose | Distribution impact |
|---|---|---|
| `com.google.mlkit:common` and Digital Ink native libraries | Model management, local model storage, JNI integration, and `libdigitalink.so`. | Google ML Kit terms; supports model downloads. |
| `com.google.android.gms:play-services-base` and `tasks` | Google Play Services task and API infrastructure. | Google Play Services components are included in the runtime graph. |
| `com.google.firebase:firebase-components` | Internal component registration used by ML Kit. | Transitive runtime component; license notices must be retained. |
| `com.google.android.datatransport:*` | Transport and scheduling infrastructure used by SDK diagnostics. | Transitive runtime code from the Google SDK stack. |
| `androidx.work:work-runtime` | Background scheduling for model downloads and constrained work. | Apache-2.0; may add scheduling services and related permissions. |
| `androidx.ink` native libraries | Native geometry, mesh, and rendering support. | Apache-2.0; packaged for the ABIs supported by the build. |

The merged manifest and final APK are the authoritative sources for the permissions and native libraries present in a particular artifact.

---

## 4. Test-only Dependencies

These dependencies are used only by `test`, `androidTest`, or `debug` source sets and are not included in the production Release APK.

| Component | Maven artifact | Configuration | Version | Purpose | License |
|---|---|---|---:|---|---|
| JUnit 4 | `junit:junit` | `testImplementation` | `4.13.2` | JVM unit tests for storage, erasing, smoothing, lasso selection, and handwriting refinement. | EPL-1.0 |
| JSON.org | `org.json:json` | `testImplementation` | `20240303` | JVM serialization tests using `JSONObject` and `JSONArray`. | JSON License |
| AndroidX Test JUnit | `androidx.test.ext:junit` | `androidTestImplementation` | `1.3.0` | Instrumented test runner and assertions. | Apache-2.0 |
| Espresso Core | `androidx.test.espresso:espresso-core` | `androidTestImplementation` | `3.7.0` | UI synchronization and interaction testing. | Apache-2.0 |
| Compose UI Tooling | `androidx.compose.ui:ui-tooling` | `debugImplementation` | BOM-managed | Layout inspection and `@Preview` support for Debug builds. | Apache-2.0 |

---

## 5. Build Plugins

| Tool | ID or distribution | Version | License |
|---|---|---:|---|
| Android Gradle Plugin | `com.android.application` | `9.2.1` | Apache-2.0 |
| Kotlin Android | `org.jetbrains.kotlin.android` | `2.1.20` | Apache-2.0 |
| Kotlin Compose Compiler | `org.jetbrains.kotlin.plugin.compose` | `2.1.20` | Apache-2.0 |
| Gradle Wrapper | `gradle-9.4.1-bin.zip` | `9.4.1` | Apache-2.0 |

The application targets Java 11 bytecode and is built with JDK 17 in the documented CI workflow.

---

## 6. Verification Notes

To inspect the resolved Release runtime graph without generating an APK:

```bash
./gradlew :app:dependencies --configuration releaseRuntimeClasspath
```

To inspect dependency licenses, compare the resolved artifacts with their upstream license files and keep the notices required by each license with any redistributed binary.

This file is documentation for LeNote `v1.0.3`; the version catalog and Gradle build scripts remain the source of truth for dependency versions.
