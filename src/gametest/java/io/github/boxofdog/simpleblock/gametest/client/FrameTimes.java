package io.github.boxofdog.simpleblock.gametest.client;

import java.util.ArrayList;
import java.util.List;

/**
 * Client tests: render time per frame ({@code mixin/FrameTimerMixin}: {@code GameRenderer.render}, start to end),
 * recorded while {@link #recording}; the M13 Trail Blazer benchmark compares them with and without the bonus.
 */
public final class FrameTimes {
	private static final List<Long> TIMES = new ArrayList<>();
	private static volatile boolean recording;
	private static long started;

	private FrameTimes() {
	}

	public static void start() {
		started = System.nanoTime();
	}

	public static void end() {
		if (recording) {
			synchronized (TIMES) {
				TIMES.add(System.nanoTime() - started);
			}
		}
	}

	public static void record() {
		synchronized (TIMES) {
			TIMES.clear();
		}
		recording = true;
	}

	/** Stops recording; frames, then median, 95th percentile and worst frame render time in ms. */
	public static double[] stop() {
		recording = false;
		List<Long> times;
		synchronized (TIMES) {
			times = new ArrayList<>(TIMES);
		}
		times.sort(null);
		if (times.isEmpty()) {
			return new double[] {0, 0, 0, 0};
		}
		return new double[] {times.size(), times.get(times.size() / 2) / 1e6, times.get(Math.min(times.size() - 1, times.size() * 95 / 100)) / 1e6,
			times.getLast() / 1e6};
	}
}
