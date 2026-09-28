import org.gradle.api.tasks.testing.Test
import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent
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
// The shared catalog emits 27 for consumers (Java 27), but this included build
// is compiled by Gradle's embedded kotlin-dsl compiler, whose JvmTarget enum
// still caps at 26 - JvmTarget.fromTarget("27") fails configuration for
// every consumer pinned at a catalog with javaBytecode=27. build-logic classes
// only ever load on the build JVM, so compile them at the highest target the
// embedded compiler understands and leave the consumer-facing key authoritative
// for module compilation. Same ceiling pattern as the detekt jvmTarget clamp
// in the consumer repos.
val buildLogicCompileVersion = buildLogicBytecodeVersion.toInt().coerceAtMost(26).toString()
val junitVersion = "6.1.1"

java {
    sourceCompatibility = JavaVersion.toVersion(buildLogicCompileVersion)
    targetCompatibility = JavaVersion.toVersion(buildLogicCompileVersion)
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.fromTarget(buildLogicCompileVersion))
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
    // The functional tests load Gradle plugins through Byte Buddy on Java 26.
    jvmArgs(
        "-Xshare:off",
        "--enable-native-access=ALL-UNNAMED",
        "-XX:+EnableDynamicAgentLoading",
    )
    if (Runtime.version().feature() >= 24) {
        jvmArgs("--sun-misc-unsafe-memory-access=allow")
    }
    testLogging {
        events(
            TestLogEvent.PASSED,
            TestLogEvent.SKIPPED,
            TestLogEvent.FAILED,
        )
        exceptionFormat = TestExceptionFormat.FULL
        showExceptions = true
        showCauses = true
        showStackTraces = true
        showStandardStreams = false
    }
}
