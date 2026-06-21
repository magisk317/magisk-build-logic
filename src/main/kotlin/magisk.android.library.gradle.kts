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
        minSdk = project.catalogInt("minSdk")
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
    testLogging {
        events("passed", "skipped", "failed")
    }
}
