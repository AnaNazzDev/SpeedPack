package dev.speedpack.client.mixin;

import java.util.List;

import dev.speedpack.client.profile.StackSampler;
import dev.speedpack.client.profile.SystemReport;
import dev.speedpack.client.profile.Timeline;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.resources.ReloadableResourceManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.server.packs.resources.ReloadInstance;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import net.minecraft.util.Unit;

@Mixin(ReloadableResourceManager.class)
public abstract class ReloadableResourceManagerMixin {
	/** Starts the stack sampler for this reload and records which packs take part. */
	@Inject(method = "createReload", at = @At("HEAD"))
	private void speedpack$begin(Executor backgroundExecutor, Executor gameExecutor,
		CompletableFuture<Unit> waitingFor, List<PackResources> packs,
		CallbackInfoReturnable<ReloadInstance> cir) {
		StackSampler.start(); // no-op if already running (the startup reload is sampled from mod init)
		StringBuilder sb = new StringBuilder("resource reload begins, packs:");
		for (PackResources p : packs) sb.append(' ').append(p.packId());
		Timeline.mark(sb.toString());
		SystemReport.writeDownloads();
	}
}
