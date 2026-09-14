import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.dsl.CommonExtension
import com.android.build.api.dsl.DynamicFeatureExtension
import com.android.build.api.dsl.LibraryExtension
import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

fun CommonExtension.configureMagiskAndroidCommon() {
    compileSdk(project.magiskCompileSdk())
    project.catalogIntOrNull("compileSdkExtension")?.let { compileSdkExtension = it }

    val javaVersion = JavaVersion.toVersion(project.magiskJavaTarget())
    compileOptions.sourceCompatibility = javaVersion
    compileOptions.targetCompatibility = javaVersion
}

configureMagiskKotlinJvmTarget()

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
