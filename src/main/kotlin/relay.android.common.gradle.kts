import com.android.build.api.dsl.CommonExtension
import com.android.build.api.variant.ApplicationAndroidComponentsExtension
import com.android.build.api.variant.LibraryAndroidComponentsExtension
import org.gradle.api.JavaVersion
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.api.tasks.testing.Test
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile

val libsCatalog = extensions.getByType<VersionCatalogsExtension>().named("libs")

fun versionString(name: String): String = libsCatalog.findVersion(name)
    .orElseThrow { IllegalArgumentException("Version '$name' not found in consumer libs catalog") }
    .requiredVersion

fun versionStringOrNull(name: String): String? {
    val version = libsCatalog.findVersion(name)
    return if (version.isPresent) version.get().requiredVersion else null
}

fun javaTargetString(): String = versionString("javaBytecode")

fun kotlinJvmTarget(): JvmTarget = JvmTarget.fromTarget(javaTargetString())

fun CommonExtension.configureRelayAndroidCommon() {
    val compileSdkApi = versionString("compileSdk").toInt()
    compileSdk = compileSdkApi
    if (compileSdkApi >= 35) {
        compileSdkMinor = 0
    }
    versionStringOrNull("compileSdkExtension")?.toIntOrNull()?.let { compileSdkExtension = it }

    if (!flavorDimensions.contains("distribution")) {
        flavorDimensions += "distribution"
    }
    productFlavors {
        maybeCreate("play").apply {
            dimension = "distribution"
            buildConfigField("boolean", "ENABLE_SMS_CHANNEL", "false")
            buildConfigField("boolean", "ALLOW_HTTP_WEBHOOK", "true")
            buildConfigField("boolean", "ENABLE_ACCESSIBILITY_AUTO_INPUT", "false")
            buildConfigField("boolean", "HAS_BILLING", "true")
            buildConfigField("boolean", "HAS_CLOUD_BACKUP", "true")
        }
        maybeCreate("github").apply {
            dimension = "distribution"
            buildConfigField("boolean", "ENABLE_SMS_CHANNEL", "true")
            buildConfigField("boolean", "ALLOW_HTTP_WEBHOOK", "true")
            buildConfigField("boolean", "ENABLE_ACCESSIBILITY_AUTO_INPUT", "true")
            buildConfigField("boolean", "HAS_BILLING", "false")
            buildConfigField("boolean", "HAS_CLOUD_BACKUP", "false")
        }
        maybeCreate("fdroid").apply {
            dimension = "distribution"
            buildConfigField("boolean", "ENABLE_SMS_CHANNEL", "true")
            buildConfigField("boolean", "ALLOW_HTTP_WEBHOOK", "false")
            buildConfigField("boolean", "ENABLE_ACCESSIBILITY_AUTO_INPUT", "true")
            buildConfigField("boolean", "HAS_BILLING", "false")
            buildConfigField("boolean", "HAS_CLOUD_BACKUP", "false")
        }
    }

    compileOptions.apply {
        val javaVersion = JavaVersion.toVersion(javaTargetString())
        sourceCompatibility = javaVersion
        targetCompatibility = javaVersion
    }
}

tasks.withType<KotlinJvmCompile>().configureEach {
    compilerOptions.jvmTarget.set(kotlinJvmTarget())
}

pluginManager.withPlugin("com.android.application") {
    extensions.configure<CommonExtension> {
        configureRelayAndroidCommon()
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
    extensions.configure<CommonExtension> {
        configureRelayAndroidCommon()
    }
    extensions.getByType<LibraryAndroidComponentsExtension>().beforeVariants(
        extensions.getByType<LibraryAndroidComponentsExtension>().selector().all(),
    ) { variantBuilder ->
        if (variantBuilder.productFlavors.toMap()["distribution"] == "fdroid") {
            variantBuilder.enable = false
        }
    }
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}
