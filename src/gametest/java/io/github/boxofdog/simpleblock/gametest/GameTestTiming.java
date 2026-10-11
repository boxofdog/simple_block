package io.github.boxofdog.simpleblock.gametest;

import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;

/** Timing helpers shared by the game tests. */
final class GameTestTiming {
	private GameTestTiming() {
	}

	/**
	 * Runs {@code test} once {@code ready} holds, then passes the test. A test environment sets the clock
	 * as the batch starts, but the level only recalculates sky darkness on its next tick: on the first
	 * tick it still has the previous batch's value. So the check starts one tick in.
	 */
	static void runWhen(GameTestHelper helper, BooleanSupplier ready, Supplier<String> waitingFor, Runnable test) {
		helper.startSequence()
				.thenIdle(1)
				.thenWaitUntil(() -> helper.assertTrue(ready.getAsBoolean(), "waiting for " + waitingFor.get()))
				.thenExecute(test)
				.thenSucceed();
	}

	/**
	 * Full night: sky darkness 11 at midnight. Dusk values (5-9) are dark by {@code isDarkOutside} but
	 * still too bright for monsters to spawn on the surface.
	 */
	static void runAtNight(GameTestHelper helper, Runnable test) {
		ensureNight(helper);
		ServerLevel level = helper.getLevel();
		runWhen(helper, () -> level.getSkyDarken() >= 10, () -> "the night sky (overworld clock " + level.getOverworldClockTime()
				+ ", sky darkness " + level.getSkyDarken() + ")", test);
	}

	/**
	 * Night tests also get their midnight from a test environment, but now and then the overworld
	 * clock is back at the previous batch's daytime value when the test starts (seen as clock 25742
	 * in a {@code phantom_night} batch whose environment sets 18000). The cause is in the game-test
	 * runner, not in the mod, so a night test sets midnight itself when the clock is not at night,
	 * and logs that it had to.
	 */
	static void ensureNight(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		long timeOfDay = level.getOverworldClockTime() % 24000L;
		if (timeOfDay < 13000L || timeOfDay > 23000L) {
			SimpleBlockTestLog.info("night environment was not in effect at test start (overworld clock {}); setting midnight",
					level.getOverworldClockTime());
			helper.setTime(18000L);
		}
	}
}
