package dev.speedpack.client.profile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import net.fabricmc.loader.api.FabricLoader;

/** Output folder for everything SpeedPack collects: &lt;gameDir&gt;/speedpack/ */
public final class Out {
	private Out() {}

	public static Path dir() {
		Path dir = FabricLoader.getInstance().getGameDir().resolve("speedpack");
		try {
			Files.createDirectories(dir);
		} catch (IOException e) {
			throw new IllegalStateException(e);
		}
		return dir;
	}
}
