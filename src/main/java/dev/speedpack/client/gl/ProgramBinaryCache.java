package dev.speedpack.client.gl;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

//? if >=26.3 {
import java.util.List;

import com.mojang.renderpearl.backend.opengl.GlShaderModule;
import com.mojang.renderpearl.backend.opengl.GlStateManager;
//?} else if >=1.21.5 {
/*import com.mojang.blaze3d.opengl.GlShaderModule;
import com.mojang.blaze3d.opengl.GlStateManager;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormatElement;
*///?}
import dev.speedpack.client.profile.Out;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11C;
import org.lwjgl.opengl.GL20C;
import org.lwjgl.opengl.GL41C;
import org.lwjgl.opengl.GLCapabilities;
import org.lwjgl.system.MemoryUtil;

/**
 * Disk cache of linked GL program binaries (glGetProgramBinary / glProgramBinary).
 * glLinkProgram is where the driver really compiles; on a driver shader-cache miss a big server pack's
 * shaders took 25 s on the render thread. Key = driver + program label + attribute layout (before 26.3, where
 * link still binds attributes) + all shader sources, so any change (pack shader, driver update) is a miss and
 * gets linked normally. Render thread only.
 */
public final class ProgramBinaryCache {
	private static final int GL_LINK_STATUS = 0x8B82;
	private static final int GL_PROGRAM_BINARY_RETRIEVABLE_HINT = 0x8257;
	private static final int GL_PROGRAM_BINARY_LENGTH = 0x8741;
	private static final int GL_NUM_PROGRAM_BINARY_FORMATS = 0x87FE;
	private static final int GL_PROGRAM_BINARY_FORMATS = 0x87FF;
	private static final long MAX_AGE_MS = TimeUnit.DAYS.toMillis(30);

	private static final ExecutorService WRITER = Executors.newSingleThreadExecutor(r -> {
		Thread t = new Thread(r, "SpeedPack-ShaderCache");
		t.setDaemon(true);
		return t;
	});

	private static Boolean supported;
	private static int[] formats = new int[0];
	private static String driverId;
	private static Path dir;
	private static String pendingKey;
	private static int hits, misses;
	private static long linkNanos;

	private ProgramBinaryCache() {}

	private static boolean supported() {
		if (supported == null) {
			supported = false;
			try {
				GLCapabilities caps = GL.getCapabilities();
				if (caps.glProgramBinary != 0L && caps.glGetProgramBinary != 0L && caps.glProgramParameteri != 0L) {
					int n = GL11C.glGetInteger(GL_NUM_PROGRAM_BINARY_FORMATS);
					if (n > 0) {
						formats = new int[n];
						GL11C.glGetIntegerv(GL_PROGRAM_BINARY_FORMATS, formats);
						driverId = GL11C.glGetString(GL11C.GL_VENDOR) + "|" + GL11C.glGetString(GL11C.GL_RENDERER)
							+ "|" + GL11C.glGetString(GL11C.GL_VERSION);
						dir = Out.dir().resolve("shader-cache");
						Files.createDirectories(dir);
						WRITER.execute(ProgramBinaryCache::deleteOldEntries);
						supported = true;
					}
				}
			} catch (Throwable t) {
				t.printStackTrace();
			}
		}
		return supported;
	}

	// Called at the start of GlProgram.link, before attributes are bound and shaders attached.
	//? if >=26.3 {
	public static void begin(List<GlShaderModule> modules, String label) {
		pendingKey = null;
		if (!supported()) return;
		try {
			MessageDigest md = MessageDigest.getInstance("SHA-256");
			update(md, driverId);
			update(md, label);
			for (GlShaderModule module : modules) update(md, GL20C.glGetShaderSource(module.getShaderId()));
			pendingKey = HexFormat.of().formatHex(md.digest());
		} catch (Throwable t) {
			pendingKey = null;
		}
	}
	//?} else if >=1.21.5 {
	/*public static void begin(GlShaderModule vertex, GlShaderModule fragment, VertexFormat[] vertexFormats, String label) {
		pendingKey = null;
		if (!supported()) return;
		try {
			MessageDigest md = MessageDigest.getInstance("SHA-256");
			update(md, driverId);
			update(md, label);
			for (VertexFormat format : vertexFormats) {
				if (format != null) updateAttributes(md, format); // null entries are skipped by link too
				update(md, "|");
			}
			update(md, GL20C.glGetShaderSource(vertex.getShaderId()));
			update(md, GL20C.glGetShaderSource(fragment.getShaderId()));
			pendingKey = HexFormat.of().formatHex(md.digest());
		} catch (Throwable t) {
			pendingKey = null;
		}
	}
	*///?}

