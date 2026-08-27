pluginManagement {
    repositories {
        maven {
            url = uri("https://pkgs.dev.azure.com/potionlabs/_packaging/potionlabs/maven/v1")
            name = "potionlabs"
            credentials(PasswordCredentials::class)
            authentication {
                create<BasicAuthentication>("basic")
            }
        }
        gradlePluginPortal()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    versionCatalogs {
        register("libs") {
            from(files("dependencies.toml"))
        }
    }
}

rootProject.name = "SimpleClaims"

includeBuild("../HytownCore")
