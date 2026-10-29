package dev.speedpack.client;

import dev.speedpack.client.profile.StackSampler;
import dev.speedpack.client.profile.SystemReport;
import dev.speedpack.client.profile.Timeline;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;

public class SpeedPackClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		if (!SpeedPackConfig.DIAGNOSTICS) return;
		Timeline.mark("speedpack client init (JVM uptime shows how long mod loading took)");
		// preLaunch -> here = Minecraft bootstrap + mod init. Sampling then restarts and covers
		// the first reload; ReloadableResourceManagerMixin restarts it for every later reload.
		StackSampler.stopAndWrite("prelaunch");
		StackSampler.start();
		SystemReport.write();

		ScreenEvents.AFTER_INIT.register((client, screen, w, h) -> Timeline.mark("screen: " + screen.getClass().getSimpleName()));
	}
}
