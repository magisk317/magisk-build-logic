import org.gradle.kotlin.dsl.dependencies

plugins {
    id("com.google.devtools.ksp")
    id("com.google.dagger.hilt.android")
}

dependencies {
    add("implementation", project.catalogLibrary("hilt-android"))
    add("ksp", project.catalogLibrary("hilt-compiler"))
    add("implementation", project.catalogLibrary("androidx-hilt-navigation-compose"))
}
