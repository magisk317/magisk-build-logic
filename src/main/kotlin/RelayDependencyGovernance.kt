import org.gradle.api.Project

internal data class RelayForcedDependency(
    val group: String,
    val name: String,
    val versionAlias: String,
) {
    fun notation(project: Project): String {
        return "$group:$name:${project.relayForcedVersion(versionAlias)}"
    }
}

internal val relayForcedDependencies = listOf(
    RelayForcedDependency("com.google.code.gson", "gson", "gson"),
    RelayForcedDependency("com.google.guava", "guava", "guava"),
    RelayForcedDependency("io.netty", "netty-codec", "netty-codec"),
    RelayForcedDependency("io.netty", "netty-codec-http", "netty-codec"),
    RelayForcedDependency("io.netty", "netty-codec-http2", "netty-codec"),
    RelayForcedDependency("io.netty", "netty-common", "netty-runtime"),
    RelayForcedDependency("io.netty", "netty-handler", "netty-runtime"),
    RelayForcedDependency("io.netty", "netty-handler-proxy", "netty-codec"),
    RelayForcedDependency("org.apache.commons", "commons-lang3", "commons-lang3"),
    RelayForcedDependency("org.bitbucket.b_c", "jose4j", "jose4j"),
    RelayForcedDependency("org.bouncycastle", "bcpkix-jdk18on", "bouncycastle"),
    RelayForcedDependency("org.bouncycastle", "bcprov-jdk18on", "bouncycastle"),
    RelayForcedDependency("org.jdom", "jdom2", "jdom2"),
    // Java 26 Robolectric bytecode compatibility
    RelayForcedDependency("org.ow2.asm", "asm", "asm"),
    RelayForcedDependency("org.ow2.asm", "asm-commons", "asm"),
    RelayForcedDependency("org.ow2.asm", "asm-tree", "asm"),
    RelayForcedDependency("org.ow2.asm", "asm-util", "asm"),
    RelayForcedDependency("org.ow2.asm", "asm-analysis", "asm"),
    RelayForcedDependency("org.jetbrains.kotlin", "kotlin-metadata-jvm", "kotlin"),
)

internal fun Project.configureRelayForcedDependencies() {
    configurations.configureEach {
        resolutionStrategy.eachDependency {
            val forcedDependency = relayForcedDependencies.firstOrNull {
                requested.group == it.group && requested.name == it.name
            } ?: return@eachDependency
            val forcedVersion = catalogVersionOrNull(forcedDependency.versionAlias)
                ?: throw IllegalArgumentException(
                    "Version '${forcedDependency.versionAlias}' not found in consumer libs catalog",
                )
            useVersion(forcedVersion)
            because("centralized relay dependency governance")
        }
    }
}

private fun Project.relayForcedVersion(versionAlias: String): String {
    return catalogVersionOrNull(versionAlias)
        ?: throw IllegalArgumentException("Version '$versionAlias' not found in consumer libs catalog")
}
