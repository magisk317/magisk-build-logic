import com.android.build.api.dsl.ApplicationExtension
import org.gradle.kotlin.dsl.configure

fun resolveSigningFile(
    explicitPath: String?,
    fallbackPaths: List<String>,
): java.io.File {
    if (!explicitPath.isNullOrBlank()) {
        return project.file(explicitPath)
    }
    return fallbackPaths
        .asSequence()
        .map(project::file)
        .firstOrNull { it.exists() }
        ?: project.file(fallbackPaths.first())
}

val keyFile = resolveSigningFile(
    explicitPath = System.getenv("KEYSTORE_FILE")
        ?: project.findProperty("tianma.keystore.path")?.toString(),
    fallbackPaths = listOf("release.jks", "app/release.jks"),
)
val propertyFile = resolveSigningFile(
    explicitPath = project.findProperty("tianma.signature.path")?.toString(),
    fallbackPaths = listOf("signature.properties", "app/signature.properties"),
)

val keyProps = java.util.Properties().apply {
    if (propertyFile.exists()) {
        propertyFile.inputStream().use(::load)
    }
}

val isSigningInfoAvailable = keyFile.exists() &&
    (
        keyProps.getProperty("STORE_PASSWORD") != null ||
            System.getenv("STORE_PASSWORD") != null
        )
val allowIncompatibleDebugSigning = project.findProperty("allowIncompatibleDebugSigning")
    ?.toString()
    ?.toBooleanStrictOrNull()
    ?: false

pluginManager.withPlugin("com.android.application") {
    val requestedTasks = gradle.startParameter.taskNames.map { it.lowercase() }
    val requiresReleaseCompatibleSigning = requestedTasks.any { taskName ->
        taskName.contains("debug") || taskName.contains("alpha")
    }
    if (requiresReleaseCompatibleSigning && !isSigningInfoAvailable && !allowIncompatibleDebugSigning) {
        error(
            "Debug/alpha builds must use release-compatible signing to preserve app data when " +
                "upgrading from a release install. Configure release signing material or pass " +
                "-PallowIncompatibleDebugSigning=true if you explicitly accept reinstall/data loss.",
        )
    }

    extensions.configure<ApplicationExtension> {
        val debugSigningConfig = signingConfigs.maybeCreate("debug").apply {
            enableV1Signing = true
            enableV2Signing = true
            enableV3Signing = true
            enableV4Signing = true
        }
        val releaseSigningConfig = signingConfigs.maybeCreate("release").apply {
            storeFile = keyFile
            storePassword = System.getenv("STORE_PASSWORD") ?: keyProps.getProperty("STORE_PASSWORD")
            keyAlias = System.getenv("KEY_ALIAS") ?: keyProps.getProperty("KEY_ALIAS")
            keyPassword = System.getenv("KEY_PASSWORD") ?: keyProps.getProperty("KEY_PASSWORD")
            enableV1Signing = true
            enableV2Signing = true
            enableV3Signing = true
            enableV4Signing = true
        }

        buildTypes {
            getByName("debug") {
                buildConfigField("int", "LOG_LEVEL", "2")
                buildConfigField("boolean", "LOG_TO_XPOSED", "true")
                signingConfig = if (isSigningInfoAvailable) releaseSigningConfig else debugSigningConfig
            }
            maybeCreate("alpha").apply {
                isMinifyEnabled = true
                isShrinkResources = true
                isDebuggable = false
                matchingFallbacks += listOf("release", "debug")

                buildConfigField("int", "LOG_LEVEL", "2")
                buildConfigField("boolean", "LOG_TO_XPOSED", "true")

                if (isSigningInfoAvailable) {
                    signingConfig = releaseSigningConfig
                }
                proguardFiles(
                    getDefaultProguardFile("proguard-android-optimize.txt"),
                    "proguard-rules.pro",
                )
                ndk {
                    debugSymbolLevel = "FULL"
                }
            }
            getByName("release") {
                isMinifyEnabled = true
                isShrinkResources = true

                buildConfigField("int", "LOG_LEVEL", "4")
                buildConfigField("boolean", "LOG_TO_XPOSED", "true")
                signingConfig = if (isSigningInfoAvailable) releaseSigningConfig else debugSigningConfig
                proguardFiles(
                    getDefaultProguardFile("proguard-android-optimize.txt"),
                    "proguard-rules.pro",
                )
                ndk {
                    debugSymbolLevel = "FULL"
                }
                lint {
                    disable.add("MissingTranslation")
                    checkReleaseBuilds = false
                }
            }
        }
    }
}
