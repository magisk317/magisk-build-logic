pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
    versionCatalogs {
        create("libs") {
            val catalog = file("shared/gradle/libs.versions.toml")
            check(catalog.isFile) {
                "magisk-build-logic requires a catalog at shared/gradle/libs.versions.toml"
            }
            from(files(catalog))
        }
    }
}
