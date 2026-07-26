import java.nio.file.Files
import java.nio.file.Path
import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

class MaintenancePluginFunctionalTest {
    @TempDir
    lateinit var projectDir: Path

    @Test
    fun `configures native test logging for subprojects`() {
        projectDir.resolve("settings.gradle.kts").write(
            """
            rootProject.name = "maintenance-test"
            include(":sample")
            """.trimIndent(),
        )
        projectDir.resolve("build.gradle").write(
            """
            plugins {
                id 'magisk.maintenance'
            }

            tasks.register('printTestLogging') {
                doLast {
                    def logging = project(':sample').tasks.named('test').get().testLogging
                    println("TEST_EVENTS ${'$'}{logging.events.toList().sort()}")
                    println("TEST_EXCEPTION_FORMAT ${'$'}{logging.exceptionFormat}")
                }
            }
            """.trimIndent(),
        )
        projectDir.resolve("sample/build.gradle").write(
            """
            plugins {
                id 'java'
            }
            """.trimIndent(),
        )

        val result = GradleRunner.create()
            .withProjectDir(projectDir.toFile())
            .withPluginClasspath()
            .withArguments("printTestLogging", "--stacktrace")
            .build()

        assertTrue(result.output.contains("TEST_EVENTS [PASSED, SKIPPED, FAILED]"), result.output)
        assertTrue(result.output.contains("TEST_EXCEPTION_FORMAT FULL"), result.output)
    }

    private fun Path.write(content: String) {
        parent?.let(Files::createDirectories)
        Files.writeString(this, content)
    }
}
