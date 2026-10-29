package dev.speedpack.client.profile;

import java.io.IOException;
import java.io.InputStream;
import java.lang.management.ManagementFactory;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;

/**
 * Writes {@code speedpack/system-report.json}: hardware, JVM flags, mod list, options.txt pack selection
 * and an inventory of every pack in resourcepacks/ (file counts, PNG sizes, zip compression...).
 * Runs on a background thread so it does not slow startup.
 */
public final class SystemReport {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

	private SystemReport() {}

	public static void write() {
		Thread t = new Thread(() -> {
			try {
				Map<String, Object> r = new LinkedHashMap<>();
				r.put("system", system());
				r.put("mods", mods());
				r.put("options", options());
				r.put("resourcepacks", packs(FabricLoader.getInstance().getGameDir().resolve("resourcepacks")));
				Files.writeString(Out.dir().resolve("system-report.json"), GSON.toJson(r));
			} catch (Throwable e) {
				e.printStackTrace();
			}
		}, "SpeedPack-Report");
		t.setDaemon(true);
		t.start();
	}

	/** Inventories the 2 most recently modified server packs in downloads/ (background thread). */
	public static void writeDownloads() {
		Thread t = new Thread(() -> {
			try {
				Path root = FabricLoader.getInstance().getGameDir().resolve("downloads");
				if (!Files.isDirectory(root)) return;
				List<Path> files = new ArrayList<>();
				try (Stream<Path> w = Files.walk(root, 2)) {
					w.filter(Files::isRegularFile).forEach(files::add);
				}
				files.sort(Comparator.comparingLong((Path p) -> p.toFile().lastModified()).reversed());
				List<Object> out = new ArrayList<>();
				for (Path p : files.subList(0, Math.min(2, files.size()))) {
					try {
						out.add(zipPack(p));
					} catch (Exception e) {
						out.add(Map.of("name", p.toString(), "error", String.valueOf(e)));
					}
				}
				Files.writeString(Out.dir().resolve("downloads-inventory.json"), GSON.toJson(out));
			} catch (Throwable e) {
				e.printStackTrace();
			}
		}, "SpeedPack-Downloads");
		t.setDaemon(true);
		t.start();
	}

	private static Map<String, Object> system() {
		Map<String, Object> m = new LinkedHashMap<>();
		Runtime rt = Runtime.getRuntime();
		m.put("os", System.getProperty("os.name") + " " + System.getProperty("os.version") + " " + System.getProperty("os.arch"));
		m.put("java", System.getProperty("java.version") + " (" + System.getProperty("java.vendor") + ")");
		m.put("cpuThreads", rt.availableProcessors());
		m.put("maxHeapMB", rt.maxMemory() >> 20);
		m.put("jvmArgs", ManagementFactory.getRuntimeMXBean().getInputArguments());
		m.put("gameDir", FabricLoader.getInstance().getGameDir().toString());
		m.put("minecraft", FabricLoader.getInstance().getModContainer("minecraft")
			.map(c -> c.getMetadata().getVersion().getFriendlyString()).orElse("?"));
		try {
			var fs = Files.getFileStore(FabricLoader.getInstance().getGameDir());
			m.put("gameDirFileStore", fs.name() + " type=" + fs.type()
				+ " freeGB=" + (fs.getUsableSpace() >> 30));
		} catch (IOException ignored) {
		}
		return m;
	}

	private static List<String> mods() {
		List<String> l = new ArrayList<>();
		for (ModContainer c : FabricLoader.getInstance().getAllMods()) {
			l.add(c.getMetadata().getId() + " " + c.getMetadata().getVersion().getFriendlyString());
		}
		l.sort(null);
		return l;
	}

	private static Map<String, String> options() throws IOException {
		Map<String, String> m = new LinkedHashMap<>();
		Path f = FabricLoader.getInstance().getGameDir().resolve("options.txt");
		if (!Files.exists(f)) return m;
		for (String line : Files.readAllLines(f)) {
			if (line.startsWith("resourcePacks:") || line.startsWith("incompatibleResourcePacks:")
				|| line.startsWith("mipmapLevels:") || line.startsWith("biomeBlendRadius:")
				|| line.startsWith("renderDistance:") || line.startsWith("graphicsMode:")
				|| line.startsWith("maxAnisotropy") || line.startsWith("lang:")) {
				int i = line.indexOf(':');
				m.put(line.substring(0, i), line.substring(i + 1));
			}
		}
		return m;
	}

	private static List<Object> packs(Path dir) throws IOException {
		List<Object> out = new ArrayList<>();
		if (!Files.isDirectory(dir)) return out;
		try (Stream<Path> s = Files.list(dir)) {
			for (Path p : (Iterable<Path>) s.sorted()::iterator) {
				try {
					out.add(Files.isDirectory(p) ? dirPack(p) : zipPack(p));
				} catch (Exception e) {
					out.add(Map.of("name", p.getFileName().toString(), "error", String.valueOf(e)));
				}
			}
		}
		return out;
	}

