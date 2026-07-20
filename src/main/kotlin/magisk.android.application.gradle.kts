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
            val optimization = debugOptimization(
                providers.gradleProperty("minifyDebug").orNull,
            )
            isMinifyEnabled = optimization.isMinifyEnabled
            isShrinkResources = optimization.isShrinkResources
            isDebuggable = optimization.isDebuggable
            if (optimization.isMinifyEnabled) {
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
