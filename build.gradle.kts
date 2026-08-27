plugins {
    alias(libs.plugins.hytown.build)
    alias(libs.plugins.hytown.publishing)
}

group = "com.buuz135"
version = "1.0.38"
description = "A chunk claiming and protection mod. Create parties, claim chunks, and protect your builds from other players."

repositories {
    maven("https://repo.helpch.at/releases") { name = "HelpChat" }
}

dependencies {
    // Resolved via settings.gradle.kts includeBuild("../HytownCore").
    compileOnly(libs.hytown.nexus)
    compileOnly("net.luckperms:api:5.4")
    compileOnly("at.helpch:placeholderapi-hytale:1.0.4")

    implementation(files("libs/codeclib-1.1.0.jar"))
    implementation("org.slf4j:slf4j-simple:2.0.17")
    implementation("org.xerial:sqlite-jdbc:3.45.1.0")
}

tasks.shadowJar {
    relocate("dev.unnm3d.codeclib", "com.buuz135.simpleclaims.libs.codeclib")
    mergeServiceFiles()
}
