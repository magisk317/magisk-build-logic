import java.nio.file.Files
import java.nio.file.Path
import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

class DistributionFlavorPluginFunctionalTest {
    @TempDir
    lateinit var projectDir: Path

    @Test
    fun `smscode convention enables fdroid application variants`() {
        val result = runTasksFor("smscode.android.common")

        assertTrue(result.output.contains("assembleFdroidDebug"), result.output)
        assertTrue(result.output.contains("assembleFdroidRelease"), result.output)
    }

    @Test
    fun `relay convention enables fdroid application variants`() {
        val result = runTasksFor("relay.android.common")

        assertTrue(result.output.contains("assembleFdroidDebug"), result.output)
        assertTrue(result.output.contains("assembleFdroidRelease"), result.output)
    }

    private fun runTasksFor(distributionPlugin: String) = createConsumerProject(distributionPlugin).let { project ->
        GradleRunner.create()
            .withProjectDir(project.toFile())
            .withPluginClasspath()
            .withArguments("tasks", "--all", "--stacktrace")
            .build()
    }

    private fun createConsumerProject(distributionPlugin: String): Path {
        val project = projectDir.resolve(distributionPlugin.substringBefore('.'))
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
        project.resolve("build.gradle").write(
            """
            plugins {
                id 'magisk.android.application'
                id '$distributionPlugin'
                id 'magisk.app.packaging'
            }

            android {
                namespace = 'io.github.magisk317.distributiontest'
                buildFeatures.buildConfig = true
            }
            """.trimIndent(),
        )
        project.resolve("src/main/AndroidManifest.xml").write("<manifest />")
        return project
    }

    private fun Path.write(content: String) {
        parent?.let(Files::createDirectories)
        Files.writeString(this, content)
    }
}
