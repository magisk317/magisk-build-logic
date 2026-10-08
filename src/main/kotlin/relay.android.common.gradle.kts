import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.dsl.DynamicFeatureExtension
import com.android.build.api.dsl.DynamicFeatureProductFlavor
import com.android.build.api.dsl.LibraryExtension
import com.android.build.api.dsl.ProductFlavor
import com.android.build.api.variant.ApplicationAndroidComponentsExtension
import com.android.build.api.variant.BuildConfigField
import com.android.build.api.variant.FilterConfiguration
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
val GITHUB_NO_E2EE_FLAVOR = "githubNoE2ee"
val GITHUB_WITH_E2EE_FLAVOR = "githubWithE2ee"
val FDROID_FLAVOR = "fdroid"

private fun configureRelayFlavor(flavor: ProductFlavor) {
    when (flavor.name) {
        PLAY_FLAVOR -> flavor.dimension = DISTRIBUTION_DIMENSION
        GITHUB_NO_E2EE_FLAVOR -> flavor.dimension = DISTRIBUTION_DIMENSION
        GITHUB_WITH_E2EE_FLAVOR -> {
            flavor.dimension = DISTRIBUTION_DIMENSION
            flavor.ndk { abiFilters.add("arm64-v8a") }
        }
        FDROID_FLAVOR -> flavor.dimension = DISTRIBUTION_DIMENSION
    }
}

private fun configureRelayBuildConfigFields(variant: Variant) {
    val fields = variant.buildConfigFields ?: return

    fun put(name: String, value: String) =
        fields.put(name, BuildConfigField<java.io.Serializable>("boolean", value, null))

    when (variant.productFlavors.toMap()[DISTRIBUTION_DIMENSION]) {
        PLAY_FLAVOR -> {
            put("ENABLE_SMS_CHANNEL", "false")
            put("ALLOW_HTTP_WEBHOOK", "true")
            put("ENABLE_ACCESSIBILITY_AUTO_INPUT", "false")
            put("HAS_BILLING", "true")
            put("HAS_CLOUD_BACKUP", "true")
            put("HAS_MATRIX_E2EE", "true")
        }
        GITHUB_NO_E2EE_FLAVOR -> {
            put("ENABLE_SMS_CHANNEL", "true")
            put("ALLOW_HTTP_WEBHOOK", "true")
            put("ENABLE_ACCESSIBILITY_AUTO_INPUT", "true")
            put("HAS_BILLING", "false")
            put("HAS_CLOUD_BACKUP", "false")
            put("HAS_MATRIX_E2EE", "false")
        }
        GITHUB_WITH_E2EE_FLAVOR -> {
            put("ENABLE_SMS_CHANNEL", "true")
            put("ALLOW_HTTP_WEBHOOK", "true")
            put("ENABLE_ACCESSIBILITY_AUTO_INPUT", "true")
            put("HAS_BILLING", "false")
            put("HAS_CLOUD_BACKUP", "false")
            put("HAS_MATRIX_E2EE", "true")
        }
        FDROID_FLAVOR -> {
            put("ENABLE_SMS_CHANNEL", "true")
            put("ALLOW_HTTP_WEBHOOK", "false")
            put("ENABLE_ACCESSIBILITY_AUTO_INPUT", "true")
            put("HAS_BILLING", "false")
            put("HAS_CLOUD_BACKUP", "false")
            put("HAS_MATRIX_E2EE", "false")
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
    extensions.getByType<ApplicationAndroidComponentsExtension>().apply {
        onVariants(selector().all()) { variant ->
            configureRelayBuildConfigFields(variant)
            if (variant.productFlavors.toMap()[DISTRIBUTION_DIMENSION] == GITHUB_WITH_E2EE_FLAVOR) {
                variant.outputs.forEach { output ->
                    val abi = output.filters.find {
                        it.filterType == FilterConfiguration.FilterType.ABI
                    }?.identifier ?: "universal"
                    if (abi != "arm64-v8a" && abi != "universal") {
                        output.enabled.set(false)
                    }
                }
            }
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
