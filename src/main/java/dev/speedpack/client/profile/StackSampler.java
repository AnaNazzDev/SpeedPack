package dev.speedpack.client.profile;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.LongAdder;

/**
 * Poor man's sampling profiler (no mixin, no Minecraft class needed).
 * Every {@link #INTERVAL_MS} ms it snapshots all threads and counts folded stacks,
 * written as "thread;frameA;frameB count" lines (flamegraph.pl / speedscope / inferno compatible).
 */
public final class StackSampler {
	private static final int INTERVAL_MS = 15;
	private static final int MAX_DEPTH = 48;

	private static final Map<String, LongAdder> STACKS = new ConcurrentHashMap<>();
	private static final AtomicBoolean RUNNING = new AtomicBoolean();
	private static volatile long startNanos;
	private static volatile long samples;

	private StackSampler() {}

	public static void start() {
		if (!RUNNING.compareAndSet(false, true)) return;
		STACKS.clear();
		samples = 0;
		startNanos = System.nanoTime();
		Thread t = new Thread(StackSampler::loop, "SpeedPack-Sampler");
		t.setDaemon(true);
		t.start();
	}

	private static void loop() {
		Thread self = Thread.currentThread();
		while (RUNNING.get()) {
			Map<Thread, StackTraceElement[]> all = Thread.getAllStackTraces();
			samples++;
			for (Map.Entry<Thread, StackTraceElement[]> e : all.entrySet()) {
				Thread th = e.getKey();
				StackTraceElement[] st = e.getValue();
				boolean render = th.getName().equals("Render thread");
				// Render thread is kept in any state: WAITING there means it is blocked on workers.
				if (th == self || st.length == 0 || (!render && th.getState() != Thread.State.RUNNABLE)) continue;
				StringBuilder sb = new StringBuilder(normalizeThreadName(th.getName()));
				if (render) sb.append('[').append(th.getState()).append(']');
				int n = Math.min(st.length, MAX_DEPTH);
				for (int i = n - 1; i >= 0; i--) { // root first
					sb.append(';').append(st[i].getClassName()).append('.').append(st[i].getMethodName());
				}
				STACKS.computeIfAbsent(sb.toString(), k -> new LongAdder()).increment();
			}
			try {
				Thread.sleep(INTERVAL_MS);
			} catch (InterruptedException ex) {
				return;
			}
		}
	}

	/** "Worker-Main-12" -> "Worker-Main-#", so that worker pool threads are merged. */
	private static String normalizeThreadName(String name) {
		return name.replaceAll("\\d+", "#");
	}

	/** Stops sampling and writes {@code <label>.folded} + {@code <label>.top.txt}. */
	public static void stopAndWrite(String label) {
		if (!RUNNING.compareAndSet(true, false)) return;
		double seconds = (System.nanoTime() - startNanos) / 1e9;
		Path dir = Out.dir();
		try {
			List<Map.Entry<String, LongAdder>> sorted = new ArrayList<>(STACKS.entrySet());
			sorted.sort((a, b) -> Long.compare(b.getValue().sum(), a.getValue().sum()));

			StringBuilder folded = new StringBuilder();
			Map<String, Long> self = new TreeMap<>();
			Map<String, Long> perThread = new TreeMap<>();
			for (var e : sorted) {
				long c = e.getValue().sum();
				folded.append(e.getKey()).append(' ').append(c).append('\n');
				String[] parts = e.getKey().split(";");
				self.merge(parts[parts.length - 1], c, Long::sum);
				perThread.merge(parts[0], c, Long::sum);
			}
			Files.writeString(dir.resolve(label + ".folded"), folded.toString());

			StringBuilder top = new StringBuilder();
			top.append(String.format("Sampled %.1fs, %d snapshots, interval %d ms%n%n", seconds, samples, INTERVAL_MS));
			top.append("== RUNNABLE samples per thread (merged by name) ==\n");
			perThread.entrySet().stream()
				.sorted((a, b) -> Long.compare(b.getValue(), a.getValue()))
				.limit(25)
				.forEach(e -> top.append(String.format("%8d  %s%n", e.getValue(), e.getKey())));
			top.append("\n== Top 60 self (leaf) methods ==\n");
			self.entrySet().stream()
				.sorted((a, b) -> Long.compare(b.getValue(), a.getValue()))
				.limit(60)
				.forEach(e -> top.append(String.format("%8d  %s%n", e.getValue(), e.getKey())));
			Files.writeString(dir.resolve(label + ".top.txt"), top.toString());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/** Milliseconds since the JVM started (for timeline events). */
	public static long jvmUptimeMs() {
		return ManagementFactory.getRuntimeMXBean().getUptime();
	}
}
