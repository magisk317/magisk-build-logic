import com.android.build.api.dsl.ApplicationExtension
import org.gradle.kotlin.dsl.configure

plugins {
    id("com.android.application")
}

extensions.configure<ApplicationExtension> {
    project.configureMipushAndroidCommon(this)

    defaultConfig {
        minSdk = project.catalogInt("minSdk")
        targetSdk = project.catalogInt("targetSdk")
    }

    signingConfigs {
        val signing = project.resolveMipushSigningMaterial()
        getByName("debug").applySigningMaterial(signing)
        maybeCreate("release").applySigningMaterial(signing)
    }

    buildTypes {
        debug {
            val minifyDebug = providers.gradleProperty("minifyDebug")
                .map { value ->
                    value.isBlank() || value.equals("true", ignoreCase = true) || value == "1"
                }
                .orElse(false)
                .get()
            signingConfig = signingConfigs.getByName("debug")
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
            signingConfig = signingConfigs.getByName("release")
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
