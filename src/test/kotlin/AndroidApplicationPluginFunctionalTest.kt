import java.nio.file.Files
import java.nio.file.Path
import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

class AndroidApplicationPluginFunctionalTest {
    @TempDir
    lateinit var projectDir: Path

    @BeforeEach
    fun createConsumerProject() {
        projectDir.resolve("settings.gradle.kts").write(
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
            rootProject.name = "debug-policy-test"
            """.trimIndent(),
        )
        projectDir.resolve("gradle/libs.versions.toml").write(
            """
            [versions]
            compileSdk = "37"
            minSdk = "26"
            targetSdk = "37"
            javaBytecode = "25"
            """.trimIndent(),
        )
        projectDir.resolve("build.gradle").write(
            """
            plugins {
                id 'magisk.android.application'
                id 'magisk.app.signing'
            }

            android {
                namespace = 'io.github.magisk317.debugpolicytest'
            }

            tasks.register('printDebugPolicy') {
                doLast {
                    def debug = android.buildTypes.getByName('debug')
                    println("DEBUG_POLICY minify=${'$'}{debug.minifyEnabled} " +
                        "shrink=${'$'}{debug.shrinkResources} debuggable=${'$'}{debug.debuggable}")
                }
            }

            tasks.register('printAlphaPolicy') {
                doLast {
                    def alpha = android.buildTypes.getByName('alpha')
                    println("ALPHA_POLICY fallbacks=${'$'}{alpha.matchingFallbacks} " +
                        "debuggable=${'$'}{alpha.debuggable}")
                }
            }
            """.trimIndent(),
        )
        projectDir.resolve("src/main/AndroidManifest.xml").write(
            "<manifest />",
        )
        projectDir.resolve("proguard-rules.pro").write("")
    }

    @Test
    fun `ordinary debug remains debuggable`() {
        val result = runner("printDebugPolicy").build()

        assertTrue(
            result.output.contains(
                "DEBUG_POLICY minify=false shrink=false debuggable=true",
            ),
        )
    }

    @Test
    fun `alpha uses release policy and dependency fallback`() {
        val result = runner("printAlphaPolicy").build()

        assertTrue(
            result.output.contains(
                "ALPHA_POLICY fallbacks=[release] debuggable=false",
            ),
        )
    }

    @Test
    fun `minified debug is shrunk and non-debuggable without AGP warning`() {
        val result = runner(
            "assembleDebug",
            "printDebugPolicy",
            "-PminifyDebug=true",
            "-PallowIncompatibleDebugSigning=true",
            "--warning-mode=all",
        ).build()

        assertTrue(
            result.output.contains(
                "DEBUG_POLICY minify=true shrink=true debuggable=false",
            ),
        )
        assertFalse(result.output.contains("DEBUGGABLE_AND_MINIFIED_ENABLED"))
        assertTrue(Files.isRegularFile(projectDir.resolve("build/outputs/apk/debug/debug-policy-test-debug.apk")))
    }

    private fun runner(vararg arguments: String): GradleRunner = GradleRunner.create()
        .withProjectDir(projectDir.toFile())
        .withPluginClasspath()
        .withArguments(*arguments, "--stacktrace")
        .forwardOutput()

    private fun Path.write(content: String) {
        parent?.let(Files::createDirectories)
        Files.writeString(this, content)
    }
}
