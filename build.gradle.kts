import org.gradle.api.tasks.testing.Test
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    `kotlin-dsl`
}

val libsCatalog = extensions.getByType<org.gradle.api.artifacts.VersionCatalogsExtension>().named("libs")

fun catalogVersion(name: String): String {
    return libsCatalog.findVersion(name)
        .orElseThrow { IllegalArgumentException("Version '$name' not found in consumer libs catalog") }
        .requiredVersion
}

val agpVersion = catalogVersion("agp")
val kotlinVersion = catalogVersion("kotlin")
val kspVersion = catalogVersion("ksp")
val buildLogicBytecodeVersion = catalogVersion("javaBytecode")
val junitVersion = "6.1.1"

java {
    sourceCompatibility = JavaVersion.toVersion(buildLogicBytecodeVersion)
    targetCompatibility = JavaVersion.toVersion(buildLogicBytecodeVersion)
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.fromTarget(buildLogicBytecodeVersion))
    }
}

fun forcedDependency(group: String, name: String, versionAlias: String): String {
    return "$group:$name:${catalogVersion(versionAlias)}"
}

configurations.configureEach {
    resolutionStrategy {
        force(forcedDependency("io.netty", "netty-codec", "netty-codec"))
        force(forcedDependency("io.netty", "netty-codec-http", "netty-codec"))
        force(forcedDependency("io.netty", "netty-codec-http2", "netty-codec"))
        force(forcedDependency("io.netty", "netty-common", "netty-runtime"))
        force(forcedDependency("io.netty", "netty-handler", "netty-runtime"))
        force(forcedDependency("io.netty", "netty-handler-proxy", "netty-codec"))
        force(forcedDependency("org.apache.commons", "commons-lang3", "commons-lang3"))
        force(forcedDependency("org.bitbucket.b_c", "jose4j", "jose4j"))
        force(forcedDependency("org.bouncycastle", "bcpkix-jdk18on", "bouncycastle"))
        force(forcedDependency("org.bouncycastle", "bcprov-jdk18on", "bouncycastle"))
        force(forcedDependency("org.jdom", "jdom2", "jdom2"))
        force(forcedDependency("org.ow2.asm", "asm", "asm"))
        force(forcedDependency("org.ow2.asm", "asm-commons", "asm"))
        force(forcedDependency("org.ow2.asm", "asm-tree", "asm"))
        force(forcedDependency("org.ow2.asm", "asm-util", "asm"))
        force(forcedDependency("org.ow2.asm", "asm-analysis", "asm"))
    }
}

dependencies {
    implementation("com.android.tools.build:gradle:$agpVersion")
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlinVersion")
    implementation("org.jetbrains.kotlin:compose-compiler-gradle-plugin:$kotlinVersion")
    implementation("com.google.devtools.ksp:symbol-processing-gradle-plugin:$kspVersion")

    testImplementation(gradleTestKit())
    testImplementation("org.junit.jupiter:junit-jupiter:$junitVersion")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:$junitVersion")
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}
