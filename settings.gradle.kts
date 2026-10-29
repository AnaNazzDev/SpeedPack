pluginManagement {
	repositories {
		mavenCentral()
		gradlePluginPortal()
		maven("https://maven.fabricmc.net/") { name = "Fabric" }
		maven("https://maven.kikugie.dev/releases") { name = "KikuGie Releases" }
	}
}

plugins {
	id("dev.kikugie.stonecutter") version "0.9.8"
	// Picks the right Loom variant per Minecraft version (unobfuscated 26.1+ vs remapped 1.21.x)
	id("dev.kikugie.loom-back-compat") version "0.4.3"
}

stonecutter {
	create(rootProject) {
		// One jar per entry; Modrinth game versions for each are in stonecutter.properties.toml
		versions("26.1.2", "26.2", "26.3")
		vcsVersion = "26.3"
	}
}

rootProject.name = "speedpack"
