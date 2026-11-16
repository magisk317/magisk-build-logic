import java.nio.file.Files
import java.nio.file.Path
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

/**
 * The distribution matrix is declared by the consuming repository and materialized by
 * the shared convention, so a useful regression test has to drive both halves.
 *
 * These tests generate a throwaway consumer project, declare a matrix in its root build
 * script, and assert on the variants AGP derives from it. Asserting on the generated task
 * list keeps the test honest: it fails either if the declaration stops being read or if
 * the convention stops applying it.
 */
class DistributionFlavorPluginFunctionalTest {
    @TempDir
    lateinit var projectDir: Path

    @Test
    fun `declared channels become application variants`() {
        val result = runTasksFor(DEFAULT_MATRIX)

        assertTrue(result.output.contains("assemblePlayDebug"), result.output)
        assertTrue(result.output.contains("assembleGithubDebug"), result.output)
        assertTrue(result.output.contains("assembleGithubRelease"), result.output)
    }

    @Test
    fun `library modules receive the same matrix`() {
        val result = runTasksFor(DEFAULT_MATRIX, modulePlugin = "magisk.android.library")

        assertTrue(result.output.contains("assemblePlayDebug"), result.output)
        assertTrue(result.output.contains("assembleGithubDebug"), result.output)
    }

    @Test
    fun `a custom dimension name is honoured`() {
        val result = runTasksFor(
            """
            magiskDistribution {
                dimension = "channel"
                flavor("play") {
                    buildConfigBoolean("HAS_BILLING", true)
                }
            }
            """.trimIndent(),
        )

        assertTrue(result.output.contains("assemblePlayDebug"), result.output)
    }

    @Test
    fun `a channel left out of the matrix produces no variant`() {
        val result = runTasksFor(
            """
            magiskDistribution {
                flavor("play") {
                    buildConfigBoolean("HAS_BILLING", true)
                }
            }
            """.trimIndent(),
        )

        assertTrue(result.output.contains("assemblePlayDebug"), result.output)
        assertFalse(result.output.contains("assembleGithubDebug"), result.output)
    }

    @Test
    fun `applying the module plugin without the root declaration fails closed`() {
        val result = runTasksFor(null, expectFailure = true)

        assertTrue(result.output.contains("magisk.distribution.policy"), result.output)
    }

    @Test
    fun `dynamic feature matches base debuggability for ordinary and minified debug`() {
        val project = createDynamicFeatureConsumer()
        val cases = listOf(
            null to true,
            "-PminifyDebug=false" to true,
            "-PminifyDebug" to false,
            "-PminifyDebug=true" to false,
            "-PminifyDebug=1" to false,
        )
        cases.forEach { (flag, debuggable) ->
            val arguments = mutableListOf(
                ":feature:processPlayDebugMainManifest",
                ":feature:processPlayReleaseMainManifest",
                "printFeatureDebugPolicy",
                "--configuration-cache",
                "--configuration-cache-problems=fail",
                "--max-workers=2",
                "--no-parallel",
                "--stacktrace",
            )
            flag?.let(arguments::add)
            System.getenv("MAGISK_ANDROID_AAPT2_OVERRIDE")
                ?.takeIf(String::isNotBlank)
                ?.let { arguments += "-Pandroid.aapt2FromMavenOverride=$it" }
            val result = GradleRunner.create()
                .withProjectDir(project.toFile())
                .withPluginClasspath()
                .withArguments(arguments)
                .build()

            assertTrue(
                result.output.contains(
                    "FEATURE_DEBUG_POLICY base=$debuggable feature=$debuggable baseRelease=false featureRelease=false",
                ),
                "flag=$flag\n${result.output}",
            )
            assertFalse(result.output.contains("corresponding build type in the base application is not debuggable"))
        }
    }

