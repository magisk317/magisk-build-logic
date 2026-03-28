import com.android.build.api.dsl.SigningConfig
import org.gradle.api.Project
import java.io.File
import java.util.Properties

internal data class MipushSigningMaterial(
    val keyStoreFile: File,
    val keyStorePassword: String?,
    val keyAlias: String?,
    val keyPassword: String?,
)

internal fun Project.resolveMipushSigningMaterial(): MipushSigningMaterial {
    var keyStoreFile = rootProject.file(".yuuta.jks")
    var keyStorePassword = System.getenv("KEYSTORE_PASS")
    var keyAlias = System.getenv("ALIAS_NAME")
    var keyPassword = System.getenv("ALIAS_PASS")
    val localProperties = rootProject.file("local.properties")
    if (localProperties.exists()) {
        val properties = Properties()
        properties.load(localProperties.inputStream())
        keyStoreFile = properties.getProperty("KEY_LOCATE")?.let(rootProject::file) ?: keyStoreFile
        keyStorePassword = properties.getProperty("KEYSTORE_PASSWORD") ?: keyStorePassword
        keyAlias = properties.getProperty("KEYSTORE_ALIAS") ?: keyAlias
        keyPassword = properties.getProperty("KEY_PASSWORD") ?: keyPassword
    }
    return MipushSigningMaterial(
        keyStoreFile = keyStoreFile,
        keyStorePassword = keyStorePassword,
        keyAlias = keyAlias,
        keyPassword = keyPassword,
    )
}

internal fun SigningConfig.applySigningMaterial(signing: MipushSigningMaterial) {
    if (!signing.keyStoreFile.exists()) return
    storeFile = signing.keyStoreFile
    storePassword = signing.keyStorePassword
    keyAlias = signing.keyAlias
    keyPassword = signing.keyPassword
}
