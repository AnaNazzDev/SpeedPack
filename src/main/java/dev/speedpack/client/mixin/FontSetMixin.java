package dev.speedpack.client.mixin;

import java.util.List;
import java.util.Set;

import com.mojang.blaze3d.font.GlyphProvider;
import net.minecraft.client.gui.font.FontOption;
import net.minecraft.client.gui.font.FontSet;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * FontSet.reload(options) runs selectProviders, which loops over every codepoint of every provider
 * (100k+ with unifont) to fill activeProviders and glyphsByWidth. A pack with ~2250 font sets pays that
 * ~18 s on the render thread, twice (apply, then again via ImmediatelyFast's updateOptions).
 * We defer it until a glyph of that font set is first requested; unused font sets cost nothing.
 */
@Mixin(FontSet.class)
public abstract class FontSetMixin {
	@Shadow
	private List<GlyphProvider> activeProviders;

	@Shadow
	private List<GlyphProvider.Conditional> allProviders;

	@Shadow
	protected abstract List<GlyphProvider> selectProviders(List<GlyphProvider.Conditional> providers, Set<FontOption> options);

	@Unique
	private Set<FontOption> speedpack$pendingOptions;

	@Unique
	private boolean speedpack$pending;

	@Redirect(method = "reload(Ljava/util/Set;)V", at = @At(value = "INVOKE",
		target = "Lnet/minecraft/client/gui/font/FontSet;selectProviders(Ljava/util/List;Ljava/util/Set;)Ljava/util/List;"))
	private List<GlyphProvider> speedpack$defer(FontSet self, List<GlyphProvider.Conditional> providers, Set<FontOption> options) {
		synchronized (this) {
			this.speedpack$pendingOptions = options;
			this.speedpack$pending = true;
		}
		return List.of();
	}

	@Unique
	private void speedpack$ensureSelected() {
		synchronized (this) {
			if (!this.speedpack$pending) return;
			this.speedpack$pending = false;
			Set<FontOption> options = this.speedpack$pendingOptions;
			this.speedpack$pendingOptions = null;
			this.activeProviders = this.selectProviders(this.allProviders, options);
		}
	}

	@Inject(method = "computeGlyphInfo", at = @At("HEAD"))
	private void speedpack$beforeCompute(int codepoint, CallbackInfoReturnable<?> cir) {
		this.speedpack$ensureSelected();
	}

	//? if >=1.21.9 {
	@Inject(method = "getRandomGlyph", at = @At("HEAD"))
	private void speedpack$beforeRandom(net.minecraft.util.RandomSource random, int width, CallbackInfoReturnable<?> cir) {
		this.speedpack$ensureSelected();
	}
	//?} else {
	/*@Inject(method = "getRandomGlyph", at = @At("HEAD"))
	private void speedpack$beforeRandom(com.mojang.blaze3d.font.GlyphInfo glyph, CallbackInfoReturnable<?> cir) {
		this.speedpack$ensureSelected();
	}

	// Before 1.21.9 baked glyphs are also built straight from activeProviders.
	@Inject(method = "computeBakedGlyph", at = @At("HEAD"))
	private void speedpack$beforeBake(int codepoint, CallbackInfoReturnable<?> cir) {
		this.speedpack$ensureSelected();
	}
	*///?}
}
