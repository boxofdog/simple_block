package io.github.boxofdog.simpleblock.gametest;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.level.block.Blocks;

import net.fabricmc.fabric.api.gametest.v1.GameTest;

import io.github.boxofdog.simpleblock.feature.BedRest;

/**
 * M13 night bed rest: lying in a bed at night heals a health point every second (a heart every two)
 * for a quarter food point each, paid a whole food point at a time with the change kept as credit.
 * Each test has its own environment (a copy of the SF-01 ones), so the SF-01 sleep tests, which remove
 * every player in their level, never share a batch with these.
 */
public class BedRestGameTests {
	private static final net.minecraft.world.level.block.Block RED_BED = Blocks.BED.pick(net.minecraft.world.item.DyeColor.RED);

	/** A player's health, food and credit before resting, to check the exact price afterwards. */
	private record Rester(String name, ServerPlayer player, float health, int food) {
		/** Health points healed. */
		int healed() {
			return Math.round(this.player.getHealth() - this.health);
		}

		/** Health points paid for: whole food points spent, less the credit still unspent. */
		int paid() {
			return (this.food - this.player.getFoodData().getFoodLevel()) * BedRest.HEALTH_PER_FOOD - this.player.getAttachedOrElse(BedRest.CREDIT, 0);
		}

		String describe() {
			return this.name + " " + this.health + "->" + this.player.getHealth() + " hp, food " + this.food + "->" + this.player.getFoodData().getFoodLevel()
				+ ", credit " + this.player.getAttachedOrElse(BedRest.CREDIT, 0);
		}
	}

	private static Rester rest(GameTestHelper helper, String name, BlockPos foot, float health, int food, net.minecraft.world.level.block.Block bed) {
		SleepGameTests.placeBed(helper, foot, bed);
		ServerPlayer player = TestPlayers.create(helper, net.minecraft.world.level.GameType.SURVIVAL, foot.east());
		player.setAttached(BedRest.CREDIT, 0);
		player.setHealth(health);
		player.getFoodData().setFoodLevel(food);
		player.getFoodData().setSaturation(Math.min(5.0F, food));
		SleepGameTests.useBed(helper, player, foot);
		helper.assertTrue(player.isSleeping(), name + " should be lying in bed at night");
		return new Rester(name, player, health, food);
	}

