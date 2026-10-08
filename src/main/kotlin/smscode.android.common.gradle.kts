import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.dsl.LibraryExtension
import com.android.build.api.dsl.ProductFlavor
import com.android.build.api.variant.ApplicationAndroidComponentsExtension
import com.android.build.api.variant.BuildConfigField
import com.android.build.api.variant.LibraryAndroidComponentsExtension
import com.android.build.api.variant.Variant
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.getByType

plugins {
    id("magisk.android.common")
}

private val SMSCODE_DISTRIBUTION_DIMENSION = "distribution"

private fun configureSmscodeFlavor(flavor: ProductFlavor) {
    when (flavor.name) {
        "play" -> flavor.dimension = SMSCODE_DISTRIBUTION_DIMENSION
        "github" -> flavor.dimension = SMSCODE_DISTRIBUTION_DIMENSION
        "fdroid" -> flavor.dimension = SMSCODE_DISTRIBUTION_DIMENSION
    }
}

private fun configureSmscodeBuildConfigFields(variant: Variant) {
    val fields = variant.buildConfigFields ?: return

    fun put(name: String, value: String) =
        fields.put(name, BuildConfigField<java.io.Serializable>("boolean", value, null))

    when (variant.productFlavors.toMap()[SMSCODE_DISTRIBUTION_DIMENSION]) {
        "play" -> {
            put("ENABLE_SMS_CHANNEL", "false")
            put("ALLOW_HTTP_WEBHOOK", "true")
            put("ENABLE_ACCESSIBILITY_AUTO_INPUT", "false")
            put("HAS_BILLING", "true")
        }
        "github" -> {
            put("ENABLE_SMS_CHANNEL", "true")
            put("ALLOW_HTTP_WEBHOOK", "true")
            put("ENABLE_ACCESSIBILITY_AUTO_INPUT", "true")
            put("HAS_BILLING", "false")
        }
        "fdroid" -> {
            put("ENABLE_SMS_CHANNEL", "true")
            put("ALLOW_HTTP_WEBHOOK", "false")
            put("ENABLE_ACCESSIBILITY_AUTO_INPUT", "true")
            put("HAS_BILLING", "false")
        }
    }
}

private fun ApplicationExtension.configureSmscodeFlavors() {
    flavorDimensions.add(SMSCODE_DISTRIBUTION_DIMENSION)
    productFlavors {
        maybeCreate("play").also(::configureSmscodeFlavor)
        maybeCreate("github").also(::configureSmscodeFlavor)
        maybeCreate("fdroid").also(::configureSmscodeFlavor)
    }
}

private fun LibraryExtension.configureSmscodeFlavors() {
    flavorDimensions.add(SMSCODE_DISTRIBUTION_DIMENSION)
    productFlavors {
        maybeCreate("play").also(::configureSmscodeFlavor)
        maybeCreate("github").also(::configureSmscodeFlavor)
        maybeCreate("fdroid").also(::configureSmscodeFlavor)
    }
}

pluginManager.withPlugin("com.android.application") {
    extensions.configure<ApplicationExtension> {
        configureSmscodeFlavors()
    }
    extensions.getByType<ApplicationAndroidComponentsExtension>().apply {
        onVariants(selector().all()) { variant ->
            configureSmscodeBuildConfigFields(variant)
        }
    }
}

pluginManager.withPlugin("com.android.library") {
    extensions.configure<LibraryExtension> {
        configureSmscodeFlavors()
    }
    extensions.getByType<LibraryAndroidComponentsExtension>().apply {
        onVariants(selector().all()) { variant ->
            configureSmscodeBuildConfigFields(variant)
        }
    }
}
