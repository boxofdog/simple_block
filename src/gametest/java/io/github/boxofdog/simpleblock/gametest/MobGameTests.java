package io.github.boxofdog.simpleblock.gametest;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BooleanSupplier;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.GameTestInfo;
import net.minecraft.gametest.framework.GameTestListener;
import net.minecraft.gametest.framework.GameTestRunner;
import net.minecraft.gametest.framework.GameTestSequence;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.WrappedGoal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import net.fabricmc.fabric.api.gametest.v1.GameTest;

import io.github.boxofdog.simpleblock.feature.CreeperEvaporation;
import io.github.boxofdog.simpleblock.feature.Entrancement;
import io.github.boxofdog.simpleblock.feature.LightLevels;
import io.github.boxofdog.simpleblock.feature.ZombieDoors;
import io.github.boxofdog.simpleblock.mixin.mobs.MobAccessor;
import io.github.boxofdog.simpleblock.tuning.Tuning;

/**
 * LIGHT-01, MOB-01..03. The mob experiments need more room than a test structure, so each runs in
 * an environment of its own (alone in its batch), builds a walled arena around the test position,
 * force-loads its chunks (the runner releases them after the batch) and removes its mobs and blocks
 * at the end. Positions below are relative to the arena centre.
 */
public class MobGameTests {
	private static final String ENTRANCEMENT = "mob.entrancement.enabled";

	/**
	 * A flat walled arena: floor at dy 0, open air above, a 3-high ring wall at the edge. Whatever
	 * the test ends with, pass or fail (a timeout included), the arena removes its mobs and players
	 * and undoes its tuning overrides.
	 */
	static final class Arena {
		final GameTestHelper helper;
		final ServerLevel level;
		final BlockPos center;
		final int radius;
		final List<Mob> mobs = new ArrayList<>();
		final List<ServerPlayer> players = new ArrayList<>();
		private final List<BooleanSupplier> lightChecks = new ArrayList<>();
		private BooleanSupplier openSky;
		/** Positions of the light sources placed with {@link #torch}. */
		final List<BlockPos> lights = new ArrayList<>();
		private final List<String> tuned = new ArrayList<>();

		/**
		 * {@code slot} is unique per test: the arena is built in a region of its own, 512 blocks north
		 * of the test grid and 256 blocks per slot along x, which no other test's structure or arena
		 * reaches. Arenas next to the grid overlapped the neighbouring test positions, and the runner
		 * unforces every forced chunk at the end of each batch; an arena built on chunks that were
		 * still unloading never got its light computed.
		 */
		Arena(GameTestHelper helper, int slot, int radius) {
			this.helper = helper;
			this.level = helper.getLevel();
			this.center = helper.absolutePos(new BlockPos(4, 0, 4)).offset(256 * slot, 0, -512);
			this.radius = radius;
			for (int cx = (this.center.getX() - radius - 16) >> 4; cx <= (this.center.getX() + radius + 16) >> 4; cx++) {
				for (int cz = (this.center.getZ() - radius - 16) >> 4; cz <= (this.center.getZ() + radius + 16) >> 4; cz++) {
					this.level.setChunkForced(cx, cz, true);
				}
			}
			for (int dx = -radius; dx <= radius; dx++) {
				for (int dz = -radius; dz <= radius; dz++) {
					this.set(dx, 0, dz, Blocks.SMOOTH_STONE);
					boolean edge = Math.abs(dx) == radius || Math.abs(dz) == radius;
					for (int dy = 1; dy <= HEIGHT; dy++) {
						this.set(dx, dy, dz, edge && dy <= 3 ? Blocks.STONE_BRICKS : Blocks.AIR);
					}
				}
			}
			BlockPos open = this.at(radius - 2, 1, radius - 2);
			this.openSky = () -> this.level.getBrightness(LightLayer.SKY, open) == 15;
			this.lightChecks.add(this.openSky);
			for (int cx = (this.center.getX() - radius) >> 4; cx <= (this.center.getX() + radius) >> 4; cx++) {
				for (int cz = (this.center.getZ() - radius) >> 4; cz <= (this.center.getZ() + radius) >> 4; cz++) {
					ChunkPos chunk = new ChunkPos(cx, cz);
					this.lightChecks.add(() -> this.level.areEntitiesActuallyLoadedAndTicking(chunk));
				}
			}
			onEnd(helper, this::cleanup);
		}

		private static final int HEIGHT = 12;

		BlockPos at(int dx, int dy, int dz) {
			return this.center.offset(dx, dy, dz);
		}

		void set(int dx, int dy, int dz, Block block) {
			this.level.setBlock(this.at(dx, dy, dz), block.defaultBlockState(), 2);
		}

		void set(int dx, int dy, int dz, BlockState state) {
			this.level.setBlock(this.at(dx, dy, dz), state, 2);
		}

		void torch(int dx, int dz) {
			this.set(dx, 1, dz, Blocks.TORCH);
			BlockPos pos = this.at(dx, 1, dz);
			this.lights.add(pos);
			// At least a torch's own light (12; vanilla 14): torches nearby give less, so it is this torch's.
			this.lightChecks.add(() -> this.level.getBrightness(LightLayer.BLOCK, pos) >= LightLevels.TORCH);
		}

		/** Closes the arena like a cave: the ring wall raised to the roof and a stone roof over it all, so no sky light gets in. */
		void enclose() {
			for (int dx = -this.radius; dx <= this.radius; dx++) {
				for (int dz = -this.radius; dz <= this.radius; dz++) {
					if (Math.abs(dx) == this.radius || Math.abs(dz) == this.radius) {
						this.set(dx, 4, dz, Blocks.STONE_BRICKS);
					}
					this.set(dx, 5, dz, Blocks.STONE);
				}
			}
			this.lightChecks.remove(this.openSky);
			BlockPos corner = this.at(this.radius - 2, 1, this.radius - 2);
			BlockPos middle = this.at(0, 1, 0);
			this.lightChecks.add(() -> this.level.getBrightness(LightLayer.SKY, corner) == 0 && this.level.getBrightness(LightLayer.SKY, middle) == 0);
		}

		/** Torches on the floor every 4 blocks over the given rectangle. */
		void torches(int x0, int x1, int z0, int z1) {
			for (int x = x0; x <= x1; x += 4) {
				for (int z = z0; z <= z1; z += 4) {
					this.torch(x, z);
				}
			}
		}

