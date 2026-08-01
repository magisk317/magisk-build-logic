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
            include("library")
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

            tasks.register('printReleasePolicy') {
                doLast {
                    def release = android.buildTypes.getByName('release')
                    println("RELEASE_POLICY minify=${'$'}{release.minifyEnabled} " +
                        "shrink=${'$'}{release.shrinkResources}")
                }
            }

            tasks.register('printSigningPolicy') {
                doLast {
                    def debug = android.buildTypes.getByName('debug')
                    def alpha = android.buildTypes.getByName('alpha')
                    def release = android.buildTypes.getByName('release')
                    println("SIGNING_POLICY debug=${'$'}{debug.signingConfig.name} " +
                        "alpha=${'$'}{alpha.signingConfig.name} " +
                        "release=${'$'}{release.signingConfig.name}")
                }
            }
            """.trimIndent(),
        )
        projectDir.resolve("src/main/AndroidManifest.xml").write(
            "<manifest />",
        )
        projectDir.resolve("proguard-rules.pro").write("")
        projectDir.resolve("library/build.gradle").write(
            """
            plugins {
                id 'magisk.android.library'
            }

            android {
                namespace = 'io.github.magisk317.debugpolicytest.library'
            }
            """.trimIndent(),
        )
        projectDir.resolve("library/src/main/AndroidManifest.xml").write("<manifest />")
        createSigningConfig()
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
    fun `release enables code and resource shrinking`() {
        val result = runner("printReleasePolicy").build()

        assertTrue(
            result.output.contains(
                "RELEASE_POLICY minify=true shrink=true",
            ),
        )
    }

    @Test
    fun `all build types use release signing`() {
        val result = runner("printSigningPolicy").build()

        assertTrue(
            result.output.contains(
                "SIGNING_POLICY debug=release alpha=release release=release",
            ),
        )
    }

    @Test
    fun `minified debug is shrunk and non-debuggable without AGP warning`() {
        val result = runner(
            "assembleDebug",
            "printDebugPolicy",
            "-PminifyDebug=true",
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

    @Test
    fun `debug packaging refuses missing release signing`() {
        deleteSigningConfig()

        val result = runner("assembleDebug").buildAndFail()

        assertTrue(result.output.contains("APK/AAB packaging requires complete release signing"))
    }

    @Test
    fun `release packaging refuses missing release signing`() {
        deleteSigningConfig()

        val result = runner("assembleRelease").buildAndFail()

        assertTrue(result.output.contains("APK/AAB packaging requires complete release signing"))
    }

    @Test
    fun `library packaging does not require application signing`() {
        deleteSigningConfig()

        val result = runner(":library:assembleDebug").build()

        assertTrue(result.output.contains("BUILD SUCCESSFUL"))
    }

    private fun runner(vararg arguments: String): GradleRunner = GradleRunner.create()
        .withProjectDir(projectDir.toFile())
        .withPluginClasspath()
        .withArguments(*arguments, "--stacktrace")
        .forwardOutput()

    private fun createSigningConfig() {
        val keytool = Path.of(
            System.getProperty("java.home"),
            "bin",
            if (System.getProperty("os.name").startsWith("Windows")) "keytool.exe" else "keytool",
        )
        val process = ProcessBuilder(
            keytool.toString(),
            "-genkeypair",
            "-alias", "test",
            "-keyalg", "RSA",
            "-keysize", "2048",
            "-validity", "3650",
            "-dname", "CN=Signing Policy Test",
            "-keystore", projectDir.resolve("release.jks").toString(),
            "-storetype", "PKCS12",
            "-storepass", "changeit",
            "-keypass", "changeit",
            "-noprompt",
        )
            .redirectErrorStream(true)
            .start()
        val output = process.inputStream.bufferedReader().use { it.readText() }
        check(process.waitFor() == 0) { "Failed to create test keystore: $output" }
        projectDir.resolve("signature.properties").write(
            """
            STORE_PASSWORD=changeit
            KEY_ALIAS=test
            KEY_PASSWORD=changeit
            """.trimIndent(),
        )
    }

    private fun deleteSigningConfig() {
        Files.deleteIfExists(projectDir.resolve("release.jks"))
        Files.deleteIfExists(projectDir.resolve("signature.properties"))
    }

    private fun Path.write(content: String) {
        parent?.let(Files::createDirectories)
        Files.writeString(this, content)
    }
}
