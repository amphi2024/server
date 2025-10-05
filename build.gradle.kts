plugins {

    alias(libs.plugins.jvm)

    id("org.jetbrains.kotlin.kapt") version "1.5.20"

    application
    id("com.github.johnrengelman.shadow") version "7.1.2"
}

repositories {
    mavenCentral()
}

dependencies {
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")

    testImplementation(libs.junit.jupiter.engine)

    testRuntimeOnly("org.junit.platform:junit-platform-launcher")

    implementation(libs.guava)

    implementation("io.vertx:vertx-core:5.0.1")
    implementation("org.xerial:sqlite-jdbc:3.50.3.0")
    implementation("io.netty:netty-resolver-dns-native-macos:4.2.3.Final:osx-aarch_64")
    implementation("de.mkammerer:argon2-jvm:2.12")

}

java {
    toolchain {
        languageVersion =  JavaLanguageVersion.of(11)
    }
}


application {
    mainClass = "com.amphi.server.MainKt"
}

tasks.jar {
    manifest {
        attributes(
            "Main-Class" to "com.amphi.server.MainKt"

        )
    }
    archiveFileName.set("server.jar")
}