		/** A closed stone box (walls and roof) around the given floor rectangle, 4 high inside. */
		void box(int x0, int x1, int z0, int z1) {
			for (int x = x0 - 1; x <= x1 + 1; x++) {
				for (int z = z0 - 1; z <= z1 + 1; z++) {
					boolean wall = x == x0 - 1 || x == x1 + 1 || z == z0 - 1 || z == z1 + 1;
					for (int y = 1; y <= 4; y++) {
						if (wall) {
							this.set(x, y, z, Blocks.STONE);
						}
					}
					this.set(x, 5, z, Blocks.STONE);
				}
			}
			BlockPos inside = this.at((x0 + x1) / 2, 2, (z0 + z1) / 2);
			this.lightChecks.add(() -> this.level.getBrightness(LightLayer.SKY, inside) == 0);
		}

		/**
		 * Waits for the light engine and the entity sections. Game-test ticks run as fast as the
		 * server can go, while light updates run on a worker thread and a new chunk's entities load
		 * asynchronously: for the first ticks the old light is still in place (no torch light, sky
		 * light in boxes) and a mob added to the arena would not tick. Mobs are only spawned once
		 * the probes show the real light and every arena chunk ticks entities.
		 */
		GameTestSequence whenLit() {
			return this.helper.startSequence()
				.thenIdle(1)
				.thenWaitUntil(() -> {
					for (BooleanSupplier check : this.lightChecks) {
						if (!check.getAsBoolean()) {
							// Give the worker threads real time rather than spinning through the tick budget.
							pause();
							this.helper.assertTrue(false, "waiting for the light engine and entity loading");
						}
					}
				})
				.thenExecute(() -> SimpleBlockTestLog.info("arena lit after {} ticks (overworld clock {}, sky darkness {})", this.helper.getTick(),
					this.level.getOverworldClockTime(), this.level.getSkyDarken()));
		}

		<T extends Mob> T spawn(EntityType<T> type, int dx, int dz) {
			T mob = type.create(this.level, EntitySpawnReason.EVENT);
			BlockPos pos = this.at(dx, 1, dz);
			mob.snapTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 0.0F, 0.0F);
			mob.setPersistenceRequired();
			this.level.addFreshEntity(mob);
			this.mobs.add(mob);
			return mob;
		}

		ServerPlayer player(GameType gameType, int dx, int dz) {
			ServerPlayer player = TestPlayers.create(this.helper, gameType, new BlockPos(4, 1, 4));
			// By absolute position: relativePos/absolutePos don't round-trip for a rotated test structure.
			Vec3 pos = Vec3.atBottomCenterOf(this.at(dx, 1, dz));
			player.teleportTo(this.level, pos.x, pos.y, pos.z, Set.of(), 0.0F, 0.0F, true);
			this.players.add(player);
			return player;
		}

		void tune(String key, Object value) {
			Tuning.set(key, value);
			this.tuned.add(key);
		}

		void moveBack(Mob mob, int dx, int dz) {
			BlockPos start = this.at(dx, 1, dz);
			mob.getNavigation().stop();
			mob.teleportTo(start.getX() + 0.5, start.getY(), start.getZ() + 0.5);
		}

		boolean inBox(Mob mob, int[] zone) {
			double x = mob.getX() - (this.center.getX() + 0.5);
			double z = mob.getZ() - (this.center.getZ() + 0.5);
			return x >= zone[0] && x <= zone[1] + 1 && z >= zone[2] && z <= zone[3] + 1;
		}

