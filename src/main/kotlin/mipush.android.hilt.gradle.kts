import org.gradle.kotlin.dsl.dependencies

plugins {
    id("com.google.devtools.ksp")
    id("com.google.dagger.hilt.android")
}

dependencies {
    add("implementation", "com.google.dagger:hilt-android:${project.catalogVersion("hilt")}")
    add("ksp", "com.google.dagger:hilt-compiler:${project.catalogVersion("hilt")}")
    add("implementation", "androidx.hilt:hilt-navigation-compose:1.3.0")
}
