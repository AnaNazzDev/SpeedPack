plugins {
	// Applies the right Loom variant for the Minecraft version of this node
	id("dev.kikugie.loom-back-compat")
}

version = "${property("mod.version")}+${sc.current.version}"
base.archivesName = property("mod.id") as String

val requiredJava: JavaVersion = when {
	sc.current.parsed >= "26.1" -> JavaVersion.VERSION_25
	else -> JavaVersion.VERSION_21
}

dependencies {
	minecraft("com.mojang:minecraft:${sc.current.version}")
	// Mojang mappings on obfuscated versions, no-op on 26.1+
	loomx.applyMojangMappings()

	// `mod*` configurations are converted by loom-back-compat on 26.1+
	modImplementation("net.fabricmc:fabric-loader:${property("deps.fabric_loader")}")
	modImplementation("net.fabricmc.fabric-api:fabric-api:${property("deps.fabric_api")}")
}

loom {
	// Header namespace differs between obfuscated and 26.1+ versions (replacement in stonecutter.gradle.kts)
	accessWidenerPath = sc.process(rootProject.file("src/main/resources/speedpack.accesswidener"), "build/processed.accesswidener")

	runConfigs.all {
		preferGradleTask = true
		runDirectory = rootProject.file("run") // shared by all versions
	}
}

java {
	// Loom attaches sourcesJar to the build task
	withSourcesJar()
	sourceCompatibility = requiredJava
	targetCompatibility = requiredJava
}

tasks {
	withType<JavaCompile>().configureEach {
		options.release = requiredJava.majorVersion.toInt()
	}

	processResources {
		val props = mapOf(
			"version" to version.toString(),
			"minecraft" to sc.properties.get<String>("mod.mc_compat"),
			"java" to requiredJava.majorVersion,
		)
		inputs.properties(props)
		filesMatching("fabric.mod.json") { expand(props) }
		filesMatching("*.mixins.json") { expand(props) }
	}

	withType<Jar> {
		val name = project.property("mod.id")
		inputs.property("mod_id", name)
		from(rootProject.file("LICENSE")) { rename { "${it}_$name" } }
	}

	register<Copy>("buildAndCollect") {
		group = "build"
		description = "Builds the mod jar and copies it to build/libs/<mod version>/"
		from(loomx.modJar.flatMap { it.archiveFile })
		into(rootProject.layout.buildDirectory.dir("libs/${project.property("mod.version")}"))
	}
}