		/** Blocks stay: nothing else uses the arena's region. */
		private void cleanup() {
			this.tuned.forEach(Tuning::reset);
			this.players.forEach(TestPlayers::remove);
			this.mobs.forEach(Mob::discard);
			this.level.getEntitiesOfClass(ItemEntity.class, new AABB(this.center).inflate(this.radius + 2)).forEach(ItemEntity::discard);
		}
	}

	private static void pause() {
		try {
			Thread.sleep(2);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}

	/** Runs {@code action} when the test passes or fails (including a timeout). */
	private static void onEnd(GameTestHelper helper, Runnable action) {
		GameTestInfo info;
		try {
			Field field = GameTestHelper.class.getDeclaredField("testInfo");
			field.setAccessible(true);
			info = (GameTestInfo) field.get(helper);
		} catch (ReflectiveOperationException e) {
			throw new IllegalStateException("GameTestHelper.testInfo not found", e);
		}
		info.addListener(new GameTestListener() {
			@Override
			public void testStructureLoaded(GameTestInfo testInfo) {
			}

			@Override
			public void testPassed(GameTestInfo testInfo, GameTestRunner runner) {
				action.run();
			}

			@Override
			public void testFailed(GameTestInfo testInfo, GameTestRunner runner) {
				action.run();
			}

			@Override
			public void testAddedForRerun(GameTestInfo original, GameTestInfo copy, GameTestRunner runner) {
			}
		});
	}

	/** The environment sets the clock, but the runner sometimes starts a batch on the previous one's time (see GameTestTiming). */
	private static void ensureClock(GameTestHelper helper, boolean night) {
		if (Entrancement.isNight(helper.getLevel()) != night) {
			SimpleBlockTestLog.info("test environment clock not in effect (overworld clock {}); setting {}", helper.getLevel().getOverworldClockTime(),
				night ? "midnight" : "noon");
			helper.setTime(night ? 18000L : 6000L);
		}
	}

	/**
	 * Counts one sample of every live mob by light: [0] on lit ground (block light above 0, the
	 * attraction's own definition), [1] on the mirror image of lit ground (x negated about the
	 * arena centre: the equally sized dark control area), [2] mob-samples taken.
	 */
	private static void sampleLight(Arena arena, double[] counts) {
		for (Mob mob : arena.mobs) {
			if (!mob.isAlive()) {
				continue;
			}
			counts[2]++;
			BlockPos pos = mob.blockPosition();
			BlockPos mirror = new BlockPos(2 * arena.center.getX() - pos.getX(), pos.getY(), pos.getZ());
			if (Entrancement.isLit(arena.level, pos)) {
				counts[0]++;
			} else if (Entrancement.isLit(arena.level, mirror)) {
				counts[1]++;
			}
		}
	}

	/**
	 * Counts, while the attraction is on, every live mob on lit ground ([1]) and those of them
	 * within 1.5 blocks (horizontally) of one of the arena's light sources ([0]).
	 */
	private static void sampleNearLights(Arena arena, double[] counts) {
		if (!Tuning.getBoolean(ENTRANCEMENT)) {
			return;
		}
		for (Mob mob : arena.mobs) {
			if (!mob.isAlive() || !Entrancement.isLit(arena.level, mob.blockPosition())) {
				continue;
			}
			counts[1]++;
			for (BlockPos light : arena.lights) {
				double dx = mob.getX() - (light.getX() + 0.5);
				double dz = mob.getZ() - (light.getZ() + 0.5);
				if (dx * dx + dz * dz < 1.5 * 1.5) {
					counts[0]++;
					break;
				}
			}
		}
	}

	/** Counts one sample of every live mob: [0] in zone A, [1] in zone B, [2] mob-samples taken. */
	private static void sample(Arena arena, double[] counts, int[] zoneA, int[] zoneB) {
		for (Mob mob : arena.mobs) {
			if (!mob.isAlive()) {
				continue;
			}
			counts[2]++;
			if (arena.inBox(mob, zoneA)) {
				counts[0]++;
			} else if (arena.inBox(mob, zoneB)) {
				counts[1]++;
			}
		}
	}

	private static String pct(double part, double total) {
		return String.format("%.0f%%", total == 0 ? 0 : 100.0 * part / total);
	}

	// ---------------------------------------------------------------- LIGHT-01

	/**
	 * LIGHT-01: torch 12 (wall and copper torches too), lantern 15, other light sources unchanged;
	 * block light from a torch stops monster spawning (checked in closed boxes, where sky light,
	 * which also blocks spawning, is 0: next to a torch never, in an identical dark box always).
	 */
	@GameTest(maxTicks = 6400)
	public void torchAndLanternLight(GameTestHelper helper) {
		Object[][] cases = {{Blocks.TORCH, 12}, {Blocks.WALL_TORCH, 12}, {Blocks.COPPER_TORCH, 12}, {Blocks.COPPER_WALL_TORCH, 12},
			{Blocks.LANTERN, 15}, {Blocks.SOUL_TORCH, 10}, {Blocks.SOUL_LANTERN, 10}, {Blocks.REDSTONE_TORCH, 7}, {Blocks.GLOWSTONE, 15}};
		List<String> problems = new ArrayList<>();
		for (Object[] row : cases) {
			int emission = ((Block) row[0]).defaultBlockState().getLightEmission();
			if (emission != (int) row[1]) {
				problems.add(row[0] + " emits " + emission + ", expected " + row[1]);
			}
		}
		Arena arena = new Arena(helper, 12, 14);
		ServerLevel level = arena.level;
		arena.box(-12, -8, -2, 2);
		arena.box(-12, -8, 6, 10);
		BlockPos torch = arena.at(-10, 1, 0);
		BlockPos besideTorch = arena.at(-9, 1, 1);
		BlockPos darkBox = arena.at(-10, 1, 8);
		BlockPos lantern = arena.at(10, 1, 0);
		arena.whenLit()
			.thenExecute(() -> {
				level.setBlockAndUpdate(torch, Blocks.TORCH.defaultBlockState());
				level.setBlockAndUpdate(lantern, Blocks.LANTERN.defaultBlockState());
			})
			// Light is computed on a worker thread (see Arena#whenLit); the two sources are walled apart.
			.thenWaitUntil(() -> helper.assertTrue(level.getBrightness(LightLayer.BLOCK, torch) > 0 && level.getBrightness(LightLayer.BLOCK, lantern) > 0,
				"waiting for the light engine"))
			.thenIdle(2)
			.thenExecute(() -> {
				int atTorch = level.getBrightness(LightLayer.BLOCK, torch);
				int nextToTorch = level.getBrightness(LightLayer.BLOCK, torch.above());
				int atLantern = level.getBrightness(LightLayer.BLOCK, lantern);
				int limit = level.dimensionType().monsterSpawnBlockLightLimit();
				int litSpawnable = 0;
				int darkSpawnable = 0;
				for (int i = 0; i < 50; i++) {
					litSpawnable += Monster.isDarkEnoughToSpawn(level, besideTorch, level.getRandom()) ? 1 : 0;
					darkSpawnable += Monster.isDarkEnoughToSpawn(level, darkBox, level.getRandom()) ? 1 : 0;
				}
				SimpleBlockTestLog.info("LIGHT-01: torch block light {} (block above {}), lantern {}; Overworld monster spawn block-light limit {}; "
					+ "monster spawn check passed {} of 50 times beside the torch (block light {}, sky light {}) and {} of 50 in an unlit closed box; "
					+ "emission problems {}", atTorch, nextToTorch, atLantern, limit, litSpawnable, level.getBrightness(LightLayer.BLOCK, besideTorch),
					level.getBrightness(LightLayer.SKY, besideTorch), darkSpawnable, problems);
				helper.assertTrue(problems.isEmpty(), String.join("; ", problems));
				helper.assertValueEqual(atTorch, 12, "block light at the torch");
				helper.assertValueEqual(nextToTorch, 11, "block light above the torch");
				helper.assertValueEqual(atLantern, 15, "block light at the lantern");
				helper.assertValueEqual(limit, 0, "Overworld monster spawn block-light limit");
				helper.assertValueEqual(litSpawnable, 0, "spawn checks passed in torch light");
				helper.assertValueEqual(darkSpawnable, 50, "spawn checks passed in the dark box");
			})
			.thenSucceed();
	}

	// ---------------------------------------------------------------- MOB-01

	/** Bands round the two closed boxes of the walled experiment (the boxes take their middle). */
	private static final int[] AROUND_LIT_BOX = {-29, -7, -11, 11};
	private static final int[] AROUND_DARK_BOX = {7, 29, -11, 11};
	/**
	 * Each phase of the open and walled experiments; presence is sampled after the first third.
	 * Every arena test's {@code maxTicks} has 6000 ticks on top for {@link Arena#whenLit}: entity
	 * loading in new chunks takes real time, and game-test ticks are only about a millisecond.
	 */
	private static final int PHASE = 900;
	private static final int SETTLE = 300;

	interface Sampler {
		void sample(Arena arena, double[] counts);
	}

	/**
	 * Two phases with the same zombies starting on the line midway between the lit and dark zones:
	 * attraction off (the control, vanilla wandering), then on. Fills {@code control} and
	 * {@code test} with presence samples taken every 10 ticks once a phase has run {@link #SETTLE}
	 * ticks, and checks the clock stays at night.
	 */
	private static GameTestSequence offThenOn(Arena arena, boolean night, int[] startZ, int phase, double[] control, double[] test, Sampler sampler) {
		GameTestHelper helper = arena.helper;
		long[] phaseStart = {0};
		return arena.whenLit()
			.thenExecute(() -> {
				ensureClock(helper, night);
				arena.tune(ENTRANCEMENT, false);
				for (int z : startZ) {
					arena.spawn(EntityTypes.ZOMBIE, 0, z);
				}
				phaseStart[0] = helper.getTick();
			})
			.thenExecuteFor(phase, () -> {
				helper.assertTrue(Entrancement.isNight(arena.level) == night, "the clock changed between day and night");
				if (helper.getTick() - phaseStart[0] >= SETTLE && helper.getTick() % 10 == 0) {
					sampler.sample(arena, control);
				}
			})
			.thenExecute(() -> {
				for (int i = 0; i < arena.mobs.size(); i++) {
					arena.moveBack(arena.mobs.get(i), 0, startZ[i]);
				}
				arena.tune(ENTRANCEMENT, true);
				phaseStart[0] = helper.getTick();
			})
			.thenExecuteFor(phase, () -> {
				helper.assertTrue(Entrancement.isNight(arena.level) == night, "the clock changed between day and night");
				if (helper.getTick() - phaseStart[0] >= SETTLE && helper.getTick() % 10 == 0) {
					sampler.sample(arena, test);
				}
			});
	}

	/**
	 * MOB-01 of the test plan, open ground at night: 24 zombies on the line midway between a
	 * torch-lit courtyard and the dark mirror image of its lit ground. With the attraction on,
	 * presence on lit ground must rise well above the dark area and above the control (vanilla
	 * wandering from the same line), and the zombies must spread over the lit ground rather than
	 * stack on one block (MOB-03).
	 */
	@GameTest(environment = "simple_block-gametest:mob_open_night", skyAccess = true, maxTicks = 9000)
	public void zombiesDriftToLitGroundAtNight(GameTestHelper helper) {
		Arena arena = new Arena(helper, 0, 34);
		arena.torches(-24, -12, -6, 6);
		arena.player(GameType.CREATIVE, 0, -20);
		double[] control = new double[3];
		double[] test = new double[3];
		// [0] zombie-samples on lit ground within 1.5 blocks of a torch, [1] zombie-samples on lit ground (both phases; reset between).
		double[] nearLights = new double[2];
		int[] startZ = new int[24];
		for (int i = 0; i < startZ.length; i++) {
			startZ[i] = -12 + i;
		}
		offThenOn(arena, true, startZ, 1200, control, test, (a, counts) -> {
			sampleLight(a, counts);
			sampleNearLights(a, nearLights);
		})
			.thenExecute(() -> {
				Set<Long> blocks = new HashSet<>();
				int onLit = 0;
				for (Mob mob : arena.mobs) {
					if (Entrancement.isLit(arena.level, mob.blockPosition())) {
						onLit++;
						blocks.add(mob.blockPosition().asLong());
					}
				}
				double litShare = test[0] / test[2];
				double controlLitShare = control[0] / control[2];
				SimpleBlockTestLog.info("MOB-01 open ground, 24 zombies, night: share of zombie-samples on lit ground / on the dark mirror area: "
					+ "control (attraction off) {} / {}, attraction on {} / {} ({} samples each); at the end {} zombies on lit ground on {} different blocks; "
					+ "of the samples on lit ground with the attraction on, {} were within 1.5 blocks of a torch (those blocks are about 10% of the lit ground)",
					pct(control[0], control[2]), pct(control[1], control[2]), pct(test[0], test[2]), pct(test[1], test[2]), (int) test[2], onLit, blocks.size(),
					pct(nearLights[0], nearLights[1]));
				helper.assertTrue(nearLights[0] < 0.25 * nearLights[1], "zombies should loiter over the lit area, not crowd the torches");
				double gap = litShare - test[1] / test[2];
				helper.assertTrue(litShare > 0.35, "lit-ground presence with attraction: " + pct(test[0], test[2]));
				helper.assertTrue(test[0] > 2.5 * test[1] && gap > 0.3, "lit ground should be far busier than the dark area");
				helper.assertTrue(gap > controlLitShare - control[1] / control[2] + 0.25, "attraction should raise lit-ground presence over the control");
				helper.assertTrue(blocks.size() >= Math.max(3, onLit * 2 / 3), "zombies should spread out, not stack on one spot");
			})
			.thenSucceed();
	}

	/**
	 * MOB-02 of the test plan, light behind walls: two identical closed stone boxes, one lit inside.
	 * The light can't be reached, but zombies gather round the lit box more than round the dark one
	 * and never get inside. More zombies and longer phases than the open test: light shut inside a
	 * box is found by sampling more slowly than light spilling over open ground, and the control
	 * (vanilla random walks of zombies starting side by side) is noisy.
	 */
	@GameTest(environment = "simple_block-gametest:mob_box_night", skyAccess = true, maxTicks = 12000)
	public void lightBehindWallsStillAttracts(GameTestHelper helper) {
		Arena arena = new Arena(helper, 1, 34);
		int[] litBox = {-24, -12, -6, 6};
		int[] darkBox = {12, 24, -6, 6};
		arena.box(litBox[0], litBox[1], litBox[2], litBox[3]);
		arena.box(darkBox[0], darkBox[1], darkBox[2], darkBox[3]);
		arena.torches(-22, -14, -4, 4);
		arena.player(GameType.CREATIVE, 0, -20);
		double[] control = new double[3];
		double[] test = new double[3];
		boolean[] inside = {false};
		int[] startZ = new int[32];
		for (int i = 0; i < startZ.length; i++) {
			startZ[i] = -16 + i;
		}
		offThenOn(arena, true, startZ, 2400, control, test, (a, counts) -> sample(a, counts, AROUND_LIT_BOX, AROUND_DARK_BOX))
			.thenExecute(() -> {
				for (Mob mob : arena.mobs) {
					inside[0] |= arena.inBox(mob, litBox) || arena.inBox(mob, darkBox);
				}
				SimpleBlockTestLog.info("MOB-01 walled: two closed boxes, one lit inside, 32 zombies; share of zombie-samples round the lit box / round the dark box: "
					+ "control {} / {}, attraction on {} / {} ({} samples); any zombie inside a box at the end: {}", pct(control[0], control[2]),
					pct(control[1], control[2]), pct(test[0], test[2]), pct(test[1], test[2]), (int) test[2], inside[0]);
				// Compared with the control rather than as a fixed ratio: the dark-box share is small and
				// noisy (5-17% in runs so far), which made a 2.5x ratio fail on runs that clearly attracted.
				double litOn = test[0] / test[2];
				double darkOn = test[1] / test[2];
				double litOff = control[0] / control[2];
				helper.assertTrue(!inside[0], "a zombie got inside a closed box");
				helper.assertTrue(litOn - darkOn > 0.1 && litOn - litOff > 0.1, "zombies should gather round the lit box more than the dark one, "
					+ "and more than without the attraction");
				double gap = litOn - darkOn;
				helper.assertTrue(gap > (control[0] - control[1]) / control[2] + 0.1, "attraction should raise presence round the lit box over the control");
			})
			.thenSucceed();
	}

	/** MOB-04 of the test plan: in daytime the attraction is off (husks, which don't burn). */
	@GameTest(environment = "simple_block-gametest:mob_open_day", skyAccess = true, maxTicks = 7400)
	public void noAttractionInDaytime(GameTestHelper helper) {
		Arena arena = new Arena(helper, 2, 34);
		arena.torches(-24, -12, -6, 6);
		arena.player(GameType.CREATIVE, 0, -20);
		double[] day = new double[3];
		long[] start = {0};
		long[] counters = new long[2];
		arena.whenLit()
			.thenExecute(() -> {
				ensureClock(helper, false);
				for (int i = 0; i < 16; i++) {
					arena.spawn(EntityTypes.HUSK, 0, -15 + 2 * i);
				}
				start[0] = helper.getTick();
				counters[0] = Entrancement.decisions;
				counters[1] = Entrancement.steered;
			})
			.thenExecuteFor(PHASE, () -> {
				helper.assertTrue(!Entrancement.isNight(arena.level), "the clock left the day");
				if (helper.getTick() - start[0] >= SETTLE && helper.getTick() % 10 == 0) {
					sampleLight(arena, day);
				}
			})
			.thenExecute(() -> {
				boolean active = Entrancement.isActiveAt(arena.level, arena.at(0, 1, 0));
				long decisions = Entrancement.decisions - counters[0];
				long steered = Entrancement.steered - counters[1];
				SimpleBlockTestLog.info("MOB-04 daytime on the surface, 16 husks: {} wander decisions, {} of them steered towards light; share of "
					+ "husk-samples on lit ground / on the dark mirror area {} / {} ({} samples, vanilla wandering); attraction active under the open "
					+ "sky at noon: {}", decisions, steered, pct(day[0], day[2]), pct(day[1], day[2]), (int) day[2], active);
				helper.assertTrue(!active, "the attraction should be off under the open sky in daytime");
				helper.assertTrue(decisions > 16, "the husks should have made wander decisions: " + decisions);
				helper.assertValueEqual(steered, 0L, "wander decisions steered towards light in daylight");
			})
			.thenSucceed();
	}

	/**
	 * Underground in daytime: the open-ground experiment at noon, in an arena roofed over like a
	 * cave so that no sky light reaches the floor. The attraction works there, by day, because the
	 * mobs get no sky light; the clock stays at noon throughout.
	 */
	@GameTest(environment = "simple_block-gametest:mob_cave_day", skyAccess = true, maxTicks = 9000)
	public void attractionWorksUndergroundInDaytime(GameTestHelper helper) {
		Arena arena = new Arena(helper, 13, 34);
		arena.enclose();
		long steeredBefore = Entrancement.steered;
		arena.torches(-24, -12, -6, 6);
		arena.player(GameType.CREATIVE, 0, -20);
		double[] control = new double[3];
		double[] test = new double[3];
		int[] startZ = new int[24];
		for (int i = 0; i < startZ.length; i++) {
			startZ[i] = -12 + i;
		}
		offThenOn(arena, false, startZ, 1200, control, test, MobGameTests::sampleLight)
			.thenExecute(() -> {
				boolean active = Entrancement.isActiveAt(arena.level, arena.at(0, 1, 0));
				double litShare = test[0] / test[2];
				double gap = litShare - test[1] / test[2];
				SimpleBlockTestLog.info("MOB-01 underground at noon (roofed arena, sky light 0), 24 zombies: share of zombie-samples on lit ground / on "
					+ "the dark mirror area: control (attraction off) {} / {}, attraction on {} / {} ({} samples each); overworld clock {}, attraction "
					+ "active under the roof: {}", pct(control[0], control[2]), pct(control[1], control[2]), pct(test[0], test[2]), pct(test[1], test[2]),
					(int) test[2], arena.level.getOverworldClockTime(), active);
				helper.assertTrue(active, "the attraction should be on where there is no sky light, by day");
				helper.assertTrue(Entrancement.steered > steeredBefore, "wander decisions should have been steered towards the light");
				helper.assertTrue(litShare > 0.35, "lit-ground presence with attraction: " + pct(test[0], test[2]));
				helper.assertTrue(test[0] > 2.5 * test[1] && gap > 0.3, "lit ground should be far busier than the dark area");
				helper.assertTrue(gap > control[0] / control[2] - control[1] / control[2] + 0.25, "attraction should raise lit-ground presence over the control");
			})
			.thenSucceed();
	}

	/**
	 * MOB-05 of the test plan: entranced zombies loitering on lit ground chase a survival player who
	 * turns up; once the player can't be targeted any more (switched to creative), they go back to
	 * the light. Four zombies, as one zombie's random walk is too noisy to measure.
	 */
	@GameTest(environment = "simple_block-gametest:mob_pursuit_night", skyAccess = true, maxTicks = 8400)
	public void pursuitOverridesThenAttractionResumes(GameTestHelper helper) {
		Arena arena = new Arena(helper, 3, 34);
		arena.torches(-24, -12, -6, 6);
		ServerPlayer player = arena.player(GameType.CREATIVE, 6, 0);
		List<Mob> chasers = new ArrayList<>();
		double[] before = new double[2];
		double[] afterwards = new double[2];
		double[] chase = new double[2];
		long[] ticks = {-1, -1};
		Runnable sampleBefore = () -> sampleOnLight(arena, before);
		arena.whenLit()
			.thenExecute(() -> {
				ensureClock(helper, true);
				for (int z = -3; z <= 3; z += 2) {
					arena.spawn(EntityTypes.ZOMBIE, -18, z);
				}
			})
			.thenExecuteFor(300, () -> {
				if (helper.getTick() % 10 == 0) {
					sampleBefore.run();
				}
			})
			.thenExecute(() -> {
				helper.assertTrue(arena.mobs.stream().allMatch(mob -> mob.getTarget() == null), "zombies should ignore a creative player");
				player.setGameMode(GameType.SURVIVAL);
			})
			// Three of the four is enough: one may have wandered out of the 35-block follow range.
			.thenWaitUntil(() -> helper.assertTrue(arena.mobs.stream().filter(mob -> mob.getTarget() == player).count() >= 3,
				"waiting for the zombies to notice the player"))
			.thenExecute(() -> {
				ticks[0] = helper.getTick();
				arena.mobs.stream().filter(mob -> mob.getTarget() == player).forEach(chasers::add);
				chase[0] = chasers.stream().mapToDouble(mob -> mob.distanceTo(player)).average().orElseThrow();
			})
			.thenIdle(60)
			.thenExecute(() -> {
				helper.assertTrue(chasers.stream().allMatch(mob -> mob.getTarget() == player), "zombies should still be chasing");
				chase[1] = chasers.stream().mapToDouble(mob -> mob.distanceTo(player)).average().orElseThrow();
				player.setGameMode(GameType.CREATIVE);
			})
			.thenWaitUntil(() -> helper.assertTrue(arena.mobs.stream().allMatch(mob -> mob.getTarget() == null), "waiting for the zombies to drop the target"))
			.thenExecute(() -> ticks[1] = helper.getTick())
			.thenExecuteFor(1200, () -> {
				if (helper.getTick() - ticks[1] >= SETTLE && helper.getTick() % 10 == 0) {
					sampleOnLight(arena, afterwards);
				}
			})
			.thenExecute(() -> {
				SimpleBlockTestLog.info("MOB-05: 4 entranced zombies spent {} of 300 ticks on lit ground; {} targeted the survival player by tick {} "
					+ "and closed from {} to {} blocks on average in 60 ticks; all dropped the target by tick {} when the player went creative; "
					+ "afterwards {} of samples (ticks {}-{} after) on lit ground", pct(before[0], before[1]), chasers.size(), ticks[0],
					String.format("%.1f", chase[0]), String.format("%.1f", chase[1]), ticks[1], pct(afterwards[0], afterwards[1]), SETTLE, 1200);
				helper.assertTrue(before[0] / before[1] > 0.5, "the zombies should loiter on lit ground before the chase");
				helper.assertTrue(chase[1] < chase[0] - 3, "the zombies should chase the player");
				helper.assertTrue(afterwards[0] / afterwards[1] > 0.5, "the zombies should return to the lit ground");
			})
			.thenSucceed();
	}

	/** Counts one sample of every live mob: [0] on lit ground, [1] mob-samples taken. */
	private static void sampleOnLight(Arena arena, double[] counts) {
		for (Mob mob : arena.mobs) {
			if (mob.isAlive()) {
				counts[1]++;
				counts[0] += Entrancement.isLit(arena.level, mob.blockPosition()) ? 1 : 0;
			}
		}
	}

	/**
	 * MOB-07 of the test plan: cost of the attraction with 40 zombies among torches. Two 400-tick
	 * windows, attraction off then on; game-test ticks run back to back, so a window's wall time is
	 * the server's work (pathfinding included). The bound is generous: this is evidence, not a
	 * benchmark.
	 */
	@GameTest(environment = "simple_block-gametest:mob_perf_night", skyAccess = true, maxTicks = 7400)
	public void attractionIsCheap(GameTestHelper helper) {
		Arena arena = new Arena(helper, 4, 34);
		arena.torches(-24, 24, -24, 24);
		arena.player(GameType.CREATIVE, 0, 0);
		long[] marks = new long[6];
		arena.whenLit()
			.thenExecute(() -> {
				ensureClock(helper, true);
				arena.tune(ENTRANCEMENT, false);
				for (int i = 0; i < 40; i++) {
					arena.spawn(EntityTypes.ZOMBIE, -20 + (i % 8) * 5, -20 + (i / 8) * 10);
				}
			})
			.thenIdle(50)
			.thenExecute(() -> marks[0] = System.nanoTime())
			.thenIdle(400)
			.thenExecute(() -> {
				marks[1] = System.nanoTime();
				arena.tune(ENTRANCEMENT, true);
				marks[2] = Entrancement.decisions;
				marks[3] = Entrancement.nanos;
			})
			.thenIdle(50)
			.thenExecute(() -> marks[4] = System.nanoTime())
			.thenIdle(400)
			.thenExecute(() -> {
				marks[5] = System.nanoTime();
				long decisions = Entrancement.decisions - marks[2];
				long nanos = Entrancement.nanos - marks[3];
				double offMs = (marks[1] - marks[0]) / 1e6;
				double onMs = (marks[5] - marks[4]) / 1e6;
				double perDecisionUs = decisions == 0 ? 0 : nanos / 1000.0 / decisions;
				SimpleBlockTestLog.info("MOB-07: 40 zombies among torches at night; 400 ticks took {} ms with the attraction off and {} ms with it on "
					+ "({} vs {} ms per tick); {} wander decisions with the attraction cost {} ms in total, {} us each", String.format("%.0f", offMs),
					String.format("%.0f", onMs), String.format("%.2f", offMs / 400), String.format("%.2f", onMs / 400), decisions,
					String.format("%.2f", nanos / 1e6), String.format("%.1f", perDecisionUs));
				helper.assertTrue(decisions > 40, "zombies should have made wander decisions: " + decisions);
				helper.assertTrue(perDecisionUs < 500, "attraction decisions should be cheap: " + perDecisionUs + " us");
				helper.assertTrue(onMs < 2 * offMs + 250, "ticks with the attraction should not be much slower");
			})
			.thenSucceed();
	}

	// ---------------------------------------------------------------- MOB-02

	/** A closed stone room on the +z side (x -4..4, z 0..6) whose north wall has a door at x = 0. */
	private static BlockPos doorway(Arena arena, BlockState door, boolean open) {
		for (int x = -4; x <= 4; x++) {
			for (int y = 1; y <= 3; y++) {
				arena.set(x, y, 0, Blocks.STONE);
				arena.set(x, y, 6, Blocks.STONE);
			}
		}
		for (int z = 0; z <= 6; z++) {
			for (int y = 1; y <= 3; y++) {
				arena.set(-4, y, z, Blocks.STONE);
				arena.set(4, y, z, Blocks.STONE);
			}
		}
		for (int x = -4; x <= 4; x++) {
			for (int z = 0; z <= 6; z++) {
				arena.set(x, 4, z, Blocks.STONE);
			}
		}
		BlockState lower = door.setValue(DoorBlock.FACING, Direction.NORTH).setValue(DoorBlock.OPEN, open).setValue(DoorBlock.HALF, DoubleBlockHalf.LOWER);
		arena.set(0, 1, 0, lower);
		arena.set(0, 2, 0, lower.setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER));
		return arena.at(0, 1, 0);
	}

	private static void setDoorOpen(Arena arena, BlockPos door, boolean open) {
		BlockState state = arena.level.getBlockState(door);
		arena.level.setBlock(door, state.setValue(DoorBlock.OPEN, open), 2);
		arena.level.setBlock(door.above(), arena.level.getBlockState(door.above()).setValue(DoorBlock.OPEN, open), 2);
	}

	private static boolean running(Mob mob, Class<?> goal) {
		for (WrappedGoal wrapped : ((MobAccessor) mob).simpleBlock$getGoalSelector().getAvailableGoals()) {
			if (wrapped.isRunning() && goal.isInstance(wrapped.getGoal())) {
				return true;
			}
		}
		return false;
	}

	private static Block block(String id) {
		return BuiltInRegistries.BLOCK.getValue(Identifier.withDefaultNamespace(id));
	}

	/**
	 * ZOMB-01 of the test plan, on Normal difficulty: a zombie chasing a player through an open
	 * doorway has the door shut in its face; it still breaks the door, at vanilla's Hard pace.
	 */
	private static void woodenDoorBreaks(GameTestHelper helper, int slot, Block doorBlock) {
		Arena arena = new Arena(helper, slot, 16);
		BlockPos door = doorway(arena, doorBlock.defaultBlockState(), true);
		ServerPlayer player = arena.player(GameType.SURVIVAL, 0, 4);
		Mob[] zombie = new Mob[1];
		long[] times = {-1, -1, -1};
		String name = BuiltInRegistries.BLOCK.getKey(doorBlock).getPath();
		arena.whenLit()
			.thenExecute(() -> {
				ensureClock(helper, true);
				zombie[0] = arena.spawn(EntityTypes.ZOMBIE, 0, -4);
			})
			.thenWaitUntil(() -> helper.assertTrue(zombie[0].getTarget() == player, "waiting for the zombie to see the player"))
			.thenExecute(() -> {
				setDoorOpen(arena, door, false);
				times[0] = helper.getTick();
			})
			.thenWaitUntil(() -> {
				if (times[1] < 0 && running(zombie[0], ZombieDoors.PursuitDoorBreakGoal.class)) {
					times[1] = helper.getTick();
				}
				helper.assertTrue(arena.level.getBlockState(door).isAir(), "waiting for the " + name + " to be broken");
			})
			.thenExecute(() -> {
				times[2] = helper.getTick();
				SimpleBlockTestLog.info("ZOMB-01 {} on {}: door shut in the chasing zombie's face at tick {}, zombie started breaking at tick {}, "
					+ "door broken at tick {} ({} ticks of breaking; vanilla Hard {})", name, arena.level.getDifficulty().getSerializedName(), times[0],
					times[1], times[2], times[2] - times[1], ZombieDoors.BREAK_TICKS);
				helper.assertTrue(times[1] >= 0 && Math.abs(times[2] - times[1] - ZombieDoors.BREAK_TICKS) <= 5, "breaking took " + (times[2] - times[1]) + " ticks");
			})
			.thenSucceed();
	}

	@GameTest(environment = "simple_block-gametest:door_wood_night", skyAccess = true, maxTicks = 6900)
	public void pursuingZombieBreaksOakDoor(GameTestHelper helper) {
		woodenDoorBreaks(helper, 5, Blocks.OAK_DOOR);
	}

	/** Nether-wood doors are in #minecraft:wooden_doors too. */
	@GameTest(environment = "simple_block-gametest:door_crimson_night", skyAccess = true, maxTicks = 6900)
	public void pursuingZombieBreaksCrimsonDoor(GameTestHelper helper) {
		woodenDoorBreaks(helper, 6, Blocks.CRIMSON_DOOR);
	}

	/**
	 * ZOMB-02 of the test plan: a pursuing zombie at a closed iron door, then a copper door (which
	 * opens by hand but is metal), attacks it but the door and everything round it stay exactly as
	 * they were.
	 */
	@GameTest(environment = "simple_block-gametest:door_metal_night", skyAccess = true, maxTicks = 7200)
	public void metalDoorsAreAttackedButNotDamaged(GameTestHelper helper) {
		Arena arena = new Arena(helper, 7, 16);
		BlockPos door = doorway(arena, Blocks.IRON_DOOR.defaultBlockState(), false);
		ServerPlayer player = arena.player(GameType.SURVIVAL, 0, 3);
		Mob[] zombie = new Mob[1];
		int[] attackTicks = {0, 0};
		List<BlockState> before = new ArrayList<>();
		Runnable snapshot = () -> {
			before.clear();
			for (BlockPos pos : BlockPos.betweenClosed(door.offset(-2, -1, -2), door.offset(2, 3, 2))) {
				before.add(arena.level.getBlockState(pos));
			}
		};
		Runnable unchanged = () -> {
			int i = 0;
			for (BlockPos pos : BlockPos.betweenClosed(door.offset(-2, -1, -2), door.offset(2, 3, 2))) {
				helper.assertTrue(arena.level.getBlockState(pos).equals(before.get(i++)), "block changed at " + pos);
			}
		};
		arena.whenLit()
			.thenExecute(() -> {
				ensureClock(helper, true);
				zombie[0] = arena.spawn(EntityTypes.ZOMBIE, 0, -3);
				// The zombie knows where the player is (it saw them go in a moment ago).
				zombie[0].setTarget(player);
				snapshot.run();
			})
			.thenExecuteFor(300, () -> attackTicks[0] += running(zombie[0], ZombieDoors.MetalDoorAttackGoal.class) ? 1 : 0)
			.thenExecute(() -> {
				unchanged.run();
				BlockState copper = block("copper_door").defaultBlockState().setValue(DoorBlock.FACING, Direction.NORTH);
				arena.set(0, 1, 0, copper.setValue(DoorBlock.HALF, DoubleBlockHalf.LOWER));
				arena.set(0, 2, 0, copper.setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER));
				zombie[0].setTarget(player);
				snapshot.run();
			})
			.thenExecuteFor(300, () -> attackTicks[1] += running(zombie[0], ZombieDoors.MetalDoorAttackGoal.class) ? 1 : 0)
			.thenExecute(() -> {
				unchanged.run();
				SimpleBlockTestLog.info("ZOMB-02 metal doors: pursuing zombie spent {} ticks attacking the iron door and {} ticks the copper door "
					+ "(of 300 each); both doors and the blocks round them unchanged", attackTicks[0], attackTicks[1]);
				helper.assertTrue(attackTicks[0] > 20, "zombie should attack the iron door");
				helper.assertTrue(attackTicks[1] > 20, "zombie should attack the copper door");
			})
			.thenSucceed();
	}

	/**
	 * ZOMB-03 of the test plan: zombies that are not chasing anyone are drawn to a lit room behind a
	 * closed oak door at night but never break it.
	 */
	@GameTest(environment = "simple_block-gametest:door_unaware_night", skyAccess = true, maxTicks = 7200)
	public void unawareZombiesLeaveDoorsAlone(GameTestHelper helper) {
		Arena arena = new Arena(helper, 8, 16);
		BlockPos door = doorway(arena, Blocks.OAK_DOOR.defaultBlockState(), false);
		arena.torch(-2, 3);
		arena.torch(2, 3);
		arena.player(GameType.CREATIVE, -10, -10);
		double[] nearDoor = new double[2];
		int[] breaking = {0};
		arena.whenLit()
			.thenExecute(() -> {
				ensureClock(helper, true);
				for (int i = 0; i < 4; i++) {
					arena.spawn(EntityTypes.ZOMBIE, -6 + 4 * i, -8);
				}
			})
			.thenExecuteFor(900, () -> {
				helper.assertTrue(arena.level.getBlockState(door).is(Blocks.OAK_DOOR), "the oak door was broken");
				for (Mob mob : arena.mobs) {
					breaking[0] += running(mob, ZombieDoors.PursuitDoorBreakGoal.class) ? 1 : 0;
				}
				if (helper.getTick() % 10 == 0) {
					for (Mob mob : arena.mobs) {
						nearDoor[1]++;
						nearDoor[0] += mob.position().closerThan(Vec3.atCenterOf(door), 4.0) ? 1 : 0;
					}
				}
			})
			.thenExecute(() -> {
				SimpleBlockTestLog.info("ZOMB-03 unaware: 4 zombies outside a closed oak door with a torch-lit room behind it, 900 ticks at night: "
					+ "door intact, door-breaking goal ran {} ticks, {} of zombie-samples within 4 blocks of the door", breaking[0], pct(nearDoor[0], nearDoor[1]));
				helper.assertValueEqual(breaking[0], 0, "ticks of door breaking");
			})
			.thenSucceed();
	}

	// ---------------------------------------------------------------- MOB-03

	/** CREEP-01: a creeper in direct sun smokes for the effect's duration, then is gone: no blast, no drops. */
	@GameTest(environment = "simple_block-gametest:creeper_sun", skyAccess = true, maxTicks = 6700)
	public void creeperDissolvesInSunlight(GameTestHelper helper) {
		Arena arena = new Arena(helper, 9, 8);
		Creeper[] creeper = new Creeper[1];
		long[] times = {-1, -1};
		arena.whenLit()
			.thenExecute(() -> {
				ensureClock(helper, false);
				creeper[0] = arena.spawn(EntityTypes.CREEPER, 0, 0);
			})
			.thenWaitUntil(() -> {
				if (times[0] < 0 && CreeperEvaporation.isEvaporating(creeper[0])) {
					times[0] = helper.getTick();
				}
				helper.assertTrue(creeper[0].isRemoved(), "waiting for the creeper to dissolve");
			})
			.thenExecute(() -> {
				times[1] = helper.getTick();
				int drops = arena.level.getEntitiesOfClass(ItemEntity.class, new AABB(arena.center).inflate(10)).size();
				boolean floorIntact = true;
				for (int dx = -3; dx <= 3; dx++) {
					for (int dz = -3; dz <= 3; dz++) {
						floorIntact &= arena.level.getBlockState(arena.at(dx, 0, dz)).is(Blocks.SMOOTH_STONE);
					}
				}
				SimpleBlockTestLog.info("CREEP-01 sun: creeper started smoking at tick {}, gone at tick {} ({} ticks of effect, {} s); drops {}, floor intact {}, "
					+ "removal reason {}, swell {}", times[0], times[1], times[1] - times[0], (times[1] - times[0]) / 20.0, drops, floorIntact,
					creeper[0].getRemovalReason(), creeper[0].getSwellDir());
				helper.assertTrue(times[0] >= 0 && times[1] - times[0] >= CreeperEvaporation.duration() - 1, "the effect should play before removal");
				helper.assertValueEqual(drops, 0, "items dropped");
				helper.assertTrue(floorIntact, "no explosion damage");
			})
			.thenSucceed();
	}

	/** CREEP-02: at noon, under a roof (fenced in so it can't wander out) and in a torch-lit closed room, creepers stay. */
	@GameTest(environment = "simple_block-gametest:creeper_shade", skyAccess = true, maxTicks = 6900)
	public void creepersInShadeOrTorchlightStay(GameTestHelper helper) {
		Arena arena = new Arena(helper, 10, 12);
		for (int dx = -2; dx <= 2; dx++) {
			for (int dz = -2; dz <= 2; dz++) {
				arena.set(dx - 5, 3, dz, Blocks.STONE);
				if (Math.abs(dx) == 2 || Math.abs(dz) == 2) {
					arena.set(dx - 5, 1, dz, Blocks.OAK_FENCE);
				}
			}
		}
		BlockPos underRoof = arena.at(-5, 1, 0);
		arena.box(4, 7, -2, 2);
		arena.torch(5, -1);
		Creeper[] creepers = new Creeper[2];
		boolean[] evaporated = {false};
		arena.whenLit()
			.thenExecute(() -> {
				ensureClock(helper, false);
				helper.assertTrue(!arena.level.canSeeSky(underRoof), "the roof should shade the pen");
				creepers[0] = arena.spawn(EntityTypes.CREEPER, -5, 0);
				creepers[1] = arena.spawn(EntityTypes.CREEPER, 6, 1);
			})
			.thenExecuteFor(600, () -> evaporated[0] |= CreeperEvaporation.isEvaporating(creepers[0]) || CreeperEvaporation.isEvaporating(creepers[1]))
			.thenExecute(() -> {
				SimpleBlockTestLog.info("CREEP-02 shade: 600 ticks at noon, creeper under a roof still there {}, creeper in a torch-lit closed room still there {}, "
					+ "any smoking {}", !creepers[0].isRemoved(), !creepers[1].isRemoved(), evaporated[0]);
				helper.assertTrue(!evaporated[0] && !creepers[0].isRemoved() && !creepers[1].isRemoved(), "shaded creepers should stay");
			})
			.thenSucceed();
	}

	/** CREEP-02: at night in the open, creepers stay. */
	@GameTest(environment = "simple_block-gametest:creeper_night", skyAccess = true, maxTicks = 6900)
	public void creepersAtNightStay(GameTestHelper helper) {
		Arena arena = new Arena(helper, 11, 8);
		Creeper[] creeper = new Creeper[1];
		boolean[] evaporated = {false};
		arena.whenLit()
			.thenExecute(() -> {
				ensureClock(helper, true);
				creeper[0] = arena.spawn(EntityTypes.CREEPER, 0, 0);
			})
			.thenExecuteFor(600, () -> evaporated[0] |= CreeperEvaporation.isEvaporating(creeper[0]))
			.thenExecute(() -> {
				SimpleBlockTestLog.info("CREEP-02 night: 600 ticks in the open at midnight, creeper still there {}, smoking {}", !creeper[0].isRemoved(), evaporated[0]);
				helper.assertTrue(!evaporated[0] && !creeper[0].isRemoved(), "creeper at night should stay");
			})
			.thenSucceed();
	}
}