	/**
	 * Five players rest at once at night: a half-health player heals to full at a heart per two
	 * seconds and pays 3 food points (12 health points' worth, 2 left as credit); a player with 1 food
	 * point heals 4 health points and stops; a starving player does not heal; a full-health player
	 * spends nothing; a straw bed heals too. The clock runs on and nobody is woken. Then the first
	 * player's credit pays for healing after getting up and lying down again, with no food spent.
	 */
	@GameTest(environment = "simple_block-gametest:bed_rest_night", maxTicks = 600)
	public void restHealsAtNightForFood(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		TestPlayers.removeAll(level);
		GameTestTiming.ensureNight(helper);
		List<Rester> resters = new ArrayList<>();
		long[] clock = new long[1];
		float[] midway = new float[1];
		helper.startSequence()
			.thenIdle(1)
			.thenWaitUntil(() -> helper.assertTrue(BedRest.isNight(level), "waiting for night (clock " + level.getOverworldClockTime() + ")"))
			.thenExecute(() -> {
				resters.add(rest(helper, "half health", new BlockPos(0, 1, 0), 10.0F, 20, RED_BED));
				resters.add(rest(helper, "one food point", new BlockPos(2, 1, 0), 4.0F, 1, RED_BED));
				resters.add(rest(helper, "starving", new BlockPos(4, 1, 0), 4.0F, 0, RED_BED));
				resters.add(rest(helper, "full health", new BlockPos(0, 1, 4), 20.0F, 20, RED_BED));
				resters.add(rest(helper, "straw bed", new BlockPos(2, 1, 4), 16.0F, 10, Blocks.STRAW_BED));
				clock[0] = level.getOverworldClockTime();
			})
			.thenExecuteAfter(5 * BedRest.TICKS_PER_HEALTH + 2, () -> midway[0] = resters.getFirst().player().getHealth())
			.thenExecuteAfter(6 * BedRest.TICKS_PER_HEALTH, () -> {
				List<String> log = new ArrayList<>();
				resters.forEach(r -> log.add(r.describe()));
				long elapsed = level.getOverworldClockTime() - clock[0];
				helper.assertTrue(elapsed >= 11 * BedRest.TICKS_PER_HEALTH, "the clock ran on, not skipped or stopped: " + elapsed);
				helper.assertTrue(BedRest.isNight(level), "still night");
				for (Rester r : resters) {
					helper.assertTrue(r.player().isSleeping(), r.name() + " still in bed: " + log);
					helper.assertValueEqual(r.paid(), r.healed(), r.name() + ": health points paid for = healed: " + log);
				}
				helper.assertValueEqual(midway[0], 15.0F, "a heart every two seconds: 5 health points in 5 seconds: " + log);
				Rester half = resters.get(0);
				helper.assertTrue(half.player().getHealth() == 20.0F && half.player().getFoodData().getFoodLevel() == 17
					&& half.player().getAttachedOrElse(BedRest.CREDIT, 0) == 2, "half health: full for 3 food points, 2 credit: " + log);
				Rester poor = resters.get(1);
				helper.assertTrue(poor.player().getHealth() == 8.0F && poor.player().getFoodData().getFoodLevel() == 0, "one food point heals 4: " + log);
				Rester starving = resters.get(2);
				helper.assertTrue(starving.player().getHealth() == 4.0F, "no food, no healing: " + log);
				helper.assertTrue(resters.get(3).healed() == 0 && resters.get(3).player().getFoodData().getFoodLevel() == 20, "full health spends nothing: " + log);
				helper.assertTrue(resters.get(4).player().getHealth() == 20.0F, "a straw bed rests too: " + log);
				SimpleBlockTestLog.info("M13 BED REST: after {} ticks at night (clock {} -> {}): {}", elapsed, clock[0], level.getOverworldClockTime(), log);

				// Up and down again: the credit pays, no food.
				half.player().stopSleepInBed(false, true);
				half.player().setHealth(18.0F);
				SleepGameTests.useBed(helper, half.player(), new BlockPos(0, 1, 0));
				helper.assertTrue(half.player().isSleeping(), "back in bed");
			})
			.thenExecuteAfter(2 * BedRest.TICKS_PER_HEALTH + 2, () -> {
				ServerPlayer player = resters.getFirst().player();
				helper.assertTrue(player.getHealth() == 20.0F && player.getFoodData().getFoodLevel() == 17
					&& player.getAttachedOrElse(BedRest.CREDIT, 0) == 0, "the credit paid for 2 health points: " + player.getHealth() + " hp, food "
					+ player.getFoodData().getFoodLevel() + ", credit " + player.getAttachedOrElse(BedRest.CREDIT, 0));
				player.stopSleepInBed(false, true);
				// Out of bed: nothing more.
				player.setHealth(12.0F);
			})
			.thenExecuteAfter(3 * BedRest.TICKS_PER_HEALTH, () -> {
				ServerPlayer player = resters.getFirst().player();
				helper.assertTrue(player.getHealth() == 12.0F && player.getFoodData().getFoodLevel() == 17, "out of bed: no healing, no food");
				resters.forEach(r -> TestPlayers.remove(r.player()));
			})
			.thenSucceed();
	}

