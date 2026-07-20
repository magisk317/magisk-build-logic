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

val isSigningInfoAvailable = keyFile.exists() && (storePasswordVal != null)

val allowIncompatibleDebugSigning = project.findProperty("allowIncompatibleDebugSigning")
    ?.toString()
    ?.toBooleanStrictOrNull()
    ?: false

pluginManager.withPlugin("com.android.application") {
    val requestedTasks = gradle.startParameter.taskNames.map { it.lowercase() }
    val requiresReleaseCompatibleSigning = requestedTasks.any { taskName ->
        val taskLeafName = taskName.substringAfterLast(":")
        val createsRunnableArtifact = taskLeafName.startsWith("assemble") ||
            taskLeafName.startsWith("bundle") ||
            taskLeafName.startsWith("install")
        createsRunnableArtifact && (taskLeafName.contains("debug") || taskLeafName.contains("alpha"))
    }
    if (requiresReleaseCompatibleSigning && !isSigningInfoAvailable && !allowIncompatibleDebugSigning) {
        throw GradleException(
            "Debug/alpha artifact tasks require release-compatible signing. " +
                "Configure KEYSTORE_FILE, STORE_PASSWORD, KEY_ALIAS, and KEY_PASSWORD, " +
                "or pass -PallowIncompatibleDebugSigning=true for local-only builds."
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
                signingConfig = if (isSigningInfoAvailable) releaseSigningConfig else debugSigningConfig
            }
            maybeCreate("alpha").apply {
                initWith(getByName("release"))
                matchingFallbacks += "release"
                if (isSigningInfoAvailable) {
                    signingConfig = releaseSigningConfig
                }
            }
            getByName("release") {
                signingConfig = if (isSigningInfoAvailable) releaseSigningConfig else debugSigningConfig
            }
        }
    }
}
