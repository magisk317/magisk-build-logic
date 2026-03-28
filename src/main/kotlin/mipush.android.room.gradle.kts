import com.google.devtools.ksp.gradle.KspExtension
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies

plugins {
    id("com.google.devtools.ksp")
}

extensions.configure<KspExtension> {
    arg("room.schemaLocation", "${project.projectDir}/schemas")
    arg("room.incremental", "true")
}

dependencies {
    add("implementation", "androidx.room:room-runtime:${project.catalogVersion("room")}")
    add("implementation", "androidx.room:room-ktx:${project.catalogVersion("room")}")
    add("ksp", "androidx.room:room-compiler:${project.catalogVersion("room")}")
}
