import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalog
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.getByType

internal fun Project.libsCatalog(): VersionCatalog = extensions.getByType<VersionCatalogsExtension>().named("libs")

internal fun Project.catalogVersion(name: String): String = libsCatalog()
    .findVersion(name)
    .orElseThrow { IllegalArgumentException("Version '$name' not found in consumer libs catalog") }
    .requiredVersion

internal fun Project.catalogVersionOrNull(name: String): String? = libsCatalog()
    .findVersion(name)
    .orNull
    ?.requiredVersion

internal fun Project.catalogInt(name: String): Int = catalogVersion(name).toInt()

internal fun Project.catalogIntOrNull(name: String): Int? = catalogVersionOrNull(name)?.toIntOrNull()

internal fun Project.mipushJavaTarget(): String = catalogVersionOrNull("java")
    ?: catalogVersionOrNull("javaBytecode")
    ?: "17"