    private fun createDynamicFeatureConsumer(): Path {
        val matrix = DEFAULT_MATRIX.replace(
            "magiskDistribution {",
            "magiskDistribution {\n    dynamicFeatureFlavors.add(\"play\")",
        )
        val project = createConsumerProject(matrix, "magisk.android.application")
        Files.writeString(
            project.resolve("settings.gradle.kts"),
            "\ninclude(\":feature\")\n",
            java.nio.file.StandardOpenOption.APPEND,
        )
        Files.writeString(
            project.resolve("app/build.gradle.kts"),
            "\n" + """
            |android {
            |    dynamicFeatures += setOf(":feature")
            |    defaultConfig {
            |        applicationId = "io.github.magisk317.distributiontest"
            |        versionCode = 1
            |        versionName = "1.0.0"
            |    }
            |}
            """.trimMargin() + "\n",
            java.nio.file.StandardOpenOption.APPEND,
        )
        project.resolve("feature/build.gradle.kts").write(
            """
            plugins {
                id("com.android.dynamic-feature")
                id("magisk.android.distribution")
            }
            android {
                namespace = "io.github.magisk317.distributiontest.feature"
                buildFeatures.buildConfig = true
            }
            dependencies { implementation(project(":app")) }
            """.trimIndent(),
        )
        project.resolve("feature/src/main/AndroidManifest.xml").write(
            """
            <manifest xmlns:dist="http://schemas.android.com/apk/distribution">
                <dist:module dist:instant="false" dist:title="@string/feature_title">
                    <dist:delivery><dist:on-demand /></dist:delivery>
                    <dist:fusing dist:include="false" />
                </dist:module>
            </manifest>
            """.trimIndent(),
        )
        project.resolve("feature/src/main/res/values/strings.xml").write(
            """<resources><string name="feature_title">Test feature</string></resources>""",
        )
        Files.writeString(
            project.resolve("build.gradle.kts"),
            "\n" + """
            |val debugPolicy = objects.property(String::class.java)
            |gradle.projectsEvaluated {
            |    val base = project(":app").extensions.getByType<com.android.build.api.dsl.ApplicationExtension>()
            |    val feature = project(":feature").extensions.getByType<com.android.build.api.dsl.DynamicFeatureExtension>()
            |    debugPolicy.set(
            |        "FEATURE_DEBUG_POLICY base=${'$'}{base.buildTypes.getByName("debug").isDebuggable} " +
            |            "feature=${'$'}{feature.buildTypes.getByName("debug").isDebuggable} " +
            |            "baseRelease=${'$'}{base.buildTypes.getByName("release").isDebuggable} " +
            |            "featureRelease=${'$'}{feature.buildTypes.getByName("release").isDebuggable}",
            |    )
            |}
            |tasks.register("printFeatureDebugPolicy") {
            |    val policy = debugPolicy
            |    inputs.property("policy", policy)
            |    doLast { println(policy.get()) }
            |}
            """.trimMargin() + "\n",
            java.nio.file.StandardOpenOption.APPEND,
        )
        return project
    }

    private fun runTasksFor(
        matrix: String?,
        modulePlugin: String = "magisk.android.application",
        expectFailure: Boolean = false,
    ): BuildResult {
        val project = createConsumerProject(matrix, modulePlugin)
        val runner = GradleRunner.create()
            .withProjectDir(project.toFile())
            .withPluginClasspath()
            .withArguments("tasks", "--all", "--stacktrace")
        return if (expectFailure) runner.buildAndFail() else runner.build()
    }

    private fun createConsumerProject(matrix: String?, modulePlugin: String): Path {
        val project = projectDir.resolve("distribution-consumer")
        project.resolve("settings.gradle.kts").write(
            """
            pluginManagement {
                repositories {
                    google()
                    mavenCentral()
                    gradlePluginPortal()
                }
            }
            dependencyResolutionManagement {
                repositories {
                    google()
                    mavenCentral()
                }
            }
            rootProject.name = "distribution-test"
            include(":app")
            """.trimIndent(),
        )
        project.resolve("gradle/libs.versions.toml").write(
            """
            [versions]
            compileSdk = "37"
            minSdk = "26"
            targetSdk = "37"
            javaBytecode = "26"
            versionName = "1.0.0"
            """.trimIndent(),
        )
        androidSdkDir()?.let { sdk ->
            project.resolve("local.properties").write("sdk.dir=$sdk\n")
        }
        project.resolve("build.gradle.kts").write(
            buildString {
                appendLine("plugins {")
                if (matrix != null) {
                    appendLine("""    id("magisk.distribution.policy")""")
                }
                appendLine("}")
                if (matrix != null) {
                    appendLine()
                    appendLine(matrix)
                }
            },
        )
        project.resolve("app/build.gradle.kts").write(
            """
            plugins {
                id("$modulePlugin")
                id("magisk.android.distribution")
            }

            android {
                namespace = "io.github.magisk317.distributiontest"
                buildFeatures.buildConfig = true
            }
            """.trimIndent(),
        )
        project.resolve("app/src/main/AndroidManifest.xml").write("<manifest />")
        return project
    }

    private fun androidSdkDir(): String? =
        System.getenv("ANDROID_HOME") ?: System.getenv("ANDROID_SDK_ROOT")

    private fun Path.write(content: String) {
        parent?.let(Files::createDirectories)
        Files.writeString(this, content)
    }

    private companion object {
        val DEFAULT_MATRIX =
            """
            magiskDistribution {
                flavor("play") {
                    buildConfigBoolean("HAS_BILLING", true)
                }
                flavor("github") {
                    buildConfigBoolean("HAS_BILLING", false)
                }
            }
            """.trimIndent()
    }
}
