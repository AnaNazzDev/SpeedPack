package dev.speedpack.client;

import java.util.List;
import java.util.Set;

import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

/** Applies each mixin only if its feature is enabled in config/speedpack.properties. */
public class SpeedPackMixinPlugin implements IMixinConfigPlugin {
	@Override
	public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
		String name = mixinClassName.substring(mixinClassName.lastIndexOf('.') + 1);
		return switch (name) {
			case "FilePackResourcesMixin" -> SpeedPackConfig.ZIP_INDEX;
			case "FontSetMixin" -> SpeedPackConfig.LAZY_FONTS;
			case "FontManagerMixin" -> SpeedPackConfig.FONT_WARM_ONCE;
			case "GlProgramMixin" -> SpeedPackConfig.SHADER_CACHE;
			case "LoadingOverlayMixin" -> SpeedPackConfig.FAST_LOADING_SCREEN;
			case "ReloadableResourceManagerMixin", "SimpleReloadInstanceMixin", "ProfiledReloadInstanceMixin" ->
				SpeedPackConfig.DIAGNOSTICS;
			default -> true;
		};
	}

	@Override
	public void onLoad(String mixinPackage) {}

	@Override
	public String getRefMapperConfig() {
		return null;
	}

	@Override
	public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {}

	@Override
	public List<String> getMixins() {
		return null;
	}

	@Override
	public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {}

	@Override
	public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {}
}