	// The attribute names GlProgram.link binds for this format.
	//? if >=1.21.5 && <26.2 {
	/*private static void updateAttributes(MessageDigest md, VertexFormat format) {
		for (String name : format.getElementAttributeNames()) update(md, name);
	}
	*///?} else if >=26.2 && <26.3 {
	/*private static void updateAttributes(MessageDigest md, VertexFormat format) {
		for (VertexFormatElement element : format.getElements()) update(md, element.name());
	}
	*///?}

	private static void update(MessageDigest md, String s) {
		md.update(String.valueOf(s).getBytes(StandardCharsets.UTF_8));
		md.update((byte) 0);
	}

	/** Replaces GlStateManager.glLinkProgram inside GlProgram.link. */
	//? if >=1.21.5 {
	public static void link(int program) {
		long start = System.nanoTime();
		String key = pendingKey;
		pendingKey = null;
		try {
			if (key == null) {
				GlStateManager.glLinkProgram(program);
				return;
			}
			if (tryLoad(program, key)) {
				hits++;
				return;
			}
			misses++;
			GL41C.glProgramParameteri(program, GL_PROGRAM_BINARY_RETRIEVABLE_HINT, GL11C.GL_TRUE);
			GlStateManager.glLinkProgram(program);
			if (GL20C.glGetProgrami(program, GL_LINK_STATUS) == GL11C.GL_TRUE) store(program, key);
		} finally {
			linkNanos += System.nanoTime() - start;
		}
	}
	//?}

	private static boolean tryLoad(int program, String key) {
		Path file = dir.resolve(key + ".bin");
		if (!Files.isRegularFile(file)) return false;
		try {
			byte[] data = Files.readAllBytes(file);
			if (data.length > 4) {
				int format = ByteBuffer.wrap(data).getInt(0);
				if (isSupportedFormat(format)) {
					ByteBuffer buf = MemoryUtil.memAlloc(data.length - 4);
					try {
						buf.put(data, 4, data.length - 4).flip();
						GL41C.glProgramBinary(program, format, buf);
					} finally {
						MemoryUtil.memFree(buf);
					}
					if (GL20C.glGetProgrami(program, GL_LINK_STATUS) == GL11C.GL_TRUE) {
						file.toFile().setLastModified(System.currentTimeMillis());
						return true;
					}
				}
			}
			// Rejected by the driver (e.g. driver updated): drop it, the program gets linked normally.
			Files.deleteIfExists(file);
		} catch (IOException e) {
			e.printStackTrace();
		}
		return false;
	}

	private static boolean isSupportedFormat(int format) {
		for (int f : formats) if (f == format) return true;
		return false;
	}

	private static void store(int program, String key) {
		int length = GL20C.glGetProgrami(program, GL_PROGRAM_BINARY_LENGTH);
		if (length <= 0) return;
		ByteBuffer buf = MemoryUtil.memAlloc(length);
		IntBuffer format = MemoryUtil.memAllocInt(1);
		IntBuffer written = MemoryUtil.memAllocInt(1);
		try {
			GL41C.glGetProgramBinary(program, written, format, buf);
			int n = written.get(0);
			if (n <= 0) return;
			byte[] data = new byte[4 + n];
			ByteBuffer.wrap(data).putInt(0, format.get(0));
			buf.get(0, data, 4, n);
			WRITER.execute(() -> write(key, data));
		} finally {
			MemoryUtil.memFree(buf);
			MemoryUtil.memFree(format);
			MemoryUtil.memFree(written);
		}
	}

	private static void write(String key, byte[] data) {
		try {
			Path tmp = dir.resolve(key + ".tmp");
			Files.write(tmp, data);
			Files.move(tmp, dir.resolve(key + ".bin"), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private static void deleteOldEntries() {
		long limit = System.currentTimeMillis() - MAX_AGE_MS;
		try (Stream<Path> s = Files.list(dir)) {
			s.filter(p -> p.toFile().lastModified() < limit).forEach(p -> p.toFile().delete());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/** "N hits, M misses, X ms linking" since the last call. */
	public static String statsAndReset() {
		String s = hits + " hits, " + misses + " misses, " + linkNanos / 1_000_000 + " ms linking"
			+ (Boolean.FALSE.equals(supported) ? " (program binaries unsupported)" : "");
		hits = 0;
		misses = 0;
		linkNanos = 0;
		return s;
	}
}
