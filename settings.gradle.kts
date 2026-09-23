pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
        maven("https://maven.fabricmc.net/")
        maven("https://maven.kikugie.dev/releases") { name = "KikuGie Releases" }
    }
}

plugins {
    // Builds the mod for several Minecraft versions from one source tree.
    id("dev.kikugie.stonecutter") version "0.9.8"
    // Applies the right Loom for each version: plain on 26.1+, remapping on obfuscated versions.
    id("dev.kikugie.loom-back-compat") version "0.4.2"
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

stonecutter {
    create(rootProject) {
        // One build per range of versions with the same APIs; see stonecutter.properties.toml.
        versions("26.1.2", "26.2", "26.3")
        vcsVersion = "26.3"
    }
}

rootProject.name = "lucentclient"
