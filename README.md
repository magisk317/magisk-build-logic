# magisk-build-logic

Shared Gradle convention plugins for the Magisk317 Android app family.

Current plugin set:

- `smscode.android.common`
- `relay.android.common`
- `magisk.android.application`
- `magisk.android.common`
- `magisk.android.compose`
- `magisk.android.library`
- `magisk.android.room`
- `magisk.app.packaging`
- `magisk.app.signing`
- `magisk.maintenance`

## Consumers

- `XposedSmsCode`
- `xinyi-relay`
- `MiPush`
- `MiPushFramework`

## Build Dependencies

When this repository is included as `build-logic/` inside a consumer project, it
reads the parent project's `gradle/libs.versions.toml` so the convention plugin
classpath follows the consuming app's AGP, Kotlin, KSP, Hilt, Java bytecode,
and forced-dependency versions.

The consumer catalog is required; this repository does not carry fallback
toolchain or dependency versions.

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

For `magisk.app.packaging`, consumers can override the artifact base name:

```kotlin
extra["artifactBaseName"] = "MiPushFramework"
```

The legacy `mipushArtifactBaseName` key is still accepted for older MiPushFramework modules, but new
consumers should use `artifactBaseName`.