	/**
	 * Vanilla's "You may not rest now; there are monsters nearby" still stops the player lying down,
	 * and so no healing or food is spent; with the monster gone, the same player rests and heals.
	 */
	@GameTest(environment = "simple_block-gametest:bed_rest_monsters", maxTicks = 400)
	public void monstersNearbyPreventRest(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		TestPlayers.removeAll(level);
		GameTestTiming.ensureNight(helper);
		BlockPos foot = new BlockPos(1, 1, 1);
		ServerPlayer[] player = new ServerPlayer[1];
		Zombie[] zombie = new Zombie[1];
		helper.startSequence()
			.thenIdle(1)
			.thenWaitUntil(() -> helper.assertTrue(BedRest.isNight(level), "waiting for night"))
			.thenExecute(() -> {
				SleepGameTests.placeBed(helper, foot, RED_BED);
				zombie[0] = helper.spawnWithNoFreeWill(EntityTypes.ZOMBIE, new BlockPos(5, 1, 5));
				zombie[0].setPersistenceRequired();
				player[0] = TestPlayers.create(helper, net.minecraft.world.level.GameType.SURVIVAL, foot.east());
				player[0].setAttached(BedRest.CREDIT, 0);
				player[0].setHealth(10.0F);
				player[0].getFoodData().setFoodLevel(20);
				SleepGameTests.useBed(helper, player[0], foot);
				helper.assertFalse(player[0].isSleeping(), "a zombie nearby: vanilla refuses rest");
			})
			.thenExecuteAfter(3 * BedRest.TICKS_PER_HEALTH, () -> {
				SleepGameTests.useBed(helper, player[0], foot);
				helper.assertFalse(player[0].isSleeping(), "still refused");
			})
			.thenExecuteAfter(3 * BedRest.TICKS_PER_HEALTH, () -> {
				helper.assertTrue(player[0].getHealth() == 10.0F && player[0].getFoodData().getFoodLevel() == 20
					&& player[0].getAttachedOrElse(BedRest.CREDIT, 0) == 0, "refused: no healing, no food: " + player[0].getHealth());
				zombie[0].discard();
				SleepGameTests.useBed(helper, player[0], foot);
				helper.assertTrue(player[0].isSleeping(), "the monster gone, the player lies down");
			})
			.thenExecuteAfter(4 * BedRest.TICKS_PER_HEALTH + 2, () -> {
				helper.assertValueEqual(player[0].getHealth(), 14.0F, "now resting heals");
				SimpleBlockTestLog.info("M13 BED REST: zombie nearby refused twice, no healing or food; zombie gone: 10 -> {} hp, food {}",
					player[0].getHealth(), player[0].getFoodData().getFoodLevel());
				player[0].stopSleepInBed(false, true);
				TestPlayers.remove(player[0]);
			})
			.thenSucceed();
	}

	/** Vanilla lets players lie down in a daytime thunderstorm; that is not night, so it does not heal. */
	@GameTest(environment = "simple_block-gametest:bed_rest_day_thunder", maxTicks = 500)
	public void noHealingByDay(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		TestPlayers.removeAll(level);
		Rester[] rester = new Rester[1];
		helper.startSequence()
			.thenIdle(1)
			.thenWaitUntil(() -> helper.assertTrue(level.isThundering() && level.isDarkOutside(), "waiting for the storm to darken the sky"))
			.thenExecute(() -> {
				helper.assertFalse(BedRest.isNight(level), "a stormy day is not night (clock " + level.getOverworldClockTime() + ")");
				rester[0] = rest(helper, "day storm", new BlockPos(1, 1, 1), 10.0F, 20, RED_BED);
			})
			.thenExecuteAfter(5 * BedRest.TICKS_PER_HEALTH, () -> {
				helper.assertTrue(rester[0].player().isSleeping(), "still lying down");
				helper.assertTrue(rester[0].healed() == 0 && rester[0].player().getFoodData().getFoodLevel() == 20, "no healing by day: " + rester[0].describe());
				SimpleBlockTestLog.info("M13 BED REST: daytime thunderstorm: lying down allowed, {}", rester[0].describe());
				rester[0].player().stopSleepInBed(false, true);
				TestPlayers.remove(rester[0].player());
			})
			.thenSucceed();
	}

	/** The unspent credit is saved with the player: a save and reload neither loses nor grants healing. */
	@GameTest
	public void restCreditIsSaved(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		ServerPlayer player = TestPlayers.create(helper, net.minecraft.world.level.GameType.SURVIVAL, new BlockPos(1, 1, 1));
		player.setAttached(BedRest.CREDIT, 3);
		net.minecraft.world.level.storage.TagValueOutput output = net.minecraft.world.level.storage.TagValueOutput.createWithContext(
			net.minecraft.util.ProblemReporter.DISCARDING, level.registryAccess());
		player.saveWithoutId(output);
		ServerPlayer reloaded = TestPlayers.create(helper, net.minecraft.world.level.GameType.SURVIVAL, new BlockPos(2, 1, 1));
		reloaded.load(net.minecraft.world.level.storage.TagValueInput.create(net.minecraft.util.ProblemReporter.DISCARDING, level.registryAccess(),
			output.buildResult()));
		helper.assertValueEqual(reloaded.getAttachedOrElse(BedRest.CREDIT, 0), 3, "credit after a save and reload");
		TestPlayers.remove(player);
		TestPlayers.remove(reloaded);
		helper.succeed();
	}
}
