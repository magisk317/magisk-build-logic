import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.variant.ApplicationAndroidComponentsExtension
import com.android.build.api.variant.FilterConfiguration
import org.gradle.kotlin.dsl.getByType
import java.text.SimpleDateFormat
import java.util.Date
import java.util.TimeZone

fun buildTimestampOverride(): String? = project.findProperty("buildTs")
    ?.toString()
    ?.trim()
    ?.takeIf { it.isNotEmpty() }

fun buildTimestamp(): String = buildTimestampOverride() ?: SimpleDateFormat("yyyyMMdd_HHmmss").apply {
    timeZone = TimeZone.getDefault()
}.format(Date())

fun releaseTime(): String = SimpleDateFormat("yyMMdd").apply {
    timeZone = TimeZone.getDefault()
}.format(Date())

fun mipushArtifactBaseName(): String {
    val configured = project.findProperty("mipushArtifactBaseName")
        ?.toString()
        ?.trim()
        ?.takeIf { it.isNotEmpty() }
        ?: rootProject.name
    return configured.replace("\\s+".toRegex(), "_")
}

fun releaseBaseName(versionName: String): String {
    val artifactBaseName = mipushArtifactBaseName()
    val normalizedVersionName = versionName.replace("\\s+".toRegex(), "_")
    val alreadyHasBuildTimestamp = versionName.matches(Regex(".*-\\d{8}(?:_\\d{6}|\\d{6})$"))
    if (alreadyHasBuildTimestamp) {
        return "${artifactBaseName}_v$normalizedVersionName"
    }
    val suffix = buildTimestampOverride() ?: releaseTime()
    return "${artifactBaseName}_v${normalizedVersionName}_$suffix"
}

fun releaseApkName(versionName: String, buildType: String, abiSuffix: String): String {
    return "${abiSuffix}_${releaseBaseName(versionName)}_${buildType}.apk"
}

fun releaseAabName(versionName: String): String = "${releaseBaseName(versionName)}_release.aab"

val debugBuildTimestamp = buildTimestamp()
val isBundleTask = gradle.startParameter.taskNames.any { it.contains("bundle", ignoreCase = true) }

pluginManager.withPlugin("com.android.application") {
    val releaseVersionName = providers.provider {
        val android = extensions.getByType<ApplicationExtension>()
        android.defaultConfig.versionName ?: rootProject.version.toString().ifBlank { project.catalogVersion("versionName") }
    }

    extensions.configure<ApplicationExtension> {
        splits {
            abi {
                isEnable = project.hasProperty("buildSplits") && !isBundleTask
                reset()
                include("armeabi-v7a", "arm64-v8a", "x86", "x86_64")
                isUniversalApk = true
            }
        }
    }

    extensions.getByType<ApplicationAndroidComponentsExtension>().apply {
        onVariants(selector().all()) { variant ->
            val isDebug = variant.buildType == "debug"
            val resolvedVersionName = if (isDebug) {
                "${releaseVersionName.get()}-$debugBuildTimestamp"
            } else {
                releaseVersionName.get()
            }

            variant.outputs.forEach { output ->
                if (isDebug) {
                    output.versionName.set(resolvedVersionName)
                }

                val abi = output.filters.find {
                    it.filterType == FilterConfiguration.FilterType.ABI
                }?.identifier ?: "universal"

                try {
                    val outputFileName = output.javaClass.getMethod("getOutputFileName").invoke(output)
                    outputFileName.javaClass
                        .getMethod("set", Any::class.java)
                        .invoke(outputFileName, releaseApkName(resolvedVersionName, variant.buildType ?: "", abi))
                } catch (_: Exception) {
                    // Keep the build tolerant across AGP preview API changes.
                }
            }
        }
    }

    val renameReleaseBundleTask = "renameReleaseAab"
    tasks.register(renameReleaseBundleTask) {
        dependsOn("bundleRelease")
        val bundleFileProvider = layout.buildDirectory.file("outputs/bundle/release/push-release.aab")
        val targetFileProvider = layout.buildDirectory.file("outputs/bundle/release/${releaseAabName(releaseVersionName.get())}")
        doLast {
            val bundleFile = bundleFileProvider.get().asFile
            if (bundleFile.exists()) {
                bundleFile.copyTo(targetFileProvider.get().asFile, overwrite = true)
            }
        }
    }

    tasks.matching { it.name == "bundleRelease" }.configureEach {
        finalizedBy(renameReleaseBundleTask)
    }
}
