# magisk-build-logic

Shared Gradle convention plugins for the Magisk317 Android app family.

Current plugin set:

- `smscode.android.common`
- `smscode.app.signing`
- `smscode.app.packaging`
- `relay.android.common`
- `relay.app.signing`
- `relay.app.packaging`
- `mipush.android.application`
- `mipush.android.library`
- `mipush.android.compose`
- `mipush.android.hilt`
- `mipush.android.room`
- `mipush.app.packaging`

## Consumers

- `XposedSmsCode`
- `xinyi-relay`
- `MiPush`
- `MiPushFramework`

## Build Dependencies

When this repository is included as `build-logic/` inside a consumer project, it
reads the parent project's `gradle/libs.versions.toml` so the convention plugin
classpath follows the consuming app's AGP, Kotlin, KSP, and Hilt versions.

Standalone checkouts use fallback versions declared in `settings.gradle.kts`.

The plugins themselves still read the target repository's `libs` catalog at
runtime for application-specific versions such as `compileSdk`, `versionName`,
and product flavor behavior.

## Usage

Add as a git submodule at `build-logic/` in the consumer repository and keep:

```kotlin
pluginManagement {
    includeBuild("build-logic")
}
```

For `mipush.app.packaging`, consumers can override the artifact base name:

```kotlin
extra["mipushArtifactBaseName"] = "MiPushFramework"
```
