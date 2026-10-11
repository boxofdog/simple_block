package io.github.boxofdog.simpleblock.gametest;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.AbstractBedBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import net.fabricmc.fabric.api.gametest.v1.GameTest;

/**
 * SF-01 evidence. Every test in this class runs in its own environment (and therefore its own
 * batch), because vanilla only skips the night when every player in the level is asleep.
 */
public class SleepGameTests {
	private static final int TICKS_IN_BED = 140; // vanilla needs 100 ticks of sleep before skipping
	private static final Block RED_BED = Blocks.BED.pick(DyeColor.RED);

	/** BED-01, BED-02, BED-03: two players sleep at midnight; the night is not skipped. */
	@GameTest(environment = "simple_block-gametest:night", maxTicks = 300)
	public void allPlayersSleepingDoNotSkipNight(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		TestPlayers.removeAll(level);
		BlockPos bedA = new BlockPos(1, 1, 1);
		BlockPos bedB = new BlockPos(4, 1, 1);
		List<ServerPlayer> players = List.of(
				sleepInNewBed(helper, bedA),
				sleepInNewBed(helper, bedB)
		);
		tickPlayers(helper, players);
		long clockAtStart = level.getOverworldClockTime();

		for (int i = 0; i < players.size(); i++) {
			ServerPlayer player = players.get(i);
			BlockPos bed = helper.absolutePos(i == 0 ? bedA : bedB).relative(Direction.SOUTH);
			helper.assertTrue(player.isSleeping(), "player " + i + " should be lying in bed at midnight");
			helper.assertTrue(player.getRespawnConfig() != null, "bed should set a respawn point");
			helper.assertValueEqual(player.getRespawnConfig().respawnData().pos(), bed, "respawn position");
			helper.assertValueEqual(player.getRespawnConfig().respawnData().dimension(), level.dimension(), "respawn dimension");
		}

		helper.runAfterDelay(TICKS_IN_BED, () -> {
			long elapsed = level.getOverworldClockTime() - clockAtStart;
			helper.assertValueInBetween(TICKS_IN_BED - 5L, elapsed, TICKS_IN_BED + 5L, "clock ticks elapsed while sleeping");
			helper.assertTrue(level.isDarkOutside(), "it should still be night");

			for (ServerPlayer player : players) {
				helper.assertTrue(player.isSleeping(), "player should not have been woken by a night skip");
				// This is exactly the state in which vanilla advances to morning and wakes everyone.
				helper.assertTrue(player.isSleepingLongEnough(), "player should have slept long enough for vanilla to skip");
			}

			SimpleBlockTestLog.info("BED-01..03: {} players slept {} ticks at midnight; clock {} -> {}, still sleeping: {}",
					players.size(), TICKS_IN_BED, clockAtStart, level.getOverworldClockTime(), players.stream().allMatch(ServerPlayer::isSleeping));

			// Getting out of bed still works (this is what the "Leave Bed" button does).
			players.forEach(player -> player.stopSleepInBed(false, true));
			helper.assertTrue(players.stream().noneMatch(ServerPlayer::isSleeping), "players should be able to leave bed");
			players.forEach(TestPlayers::remove);
			helper.succeed();
		});
	}

	/** BED-04: sleeping through a night-time thunderstorm neither skips the night nor clears the storm. */
	@GameTest(environment = "simple_block-gametest:night_thunder", maxTicks = 500)
	public void sleepingDoesNotClearNightStorm(GameTestHelper helper) {
		assertStormSurvivesSleep(helper);
	}

	/** BED-04: sleeping is allowed in a daytime thunderstorm in vanilla; it still must not clear it. */
	@GameTest(environment = "simple_block-gametest:day_thunder", maxTicks = 500)
	public void sleepingDoesNotClearDayStorm(GameTestHelper helper) {
		assertStormSurvivesSleep(helper);
	}

	/** BED-05: Nether beds still explode instead of setting spawn or letting anyone sleep. */
	@GameTest(dimension = "minecraft:the_nether", environment = "simple_block-gametest:nether_bed")
	public void netherBedStillExplodes(GameTestHelper helper) {
		assertBedExplodes(helper);
	}

	/** BED-05: End beds still explode instead of setting spawn or letting anyone sleep. */
	@GameTest(dimension = "minecraft:the_end", environment = "simple_block-gametest:end_bed")
	public void endBedStillExplodes(GameTestHelper helper) {
		assertBedExplodes(helper);
	}