	private static Map<String, Object> zipPack(Path p) throws IOException {
		Map<String, Object> m = new LinkedHashMap<>();
		m.put("name", p.getFileName().toString());
		m.put("type", "zip");
		m.put("zipSizeMB", Files.size(p) >> 20);
		Stats st = new Stats();
		try (ZipFile z = new ZipFile(p.toFile())) {
			var en = z.entries();
			while (en.hasMoreElements()) {
				ZipEntry e = en.nextElement();
				if (e.isDirectory()) continue;
				st.add(e.getName(), e.getSize(), e.getMethod() == ZipEntry.STORED, () -> {
					try (InputStream in = z.getInputStream(e)) {
						return in.readNBytes(32);
					}
				});
			}
		}
		return st.into(m);
	}

	private static Map<String, Object> dirPack(Path p) throws IOException {
		Map<String, Object> m = new LinkedHashMap<>();
		m.put("name", p.getFileName().toString());
		m.put("type", "directory");
		Stats st = new Stats();
		try (Stream<Path> w = Files.walk(p)) {
			for (Path f : (Iterable<Path>) w.filter(Files::isRegularFile)::iterator) {
				st.add(p.relativize(f).toString().replace('\\', '/'), Files.size(f), false, () -> {
					try (InputStream in = Files.newInputStream(f)) {
						return in.readNBytes(32);
					}
				});
			}
		}
		return st.into(m);
	}

	private interface Header {
		byte[] read() throws IOException;
	}

	/** Aggregates entry statistics for one pack. */
	private static final class Stats {
		long files, bytes, stored;
		long pngCount, pngBytes, pngPixels;
		final Map<String, Long> byExt = new TreeMap<>();
		final Map<String, Long> bySize = new TreeMap<>(); // "WxH" -> count
		final Map<String, Long> byFolder = new TreeMap<>(); // assets/<ns>/<top> -> count
		final List<String[]> bigPng = new ArrayList<>(); // {pixels, desc}

		void add(String name, long size, boolean isStored, Header h) {
			files++;
			bytes += Math.max(size, 0);
			if (isStored) stored++;
			int dot = name.lastIndexOf('.');
			String ext = dot < 0 ? "(none)" : name.substring(dot + 1).toLowerCase();
			byExt.merge(ext, 1L, Long::sum);
			String[] seg = name.split("/");
			if (seg.length >= 3 && seg[0].equals("assets")) byFolder.merge(seg[1] + "/" + seg[2], 1L, Long::sum);
			if (ext.equals("png")) {
				pngCount++;
				pngBytes += Math.max(size, 0);
				try {
					byte[] b = h.read();
					// PNG IHDR: width at 16..19, height at 20..23 (big endian)
					if (b.length >= 24 && b[1] == 'P') {
						int w = (b[16] & 255) << 24 | (b[17] & 255) << 16 | (b[18] & 255) << 8 | (b[19] & 255);
						int hh = (b[20] & 255) << 24 | (b[21] & 255) << 16 | (b[22] & 255) << 8 | (b[23] & 255);
						pngPixels += (long) w * hh;
						bySize.merge(w + "x" + hh, 1L, Long::sum);
						bigPng.add(new String[] {String.valueOf((long) w * hh), w + "x" + hh + "  " + name});
					}
				} catch (IOException ignored) {
				}
			}
		}

		Map<String, Object> into(Map<String, Object> m) {
			m.put("files", files);
			m.put("uncompressedMB", bytes >> 20);
			m.put("zipEntriesStoredNoCompression", stored);
			m.put("pngCount", pngCount);
			m.put("pngFileMB", pngBytes >> 20);
			m.put("pngTotalMegaPixels", pngPixels / 1_000_000);
			m.put("pngDecodedRamMB_RGBA", pngPixels * 4 >> 20);
			m.put("filesByExtension", sortDesc(byExt, 20));
			m.put("pngBySize_top25", sortDesc(bySize, 25));
			m.put("filesByAssetFolder_top30", sortDesc(byFolder, 30));
			bigPng.sort(Comparator.comparingLong((String[] a) -> Long.parseLong(a[0])).reversed());
			List<String> top = new ArrayList<>();
			for (int i = 0; i < Math.min(25, bigPng.size()); i++) top.add(bigPng.get(i)[1]);
			m.put("largestPng_top25", top);
			return m;
		}

		private static Map<String, Long> sortDesc(Map<String, Long> in, int limit) {
			Map<String, Long> o = new LinkedHashMap<>();
			in.entrySet().stream()
				.sorted((a, b) -> Long.compare(b.getValue(), a.getValue()))
				.limit(limit)
				.forEach(e -> o.put(e.getKey(), e.getValue()));
			return o;
		}
	}
}
