plugins {
    `kotlin-dsl`
}

val libsCatalog = extensions.getByType<org.gradle.api.artifacts.VersionCatalogsExtension>().named("libs")

fun catalogVersion(name: String, fallback: String): String {
    return libsCatalog.findVersion(name)
        .map { it.requiredVersion }
        .orElse(fallback)
}

val agpVersion = catalogVersion("agp", "9.2.1")
val kotlinVersion = catalogVersion("kotlin", "2.3.21")
val kspVersion = catalogVersion("ksp", "2.3.7")
val hiltVersion = catalogVersion("hilt", "2.59.2")

repositories {
    google()
    mavenCentral()
    gradlePluginPortal()
}

configurations.configureEach {
    resolutionStrategy {
        force("io.netty:netty-codec:4.1.133.Final")
        force("io.netty:netty-codec-http:4.1.133.Final")
        force("io.netty:netty-codec-http2:4.1.133.Final")
        force("io.netty:netty-common:4.1.118.Final")
        force("io.netty:netty-handler:4.1.118.Final")
        force("io.netty:netty-handler-proxy:4.1.133.Final")
        force("org.apache.commons:commons-lang3:3.18.0")
        force("org.apache.httpcomponents:httpclient:4.5.13")
        force("org.bitbucket.b_c:jose4j:0.9.6")
        force("org.bouncycastle:bcpkix-jdk18on:1.84")
        force("org.bouncycastle:bcprov-jdk18on:1.84")
        force("org.jdom:jdom2:2.0.6.1")
    }
}

dependencies {
    implementation("com.android.tools.build:gradle:$agpVersion")
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlinVersion")
    implementation("org.jetbrains.kotlin:compose-compiler-gradle-plugin:$kotlinVersion")
    implementation("com.google.devtools.ksp:symbol-processing-gradle-plugin:$kspVersion")
    implementation("com.google.dagger:hilt-android-gradle-plugin:$hiltVersion")
}
