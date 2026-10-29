package dev.speedpack.client;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import net.fabricmc.loader.api.FabricLoader;

/** config/speedpack.properties. Read once, before mixins apply; changes need a game restart. */
public final class SpeedPackConfig {
	public static final boolean ZIP_INDEX;
	public static final boolean LAZY_FONTS;
	public static final boolean FONT_WARM_ONCE;
	public static final boolean SHADER_CACHE;
	public static final boolean FAST_LOADING_SCREEN;
	public static final boolean DIAGNOSTICS;

	private static final String HEADER = """
		SpeedPack config (restart the game after editing)
		zipIndex          : index zipped resource packs instead of scanning every entry on each lookup
		lazyFonts         : build font glyph tables on first use instead of for every font at reload
		fontWarmOnce      : pre-warm each font provider once instead of once per font
		shaderCache       : cache linked shader programs on disk (speedpack/shader-cache)
		fastLoadingScreen : shorten the Mojang loading screen fade (vanilla keeps it ~2 s after loading ends)
		diagnostics       : profile every resource reload and write reports to speedpack/ (for bug reports)""";

	static {
		Properties p = new Properties();
		Path file = FabricLoader.getInstance().getConfigDir().resolve("speedpack.properties");
		try {
			if (Files.exists(file)) {
				try (Reader r = Files.newBufferedReader(file)) {
					p.load(r);
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		ZIP_INDEX = get(p, "zipIndex", true);
		LAZY_FONTS = get(p, "lazyFonts", true);
		FONT_WARM_ONCE = get(p, "fontWarmOnce", true);
		SHADER_CACHE = get(p, "shaderCache", true);
		FAST_LOADING_SCREEN = get(p, "fastLoadingScreen", true);
		DIAGNOSTICS = get(p, "diagnostics", false);
		try (Writer w = Files.newBufferedWriter(file)) {
			p.store(w, HEADER);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private SpeedPackConfig() {}

	private static boolean get(Properties p, String key, boolean def) {
		String v = p.getProperty(key);
		boolean b = v == null ? def : Boolean.parseBoolean(v.trim());
		p.setProperty(key, String.valueOf(b));
		return b;
	}
}
