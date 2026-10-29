package dev.speedpack.client;

import dev.speedpack.client.profile.StackSampler;
import dev.speedpack.client.profile.Timeline;
import net.fabricmc.loader.api.entrypoint.PreLaunchEntrypoint;

/** Runs before Minecraft classes load: samples Minecraft bootstrap + mod init (written as prelaunch.*). */
public class SpeedPackPreLaunch implements PreLaunchEntrypoint {
	@Override
	public void onPreLaunch() {
		if (!SpeedPackConfig.DIAGNOSTICS) return;
		Timeline.mark("preLaunch (JVM start + Fabric loader before this)");
		StackSampler.start();
	}
}
