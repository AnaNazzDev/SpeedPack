package dev.speedpack.client.mixin;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.List;

import dev.speedpack.client.gl.ProgramBinaryCache;
import dev.speedpack.client.profile.Out;
import dev.speedpack.client.profile.StackSampler;
import dev.speedpack.client.profile.Timeline;
import net.minecraft.server.packs.resources.ProfiledReloadInstance;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ProfiledReloadInstance.class)
public abstract class ProfiledReloadInstanceMixin {
	private static int speedpack$reloadIndex;

	/** Dumps the per-listener prepare/apply times of each resource reload to reload-listeners.tsv. */
	@Inject(method = "finish", at = @At("HEAD"))
	private void speedpack$dump(List<ProfiledReloadInstance.State> states,
		CallbackInfoReturnable<List<ProfiledReloadInstance.State>> cir) {
		int idx = ++speedpack$reloadIndex;
		Timeline.mark("resource reload #" + idx + " finished; shader cache: " + ProgramBinaryCache.statsAndReset());
		Timeline.write();
		StackSampler.stopAndWrite("reload-" + idx);
		StringBuilder sb = new StringBuilder();
		for (ProfiledReloadInstance.State s : states) {
			sb.append(idx).append('\t').append(s.name()).append('\t')
				.append(s.preparationNanos().get() / 1_000_000).append('\t').append(s.preparationCount().get()).append('\t')
				.append(s.reloadNanos().get() / 1_000_000).append('\t').append(s.reloadCount().get()).append('\n');
		}
		try {
			var f = Out.dir().resolve("reload-listeners.tsv");
			if (!Files.exists(f)) {
				Files.writeString(f, "reload#\tlistener\tprepare_ms\tprepare_tasks\tapply_ms\tapply_tasks\n");
			}
			Files.writeString(f, sb.toString(), StandardOpenOption.APPEND);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
