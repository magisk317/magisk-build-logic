import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.dsl.LibraryExtension
import com.android.build.api.dsl.ProductFlavor
import com.android.build.api.variant.ApplicationAndroidComponentsExtension
import com.android.build.api.variant.BuildConfigField
import com.android.build.api.variant.LibraryAndroidComponentsExtension
import com.android.build.api.variant.Variant
import org.gradle.api.provider.Provider
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

private fun configureXscFlavor(flavor: ProductFlavor) {
    when (flavor.name) {
        PLAY_FLAVOR -> flavor.dimension = DISTRIBUTION_DIMENSION
        GITHUB_FLAVOR -> flavor.dimension = DISTRIBUTION_DIMENSION
        // FDROID_FLAVOR -> flavor.dimension = DISTRIBUTION_DIMENSION
    }
}

private fun configureXscBuildConfigFields(
    variant: Variant,
    commitHashField: Provider<BuildConfigField<java.io.Serializable>>,
) {
    val fields = variant.buildConfigFields ?: return

    fields.put("COMMIT_HASH", commitHashField)

    fun put(name: String, value: String) =
        fields.put(name, BuildConfigField<java.io.Serializable>("boolean", value, null))

    when (variant.productFlavors.toMap()[DISTRIBUTION_DIMENSION]) {
        PLAY_FLAVOR -> {
            put("ENABLE_SMS_CHANNEL", "false")
            put("ALLOW_HTTP_WEBHOOK", "true")
            put("ENABLE_ACCESSIBILITY_AUTO_INPUT", "false")
            put("HAS_BILLING", "true")
        }
        GITHUB_FLAVOR -> {
            put("ENABLE_SMS_CHANNEL", "true")
            put("ALLOW_HTTP_WEBHOOK", "true")
            put("ENABLE_ACCESSIBILITY_AUTO_INPUT", "true")
            put("HAS_BILLING", "false")
        }
        // FDROID_FLAVOR -> {
        //     put("ENABLE_SMS_CHANNEL", "true")
        //     put("ALLOW_HTTP_WEBHOOK", "false")
        //     put("ENABLE_ACCESSIBILITY_AUTO_INPUT", "true")
        //     put("HAS_BILLING", "false")
        // }
    }
}

private fun ApplicationExtension.configureXscFlavors() {
    flavorDimensions.add(DISTRIBUTION_DIMENSION)
    productFlavors {
        maybeCreate(PLAY_FLAVOR).also(::configureXscFlavor)
        maybeCreate(GITHUB_FLAVOR).also(::configureXscFlavor)
        // F-Droid paused: re-enable this line to restore the flavor.
        // maybeCreate(FDROID_FLAVOR).also(::configureXscFlavor)
    }
}

private fun LibraryExtension.configureXscFlavors() {
    flavorDimensions.add(DISTRIBUTION_DIMENSION)
    productFlavors {
        maybeCreate(PLAY_FLAVOR).also(::configureXscFlavor)
        maybeCreate(GITHUB_FLAVOR).also(::configureXscFlavor)
        // maybeCreate(FDROID_FLAVOR).also(::configureXscFlavor)
    }
}

pluginManager.withPlugin("com.android.application") {
    extensions.configure<ApplicationExtension> {
        configureXscFlavors()
    }
    val commitHashField = registerCommitHashBuildConfigField()
    extensions.getByType<ApplicationAndroidComponentsExtension>().apply {
        onVariants(selector().all()) { variant ->
            configureXscBuildConfigFields(variant, commitHashField)
        }
    }
}

pluginManager.withPlugin("com.android.library") {
    extensions.configure<LibraryExtension> {
        configureXscFlavors()
    }
    val commitHashField = registerCommitHashBuildConfigField()
    extensions.getByType<LibraryAndroidComponentsExtension>().apply {
        onVariants(selector().all()) { variant ->
            configureXscBuildConfigFields(variant, commitHashField)
        }
    }
}
