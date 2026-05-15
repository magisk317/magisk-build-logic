val cleanupTask = tasks.register("cleanupGradleCaches") {
    group = "maintenance"
    description = "Remove stale Gradle version caches under .gradle directories."
    val workspaceDir = layout.projectDirectory.asFile
    
    doLast {
        // Find all .gradle directories in the workspace
        val gradleDirs = workspaceDir.walk()
            .maxDepth(3)
            .filter { it.isDirectory && it.name == ".gradle" }
            .toList()

        if (gradleDirs.isEmpty()) {
            println("No .gradle directories found to clean.")
            return@doLast
        }

        gradleDirs.forEach { dir ->
            // Versions are directories starting with a digit (e.g., 8.10, 9.5.0-rc-4)
            val versions = dir.listFiles { file ->
                file.isDirectory && file.name.firstOrNull()?.isDigit() == true
            }?.sortedWith(compareBy({ version ->
                // Semantic sort: pre-release (rc, milestone, etc.) sorts before
                // the corresponding release. E.g. 9.5.0-rc-4 < 9.5.0.
                val base = version.name.substringBefore("-")
                val parts = base.split(".").map { it.toIntOrNull() ?: 0 }
                (parts + List(3) { 0 }).take(3).joinToString("%05d")
            }, { version ->
                // Release (no suffix) sorts after pre-release.
                if (version.name.contains("-")) "0" else "1"
            }))

            if (versions != null && versions.size > 1) {
                versions.dropLast(1).forEach { oldVersion ->
                    val relativePath = oldVersion.parentFile.relativeTo(workspaceDir)
                    println("Cleaning up old Gradle cache in $relativePath: ${oldVersion.name}")
                    oldVersion.deleteRecursively()
                }
            }
        }
    }
}

// Automatically hook to all projects' assemble tasks and the root build task
if (project == rootProject) {
    allprojects {
        tasks.matching { it.name.startsWith("assemble") || it.name == "build" }.all {
            finalizedBy(cleanupTask)
        }
        // Also ensure 'clean' triggers it for a full sweep
        tasks.matching { it.name == "clean" }.all {
            finalizedBy(cleanupTask)
        }
    }
}
