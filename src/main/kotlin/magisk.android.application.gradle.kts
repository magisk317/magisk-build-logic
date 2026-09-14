import com.android.build.api.dsl.ApplicationExtension
import org.gradle.kotlin.dsl.configure

plugins {
    id("com.android.application")
    id("magisk.android.common")
}

extensions.configure<ApplicationExtension> {
    val projectProguardFile = sequenceOf("proguard-rules.pro", "proguard-common.pro")
        .firstOrNull { project.file(it).isFile }

    defaultConfig {
        when (val spec = parseAndroidSdkSpec(project.propertyOrCatalogVersion("minSdk", "minSdk"))) {
            is AndroidSdkSpec.ApiLevel -> minSdk = spec.value
            is AndroidSdkSpec.Minor -> minSdk = spec.apiLevel
            is AndroidSdkSpec.Beta -> minSdk = spec.apiLevel
            is AndroidSdkSpec.Preview -> {
                @Suppress("DEPRECATION")
                minSdkPreview = spec.codename
            }
        }
        when (val spec = parseAndroidSdkSpec(project.catalogVersion("targetSdk"))) {
            is AndroidSdkSpec.ApiLevel -> targetSdk = spec.value
            is AndroidSdkSpec.Minor -> targetSdk = spec.apiLevel
            is AndroidSdkSpec.Beta -> targetSdk = spec.apiLevel
            is AndroidSdkSpec.Preview -> targetSdkPreview = spec.codename
        }
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
                proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"))
                projectProguardFile?.let(::proguardFile)
            }
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"))
            projectProguardFile?.let(::proguardFile)
        }
    }

    packaging {
        jniLibs {
            keepDebugSymbols += "**/*.so"
        }
    }
}
