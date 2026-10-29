package dev.speedpack.client.mixin;

import java.io.InputStream;
import java.util.Enumeration;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.FilePackResources;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.IoSupplier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Vanilla's listResources walks every entry of the zip for each call (24k entries for a big server pack,
 * called thousands of times), and ZipFile.entries()/getEntry() are synchronized so worker threads serialize
 * on them. We build a sorted name index once per pack and answer listings with a prefix range scan and
 * single-file lookups with a map get (lock-free once the index is published).
 */
@Mixin(FilePackResources.class)
public abstract class FilePackResourcesMixin {
	@Shadow
	private FilePackResources.SharedZipFileAccess zipFileAccess;

	@Shadow
	protected abstract String addPrefix(String path);

	/** All entries (directories included, like ZipFile.getEntry). Replaced as a whole, never mutated. */
	@Unique
	private volatile NavigableMap<String, ZipEntry> speedpack$index;

	@Unique
	private volatile ZipFile speedpack$indexedZip;

	@Unique
	private NavigableMap<String, ZipEntry> speedpack$index(ZipFile zip) {
		NavigableMap<String, ZipEntry> idx = this.speedpack$index;
		if (idx != null && this.speedpack$indexedZip == zip) return idx;
		synchronized (this) {
			if (this.speedpack$index != null && this.speedpack$indexedZip == zip) return this.speedpack$index;
			NavigableMap<String, ZipEntry> built = new TreeMap<>();
			Enumeration<? extends ZipEntry> en = zip.entries();
			while (en.hasMoreElements()) {
				ZipEntry e = en.nextElement();
				built.put(e.getName(), e);
			}
			this.speedpack$indexedZip = zip;
			this.speedpack$index = built;
			return built;
		}
	}

	@Inject(method = "listResources", at = @At("HEAD"), cancellable = true)
	private void speedpack$indexedList(PackType type, String namespace, String path,
		PackResources.ResourceOutput output, CallbackInfo ci) {
		ZipFile zip = this.zipFileAccess.getOrCreateZipFile();
		if (zip == null) {
			ci.cancel();
			return;
		}
		String base = this.addPrefix(type.getDirectory() + "/" + namespace + "/");
		String prefix = base + path + "/";
		for (Map.Entry<String, ZipEntry> e : this.speedpack$index(zip).tailMap(prefix, true).entrySet()) {
			String name = e.getKey();
			if (!name.startsWith(prefix)) break;
			if (e.getValue().isDirectory()) continue;
			Identifier id = Identifier.tryBuild(namespace, name.substring(base.length()));
			if (id != null) output.accept(id, IoSupplier.create(zip, e.getValue()));
		}
		ci.cancel();
	}

	@Inject(method = "getResource(Ljava/lang/String;)Lnet/minecraft/server/packs/resources/IoSupplier;",
		at = @At("HEAD"), cancellable = true)
	private void speedpack$indexedGet(String path, CallbackInfoReturnable<IoSupplier<InputStream>> cir) {
		ZipFile zip = this.zipFileAccess.getOrCreateZipFile();
		if (zip == null) {
			cir.setReturnValue(null);
			return;
		}
		NavigableMap<String, ZipEntry> idx = this.speedpack$index(zip);
		String name = this.addPrefix(path);
		ZipEntry entry = idx.get(name);
		if (entry == null) entry = idx.get(name + "/"); // ZipFile.getEntry also matches a directory entry
		cir.setReturnValue(entry == null ? null : IoSupplier.create(zip, entry));
	}
}
