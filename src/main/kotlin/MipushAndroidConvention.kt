import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.dsl.LibraryExtension
import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile

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
    configureKotlinJvmTarget()
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
    configureKotlinJvmTarget()
}

internal fun Project.configureKotlinJvmTarget() {
    tasks.withType<KotlinJvmCompile>().configureEach {
        compilerOptions.jvmTarget.set(mipushKotlinJvmTarget())
    }
}
