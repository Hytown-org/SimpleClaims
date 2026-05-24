import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.gradle.api.artifacts.ExternalModuleDependency
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.tasks.bundling.Jar
import org.gradle.external.javadoc.StandardJavadocDocletOptions

plugins {
    java
    idea
    `maven-publish`
    id("com.gradleup.shadow") version "9.3.0"
}

group = providers.gradleProperty("pluginGroup").orElse("com.buuz135").get()
version = providers.gradleProperty("pluginVersion").orElse("1.0.34").get()
description =
    providers.gradleProperty("pluginDescription")
        .orElse("A chunk claiming and protection mod. Create parties, claim chunks, and protect your builds from other players.")
        .get()

val patchline =
    providers.gradleProperty("patchline")
        .orElse("release")
        .get()
        .also {
            require(it in setOf("release", "pre-release")) {
                "Invalid patchline '$it'. Use 'release' or 'pre-release'."
            }
        }
val hytaleServerVersion = providers.gradleProperty("server_version").orElse("+").get()
val manifestGroup = providers.gradleProperty("manifestGroup").orElse("Buuz135").get()
val website = providers.gradleProperty("website").orElse("buuz135.com").get()
val entryPoint = providers.gradleProperty("entryPoint").orElse("com.buuz135.simpleclaims.Main").get()
val shadowBundle by configurations.creating

configurations.named(JavaPlugin.IMPLEMENTATION_CONFIGURATION_NAME) {
    extendsFrom(shadowBundle)
}

fun resolveHytaleServerVersion(): String {
    val forced = providers.gradleProperty("serverVersion").orNull
    if (!forced.isNullOrBlank()) return forced

    return try {
        val serverDependency =
            dependencies.create("com.hypixel.hytale:Server:$hytaleServerVersion").also { dependency ->
                if (dependency is ExternalModuleDependency) {
                    dependency.isTransitive = false
                }
            }
        val detached = configurations.detachedConfiguration(serverDependency).apply { isTransitive = false }

        detached.resolvedConfiguration.firstLevelModuleDependencies
            .singleOrNull { it.moduleGroup == "com.hypixel.hytale" && it.moduleName == "Server" }
            ?.moduleVersion
            ?: "*"
    } catch (_: Exception) {
        "*"
    }
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(25))
    }
    withSourcesJar()
    withJavadocJar()
}

repositories {
    mavenCentral()
    maven("https://maven.hytale.com/$patchline") { name = "hytale-$patchline" }
    maven("https://jitpack.io") { name = "Jitpack" }
    maven("https://repo.helpch.at/releases") { name = "HelpChat" }
}

val resolvedServerVersion = resolveHytaleServerVersion()

dependencies {
    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")

    compileOnly("com.hypixel.hytale:Server:$hytaleServerVersion")
    compileOnly("com.hytown:HytownNexus:1.2.16")
    compileOnly("net.luckperms:api:5.4")
    compileOnly("at.helpch:placeholderapi-hytale:1.0.4")

    add(shadowBundle.name, files("libs/codeclib-1.1.0.jar"))
    add(shadowBundle.name, "org.slf4j:slf4j-simple:2.0.17")
    add(shadowBundle.name, "org.xerial:sqlite-jdbc:3.45.1.0")
}

tasks.compileJava {
    options.encoding = Charsets.UTF_8.name()
    options.release.set(25)
}

tasks.javadoc {
    (options as StandardJavadocDocletOptions).addStringOption("Xdoclint:-missing", "-quiet")
}

tasks.processResources {
    filteringCharset = Charsets.UTF_8.name()
    val manifestProps =
        mapOf(
            "manifestGroup" to manifestGroup,
            "name" to rootProject.name,
            "version" to project.version.toString(),
            "description" to (project.description ?: ""),
            "website" to website,
            "server_version" to resolvedServerVersion,
            "entryPoint" to entryPoint
        )

    inputs.properties(manifestProps)
    filesMatching("manifest.json") {
        expand(manifestProps)
    }
}

tasks.test {
    useJUnitPlatform()
}

tasks.named<ShadowJar>("shadowJar") {
    archiveClassifier.set("")
    mergeServiceFiles()
    configurations = listOf(shadowBundle)
    relocate("dev.unnm3d.codeclib", "com.buuz135.simpleclaims.libs.codeclib")
    manifest {
        attributes["Main-Class"] = entryPoint
    }
}

tasks.build {
    dependsOn(tasks.named("shadowJar"))
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            artifact(tasks.named<ShadowJar>("shadowJar"))
            artifact(tasks.named<Jar>("sourcesJar"))
            artifact(tasks.named<Jar>("javadocJar"))
            artifactId = rootProject.name
        }
    }

    val mavenUsername = providers.environmentVariable("HT_MAVEN_USERNAME").orNull
    val mavenPassword = providers.environmentVariable("HT_MAVEN_PASSWORD").orNull
    if (!mavenUsername.isNullOrBlank() && !mavenPassword.isNullOrBlank()) {
        repositories {
            maven {
                url = uri("https://maven.hytale-mods.dev/releases")
                credentials {
                    username = mavenUsername
                    password = mavenPassword
                }
            }
        }
    }
}

idea {
    module {
        isDownloadSources = true
        isDownloadJavadoc = true
    }
}
