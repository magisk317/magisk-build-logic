import org.gradle.kotlin.dsl.dependencies

plugins {
    id("org.jetbrains.kotlin.plugin.compose")
}

dependencies {
    add("implementation", platform("androidx.compose:compose-bom:${project.catalogVersion("compose-bom")}"))
    add("implementation", "androidx.compose.ui:ui")
    add("implementation", "androidx.compose.material3:material3:${project.catalogVersion("material3")}")
    add("implementation", "androidx.compose.ui:ui-tooling-preview")
    add("debugImplementation", "androidx.compose.ui:ui-tooling")
    add("implementation", "androidx.lifecycle:lifecycle-runtime-ktx:${project.catalogVersion("lifecycle")}")
    add("implementation", "androidx.activity:activity-compose:${project.catalogVersion("activity-compose")}")
    add("implementation", "androidx.navigation:navigation-compose:${project.catalogVersion("navigation")}")
    add("implementation", "androidx.lifecycle:lifecycle-viewmodel-compose:${project.catalogVersion("lifecycle")}")
    add("implementation", "androidx.lifecycle:lifecycle-runtime-compose:${project.catalogVersion("lifecycle")}")
}
