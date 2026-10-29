package dev.speedpack.client.mixin;

import net.minecraft.server.packs.resources.SimpleReloadInstance;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(SimpleReloadInstance.class)
public abstract class SimpleReloadInstanceMixin {
	/** Force vanilla's own per-listener profiling (ProfiledReloadInstance) for every reload. */
	@ModifyVariable(method = "create", at = @At("HEAD"), argsOnly = true)
	private static boolean speedpack$forceProfiled(boolean profiled) {
		return true;
	}
}
