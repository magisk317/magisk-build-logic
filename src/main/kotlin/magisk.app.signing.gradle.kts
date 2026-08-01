import com.android.build.api.dsl.ApplicationExtension
import org.gradle.api.GradleException
import org.gradle.kotlin.dsl.configure
import java.util.Properties

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
    fallbackPaths = listOf("release.jks", "app/release.jks", ".yuuta.jks",
        "${rootProject.projectDir}/release.jks",
        "${rootProject.projectDir}/app/release.jks",
        "${rootProject.projectDir}/.yuuta.jks"),
)
val propertyFile = resolveSigningFile(
    explicitPath = project.findProperty("tianma.signature.path")?.toString(),
    fallbackPaths = listOf("signature.properties", "app/signature.properties", "local.properties",
        "${rootProject.projectDir}/signature.properties",
        "${rootProject.projectDir}/app/signature.properties",
        "${rootProject.projectDir}/local.properties"),
)

val keyProps = Properties().apply {
    if (propertyFile.exists()) {
        propertyFile.inputStream().use(::load)
    }
}

val storePasswordVal = System.getenv("STORE_PASSWORD") ?: System.getenv("KEYSTORE_PASS") ?: keyProps.getProperty("STORE_PASSWORD") ?: keyProps.getProperty("KEYSTORE_PASSWORD")
val keyAliasVal = System.getenv("KEY_ALIAS") ?: System.getenv("ALIAS_NAME") ?: keyProps.getProperty("KEY_ALIAS") ?: keyProps.getProperty("KEYSTORE_ALIAS")
val keyPasswordVal = System.getenv("KEY_PASSWORD") ?: System.getenv("ALIAS_PASS") ?: keyProps.getProperty("KEY_PASSWORD")

val isSigningInfoAvailable = keyFile.isFile &&
    !storePasswordVal.isNullOrBlank() &&
    !keyAliasVal.isNullOrBlank() &&
    !keyPasswordVal.isNullOrBlank()

pluginManager.withPlugin("com.android.application") {
    if (requiresReleaseSigning(gradle.startParameter.taskNames, project.path) && !isSigningInfoAvailable) {
        throw GradleException(
            "APK/AAB packaging requires complete release signing. " +
                "Configure KEYSTORE_FILE, STORE_PASSWORD, KEY_ALIAS, and KEY_PASSWORD."
        )
    }

    extensions.configure<ApplicationExtension> {
        val releaseSigningConfig = signingConfigs.maybeCreate("release").apply {
            storeFile = keyFile
            storePassword = storePasswordVal
            keyAlias = keyAliasVal
            keyPassword = keyPasswordVal
            enableV1Signing = true
            enableV2Signing = true
            enableV3Signing = true
            enableV4Signing = true
        }

        buildTypes {
            getByName("debug") {
                signingConfig = releaseSigningConfig
            }
            maybeCreate("alpha").apply {
                initWith(getByName("release"))
                matchingFallbacks += "release"
                signingConfig = releaseSigningConfig
            }
            getByName("release") {
                signingConfig = releaseSigningConfig
            }
        }
    }
}
