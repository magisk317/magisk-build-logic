import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ArtifactNamingTest {
    @Test
    fun `omits plain GitHub distribution labels`() {
        listOf("", "github", "githubNoE2ee").forEach { flavorName ->
            assertEquals(
                "arm64-v8a_MiPush_v0.7.0_release.apk",
                releaseApkName("MiPush", "0.7.0", "release", "arm64-v8a", flavorName),
            )
        }
    }

    @Test
    fun `uses concise E2EE distribution label`() {
        assertEquals(
            "arm64-v8a_e2ee_XinyiRelay_v0.2.2_release.apk",
            releaseApkName("XinyiRelay", "0.2.2", "release", "arm64-v8a", "githubWithE2ee"),
        )
    }

    @Test
    fun `preserves non-distribution flavor labels`() {
        assertEquals(
            "universal_vc105_xmsf_v0.7.0_release.apk",
            releaseApkName("xmsf", "0.7.0", "release", "universal", "vc105"),
        )
    }

    @Test
    fun `normalizes surrounding and repeated whitespace`() {
        assertEquals(
            "arm64-v8a_MiPush_Framework_v0.7.0_rc1_release.apk",
            releaseApkName("  MiPush  Framework  ", " 0.7.0  rc1 ", "release", "arm64-v8a"),
        )
    }
}
