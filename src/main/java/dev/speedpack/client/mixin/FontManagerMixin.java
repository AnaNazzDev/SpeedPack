package dev.speedpack.client.mixin;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import com.mojang.blaze3d.font.GlyphProvider;
import it.unimi.dsi.fastutil.ints.IntIterator;
import net.minecraft.client.gui.font.FontManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Vanilla's finalizeProviderLoading pre-warms getGlyph() for every codepoint of every font set, on worker
 * threads. Font sets share their provider instances (unifont is in most of the ~2250 sets of a big pack), so
 * the same ~100k glyphs get warmed once per font set. The result of the loop is discarded, so we warm each
 * provider instance once instead.
 */
@Mixin(FontManager.class)
public abstract class FontManagerMixin {
	private static final Map<GlyphProvider, Boolean> speedpack$WARMED = Collections.synchronizedMap(new WeakHashMap<>());

	@Inject(method = "finalizeProviderLoading", at = @At("HEAD"), cancellable = true)
	private void speedpack$warmOncePerProvider(List<GlyphProvider.Conditional> providers,
		GlyphProvider.Conditional missingProvider, CallbackInfo ci) {
		providers.add(0, missingProvider); // first statement of the vanilla method, its only lasting effect
		for (GlyphProvider.Conditional conditional : providers) {
			GlyphProvider provider = conditional.provider();
			if (speedpack$WARMED.put(provider, Boolean.TRUE) != null) continue;
			IntIterator it = provider.getSupportedGlyphs().iterator();
			while (it.hasNext()) {
				int codepoint = it.nextInt();
				if (codepoint != 32) provider.getGlyph(codepoint);
			}
		}
		ci.cancel();
	}
}
