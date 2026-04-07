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

## Version Catalog

This repository owns the minimal version catalog required to compile the
convention plugins in `gradle/libs.versions.toml`.

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
