package dev.speedpack.client.mixin;

import net.minecraft.client.gui.screens.LoadingOverlay;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

/**
 * The Mojang overlay keeps covering the game well after the reload is done: in-game reloads wait at least
 * 1000 ms of fade-in, and the overlay is only removed 2x FADE_OUT_TIME (2000 ms) after the reload finished,
 * blocking input meanwhile. We keep a short fade so it still looks smooth.
 */
@Mixin(LoadingOverlay.class)
public abstract class LoadingOverlayMixin {
	private static final float SPEEDPACK$FADE_OUT_MS = 150.0f; // overlay removed after 2x this
	private static final float SPEEDPACK$FADE_IN_MS = 100.0f;
	private static final long SPEEDPACK$MIN_FADE_IN_MS = 100L;

	//? if >=26.1 {
	@ModifyConstant(method = "extractRenderState", constant = @Constant(floatValue = 1000.0f))
	private float speedpack$fadeOut(float original) {
		return SPEEDPACK$FADE_OUT_MS;
	}

	@ModifyConstant(method = "extractRenderState", constant = @Constant(floatValue = 500.0f))
	private float speedpack$fadeIn(float original) {
		return SPEEDPACK$FADE_IN_MS;
	}
	//?} else {
	/*@ModifyConstant(method = "render", constant = @Constant(floatValue = 1000.0f))
	private float speedpack$fadeOut(float original) {
		return SPEEDPACK$FADE_OUT_MS;
	}

	@ModifyConstant(method = "render", constant = @Constant(floatValue = 500.0f))
	private float speedpack$fadeIn(float original) {
		return SPEEDPACK$FADE_IN_MS;
	}
	*///?}

	// The minimum in-game fade-in only exists since 1.21.9.
	//? if >=1.21.9 {
	@ModifyConstant(method = "isReadyToFadeOut", constant = @Constant(longValue = 1000L))
	private long speedpack$minFadeIn(long original) {
		return SPEEDPACK$MIN_FADE_IN_MS;
	}
	//?}
}