	/** BED-06: after dying, the player respawns at the bed they used. */
	@GameTest(maxTicks = 40)
	public void respawnAtBedAfterDeath(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos bed = new BlockPos(1, 1, 1);
		ServerPlayer player = sleepInNewBed(helper, bed);
		BlockPos bedHead = helper.absolutePos(bed).relative(Direction.SOUTH);
		player.stopSleepInBed(false, true);

		// Walk away, die, respawn.
		Vec3 away = helper.absoluteVec(new Vec3(6.5, 1, 6.5));
		player.teleportTo(level, away.x, away.y, away.z, java.util.Set.of(), 0.0F, 0.0F, true);
		player.setHealth(0.0F);
		ServerPlayer respawned = level.getServer().getPlayerList().respawn(player, false, Entity.RemovalReason.KILLED);

		helper.assertValueEqual(respawned.level().dimension(), level.dimension(), "respawn dimension");
		double distance = respawned.position().distanceTo(Vec3.atBottomCenterOf(bedHead));
		helper.assertTrue(distance < 3.0, "respawned " + distance + " blocks from the bed, expected next to it");
		TestPlayers.remove(respawned);
		helper.succeed();
	}

	private static void assertStormSurvivesSleep(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		TestPlayers.removeAll(level);
		List<ServerPlayer> players = new ArrayList<>();
		tickPlayers(helper, players);
		long[] clockAtStart = new long[1];

		helper.startSequence()
				// Thunder fades in over ~100 ticks after the weather is set.
				.thenWaitUntil(() -> helper.assertTrue(level.isThundering(), "waiting for the thunderstorm to build"))
				.thenExecute(() -> {
					players.add(sleepInNewBed(helper, new BlockPos(1, 1, 1)));
					helper.assertTrue(players.getFirst().isSleeping(), "vanilla lets players sleep during a thunderstorm");
					clockAtStart[0] = level.getOverworldClockTime();
				})
				.thenExecuteAfter(TICKS_IN_BED, () -> {
					ServerPlayer player = players.getFirst();
					long elapsed = level.getOverworldClockTime() - clockAtStart[0];
					helper.assertValueInBetween(TICKS_IN_BED - 5L, elapsed, TICKS_IN_BED + 5L, "clock ticks elapsed while sleeping");
					helper.assertTrue(level.isRaining(), "rain should continue");
					helper.assertTrue(level.isThundering(), "thunder should continue");
					helper.assertTrue(player.isSleeping(), "player should not have been woken by a skip");
					helper.assertTrue(player.isSleepingLongEnough(), "player should have slept long enough for vanilla to skip");
					SimpleBlockTestLog.info("BED-04: slept {} ticks in a thunderstorm; clock {} -> {}, thundering: {}, still sleeping: {}",
							TICKS_IN_BED, clockAtStart[0], level.getOverworldClockTime(), level.isThundering(), player.isSleeping());
					player.stopSleepInBed(false, true);
					TestPlayers.remove(player);
				})
				.thenSucceed();
	}

	/**
	 * Mock players have no network connection, so the server never runs their per-tick player update
	 * (where the sleep timer counts up). Do it here, like a connected player's connection would.
	 */
	private static void tickPlayers(GameTestHelper helper, List<ServerPlayer> players) {
		helper.onEachTick(() -> players.forEach(ServerPlayer::doTick));
	}

	private static void assertBedExplodes(GameTestHelper helper) {
		BlockPos foot = new BlockPos(2, 1, 2);
		placeBed(helper, foot);
		ServerPlayer player = TestPlayers.create(helper, GameType.SURVIVAL, foot.east());
		useBed(helper, player, foot);
		helper.assertFalse(player.isSleeping(), "nobody can sleep here");
		helper.assertTrue(player.getRespawnConfig() == null, "bed should not set spawn here");
		helper.assertBlockNotPresent(RED_BED, foot);
		helper.assertBlockNotPresent(RED_BED, foot.south());
		TestPlayers.remove(player);
		helper.succeed();
	}

	/** Places a bed (foot at {@code foot}, head to the south), stands a survival player next to it and right-clicks it. */
	static ServerPlayer sleepInNewBed(GameTestHelper helper, BlockPos foot) {
		placeBed(helper, foot);
		ServerPlayer player = TestPlayers.create(helper, GameType.SURVIVAL, foot.east());
		useBed(helper, player, foot);
		return player;
	}

	static void placeBed(GameTestHelper helper, BlockPos foot) {
		BlockState base = RED_BED.defaultBlockState().setValue(HorizontalDirectionalBlock.FACING, Direction.SOUTH);
		helper.setBlock(foot, base.setValue(AbstractBedBlock.PART, BedPart.FOOT));
		helper.setBlock(foot.south(), base.setValue(AbstractBedBlock.PART, BedPart.HEAD));
	}

	/** Same server path as a right-click packet. */
	static void useBed(GameTestHelper helper, ServerPlayer player, BlockPos relativePos) {
		BlockPos pos = helper.absolutePos(relativePos);
		BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false);
		player.gameMode.useItemOn(player, helper.getLevel(), player.getMainHandItem(), InteractionHand.MAIN_HAND, hit);
	}
}
