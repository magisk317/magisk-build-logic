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
- `magisk.kotlin.library`
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

Because the build reads the consumer catalog, verify plugin changes through a
consumer checkout with this repository mounted at `build-logic/`. The wrapper
under `shared/gradle/` is a synchronization source for consumers, not a
standalone wrapper for this repository.

The plugins themselves still read the target repository's `libs` catalog at
runtime for application-specific versions such as `compileSdk`, `versionName`,
and product flavor behavior.

Android library lint keeps AGP's blocking default. Consumers may suppress
specific, documented checks per module, but the shared convention never turns
off lint failures globally.

### Distribution policy

`smscode.android.common` and `relay.android.common` own their product flavor matrices and
BuildConfig capability flags. Play, GitHub, and F-Droid application/library variants are
all real build variants; a convention must not create a flavor and then silently disable it.
Relay dynamic-feature variants remain Play-only because Play Feature Delivery is a store
capability rather than a runtime mode.

`magisk.app.packaging` owns generic APK split, bundle, timestamp, and artifact-name behavior.
Product-specific output constraints stay with the product convention: in particular,
`relay.android.common` owns the arm64-only `githubWithE2ee` output policy.

Runtime capability states such as Enhanced (Xposed active) and Standard (system APIs) belong
in the consuming application's runtime policy and must not be added here as `full` / `lite`
flavors. A separate binary distribution is justified only by a concrete packaging or
store-compliance requirement, not by the absence of Xposed at runtime.

### Test runtime policy

Applying `magisk.maintenance` to the root project standardizes JUnit Platform execution,
Java 26 native/dynamic-agent flags, worker heap/fork limits, and test logging for every
subproject. Android compile conventions do not mutate test workers.

### Signing contract

`magisk.app.signing` accepts the standard environment variables `KEYSTORE_FILE`,
`STORE_PASSWORD`, `KEY_ALIAS`, and `KEY_PASSWORD`. Relative keystore paths are resolved from
the root project. Local builds may instead use `-Pmagisk.signing.keystore=...` and
`-Pmagisk.signing.properties=...`; defaults are `release.jks`/`app/release.jks` and
`signature.properties`/`app/signature.properties`. Historical Tianma/Yuuta properties and
credential aliases are intentionally unsupported. Every application build type declared by a
consumer uses the shared release signing config; the signing convention does not create
product-specific build types.

## Shared Gradle Assets

This repository also carries a shared Gradle asset bundle under
`shared/gradle/` for wrapper scripts, wrapper metadata, and common
`gradle.properties` defaults.

- `scripts/sync_consumer_gradle_assets.sh <repo...>` syncs one or more consumer
  repositories to the shared wrapper and common properties.
- `scripts/sync_workspace_consumers.sh` syncs the known sibling consumers in the
  local workspace (`xinyi-relay`, `XposedSmsCode`, `push/MiPushFramework`).

The sync is intentionally copy-based. It keeps consumer builds reproducible and
avoids making Gradle startup depend on a moving remote HEAD.

`magisk.maintenance` standardizes test logging and exposes an explicit
`cleanupGradleCaches` task. Normal build, assemble, and clean tasks never delete
Gradle caches as a side effect.

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

Release APK names omit plain GitHub distribution labels, shorten
`githubWithE2ee` to `e2ee`, and preserve product-specific flavors such as
`normal` or `vc105`. Play App Bundles keep the Android Gradle Plugin output
path and filename; release pipelines should consume the `playRelease` bundle
directly instead of relying on a second renamed copy.
