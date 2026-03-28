import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.dsl.LibraryExtension
import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinAndroidProjectExtension

internal fun Project.configureMipushAndroidCommon(applicationExtension: ApplicationExtension) {
    applicationExtension.apply {
        compileSdk = catalogInt("compileSdk")
        catalogIntOrNull("compileSdkExtension")?.let { compileSdkExtension = it }

        compileOptions {
            val javaVersion = JavaVersion.toVersion(mipushJavaTarget())
            sourceCompatibility = javaVersion
            targetCompatibility = javaVersion
        }
    }

    configureMipushKotlinJvm()
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
    }

    configureMipushKotlinJvm()
}

private fun Project.configureMipushKotlinJvm() {
    extensions.configure<KotlinAndroidProjectExtension> {
        compilerOptions {
            jvmTarget.set(JvmTarget.fromTarget(mipushJavaTarget()))
        }
    }
}
