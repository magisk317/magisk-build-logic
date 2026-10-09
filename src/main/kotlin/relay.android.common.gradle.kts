import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.dsl.DynamicFeatureExtension
import com.android.build.api.dsl.DynamicFeatureProductFlavor
import com.android.build.api.dsl.LibraryExtension
import com.android.build.api.dsl.ProductFlavor
import com.android.build.api.variant.ApplicationAndroidComponentsExtension
import com.android.build.api.variant.BuildConfigField
import com.android.build.api.variant.DynamicFeatureAndroidComponentsExtension
import com.android.build.api.variant.LibraryAndroidComponentsExtension
import com.android.build.api.variant.Variant
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.getByType

plugins {
    id("magisk.android.common")
}

val DISTRIBUTION_DIMENSION = "distribution"
val PLAY_FLAVOR = "play"
val GITHUB_FLAVOR = "github"
// F-Droid distribution is paused: keep the block below as the resume point.
// val FDROID_FLAVOR = "fdroid"

private fun configureRelayFlavor(flavor: ProductFlavor) {
    when (flavor.name) {
        PLAY_FLAVOR -> flavor.dimension = DISTRIBUTION_DIMENSION
        GITHUB_FLAVOR -> flavor.dimension = DISTRIBUTION_DIMENSION
        // FDROID_FLAVOR -> flavor.dimension = DISTRIBUTION_DIMENSION
    }
}

private fun configureRelayBuildConfigFields(variant: Variant) {
    val fields = variant.buildConfigFields ?: return

    fun put(name: String, value: String) =
        fields.put(name, BuildConfigField<java.io.Serializable>("boolean", value, null))

    fun putString(name: String, value: String) =
        // BuildConfigField values are emitted verbatim into the generated Java
        // source, so a String field must carry its own literal quotes.
        fields.put(name, BuildConfigField<java.io.Serializable>("String", "\"$value\"", null))

    val matrixSdkVersion = project.extensions.getByType<org.gradle.api.artifacts.VersionCatalogsExtension>()
        .named("libs")
        .findVersion("matrix-sdk-android")
        .map { it.requiredVersion }
        .orElse("")

    when (variant.productFlavors.toMap()[DISTRIBUTION_DIMENSION]) {
        PLAY_FLAVOR -> {
            put("ENABLE_SMS_CHANNEL", "false")
            put("ALLOW_HTTP_WEBHOOK", "true")
            put("ENABLE_ACCESSIBILITY_AUTO_INPUT", "false")
            put("HAS_BILLING", "true")
            put("HAS_CLOUD_BACKUP", "true")
            put("HAS_MATRIX_E2EE", "true")
        }
        GITHUB_FLAVOR -> {
            put("ENABLE_SMS_CHANNEL", "true")
            put("ALLOW_HTTP_WEBHOOK", "true")
            put("ENABLE_ACCESSIBILITY_AUTO_INPUT", "true")
            put("HAS_BILLING", "false")
            put("HAS_CLOUD_BACKUP", "false")
            // The GitHub flavor ships no E2EE code: the matrix-rust-sdk runtime
            // arrives as a separately versioned plugin APK (features/matrix-e2ee-plugin)
            // that is downloaded and DexClassLoader-loaded at runtime. The app only
            // pins the upstream SDK version it expects the plugin to carry.
            put("HAS_MATRIX_E2EE", "false")
            putString("MATRIX_PLUGIN_SDK_VERSION", matrixSdkVersion)
        }
        // FDROID_FLAVOR -> {
        //     put("ENABLE_SMS_CHANNEL", "true")
        //     put("ALLOW_HTTP_WEBHOOK", "false")
        //     put("ENABLE_ACCESSIBILITY_AUTO_INPUT", "true")
        //     put("HAS_BILLING", "false")
        //     put("HAS_CLOUD_BACKUP", "false")
        //     put("HAS_MATRIX_E2EE", "false")
        // }
    }
}

private fun configureRelayFlavor(flavor: DynamicFeatureProductFlavor) {
    when (flavor.name) {
        PLAY_FLAVOR,
        GITHUB_FLAVOR,
        // FDROID_FLAVOR,
        -> flavor.dimension = DISTRIBUTION_DIMENSION
    }
}

private fun isSupportedRelayDynamicFeatureVariant(productFlavors: Map<String, String>): Boolean {
    return productFlavors[DISTRIBUTION_DIMENSION] == PLAY_FLAVOR
}

private fun ApplicationExtension.configureRelayFlavors() {
    flavorDimensions.add(DISTRIBUTION_DIMENSION)
    productFlavors {
        maybeCreate(PLAY_FLAVOR).also(::configureRelayFlavor)
        maybeCreate(GITHUB_FLAVOR).also(::configureRelayFlavor)
        // F-Droid paused: re-enable this line to restore the flavor.
        // maybeCreate(FDROID_FLAVOR).also(::configureRelayFlavor)
    }
}

private fun LibraryExtension.configureRelayFlavors() {
    flavorDimensions.add(DISTRIBUTION_DIMENSION)
    productFlavors {
        maybeCreate(PLAY_FLAVOR).also(::configureRelayFlavor)
        maybeCreate(GITHUB_FLAVOR).also(::configureRelayFlavor)
        // maybeCreate(FDROID_FLAVOR).also(::configureRelayFlavor)
    }
}

private fun DynamicFeatureExtension.configureRelayFlavors() {
    flavorDimensions.add(DISTRIBUTION_DIMENSION)
    productFlavors {
        maybeCreate(PLAY_FLAVOR).also(::configureRelayFlavor)
        maybeCreate(GITHUB_FLAVOR).also(::configureRelayFlavor)
        // maybeCreate(FDROID_FLAVOR).also(::configureRelayFlavor)
    }
}

pluginManager.withPlugin("com.android.application") {
    extensions.configure<ApplicationExtension> {
        configureRelayFlavors()
    }
    extensions.getByType<ApplicationAndroidComponentsExtension>().apply {
        onVariants(selector().all()) { variant ->
            configureRelayBuildConfigFields(variant)
        }
    }
}

pluginManager.withPlugin("com.android.library") {
    extensions.configure<LibraryExtension> {
        configureRelayFlavors()
    }
    extensions.getByType<LibraryAndroidComponentsExtension>().apply {
        onVariants(selector().all()) { variant ->
            configureRelayBuildConfigFields(variant)
        }
    }
}

pluginManager.withPlugin("com.android.dynamic-feature") {
    extensions.configure<DynamicFeatureExtension> {
        configureRelayFlavors()
    }
    extensions.getByType<DynamicFeatureAndroidComponentsExtension>().beforeVariants(
        extensions.getByType<DynamicFeatureAndroidComponentsExtension>().selector().all(),
    ) { variantBuilder ->
        if (!isSupportedRelayDynamicFeatureVariant(variantBuilder.productFlavors.toMap())) {
            variantBuilder.enable = false
        }
    }
}
