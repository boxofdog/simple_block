package io.github.boxofdog.simpleblock.gametest;

import java.util.function.BooleanSupplier;

import net.minecraft.gametest.framework.GameTestHelper;

/** Timing helpers shared by the game tests. */
final class GameTestTiming {
	private GameTestTiming() {
	}

	/**
	 * Runs {@code test} once {@code ready} holds, then passes the test. A test environment sets the clock
	 * as the batch starts, but the level only recalculates sky darkness on its next tick, so a test that
	 * checks night-time behaviour on its first tick can still see the previous batch's daylight.
	 */
	static void runWhen(GameTestHelper helper, BooleanSupplier ready, String waitingFor, Runnable test) {
		helper.startSequence()
				.thenWaitUntil(() -> helper.assertTrue(ready.getAsBoolean(), "waiting for " + waitingFor))
				.thenExecute(test)
				.thenSucceed();
	}

	static void runAtNight(GameTestHelper helper, Runnable test) {
		runWhen(helper, () -> helper.getLevel().getSkyDarken() >= 5, "the night sky", test);
	}
}
