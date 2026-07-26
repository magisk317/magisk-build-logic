import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class AndroidSdkSpecTest {
    @Test
    fun `parses numeric SDK levels`() {
        assertEquals(AndroidSdkSpec.ApiLevel(37), parseAndroidSdkSpec("37"))
        assertEquals(AndroidSdkSpec.ApiLevel(37), parseAndroidSdkSpec("android-37"))
    }

    @Test
    fun `parses preview SDK codenames`() {
        assertEquals(AndroidSdkSpec.Preview("Baklava"), parseAndroidSdkSpec("android-Baklava"))
        assertEquals(AndroidSdkSpec.Preview("Baklava"), parseAndroidSdkSpec("Baklava"))
    }

    @Test
    fun `rejects blank SDK values`() {
        assertThrows(IllegalArgumentException::class.java) {
            parseAndroidSdkSpec("  ")
        }
    }
}
