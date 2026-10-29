plugins {
	id("dev.kikugie.stonecutter")
	// Loom used on 26.1+ (loom-back-compat applies it in each version project)
	id("net.fabricmc.fabric-loom") version "1.19-SNAPSHOT" apply false
}

stonecutter active "26.3"

stonecutter parameters {
	replacements {
		string(current.parsed >= "1.21.11") {
			replace("ResourceLocation", "Identifier")
		}
		string(current.parsed >= "26.1") {
			replace("accessWidener v2 named", "accessWidener v2 official")
		}
	}
}
