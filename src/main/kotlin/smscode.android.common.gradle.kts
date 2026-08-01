import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.dsl.LibraryExtension
import com.android.build.api.dsl.ProductFlavor
import com.android.build.api.variant.ApplicationAndroidComponentsExtension
import com.android.build.api.variant.LibraryAndroidComponentsExtension
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.getByType

plugins {
    id("magisk.android.common")
}

private fun configureSmscodeFlavor(flavor: ProductFlavor) {
    when (flavor.name) {
        "play" -> {
            flavor.dimension = "distribution"
            flavor.buildConfigField("boolean", "ENABLE_SMS_CHANNEL", "false")
            flavor.buildConfigField("boolean", "ALLOW_HTTP_WEBHOOK", "true")
            flavor.buildConfigField("boolean", "ENABLE_ACCESSIBILITY_AUTO_INPUT", "false")
            flavor.buildConfigField("boolean", "HAS_BILLING", "true")
        }
        "github" -> {
            flavor.dimension = "distribution"
            flavor.buildConfigField("boolean", "ENABLE_SMS_CHANNEL", "true")
            flavor.buildConfigField("boolean", "ALLOW_HTTP_WEBHOOK", "true")
            flavor.buildConfigField("boolean", "ENABLE_ACCESSIBILITY_AUTO_INPUT", "true")
            flavor.buildConfigField("boolean", "HAS_BILLING", "false")
        }
        "fdroid" -> {
            flavor.dimension = "distribution"
            flavor.buildConfigField("boolean", "ENABLE_SMS_CHANNEL", "true")
            flavor.buildConfigField("boolean", "ALLOW_HTTP_WEBHOOK", "false")
            flavor.buildConfigField("boolean", "ENABLE_ACCESSIBILITY_AUTO_INPUT", "true")
            flavor.buildConfigField("boolean", "HAS_BILLING", "false")
        }
    }
}

private fun ApplicationExtension.configureSmscodeFlavors() {
    flavorDimensions.add("distribution")
    productFlavors {
        maybeCreate("play").also(::configureSmscodeFlavor)
        maybeCreate("github").also(::configureSmscodeFlavor)
        maybeCreate("fdroid").also(::configureSmscodeFlavor)
    }
}

private fun LibraryExtension.configureSmscodeFlavors() {
    flavorDimensions.add("distribution")
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
    extensions.getByType<ApplicationAndroidComponentsExtension>().beforeVariants(
        extensions.getByType<ApplicationAndroidComponentsExtension>().selector().all(),
    ) { variantBuilder ->
        if (variantBuilder.productFlavors.toMap()["distribution"] == "fdroid") {
            variantBuilder.enable = false
        }
    }
}

pluginManager.withPlugin("com.android.library") {
    extensions.configure<LibraryExtension> {
        configureSmscodeFlavors()
    }
    extensions.getByType<LibraryAndroidComponentsExtension>().beforeVariants(
        extensions.getByType<LibraryAndroidComponentsExtension>().selector().all(),
    ) { variantBuilder ->
        if (variantBuilder.productFlavors.toMap()["distribution"] == "fdroid") {
            variantBuilder.enable = false
        }
    }
}
