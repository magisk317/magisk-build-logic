import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.dsl.CommonExtension
import com.android.build.api.dsl.DynamicFeatureExtension
import com.android.build.api.dsl.LibraryExtension
import org.gradle.api.JavaVersion
import org.gradle.api.tasks.testing.Test
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile

fun CommonExtension.configureMagiskAndroidCommon() {
    val compileSdkApi = project.catalogInt("compileSdk")
    compileSdk = compileSdkApi
    if (compileSdkApi >= 35) {
        compileSdkMinor = 0
    }
    project.catalogIntOrNull("compileSdkExtension")?.let { compileSdkExtension = it }

    val javaVersion = JavaVersion.toVersion(project.magiskJavaTarget())
    compileOptions.sourceCompatibility = javaVersion
    compileOptions.targetCompatibility = javaVersion
}

tasks.withType<KotlinJvmCompile>().configureEach {
    compilerOptions.jvmTarget.set(project.magiskKotlinJvmTarget())
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}

pluginManager.withPlugin("com.android.application") {
    extensions.configure<ApplicationExtension> {
        configureMagiskAndroidCommon()
    }
}

pluginManager.withPlugin("com.android.library") {
    extensions.configure<LibraryExtension> {
        configureMagiskAndroidCommon()
    }
}

pluginManager.withPlugin("com.android.dynamic-feature") {
    extensions.configure<DynamicFeatureExtension> {
        configureMagiskAndroidCommon()
    }
}
