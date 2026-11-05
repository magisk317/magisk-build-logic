import java.nio.file.Files
import java.nio.file.Path
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class LoggingBuildConfigContractTest {
    @Test
    fun `CI identity remains but plaintext log escape is removed`() {
        val source = Files.readString(Path.of("src/main/kotlin/magisk.android.common.gradle.kts"))

        assertTrue(source.contains("IS_CI_BUILD"))
        assertTrue(source.contains("MAGISK_IS_CI_BUILD"))
        assertFalse(source.contains("ALLOW_" + "PLAINTEXT_LOGS"))
        assertFalse(source.contains("MAGISK_ALLOW_" + "PLAINTEXT_LOGS"))
        assertFalse(source.contains("allow" + "PlaintextLogs"))
    }

    @Test
    fun `public release loads stripping rules while internal CI release skips them`() {
        val source = Files.readString(Path.of("src/main/kotlin/magisk.android.application.gradle.kts"))

        assertTrue(source.contains("if (!magiskIsCiBuildForProguard)"))
        assertTrue(source.contains("proguard-release.pro"))
    }
}
