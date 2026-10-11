package io.github.boxofdog.simpleblock.gametest;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.food.FoodConstants;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;

import net.fabricmc.fabric.api.gametest.v1.GameTest;

import io.github.boxofdog.simpleblock.feature.Exhaustion;

/**
 * FOOD-01: each activity is run through the game's own code (the methods vanilla calls for a
 * player's movement packets, jumps and block breaking, attacks, damage, regeneration and the Hunger
 * effect) and the exhaustion it adds is compared with the 26.3 vanilla value.
 */
public class FoodGameTests {
	private static final Field EXHAUSTION = field("exhaustionLevel");
	private static final Field TICK_TIMER = field("tickTimer");

	private static Field field(String name) {
		try {
			Field field = FoodData.class.getDeclaredField(name);
			field.setAccessible(true);
			return field;
		} catch (ReflectiveOperationException e) {
			throw new IllegalStateException("FoodData." + name + " not found", e);
		}
	}

	private static float exhaustion(ServerPlayer player) {
		try {
			return EXHAUSTION.getFloat(player.getFoodData());
		} catch (IllegalAccessException e) {
			throw new IllegalStateException(e);
		}
	}

	private static void reset(ServerPlayer player) {
		try {
			EXHAUSTION.setFloat(player.getFoodData(), 0.0F);
			TICK_TIMER.setInt(player.getFoodData(), 0);
		} catch (IllegalAccessException e) {
			throw new IllegalStateException(e);
		}
	}

	/** Runs {@code activity} from zero exhaustion and returns what it added. */
	private static float measure(ServerPlayer player, Runnable activity) {
		reset(player);
		activity.run();
		return exhaustion(player);
	}

	private static void check(GameTestHelper helper, Map<String, float[]> rows, List<String> problems, String name, float measured, float vanilla,
		float factor) {
		rows.put(name, new float[] {measured, vanilla});
		if (Math.abs(measured - vanilla * factor) > 1.0e-4F) {
			problems.add(name + ": " + measured + " instead of " + vanilla * factor + " (vanilla " + vanilla + ")");
		}
	}

	private static String table(Map<String, float[]> rows) {
		List<String> lines = new ArrayList<>();
		rows.forEach((name, values) -> lines.add(String.format("%s %.4f (vanilla %.4f, x%.2f)", name, values[0], values[1],
			values[1] == 0 ? 0.0F : values[0] / values[1])));
		return String.join("; ", lines);
	}

	/** FOOD-01: sprinting, jumping, sprint-jumping, swimming, walking under water and breaking blocks cost half. */
	@GameTest
	public void activityExhaustionIsHalved(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		ServerPlayer player = TestPlayers.create(helper, GameType.SURVIVAL, new BlockPos(1, 1, 1));
		for (int x = 3; x <= 7; x++) {
			for (int z = 3; z <= 4; z++) {
				helper.setBlock(new BlockPos(x, 1, z), Blocks.DIRT);
			}
		}
		helper.startSequence()
			.thenIdle(1)
			.thenExecute(() -> {
				Map<String, float[]> rows = new LinkedHashMap<>();
				List<String> problems = new ArrayList<>();
				float half = Exhaustion.ACTIVITY_FACTOR;

				// Movement packets are handled by checkMovementStatistics, with the distance of each move.
				player.setOnGround(true);
				player.setSprinting(true);
				check(helper, rows, problems, "sprint 10 blocks", measure(player, () -> player.checkMovementStatistics(10.0, 0.0, 0.0)),
					10 * FoodConstants.EXHAUSTION_SPRINT, half);
				player.setSprinting(false);
				check(helper, rows, problems, "walk 10 blocks", measure(player, () -> player.checkMovementStatistics(10.0, 0.0, 0.0)), 0.0F, half);
				player.setSwimming(true);
				check(helper, rows, problems, "swim 10 blocks", measure(player, () -> player.checkMovementStatistics(10.0, 0.0, 0.0)),
					10 * FoodConstants.EXHAUSTION_SWIM, half);
				player.setSwimming(false);

				check(helper, rows, problems, "10 jumps", measure(player, () -> {
					for (int i = 0; i < 10; i++) {
						player.jumpFromGround();
					}
				}), 10 * FoodConstants.EXHAUSTION_JUMP, half);
				player.setSprinting(true);
				check(helper, rows, problems, "10 sprint-jumps", measure(player, () -> {
					for (int i = 0; i < 10; i++) {
						player.jumpFromGround();
					}
				}), 10 * FoodConstants.EXHAUSTION_SPRINT_JUMP, half);
				player.setSprinting(false);

				int[] mined = {0};
				check(helper, rows, problems, "break 10 dirt", measure(player, () -> {
					for (int x = 3; x <= 7; x++) {
						for (int z = 3; z <= 4; z++) {
							mined[0] += player.gameMode.destroyBlock(helper.absolutePos(new BlockPos(x, 1, z))) ? 1 : 0;
						}
					}
				}), 10 * FoodConstants.EXHAUSTION_MINE, half);
				helper.assertValueEqual(mined[0], 10, "dirt blocks broken");

				// Walking under water: put the player's head in water and let the game notice it.
				helper.setBlock(new BlockPos(1, 1, 1), Blocks.WATER);
				helper.setBlock(new BlockPos(1, 2, 1), Blocks.WATER);
				SimpleBlockTestLog.info("FOOD-01 activities (exhaustion added; vanilla 26.3 value; ratio): {}", table(rows));
				helper.assertTrue(problems.isEmpty(), String.join("; ", problems));
			})
			// A test player has no connection driving its movement tick, so step it here to update its fluid state.
			.thenWaitUntil(() -> {
				player.doTick();
				helper.assertTrue(player.isEyeInFluid(FluidTags.WATER), "waiting for the player to be under water");
			})
			.thenExecute(() -> {
				Map<String, float[]> rows = new LinkedHashMap<>();
				List<String> problems = new ArrayList<>();
				check(helper, rows, problems, "walk 10 blocks under water", measure(player, () -> player.checkMovementStatistics(10.0, 0.0, 0.0)),
					10 * FoodConstants.EXHAUSTION_SWIM, Exhaustion.ACTIVITY_FACTOR);
				SimpleBlockTestLog.info("FOOD-01 activities (exhaustion added; vanilla 26.3 value; ratio): {}", table(rows));
				helper.assertTrue(problems.isEmpty(), String.join("; ", problems));
				TestPlayers.remove(player);
			})
			.thenSucceed();
	}

