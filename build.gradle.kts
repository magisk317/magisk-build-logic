plugins {
    `kotlin-dsl`
}

val libsCatalog = extensions.getByType<org.gradle.api.artifacts.VersionCatalogsExtension>().named("libs")

repositories {
    google()
    mavenCentral()
    gradlePluginPortal()
}

dependencies {
    implementation(libsCatalog.findLibrary("android-gradlePlugin").get())
    implementation(libsCatalog.findLibrary("kotlin-gradlePlugin").get())
    implementation(libsCatalog.findLibrary("kotlin-compose-gradlePlugin").get())
    implementation(libsCatalog.findLibrary("google-services-gradlePlugin").get())
    implementation(libsCatalog.findLibrary("ksp-gradlePlugin").get())
    implementation(libsCatalog.findLibrary("hilt-gradlePlugin").get())
}
