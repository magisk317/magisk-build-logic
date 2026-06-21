import com.android.build.api.dsl.ApplicationExtension
import org.gradle.kotlin.dsl.configure

plugins {
    id("com.android.application")
    id("magisk.android.common")
}

extensions.configure<ApplicationExtension> {
    defaultConfig {
        minSdk = project.catalogInt("minSdk")
        targetSdk = project.catalogInt("targetSdk")
    }

    buildTypes {
        debug {
            val minifyDebug = providers.gradleProperty("minifyDebug")
                .map { value ->
                    value.isBlank() || value.equals("true", ignoreCase = true) || value == "1"
                }
                .orElse(false)
                .get()
            isMinifyEnabled = minifyDebug
            isShrinkResources = minifyDebug
            isDebuggable = true
            if (minifyDebug) {
                proguardFiles(
                    getDefaultProguardFile("proguard-android-optimize.txt"),
                    "proguard-rules.pro",
                )
            }
        }
        release {
            isMinifyEnabled = false
            isShrinkResources = false
        }
    }

    packaging {
        jniLibs {
            keepDebugSymbols += "**/*.so"
        }
    }
}