	/** FOOD-02: attacking, taking damage, natural regeneration and the Hunger effect keep vanilla's exhaustion. */
	@GameTest
	public void combatRegenerationAndHungerEffectUnchanged(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		ServerPlayer player = TestPlayers.create(helper, GameType.SURVIVAL, new BlockPos(1, 1, 1));
		Zombie zombie = EntityTypes.ZOMBIE.create(level, EntitySpawnReason.EVENT);
		BlockPos zombiePos = helper.absolutePos(new BlockPos(2, 1, 1));
		zombie.snapTo(zombiePos.getX() + 0.5, zombiePos.getY(), zombiePos.getZ() + 0.5, 0.0F, 0.0F);
		zombie.setNoAi(true);
		zombie.setPersistenceRequired();
		level.addFreshEntity(zombie);
		helper.startSequence()
			.thenIdle(1)
			.thenExecute(() -> {
				Map<String, float[]> rows = new LinkedHashMap<>();
				List<String> problems = new ArrayList<>();

				float healthBefore = zombie.getHealth();
				check(helper, rows, problems, "attack a zombie", measure(player, () -> player.attack(zombie)), FoodConstants.EXHAUSTION_ATTACK, 1.0F);
				helper.assertTrue(zombie.getHealth() < healthBefore, "the attack should hit");

				DamageSource hit = level.damageSources().mobAttack(zombie);
				check(helper, rows, problems, "take a zombie hit", measure(player, () -> player.hurtServer(level, hit, 2.0F)), hit.getFoodExhaustion(), 1.0F);

				// Saturation-fuelled regeneration: full food, spends up to 6 saturation per 10 ticks.
				player.setHealth(10.0F);
				player.getFoodData().setFoodLevel(20);
				player.getFoodData().setSaturation(3.0F);
				check(helper, rows, problems, "regenerate from saturation (10 food ticks)", measure(player, () -> {
					for (int i = 0; i < 10; i++) {
						player.getFoodData().tick(player);
					}
				}), 3.0F, 1.0F);
				helper.assertValueEqual(player.getHealth(), 10.5F, "health after saturation regeneration");

				// Food-fuelled regeneration: 18+ food, no saturation, 1 health per 80 ticks.
				player.getFoodData().setFoodLevel(18);
				player.getFoodData().setSaturation(0.0F);
				check(helper, rows, problems, "regenerate from food (80 food ticks)", measure(player, () -> {
					for (int i = 0; i < 80; i++) {
						player.getFoodData().tick(player);
					}
				}), FoodConstants.EXHAUSTION_HEAL, 1.0F);
				helper.assertValueEqual(player.getHealth(), 11.5F, "health after food regeneration");

				check(helper, rows, problems, "Hunger effect, 20 ticks", measure(player, () -> {
					for (int i = 0; i < 20; i++) {
						MobEffects.HUNGER.value().applyEffectTick(level, player, 0);
					}
				}), 20 * 0.005F, 1.0F);

				SimpleBlockTestLog.info("FOOD-02 unchanged costs (exhaustion added; vanilla 26.3 value; ratio): {}", table(rows));
				helper.assertTrue(problems.isEmpty(), String.join("; ", problems));
				zombie.discard();
				TestPlayers.remove(player);
			})
			.thenSucceed();
	}
}
