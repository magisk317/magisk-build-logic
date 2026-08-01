import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.dsl.DynamicFeatureExtension
import com.android.build.api.dsl.DynamicFeatureProductFlavor
import com.android.build.api.dsl.LibraryExtension
import com.android.build.api.dsl.ProductFlavor
import com.android.build.api.variant.ApplicationAndroidComponentsExtension
import com.android.build.api.variant.DynamicFeatureAndroidComponentsExtension
import com.android.build.api.variant.LibraryAndroidComponentsExtension
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.getByType

plugins {
    id("magisk.android.common")
}

val DISTRIBUTION_DIMENSION = "distribution"
val PLAY_FLAVOR = "play"
val GITHUB_NO_E2EE_FLAVOR = "githubNoE2ee"
val GITHUB_WITH_E2EE_FLAVOR = "githubWithE2ee"
val FDROID_FLAVOR = "fdroid"

private fun configureRelayFlavor(flavor: ProductFlavor) {
    when (flavor.name) {
        PLAY_FLAVOR -> {
            flavor.dimension = DISTRIBUTION_DIMENSION
            flavor.buildConfigField("boolean", "ENABLE_SMS_CHANNEL", "false")
            flavor.buildConfigField("boolean", "ALLOW_HTTP_WEBHOOK", "true")
            flavor.buildConfigField("boolean", "ENABLE_ACCESSIBILITY_AUTO_INPUT", "false")
            flavor.buildConfigField("boolean", "HAS_BILLING", "true")
            flavor.buildConfigField("boolean", "HAS_CLOUD_BACKUP", "true")
            flavor.buildConfigField("boolean", "HAS_MATRIX_E2EE", "true")
        }
        GITHUB_NO_E2EE_FLAVOR -> {
            flavor.dimension = DISTRIBUTION_DIMENSION
            flavor.buildConfigField("boolean", "ENABLE_SMS_CHANNEL", "true")
            flavor.buildConfigField("boolean", "ALLOW_HTTP_WEBHOOK", "true")
            flavor.buildConfigField("boolean", "ENABLE_ACCESSIBILITY_AUTO_INPUT", "true")
            flavor.buildConfigField("boolean", "HAS_BILLING", "false")
            flavor.buildConfigField("boolean", "HAS_CLOUD_BACKUP", "false")
            flavor.buildConfigField("boolean", "HAS_MATRIX_E2EE", "false")
        }
        GITHUB_WITH_E2EE_FLAVOR -> {
            flavor.dimension = DISTRIBUTION_DIMENSION
            flavor.buildConfigField("boolean", "ENABLE_SMS_CHANNEL", "true")
            flavor.buildConfigField("boolean", "ALLOW_HTTP_WEBHOOK", "true")
            flavor.buildConfigField("boolean", "ENABLE_ACCESSIBILITY_AUTO_INPUT", "true")
            flavor.buildConfigField("boolean", "HAS_BILLING", "false")
            flavor.buildConfigField("boolean", "HAS_CLOUD_BACKUP", "false")
            flavor.buildConfigField("boolean", "HAS_MATRIX_E2EE", "true")
            flavor.ndk { abiFilters.add("arm64-v8a") }
        }
        FDROID_FLAVOR -> {
            flavor.dimension = DISTRIBUTION_DIMENSION
            flavor.buildConfigField("boolean", "ENABLE_SMS_CHANNEL", "true")
            flavor.buildConfigField("boolean", "ALLOW_HTTP_WEBHOOK", "false")
            flavor.buildConfigField("boolean", "ENABLE_ACCESSIBILITY_AUTO_INPUT", "true")
            flavor.buildConfigField("boolean", "HAS_BILLING", "false")
            flavor.buildConfigField("boolean", "HAS_CLOUD_BACKUP", "false")
            flavor.buildConfigField("boolean", "HAS_MATRIX_E2EE", "false")
        }
    }
}

private fun configureRelayFlavor(flavor: DynamicFeatureProductFlavor) {
    when (flavor.name) {
        PLAY_FLAVOR,
        GITHUB_NO_E2EE_FLAVOR,
        GITHUB_WITH_E2EE_FLAVOR,
        FDROID_FLAVOR,
        -> flavor.dimension = DISTRIBUTION_DIMENSION
    }
}

private fun isSupportedRelayVariant(productFlavors: Map<String, String>): Boolean {
    return productFlavors[DISTRIBUTION_DIMENSION] != FDROID_FLAVOR
}

private fun isSupportedRelayDynamicFeatureVariant(productFlavors: Map<String, String>): Boolean {
    return productFlavors[DISTRIBUTION_DIMENSION] == PLAY_FLAVOR
}

private fun ApplicationExtension.configureRelayFlavors() {
    flavorDimensions.add(DISTRIBUTION_DIMENSION)
    productFlavors {
        maybeCreate(PLAY_FLAVOR).also(::configureRelayFlavor)
        maybeCreate(GITHUB_NO_E2EE_FLAVOR).also(::configureRelayFlavor)
        maybeCreate(GITHUB_WITH_E2EE_FLAVOR).also(::configureRelayFlavor)
        maybeCreate(FDROID_FLAVOR).also(::configureRelayFlavor)
    }
}

private fun LibraryExtension.configureRelayFlavors() {
    flavorDimensions.add(DISTRIBUTION_DIMENSION)
    productFlavors {
        maybeCreate(PLAY_FLAVOR).also(::configureRelayFlavor)
        maybeCreate(GITHUB_NO_E2EE_FLAVOR).also(::configureRelayFlavor)
        maybeCreate(GITHUB_WITH_E2EE_FLAVOR).also(::configureRelayFlavor)
        maybeCreate(FDROID_FLAVOR).also(::configureRelayFlavor)
    }
}

private fun DynamicFeatureExtension.configureRelayFlavors() {
    flavorDimensions.add(DISTRIBUTION_DIMENSION)
    productFlavors {
        maybeCreate(PLAY_FLAVOR).also(::configureRelayFlavor)
        maybeCreate(GITHUB_NO_E2EE_FLAVOR).also(::configureRelayFlavor)
        maybeCreate(GITHUB_WITH_E2EE_FLAVOR).also(::configureRelayFlavor)
        maybeCreate(FDROID_FLAVOR).also(::configureRelayFlavor)
    }
}

pluginManager.withPlugin("com.android.application") {
    extensions.configure<ApplicationExtension> {
        configureRelayFlavors()
    }
    extensions.getByType<ApplicationAndroidComponentsExtension>().beforeVariants(
        extensions.getByType<ApplicationAndroidComponentsExtension>().selector().all(),
    ) { variantBuilder ->
        if (!isSupportedRelayVariant(variantBuilder.productFlavors.toMap())) {
            variantBuilder.enable = false
        }
    }
}

pluginManager.withPlugin("com.android.library") {
    extensions.configure<LibraryExtension> {
        configureRelayFlavors()
    }
    extensions.getByType<LibraryAndroidComponentsExtension>().beforeVariants(
        extensions.getByType<LibraryAndroidComponentsExtension>().selector().all(),
    ) { variantBuilder ->
        if (!isSupportedRelayVariant(variantBuilder.productFlavors.toMap())) {
            variantBuilder.enable = false
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
