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
val versionNameStr = libsCatalog.findVersion("versionName").get().requiredVersion
val versionCodeStr = libsCatalog.findVersion("versionCode").get().requiredVersion
val releaseArtifactVersionName = "$versionNameStr($versionCodeStr)"

fun releaseTime(): String = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").apply {
    timeZone = TimeZone.getDefault()
}.format(Date())

fun buildTimestampOverride(): String? = project.findProperty("buildTs")
    ?.toString()
    ?.trim()
    ?.takeIf { it.isNotEmpty() }

fun buildTimestamp(): String = buildTimestampOverride() ?: SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").apply {
    timeZone = TimeZone.getDefault()
}.format(Date())

fun releaseBaseName(versionName: String): String {
    val normalizedVersionName = versionName.replace("\\s+".toRegex(), "_")
    return "XinyiRelay_v$normalizedVersionName"
}

fun releaseApkName(versionName: String, buildType: String, abiSuffix: String, e2eeFlavor: String = ""): String {
    val base = releaseBaseName(versionName)
    val e2eeTag = when (e2eeFlavor) {
        "withE2ee" -> "_e2ee"
        "noE2ee" -> "_noE2ee"
        else -> ""
    }
    if (abiSuffix == "universal" || abiSuffix.isEmpty()) {
        return "${base}_universal${e2eeTag}_${buildType}.apk"
    }
    return "${base}_${abiSuffix}${e2eeTag}_${buildType}.apk"
}

fun releaseAabName(versionName: String): String = "${releaseBaseName(versionName)}_release.aab"

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
        if (!name.contains("Play") && !name.contains("ClassesTo")) {
            enabled = false
        }
    }

    extensions.getByType<ApplicationAndroidComponentsExtension>().apply {
        beforeVariants(selector().all()) { variantBuilder ->
            val flavors = variantBuilder.productFlavors.toMap()
            val distribution = flavors["distribution"]
            val distributionEnabled = when (distribution) {
                "play" -> true
                "github" -> true
                "fdroid" -> false
                else -> false
            }
            if (!distributionEnabled) {
                variantBuilder.enable = false
                return@beforeVariants
            }

            // only github ships both noE2ee and withE2ee APKs;
            // play uses Dynamic Feature Module for on-demand E2EE
            val e2ee = flavors["e2ee"]
            if (e2ee == "withE2ee" && distribution != "github") {
                variantBuilder.enable = false
                return@beforeVariants
            }

            if (isBundleTask && distribution != "play") {
                variantBuilder.enable = false
            }
        }

        onVariants(selector().all()) { variant ->
            val isDebug = variant.buildType == "debug"
            val suffix = if (isDebug) debugBuildTimestamp else ""
            val resolvedVersionName = if (isDebug) "$versionNameStr-$suffix" else versionNameStr
            val e2eeFlavor = variant.productFlavors.toMap()["e2ee"] ?: ""

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
                        .invoke(outputFileName, releaseApkName(resolvedVersionName, variant.buildType ?: "", abi, e2eeFlavor))
                } catch (_: Exception) {
                    // AGP preview APIs can shift; keep the build tolerant here.
                }
            }
        }
    }

    tasks.register("renamePlayReleaseAab") {
        dependsOn("bundlePlayRelease")
        val bundleFileProvider = layout.buildDirectory.file("outputs/bundle/playRelease/app-play-release.aab")
        val targetFileProvider = layout.buildDirectory.file("outputs/bundle/playRelease/${releaseAabName(releaseArtifactVersionName)}")
        doLast {
            val bundleFile = bundleFileProvider.get().asFile
            if (bundleFile.exists()) {
                val target = targetFileProvider.get().asFile
                bundleFile.copyTo(target, overwrite = true)
            }
        }
    }

    tasks.matching { it.name == "bundlePlayRelease" }.configureEach {
        finalizedBy("renamePlayReleaseAab")
    }
}
