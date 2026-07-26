import com.android.build.api.dsl.LibraryExtension
import org.gradle.api.tasks.testing.Test
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.withType

plugins {
    id("com.android.library")
    id("magisk.android.common")
}

extensions.configure<LibraryExtension> {
    defaultConfig {
        when (val spec = parseAndroidSdkSpec(project.catalogVersion("minSdk"))) {
            is AndroidSdkSpec.ApiLevel -> minSdk = spec.value
            is AndroidSdkSpec.Preview -> {
                @Suppress("DEPRECATION")
                minSdkPreview = spec.codename
            }
        }
    }

    buildTypes {
        debug {
            isMinifyEnabled = false
            isShrinkResources = false
        }
        release {
            isMinifyEnabled = false
            isShrinkResources = false
        }
    }

    lint {
        abortOnError = false
    }

    packaging {
        jniLibs {
            keepDebugSymbols += "**/*.so"
        }
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}
