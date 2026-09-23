plugins {
    id("dev.kikugie.loom-back-compat")
    id("maven-publish")
}

version = "${property("mod.version")}+${sc.current.version}"
base.archivesName = property("mod.id") as String

val requiredJava: JavaVersion = when {
    sc.current.parsed >= "26.1" -> JavaVersion.VERSION_25
    sc.current.parsed >= "1.20.5" -> JavaVersion.VERSION_21
    else -> JavaVersion.VERSION_17
}

dependencies {
    minecraft("com.mojang:minecraft:${sc.current.version}")
    // Mojang's names on obfuscated versions; 26.1+ ships unobfuscated.
    loomx.applyMojangMappings()
    modImplementation("net.fabricmc:fabric-loader:${property("deps.fabric_loader")}")
    modImplementation("net.fabricmc.fabric-api:fabric-api:${sc.properties.get<String>("deps.fabric_api")}")
}

loom {
    runConfigs.all {
        preferGradleTask = true
        runDirectory = rootProject.file("run")
    }
}

java {
    withSourcesJar()
    targetCompatibility = requiredJava
    sourceCompatibility = requiredJava
    toolchain {
        languageVersion = JavaLanguageVersion.of(requiredJava.majorVersion)
    }
}

tasks {
    processResources {
        val props = mapOf(
            "version" to project.version.toString(),
            "minecraft" to sc.properties.get<String>("mod.mc_compat"),
            "java" to requiredJava.majorVersion,
        )
        inputs.properties(props)
        filesMatching("fabric.mod.json") { expand(props) }
        filesMatching("*.mixins.json") { expand(props) }
    }

    withType<Jar> {
        from(rootProject.file("LICENSE")) { rename { "${it}_lucentclient" } }
    }

    register<Copy>("buildAndCollect") {
        group = "build"
        description = "Builds the jar for this version and copies it to build/libs/<mod version>/"
        from(loomx.modJar.flatMap { it.archiveFile })
        into(rootProject.layout.buildDirectory.dir("libs/${property("mod.version")}"))
    }
}
