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
            val parentCatalog = file("../gradle/libs.versions.toml")
            check(parentCatalog.isFile) {
                "magisk-build-logic requires a consumer catalog at ../gradle/libs.versions.toml"
            }
            from(files(parentCatalog))
        }
    }
}
