package io.github.boxofdog.simpleblock.gametest;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.NaturalSpawner;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.PhantomSpawner;
import net.minecraft.world.phys.AABB;

import net.fabricmc.fabric.api.gametest.v1.GameTest;

/** SF-02 evidence. */
public class PhantomGameTests {
	/**
	 * PHA-01: a survival player who has not slept for days stands under open sky at midnight on hard
	 * difficulty. That is the worst case for vanilla's insomnia spawner, which tries roughly every
	 * 60-120 seconds and, under these conditions, spawns phantoms on about half of its tries. Simulate
	 * ~4 in-game days of spawner ticks.
	 */
	@GameTest(environment = "simple_block-gametest:phantom_night", skyAccess = true, maxTicks = 40)
	public void insomniaNeverSpawnsPhantoms(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		ServerPlayer player = TestPlayers.create(helper, GameType.SURVIVAL, new BlockPos(3, 1, 3));
		player.getStats().setValue(player, Stats.CUSTOM.get(Stats.TIME_SINCE_REST), 10 * 24000);
		BlockPos pos = player.blockPosition();

		// Make sure the vanilla spawn conditions really hold, so a pass means something.
		helper.assertTrue(level.getSkyDarken() >= 5, "sky should be dark enough for phantoms (skyDarken=" + level.getSkyDarken() + ")");
		helper.assertTrue(level.canSeeSky(pos), "player should be under open sky");
		helper.assertTrue(pos.getY() >= level.getSeaLevel(), "player should be at or above sea level");

		PhantomSpawner spawner = new PhantomSpawner();
		int ticks = 4 * 24000;
		for (int i = 0; i < ticks; i++) {
			spawner.tick(level, true);
		}

		int phantoms = level.getEntities(EntityTypes.PHANTOM, new AABB(pos).inflate(64), e -> true).size();
		SimpleBlockTestLog.info("PHA-01: {} insomnia spawner ticks at midnight on hard, player sleepless for 10 days: {} phantoms", ticks, phantoms);
		helper.assertValueEqual(phantoms, 0, "phantoms spawned after " + ticks + " insomnia spawner ticks");
		TestPlayers.remove(player);
		helper.succeed();
	}

	/**
	 * PHA-02: ordinary night-time hostile spawning is untouched. Runs vanilla's natural spawner for
	 * the MONSTER category around a survival player at midnight and expects monsters to appear.
	 */
	@GameTest(environment = "simple_block-gametest:monster_night", skyAccess = true, maxTicks = 40)
	public void otherHostilesStillSpawnAtNight(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		ServerPlayer player = TestPlayers.create(helper, GameType.SURVIVAL, new BlockPos(3, 1, 3));
		BlockPos center = player.blockPosition();
		RandomSource random = level.getRandom();

		for (int i = 0; i < 400; i++) {
			// Vanilla only spawns 24-128 blocks from the nearest player, on the ground.
			double angle = random.nextDouble() * Math.PI * 2;
			int distance = 32 + random.nextInt(32);
			int x = center.getX() + (int) (Math.cos(angle) * distance);
			int z = center.getZ() + (int) (Math.sin(angle) * distance);
			BlockPos start = new BlockPos(x, level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z), z);
			NaturalSpawner.spawnCategoryForPosition(MobCategory.MONSTER, level, start);
		}

		AABB area = new AABB(center).inflate(80);
		List<Monster> monsters = level.getEntitiesOfClass(Monster.class, area, monster -> true);
		SimpleBlockTestLog.info("PHA-02: natural spawner produced {} monsters: {} (skyDarken={})", monsters.size(),
				monsters.stream().map(m -> m.getType().getDescriptionId()).distinct().sorted().toList(), level.getSkyDarken());
		helper.assertTrue(!monsters.isEmpty(), "natural spawning should still produce hostile mobs at night");
		helper.assertTrue(monsters.stream().noneMatch(monster -> monster.getType() == EntityTypes.PHANTOM), "no phantoms");
		monsters.forEach(Entity::discard);
		TestPlayers.remove(player);
		helper.succeed();
	}

	/** Phantoms stay registered: they can still be summoned explicitly (commands/spawn eggs/testing). */
	@GameTest
	public void phantomEntityStillExists(GameTestHelper helper) {
		helper.spawn(EntityTypes.PHANTOM, new BlockPos(3, 2, 3));
		helper.assertEntityPresent(EntityTypes.PHANTOM);
		helper.killAllEntities();
		helper.succeed();
	}
}
