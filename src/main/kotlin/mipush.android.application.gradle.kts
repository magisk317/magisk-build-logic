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
            signingConfig = signingConfigs.getByName("debug")
            isMinifyEnabled = true
            isShrinkResources = true
            isDebuggable = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
        release {
            signingConfig = signingConfigs.getByName("release")
            isMinifyEnabled = true
            isShrinkResources = true
        }
    }

    packaging {
        jniLibs {
            keepDebugSymbols += "**/*.so"
        }
    }
}
