package dev.speedpack.client.profile;

import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/** Named timestamps (ms since JVM start) so we can see when each startup phase happens. */
public final class Timeline {
	private record Event(long uptimeMs, String thread, String name) {}

	private static final List<Event> EVENTS = new CopyOnWriteArrayList<>();

	private Timeline() {}

	public static void mark(String name) {
		EVENTS.add(new Event(StackSampler.jvmUptimeMs(), Thread.currentThread().getName(), name));
	}

	public static void write() {
		StringBuilder sb = new StringBuilder("uptime_ms\tdelta_ms\tthread\tevent\n");
		long prev = 0;
		for (Event e : EVENTS) {
			sb.append(e.uptimeMs).append('\t').append(e.uptimeMs - prev).append('\t')
				.append(e.thread).append('\t').append(e.name).append('\n');
			prev = e.uptimeMs;
		}
		try {
			Files.writeString(Out.dir().resolve("timeline.tsv"), sb.toString());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
