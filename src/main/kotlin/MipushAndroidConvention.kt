import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.dsl.LibraryExtension
import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

internal fun Project.configureMipushAndroidCommon(applicationExtension: ApplicationExtension) {
    applicationExtension.apply {
        compileSdk = catalogInt("compileSdk")
        catalogIntOrNull("compileSdkExtension")?.let { compileSdkExtension = it }

        compileOptions {
            val javaVersion = JavaVersion.toVersion(mipushJavaTarget())
            sourceCompatibility = javaVersion
            targetCompatibility = javaVersion
        }

        kotlin {
            compilerOptions {
                jvmTarget.set(JvmTarget.fromTarget(project.mipushJavaTarget()))
            }
        }
    }
}

internal fun Project.configureMipushAndroidCommon(libraryExtension: LibraryExtension) {
    libraryExtension.apply {
        compileSdk = catalogInt("compileSdk")
        catalogIntOrNull("compileSdkExtension")?.let { compileSdkExtension = it }

        compileOptions {
            val javaVersion = JavaVersion.toVersion(mipushJavaTarget())
            sourceCompatibility = javaVersion
            targetCompatibility = javaVersion
        }

        kotlin {
            compilerOptions {
                jvmTarget.set(JvmTarget.fromTarget(project.mipushJavaTarget()))
            }
        }
    }
}
