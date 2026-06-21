import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.variant.ApplicationAndroidComponentsExtension
import com.android.build.api.variant.FilterConfiguration
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.getByType
import java.text.SimpleDateFormat
import java.util.Date
import java.util.TimeZone

val libsCatalog = extensions.getByType<VersionCatalogsExtension>().named("libs")

fun versionStringOrNull(name: String): String? {
    val version = libsCatalog.findVersion(name)
    return if (version.isPresent) version.get().requiredVersion else null
}

val versionNameStr = versionStringOrNull("versionName") ?: rootProject.version.toString()
val versionCodeStr = versionStringOrNull("versionCode") ?: "1"

fun releaseTime(): String = SimpleDateFormat("yyyyMMdd_HHmmss").apply {
    timeZone = TimeZone.getDefault()
}.format(Date())

fun buildTimestampOverride(): String? = project.findProperty("buildTs")
    ?.toString()
    ?.trim()
    ?.takeIf { it.isNotEmpty() }

fun buildTimestamp(): String = buildTimestampOverride() ?: releaseTime()

fun releaseBaseName(versionName: String): String {
    val configured = project.findProperty("artifactBaseName")
        ?.toString()
        ?.trim()
        ?.takeIf { it.isNotEmpty() }
        ?: rootProject.name
    val base = configured.replace("\\s+".toRegex(), "_")
    val normalizedVersionName = versionName.replace("\\s+".toRegex(), "_")
    return "${base}_v$normalizedVersionName"
}

fun releaseApkName(versionName: String, buildType: String, abiSuffix: String, flavorName: String = ""): String {
    val base = releaseBaseName(versionName)
    val flavorTag = if (flavorName.isNotEmpty()) "${flavorName}_" else ""
    return "${abiSuffix}_${flavorTag}${base}_${buildType}.apk"
}

fun releaseAabName(versionName: String): String = "${releaseBaseName(versionName)}_release.aab"

fun registerTaskAlias(aliasName: String, targetName: String, descriptionText: String) {
    if (tasks.names.contains(aliasName)) {
        return
    }
    tasks.register(aliasName) {
        group = "build"
        description = descriptionText
        dependsOn(targetName)
    }
}

val debugBuildTimestamp = buildTimestamp()
val isBundleTask = gradle.startParameter.taskNames.any { it.contains("bundle", ignoreCase = true) }

pluginManager.withPlugin("com.android.application") {
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

    tasks.matching {
        it.name.startsWith("assemble") && it.name.contains("Play")
    }.configureEach {
        enabled = false
    }

    tasks.matching {
        it.name.startsWith("packagePlay") &&
            !it.name.contains("Bundle") &&
            !it.name.endsWith("Resources") &&
            !it.name.contains("AndroidTest") &&
            !it.name.contains("UnitTest")
    }.configureEach {
        enabled = false
    }

    tasks.matching {
        it.name.startsWith("createPlay") &&
            it.name.endsWith("ApkListingFileRedirect")
    }.configureEach {
        enabled = false
    }

    tasks.matching {
        it.name.startsWith("bundle")
    }.configureEach {
        if (!name.contains("Play") && !name.contains("ClassesTo") && !name.contains("Release")) {
            enabled = false
        }
    }

    extensions.getByType<ApplicationAndroidComponentsExtension>().apply {
        beforeVariants(selector().all()) { variantBuilder ->
            val flavors = variantBuilder.productFlavors.toMap()
            val distribution = flavors["distribution"]
            if (distribution != null) {
                val distributionEnabled = when (distribution) {
                    "play", "github", "githubNoE2ee", "githubWithE2ee" -> true
                    "fdroid" -> false
                    else -> false
                }
                if (!distributionEnabled) {
                    variantBuilder.enable = false
                    return@beforeVariants
                }
            }

            if (isBundleTask && distribution != "play" && distribution != null) {
                variantBuilder.enable = false
            }
        }

        onVariants(selector().all()) { variant ->
            val isDebug = variant.buildType == "debug"
            val suffix = if (isDebug) "-$debugBuildTimestamp" else ""
            val resolvedVersionName = "$versionNameStr$suffix"
            
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
                        .invoke(outputFileName, releaseApkName(resolvedVersionName, variant.buildType ?: "", abi, variant.flavorName ?: ""))
                } catch (_: Exception) {
                    // Ignore
                }
            }
        }
    }

    tasks.register("renameReleaseAab") {
        dependsOn("bundleRelease", "bundlePlayRelease")
        val bundleFileProvider = layout.buildDirectory.file("outputs/bundle/release/app-release.aab")
        val playBundleFileProvider = layout.buildDirectory.file("outputs/bundle/playRelease/app-play-release.aab")
        val targetFileProvider = layout.buildDirectory.file("outputs/bundle/release/${releaseAabName(versionNameStr)}")
        doLast {
            val bundleFile = bundleFileProvider.get().asFile
            val playBundleFile = playBundleFileProvider.get().asFile
            val target = targetFileProvider.get().asFile
            
            if (playBundleFile.exists()) {
                playBundleFile.copyTo(target, overwrite = true)
            } else if (bundleFile.exists()) {
                bundleFile.copyTo(target, overwrite = true)
            }
        }
    }

    tasks.matching { it.name == "bundlePlayRelease" || it.name == "bundleRelease" }.configureEach {
        finalizedBy("renameReleaseAab")
    }
}
