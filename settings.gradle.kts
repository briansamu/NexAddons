pluginManagement {
    val kotlinVersion = providers.gradleProperty("kotlin_version").get()
    val loomVersion = providers.gradleProperty("loom_version").get()

    repositories {
        maven("https://maven.fabricmc.net/")
        gradlePluginPortal()
        mavenCentral()
    }

    plugins {
        id("fabric-loom") version loomVersion
        id("org.jetbrains.kotlin.jvm") version kotlinVersion
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

rootProject.name = "NexAddons"
