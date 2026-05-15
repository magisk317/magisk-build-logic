pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
    versionCatalogs {
        create("libs") {
            val parentCatalog = file("../gradle/libs.versions.toml")
            if (parentCatalog.isFile) {
                from(files(parentCatalog))
            } else {
                version("agp", "9.2.1")
                version("kotlin", "2.3.21")
                version("ksp", "2.3.7")
                version("hilt", "2.59.2")
            }
        }
    }
}
