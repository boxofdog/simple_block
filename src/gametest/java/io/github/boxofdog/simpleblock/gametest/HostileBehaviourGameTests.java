package io.github.boxofdog.simpleblock.gametest;

import static io.github.boxofdog.simpleblock.gametest.MobGameTests.doorway;
import static io.github.boxofdog.simpleblock.gametest.MobGameTests.ensureClock;
import static io.github.boxofdog.simpleblock.gametest.MobGameTests.pct;
import static io.github.boxofdog.simpleblock.gametest.MobGameTests.running;

import java.util.Set;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.WrappedGoal;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.skeleton.Skeleton;
import net.minecraft.world.entity.monster.spider.Spider;
import net.minecraft.world.entity.projectile.arrow.Arrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.Vec3;

import net.fabricmc.fabric.api.gametest.v1.GameTest;

import io.github.boxofdog.simpleblock.feature.CreeperRetaliation;
import io.github.boxofdog.simpleblock.feature.Entrancement;
import io.github.boxofdog.simpleblock.feature.LightRoutes;
import io.github.boxofdog.simpleblock.feature.SpiderJockeys;
import io.github.boxofdog.simpleblock.feature.ZombieDoors;
import io.github.boxofdog.simpleblock.gametest.MobGameTests.Arena;
import io.github.boxofdog.simpleblock.mixin.mobs.MobAccessor;
import io.github.boxofdog.simpleblock.tuning.Tuning;

/**
 * M09: light interest and doors (HOST-01..07), creeper retaliation (CRET-01..04) and skeleton-spider
 * jockeys (JOCK-01..07). Built on {@link MobGameTests}' arenas: each test has an environment of its
 * own (so it runs alone in its batch) and an arena slot of its own.
 */
public class HostileBehaviourGameTests {
	// ---------------------------------------------------------------- light interest and doors

	/**
	 * HOST-01: a light-drawn mob that is not chasing anyone, outside a closed oak door with a lit room
	 * behind it, breaks the door at the M02 pace and then goes in. A skeleton (not of the zombie
	 * family) and a zombie (whose M02 rule only broke doors in pursuit).
	 */
	private static void lightDrawnMobBreaksDoor(GameTestHelper helper, int slot, EntityType<? extends Mob> type) {
		Arena arena = new Arena(helper, slot, 16);
		BlockPos door = doorway(arena, Blocks.OAK_DOOR.defaultBlockState(), false);
		arena.torch(-2, 3);
		arena.torch(2, 3);
		// Near the middle: a mob more than 32 blocks from every player stops wandering (vanilla).
		arena.player(GameType.CREATIVE, -4, -6);
		Mob[] mob = new Mob[1];
		long[] times = {-1, -1, -1, -1};
		long breaches = Entrancement.breaches;
		String name = BuiltInRegistries.ENTITY_TYPE.getKey(type).getPath();
		arena.whenLit()
			.thenExecute(() -> {
				ensureClock(helper, true);
				mob[0] = arena.spawn(type, 0, -3);
				times[0] = helper.getTick();
			})
			.thenWaitUntil(() -> {
				if (helper.getTick() % 2000 == 0) {
					SimpleBlockTestLog.info("HOST-01 {} at tick {}: at {}, interested {}, door route {}, can open doors {}, navigating {}, routes so far: reachable {}, "
						+ "open door {}, wooden door {}, loiter {}", name, helper.getTick(), mob[0].blockPosition().subtract(arena.center),
						Entrancement.isInterested((PathfinderMob) mob[0]), LightRoutes.doorRouteActive(mob[0]), mob[0].getNavigation().getNodeEvaluator().canOpenDoors(),
						!mob[0].getNavigation().isDone(), LightRoutes.reachable, LightRoutes.openDoors, LightRoutes.doorRoutes, LightRoutes.loiters);
				}
				helper.assertTrue(mob[0].getTarget() == null, "the " + name + " should not have a target");
				helper.assertTrue(!running(mob[0], ZombieDoors.PursuitDoorBreakGoal.class), "not a pursuit");
				if (times[1] < 0 && running(mob[0], LightRoutes.LightDoorBreakGoal.class)) {
					times[1] = helper.getTick();
				}
				helper.assertTrue(arena.level.getBlockState(door).isAir(), "waiting for the " + name + " to break the door");
			})
			.thenExecute(() -> times[2] = helper.getTick())
			.thenWaitUntil(() -> helper.assertTrue(arena.inBox(mob[0], new int[] {-3, 3, 1, 5}), "waiting for the " + name + " to go into the lit room"))
			.thenExecute(() -> {
				times[3] = helper.getTick();
				SimpleBlockTestLog.info("HOST-01 {}: light-drawn, no target, outside a closed oak door to a lit room; spawned tick {}, started breaking tick {}, "
					+ "door gone tick {} ({} ticks of breaking; M02 pace {}), inside the room tick {}; breaches signalled {}", name, times[0], times[1],
					times[2], times[2] - times[1], ZombieDoors.BREAK_TICKS, times[3], Entrancement.breaches - breaches);
				helper.assertTrue(times[1] >= 0 && Math.abs(times[2] - times[1] - ZombieDoors.BREAK_TICKS) <= 5, "breaking took " + (times[2] - times[1]));
				helper.assertTrue(Entrancement.breaches > breaches, "the broken door should make mobs around re-check their routes");
			})
			.thenSucceed();
	}

	@GameTest(environment = "simple_block-gametest:m09_skeleton_door_night", skyAccess = true, maxTicks = 24000)
	public void lightDrawnSkeletonBreaksWoodenDoor(GameTestHelper helper) {
		lightDrawnMobBreaksDoor(helper, 20, EntityTypes.SKELETON);
	}

	@GameTest(environment = "simple_block-gametest:m09_zombie_door_night", skyAccess = true, maxTicks = 24000)
	public void lightDrawnZombieBreaksWoodenDoor(GameTestHelper helper) {
		lightDrawnMobBreaksDoor(helper, 21, EntityTypes.ZOMBIE);
	}

	/**
	 * HOST-02, route choice from where the mob stands (a skeleton beside a lit building's north wall):
	 * <ul>
	 *   <li>the only door is a closed oak door on the far side: no door route, it loiters (no
	 *       omniscient door finding);</li>
	 *   <li>a closed iron door right in front of it: no door route (only wood is broken);</li>
	 *   <li>a closed oak door right in front of it: door route;</li>
	 *   <li>that door open, with the lit part of the building walled off behind a vestibule: it goes
	 *       to the open door.</li>
	 * </ul>
	 */
	@GameTest(environment = "simple_block-gametest:m09_route_night", skyAccess = true, maxTicks = 7000)
	public void doorRoutesArePhysical(GameTestHelper helper) {
		Arena arena = new Arena(helper, 22, 20);
		// Building x -4..4, z 0..14 (walls), roof at y 4.
		for (int x = -4; x <= 4; x++) {
			for (int z = 0; z <= 14; z++) {
				boolean wall = x == -4 || x == 4 || z == 0 || z == 14;
				for (int y = 1; y <= 3; y++) {
					arena.set(x, y, z, wall ? Blocks.STONE.defaultBlockState() : Blocks.AIR.defaultBlockState());
				}
				arena.set(x, 4, z, Blocks.STONE);
			}
		}
		setDoor(arena, 0, 14, Blocks.OAK_DOOR.defaultBlockState(), false);
		arena.torch(0, 3);
		arena.player(GameType.CREATIVE, -15, -15);
		Mob[] skeleton = new Mob[1];
		arena.whenLit()
			.thenExecute(() -> {
				ensureClock(helper, true);
				skeleton[0] = arena.spawn(EntityTypes.SKELETON, 0, -2);
			})
			.thenIdle(10)
			.thenExecute(() -> {
				Skeleton mob = (Skeleton) skeleton[0];
				BlockPos ground = arena.at(0, 1, 3);
				arena.moveBack(mob, 0, -2);
				LightRoutes.clear(mob);
				Vec3 far = LightRoutes.route(mob, arena.level, ground);
				boolean farRoute = LightRoutes.doorRouteActive(mob);

				setDoor(arena, 0, 0, Blocks.IRON_DOOR.defaultBlockState(), false);
				LightRoutes.clear(mob);
				LightRoutes.route(mob, arena.level, ground);
				boolean ironRoute = LightRoutes.doorRouteActive(mob);

				setDoor(arena, 0, 0, Blocks.OAK_DOOR.defaultBlockState(), false);
				LightRoutes.clear(mob);
				LightRoutes.route(mob, arena.level, ground);
				boolean oakRoute = LightRoutes.doorRouteActive(mob);

				setDoor(arena, 0, 0, Blocks.OAK_DOOR.defaultBlockState(), true);
				for (int x = -3; x <= 3; x++) {
					for (int y = 1; y <= 3; y++) {
						arena.set(x, y, 2, Blocks.STONE);
					}
				}
				LightRoutes.clear(mob);
				mob.getNavigation().setCanOpenDoors(false);
				net.minecraft.world.level.pathfinder.Path plain = io.github.boxofdog.simpleblock.feature.PathProbe.probe(mob, ground, 0);
				net.minecraft.world.level.pathfinder.Path toDoor = io.github.boxofdog.simpleblock.feature.PathProbe.probe(mob, arena.at(0, 1, 0), 0);
				SimpleBlockTestLog.info("HOST-02 vestibule probes: skeleton at {}; path to the walled-off lit spot reaches {} and ends at {}; path to the open "
					+ "door reaches {}", mob.blockPosition().subtract(arena.center), plain != null && plain.canReach(),
					plain == null || plain.getEndNode() == null ? null : plain.getEndNode().asBlockPos().subtract(arena.center), toDoor != null && toDoor.canReach());
				Vec3 vestibule = LightRoutes.route(mob, arena.level, ground);
				boolean vestibuleRoute = LightRoutes.doorRouteActive(mob);
				Vec3 openDoor = Vec3.atBottomCenterOf(arena.at(0, 1, 0));
				SimpleBlockTestLog.info("HOST-02 routes from beside the north wall: far-side oak door only -> door route {}, destination {} (the lit spot); "
					+ "iron door in front -> door route {}; oak door in front -> door route {}; that door open with the lit part walled off -> destination "
					+ "the open door {}, door route {}", farRoute, far, ironRoute, oakRoute, vestibule != null && vestibule.closerThan(openDoor, 0.01), vestibuleRoute);
				helper.assertTrue(!farRoute, "a door on the far side must not be sought out");
				helper.assertTrue(far != null && far.closerThan(Vec3.atBottomCenterOf(ground), 0.01), "it should keep heading for the light (loitering at the wall)");
				helper.assertTrue(!ironRoute, "an iron door is no way in for light-drawn mobs");
				helper.assertTrue(oakRoute, "a closed oak door right in the way should be taken");
				helper.assertTrue(vestibule != null && vestibule.closerThan(openDoor, 0.01) && !vestibuleRoute, "it should go to the open door nearby");
			})
			.thenSucceed();
	}

	/** A door in a wall, lower half at (dx, 1, dz), facing north. */
	private static void setDoor(Arena arena, int dx, int dz, BlockState door, boolean open) {
		BlockState lower = door.setValue(DoorBlock.FACING, Direction.NORTH).setValue(DoorBlock.OPEN, open).setValue(DoorBlock.HALF, DoubleBlockHalf.LOWER);
		arena.set(dx, 1, dz, lower);
		arena.set(dx, 2, dz, lower.setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER));
	}

	/**
	 * HOST-03: two identical lit rooms, one with its oak door left open, the other shut by an iron
	 * door (which light-drawn mobs never break). Light-drawn zombies and skeletons get into the room
	 * through the open door; nobody gets into the shut one. The open door is not broken.
	 */
	@GameTest(environment = "simple_block-gametest:m09_open_door_night", skyAccess = true, maxTicks = 8000)
	public void openDoorDrawsThemIn(GameTestHelper helper) {
		Arena arena = new Arena(helper, 23, 20);
		BlockPos openDoor = room(arena, -8, Blocks.OAK_DOOR.defaultBlockState(), true);
		room(arena, 8, Blocks.IRON_DOOR.defaultBlockState(), false);
		arena.player(GameType.CREATIVE, 0, -14);
		// [0] samples inside the open room, [1] inside the shut room, [2] on lit ground (anywhere), [3] all.
		double[] counts = new double[4];
		int[] openInside = {-11, -5, 1, 5};
		int[] shutInside = {5, 11, 1, 5};
		java.util.Set<Mob> wentIn = new java.util.HashSet<>();
		arena.whenLit()
			.thenExecute(() -> {
				ensureClock(helper, true);
				for (int i = 0; i < 10; i++) {
					if (i % 2 == 0) {
						arena.spawn(EntityTypes.ZOMBIE, -12 + 3 * i, -9 - i % 3);
					} else {
						arena.spawn(EntityTypes.SKELETON, -12 + 3 * i, -9 - i % 3);
					}
				}
			})
			.thenExecuteFor(3600, () -> {
				helper.assertTrue(arena.level.getBlockState(openDoor).is(Blocks.OAK_DOOR), "the open door was broken");
				for (Mob mob : arena.mobs) {
					if (arena.inBox(mob, openInside)) {
						wentIn.add(mob);
					}
					helper.assertTrue(!arena.inBox(mob, shutInside), "a mob got into the shut room");
				}
				if (helper.getTick() % 10 == 0) {
					for (Mob mob : arena.mobs) {
						counts[3]++;
						counts[0] += arena.inBox(mob, openInside) ? 1 : 0;
						counts[2] += Entrancement.isLit(arena.level, mob.blockPosition()) ? 1 : 0;
					}
				}
			})
			.thenExecute(() -> {
				SimpleBlockTestLog.info("HOST-03 open door: 5 zombies and 5 skeletons between two lit rooms, one with its oak door open, one shut by an iron "
					+ "door, 3600 ticks at night: {} of the 10 went into the open room ({} of mob-samples inside it, {} on lit ground in all); none in the "
					+ "shut room; route decisions so far: reachable {}, to an open door {}, through a wooden door {}, loiter {}", wentIn.size(),
					pct(counts[0], counts[3]), pct(counts[2], counts[3]), LightRoutes.reachable, LightRoutes.openDoors, LightRoutes.doorRoutes,
					LightRoutes.loiters);
				helper.assertTrue(wentIn.size() >= 2, "mobs should go in through the open door");
			})
			.thenSucceed();
	}

	/** A lit room like {@link MobGameTests#doorway} centred on x {@code cx}: walls x cx-4..cx+4, z 0..6, the door at (cx, 0), torches inside. */
	private static BlockPos room(Arena arena, int cx, BlockState door, boolean open) {
		for (int x = cx - 4; x <= cx + 4; x++) {
			for (int z = 0; z <= 6; z++) {
				boolean wall = x == cx - 4 || x == cx + 4 || z == 0 || z == 6;
				for (int y = 1; y <= 3; y++) {
					if (wall) {
						arena.set(x, y, z, Blocks.STONE);
					}
				}
				arena.set(x, 4, z, Blocks.STONE);
			}
		}
		setDoor(arena, cx, 0, door, open);
		arena.torch(cx - 2, 3);
		arena.torch(cx + 2, 3);
		return arena.at(cx, 1, 0);
	}

	/**
	 * HOST-04: zombies loitering on lit ground lose all interest once the lights go out: their memory
	 * of lit spots empties at their next wander decisions and no decision is steered any more.
	 */
	@GameTest(environment = "simple_block-gametest:m09_dark_night", skyAccess = true, maxTicks = 9000)
	public void extinguishedAreaStopsAttracting(GameTestHelper helper) {
		Arena arena = new Arena(helper, 24, 34);
		arena.torches(-24, -12, -6, 6);
		// Within 32 blocks of every zombie: farther from all players, vanilla mobs stop wandering.
		arena.player(GameType.CREATIVE, -12, -16);
		long[] marks = new long[3];
		int[] interested = new int[2];
		arena.whenLit()
			.thenExecute(() -> {
				ensureClock(helper, true);
				for (int i = 0; i < 6; i++) {
					arena.spawn(EntityTypes.ZOMBIE, -10, -5 + 2 * i);
				}
			})
			.thenIdle(900)
			.thenExecute(() -> {
				interested[0] = (int) arena.mobs.stream().filter(mob -> Entrancement.isInterested((PathfinderMob) mob)).count();
				for (BlockPos light : arena.lights) {
					arena.level.setBlock(light, Blocks.AIR.defaultBlockState(), 2);
				}
			})
			.thenWaitUntil(() -> helper.assertTrue(arena.lights.stream().allMatch(pos -> !Entrancement.isLit(arena.level, pos)), "waiting for the light to go"))
			.thenIdle(900)
			.thenExecute(() -> {
				marks[0] = Entrancement.steered;
				marks[1] = Entrancement.decisions;
			})
			.thenIdle(900)
			.thenExecute(() -> {
				interested[1] = (int) arena.mobs.stream().filter(mob -> Entrancement.isInterested((PathfinderMob) mob)).count();
				SimpleBlockTestLog.info("HOST-04 lights out: 6 zombies, {} remembered lit ground after 900 ticks with the torches; torches removed; "
					+ "900 ticks later {} still remembered any; over the next 900 ticks {} wander decisions, {} steered", interested[0], interested[1],
					Entrancement.decisions - marks[1], Entrancement.steered - marks[0]);
				helper.assertTrue(interested[0] >= 4, "the zombies should have been interested in the lit area");
				helper.assertValueEqual(interested[1], 0, "zombies still remembering lit ground");
				helper.assertTrue(Entrancement.decisions > marks[1], "the zombies should have made wander decisions");
				helper.assertValueEqual(Entrancement.steered - marks[0], 0L, "wander decisions steered after the lights went out");
			})
			.thenSucceed();
	}

	/**
	 * HOST-05: player first. A zombie by a closed oak door that has found the lit room behind it gets
	 * a survival player in sight but out of reach (on a pillar behind it): it keeps its eye on the
	 * player and leaves the door alone; when the player goes creative it goes back to the light and
	 * breaks the door.
	 */
	@GameTest(environment = "simple_block-gametest:m09_pursuit_night", skyAccess = true, maxTicks = 24000)
	public void pursuitBeatsTheDoor(GameTestHelper helper) {
		Arena arena = new Arena(helper, 25, 16);
		BlockPos door = doorway(arena, Blocks.OAK_DOOR.defaultBlockState(), false);
		arena.torch(-2, 3);
		arena.torch(2, 3);
		for (int y = 1; y <= 3; y++) {
			arena.set(0, y, -5, Blocks.STONE);
		}
		ServerPlayer player = arena.player(GameType.CREATIVE, 0, -5);
		Vec3 top = Vec3.atBottomCenterOf(arena.at(0, 4, -5));
		player.teleportTo(arena.level, top.x, top.y, top.z, Set.of(), 0.0F, 0.0F, true);
		Mob[] zombie = new Mob[1];
		int[] counts = new int[3];
		long[] times = {-1, -1};
		arena.whenLit()
			.thenExecute(() -> {
				ensureClock(helper, true);
				zombie[0] = arena.spawn(EntityTypes.ZOMBIE, 2, -3);
			})
			// Light behind walls is found by random sampling, which takes a while (MOB-02).
			.thenWaitUntil(() -> helper.assertTrue(Entrancement.isInterested((PathfinderMob) zombie[0]), "waiting for the zombie to find the lit room"))
			.thenExecute(() -> {
				helper.assertTrue(arena.level.getBlockState(door).is(Blocks.OAK_DOOR), "the door went before the player turned up");
				player.setGameMode(GameType.SURVIVAL);
			})
			.thenWaitUntil(() -> helper.assertTrue(zombie[0].getTarget() == player, "waiting for the zombie to see the player"))
			.thenExecuteFor(900, () -> {
				helper.assertTrue(arena.level.getBlockState(door).is(Blocks.OAK_DOOR), "the door was broken while the zombie had a player to chase");
				counts[0]++;
				counts[1] += zombie[0].getTarget() == player ? 1 : 0;
				counts[2] += running(zombie[0], LightRoutes.LightDoorBreakGoal.class) ? 1 : 0;
			})
			.thenExecute(() -> {
				player.setGameMode(GameType.CREATIVE);
				times[0] = helper.getTick();
			})
			.thenWaitUntil(() -> helper.assertTrue(arena.level.getBlockState(door).isAir(), "waiting for the zombie to break the door once the player is gone"))
			.thenExecute(() -> {
				times[1] = helper.getTick();
				SimpleBlockTestLog.info("HOST-05 player first: zombie by a closed oak door to a lit room with a survival player in sight on a pillar: "
					+ "targeted the player {} of {} ticks, light door-breaking ran {} ticks, door intact; player made creative at tick {}, door broken at tick {}",
					counts[1], counts[0], counts[2], times[0], times[1]);
				helper.assertValueEqual(counts[2], 0, "ticks of light door-breaking during the chase");
				helper.assertTrue(counts[1] > counts[0] * 0.9, "the zombie should keep the player as its target");
			})
			.thenSucceed();
	}

	/**
	 * HOST-06: breach. Zombies loiter outside a closed building of two lit rooms; an explosion
	 * blows a hole in one room's far wall. They re-check at once, the room that became reachable
	 * fills, and the room still sealed stays empty.
	 */
	@GameTest(environment = "simple_block-gametest:m09_breach_night", skyAccess = true, maxTicks = 9000)
	public void breachRedistributes(GameTestHelper helper) {
		Arena arena = new Arena(helper, 26, 24);
		int[] roomA = {-6, -2, 1, 8};
		int[] roomB = {0, 4, 1, 8};
		arena.box(roomA[0], roomA[1], roomA[2], roomA[3]);
		arena.box(roomB[0], roomB[1], roomB[2], roomB[3]);
		// The patch the explosion opens, in room A's west wall (x -7).
		for (int y = 1; y <= 2; y++) {
			for (int z = 5; z <= 6; z++) {
				arena.set(-7, y, z, Blocks.DIRT);
			}
		}
		arena.torch(-4, 4);
		arena.torch(2, 4);
		arena.player(GameType.CREATIVE, 0, -14);
		double[] near = new double[2];
		boolean[] insideBefore = {false};
		boolean[] enteredB = {false};
		long[] marks = new long[6];
		java.util.Set<Mob> enteredA = new java.util.HashSet<>();
		java.util.Set<Mob> interested = new java.util.HashSet<>();
		arena.whenLit()
			.thenExecute(() -> {
				ensureClock(helper, true);
				for (int x = -6; x <= 4; x += 2) {
					arena.spawn(EntityTypes.ZOMBIE, x, -6);
					arena.spawn(EntityTypes.ZOMBIE, x, -8);
				}
			})
			.thenExecuteFor(1500, () -> {
				for (Mob mob : arena.mobs) {
					insideBefore[0] |= arena.inBox(mob, roomA) || arena.inBox(mob, roomB);
				}
				if (helper.getTick() % 10 == 0) {
					// Mobs that know of the lit rooms (light behind walls is found by sampling, slowly: MOB-02).
					for (Mob mob : arena.mobs) {
						if (Entrancement.isInterested((PathfinderMob) mob)) {
							near[1]++;
							near[0] += arena.inBox(mob, new int[] {-10, 8, -3, 11}) ? 1 : 0;
						}
					}
				}
			})
			.thenExecute(() -> {
				marks[0] = Entrancement.breaches;
				marks[1] = Entrancement.reconsidered;
				marks[4] = Entrancement.decisions;
				arena.mobs.stream().filter(mob -> Entrancement.isInterested((PathfinderMob) mob)).forEach(interested::add);
				BlockPos hole = arena.at(-7, 1, 5);
				arena.level.explode(null, hole.getX() + 0.5, hole.getY() + 1.0, hole.getZ() + 1.0, 2.0F, Level.ExplosionInteraction.TNT);
				marks[2] = helper.getTick();
				helper.assertTrue(arena.level.getBlockState(hole).isAir() && arena.level.getBlockState(hole.above()).isAir(), "the explosion should open the wall");
			})
			.thenExecuteFor(1200, () -> {
				if (helper.getTick() == marks[2] + 5) {
					// Wander decisions in the 5 ticks after the blast: without the re-check about 5/120 per mob.
					marks[5] = Entrancement.decisions - marks[4];
				}
				for (Mob mob : arena.mobs) {
					if (mob.isAlive() && arena.inBox(mob, roomA)) {
						if (enteredA.isEmpty()) {
							marks[3] = helper.getTick();
						}
						enteredA.add(mob);
					}
					enteredB[0] |= arena.inBox(mob, roomB);
				}
			})
			.thenExecute(() -> {
				long interestedAlive = interested.stream().filter(Mob::isAlive).count();
				long interestedIn = interested.stream().filter(enteredA::contains).count();
				SimpleBlockTestLog.info("HOST-06 breach: 12 zombies outside two closed lit rooms for 1500 ticks: any inside {}, {} of the samples of zombies that "
					+ "knew of the light within 3 blocks of the building (about 6% of the floor); explosion at tick {} opened room A's far wall; {} zombies knew of the light then; "
					+ "breaches signalled {}, mobs made to re-check {}, wander decisions in the next 5 ticks {}; first zombie in room A at tick {}; {} zombies entered room A in 1200 ticks, {} of the "
					+ "{} that knew of the light and lived; any in the sealed room B {}", insideBefore[0], pct(near[0], near[1]), marks[2], interested.size(),
					Entrancement.breaches - marks[0], Entrancement.reconsidered - marks[1], marks[5], marks[3], enteredA.size(), interestedIn, interestedAlive, enteredB[0]);
				helper.assertTrue(!insideBefore[0], "no zombie should get into a closed room");
				// The 3-block band round the building is about 6% of the arena floor.
				helper.assertTrue(near[0] / near[1] > 0.3, "the zombies should loiter along the outside of the lit building");
				helper.assertTrue(Entrancement.breaches > marks[0] && Entrancement.reconsidered > marks[1], "the explosion should make interested mobs re-check");
				helper.assertTrue(interested.size() >= 3, "several zombies should have found the light before the breach");
				// Without the re-check about 12 x 5/120 = 0.5 decisions fall in those 5 ticks; zombies still walking a path decide
				// only when it ends, so not all of them do at once (Build #41 saw 3).
				helper.assertTrue(marks[5] >= 2 && marks[5] * 3 >= interested.size(), "the zombies that knew of the light should re-check at once: "
					+ marks[5] + " decisions for " + interested.size());
				helper.assertTrue(marks[3] >= 0 && marks[3] - marks[2] <= 300, "the first zombie should be in the breached room soon after the explosion");
				helper.assertTrue(interestedIn * 3 >= interestedAlive, "at least a third of the zombies that knew of the light should press into the breached room");
				helper.assertTrue(!enteredB[0], "the sealed room must stay empty");
			})
			.thenSucceed();
	}

	// ---------------------------------------------------------------- creeper retaliation

	private static void shoot(Arena arena, Creeper creeper, @Nullable LivingEntity owner) {
		Arrow arrow = owner != null ? new Arrow(arena.level, owner, new ItemStack(Items.ARROW), null)
			: new Arrow(arena.level, creeper.getX(), creeper.getY() + 3, creeper.getZ(), new ItemStack(Items.ARROW), null);
		creeper.hurtServer(arena.level, arena.level.damageSources().arrow(arrow, owner), 1.0F);
	}

	/**
	 * CRET-01/04: a creeper shot by a player shut in a room (no way to walk to them) goes to the
	 * nearest door, the room's oak door rather than a trapdoor further off, and explodes against
	 * it. With {@code mob_griefing} the door is destroyed; without it the creeper still explodes
	 * there but no block is damaged.
	 */
	private static void creeperBlowsTheDoor(GameTestHelper helper, int slot, boolean griefing) {
		Arena arena = new Arena(helper, slot, 16);
		BlockPos door = doorway(arena, Blocks.OAK_DOOR.defaultBlockState(), false);
		arena.set(8, 1, -4, Blocks.OAK_TRAPDOOR.defaultBlockState().setValue(TrapDoorBlock.FACING, Direction.NORTH));
		BlockPos trapdoor = arena.at(8, 1, -4);
		ServerPlayer player = arena.player(GameType.CREATIVE, 0, 4);
		Creeper[] creeper = new Creeper[1];
		Vec3[] last = new Vec3[1];
		long[] times = {-1, -1};
		CreeperRetaliation.Grudge[] grudge = new CreeperRetaliation.Grudge[1];
		long breaches = Entrancement.breaches;
		arena.whenLit()
			.thenExecute(() -> {
				ensureClock(helper, true);
				creeper[0] = arena.spawn(EntityTypes.CREEPER, 0, -5);
			})
			.thenIdle(10)
			.thenExecute(() -> {
				helper.assertTrue(!CreeperRetaliation.canReach(creeper[0], player), "the shooter should be out of reach");
				shoot(arena, creeper[0], player);
				grudge[0] = CreeperRetaliation.grudge(creeper[0]);
				times[0] = helper.getTick();
			})
			.thenWaitUntil(() -> {
				if (creeper[0].isAlive()) {
					last[0] = creeper[0].position();
				}
				helper.assertTrue(creeper[0].isRemoved(), "waiting for the creeper to explode");
			})
			.thenExecute(() -> {
				times[1] = helper.getTick();
				double distance = last[0].distanceTo(Vec3.atCenterOf(door));
				boolean doorGone = !(arena.level.getBlockState(door).getBlock() instanceof DoorBlock);
				boolean trapdoorThere = arena.level.getBlockState(trapdoor).getBlock() instanceof TrapDoorBlock;
				SimpleBlockTestLog.info("CRET-0{} mob_griefing {}: creeper shot by a player shut in a room; grudge at {} (door {}); exploded at tick {} ({} after the hit), "
					+ "{} blocks from the door's centre; door destroyed {}; trapdoor 8 blocks off untouched {}; breaches signalled {}", griefing ? 1 : 4, griefing,
					grudge[0] == null ? null : grudge[0].target().subtract(arena.center), grudge[0] != null && grudge[0].door(), times[1], times[1] - times[0], String.format("%.2f", distance), doorGone, trapdoorThere, Entrancement.breaches - breaches);
				helper.assertTrue(grudge[0] != null && grudge[0].door() && grudge[0].target().equals(door), "the creeper should pick the nearest door");
				helper.assertTrue(distance < 2.5, "it should explode against the door");
				helper.assertTrue(doorGone == griefing, griefing ? "the door should be blown up" : "without mob_griefing no block is damaged");
				helper.assertTrue(trapdoorThere, "the trapdoor further off should be untouched");
				helper.assertTrue(Entrancement.breaches > breaches, "the explosion should be signalled as a breach");
			})
			.thenSucceed();
	}

	@GameTest(environment = "simple_block-gametest:m09_creeper_door_night", skyAccess = true, maxTicks = 7000)
	public void provokedCreeperBlowsTheNearestDoor(GameTestHelper helper) {
		creeperBlowsTheDoor(helper, 27, true);
	}

	@GameTest(environment = "simple_block-gametest:m09_creeper_nogrief_night", skyAccess = true, maxTicks = 7000)
	public void provokedCreeperKeepsMobGriefing(GameTestHelper helper) {
		creeperBlowsTheDoor(helper, 30, false);
	}

	/**
	 * CRET-02: no door anywhere near, the shooter on top of an 8-high pillar: the creeper walks to the
	 * foot of the pillar (the nearest reachable point to the shooter's X/Z) and explodes there.
	 */
	@GameTest(environment = "simple_block-gametest:m09_creeper_pillar_night", skyAccess = true, maxTicks = 7000)
	public void provokedCreeperGoesUnderTheShooter(GameTestHelper helper) {
		Arena arena = new Arena(helper, 28, 16);
		// A player's tower of planks (stone shrugs off a creeper's blast by vanilla rules).
		for (int y = 1; y <= 8; y++) {
			arena.set(0, y, 0, Blocks.OAK_PLANKS);
		}
		ServerPlayer player = arena.player(GameType.CREATIVE, 0, 0);
		Vec3 top = Vec3.atBottomCenterOf(arena.at(0, 9, 0));
		player.teleportTo(arena.level, top.x, top.y, top.z, Set.of(), 0.0F, 0.0F, true);
		Creeper[] creeper = new Creeper[1];
		Vec3[] last = new Vec3[1];
		CreeperRetaliation.Grudge[] grudge = new CreeperRetaliation.Grudge[1];
		int[] pillarLeft = {0};
		String[] lit = {null};
		arena.whenLit()
			.thenExecute(() -> {
				ensureClock(helper, true);
				creeper[0] = arena.spawn(EntityTypes.CREEPER, 13, 9);
			})
			.thenIdle(10)
			.thenExecute(() -> {
				shoot(arena, creeper[0], player);
				grudge[0] = CreeperRetaliation.grudge(creeper[0]);
			})
			.thenWaitUntil(() -> {
				if (creeper[0].isAlive()) {
					last[0] = creeper[0].position();
					if (lit[0] == null && creeper[0].isIgnited()) {
						lit[0] = String.format("tick %d at %.2f from the X/Z, target %s, grudge left %s", helper.getTick(),
							creeper[0].position().subtract(player.position()).horizontalDistance(), creeper[0].getTarget(), CreeperRetaliation.grudge(creeper[0]));
					}
				}
				helper.assertTrue(creeper[0].isRemoved(), "waiting for the creeper to explode");
			})
			.thenExecute(() -> {
				double horizontal = last[0].subtract(player.position()).horizontalDistance();
				for (int y = 1; y <= 8; y++) {
					pillarLeft[0] += arena.level.getBlockState(arena.at(0, y, 0)).is(Blocks.OAK_PLANKS) ? 1 : 0;
				}
				SimpleBlockTestLog.info("CRET-02 no door, shooter {} blocks up on a planks pillar: grudge at {} (door {}); ignited {}; exploded {} blocks from the shooter's X/Z "
					+ "({} vertically); {} of 8 pillar blocks left", String.format("%.0f", player.getY() - last[0].y),
					grudge[0] == null ? null : grudge[0].target().subtract(arena.center), grudge[0] != null && grudge[0].door(), lit[0],
					String.format("%.2f", horizontal), String.format("%.1f", player.getY() - last[0].y), pillarLeft[0]);
				helper.assertTrue(grudge[0] != null && !grudge[0].door(), "no door: the X/Z fallback");
				helper.assertTrue(horizontal < 2.0, "it should explode at the foot of the pillar");
				helper.assertTrue(pillarLeft[0] < 8, "the explosion should damage what the shooter stands on");
			})
			.thenSucceed();
	}

	/**
	 * CRET-03 negative controls: a sword hit from the shut-in player, an arrow from a skeleton, an
	 * arrow with no owner (a dispenser's) and an arrow from a player standing in the open all leave
	 * the creeper ordinary; the same arrow from the shut-in player (positive control) provokes it.
	 */
	@GameTest(environment = "simple_block-gametest:m09_creeper_controls_night", skyAccess = true, maxTicks = 7000)
	public void creeperRetaliationNegativeControls(GameTestHelper helper) {
		Arena arena = new Arena(helper, 29, 20);
		doorway(arena, Blocks.OAK_DOOR.defaultBlockState(), false);
		ServerPlayer shutIn = arena.player(GameType.CREATIVE, 0, 4);
		ServerPlayer open = arena.player(GameType.CREATIVE, -12, -12);
		Creeper[] creepers = new Creeper[5];
		String[] cases = {"sword from the shut-in player", "skeleton's arrow", "ownerless (dispenser) arrow", "arrow from a reachable player",
			"arrow from the shut-in player (positive control)"};
		Mob[] skeleton = new Mob[1];
		boolean[] reachable = new boolean[1];
		boolean[] provoked = new boolean[5];
		arena.whenLit()
			.thenExecute(() -> {
				ensureClock(helper, true);
				creepers[0] = arena.spawn(EntityTypes.CREEPER, -3, -5);
				creepers[1] = arena.spawn(EntityTypes.CREEPER, 0, -5);
				creepers[2] = arena.spawn(EntityTypes.CREEPER, 3, -5);
				creepers[3] = arena.spawn(EntityTypes.CREEPER, -12, -8);
				creepers[4] = arena.spawn(EntityTypes.CREEPER, 6, -5);
				skeleton[0] = arena.spawn(EntityTypes.SKELETON, 12, -12);
				skeleton[0].setNoAi(true);
			})
			.thenIdle(10)
			.thenExecute(() -> {
				reachable[0] = CreeperRetaliation.canReach(creepers[3], open);
				creepers[0].hurtServer(arena.level, arena.level.damageSources().playerAttack(shutIn), 1.0F);
				shoot(arena, creepers[1], skeleton[0]);
				shoot(arena, creepers[2], null);
				shoot(arena, creepers[3], open);
				shoot(arena, creepers[4], shutIn);
				for (int i = 0; i < 5; i++) {
					provoked[i] = CreeperRetaliation.grudge(creepers[i]) != null;
				}
				creepers[4].discard();
			})
			.thenIdle(60)
			.thenExecute(() -> {
				StringBuilder log = new StringBuilder();
				for (int i = 0; i < 5; i++) {
					log.append(cases[i]).append(" -> provoked ").append(provoked[i]).append("; ");
				}
				SimpleBlockTestLog.info("CRET-03 controls: {}the open player reachable {}; after 60 ticks none of the controls ignited {}", log, reachable[0],
					java.util.stream.IntStream.range(0, 4).noneMatch(i -> creepers[i].isIgnited()));
				helper.assertTrue(reachable[0], "the open player should be reachable");
				for (int i = 0; i < 4; i++) {
					helper.assertTrue(!provoked[i], cases[i] + " must not provoke retaliation");
					helper.assertTrue(!creepers[i].isIgnited(), cases[i] + ": no ignition");
				}
				helper.assertTrue(provoked[4], "the positive control should provoke retaliation");
			})
			.thenSucceed();
	}

	// ---------------------------------------------------------------- jockeys

	@SuppressWarnings("unchecked")
	private static <T extends Goal> T goal(Mob mob, Class<T> type) {
		for (WrappedGoal wrapped : ((MobAccessor) mob).simpleBlock$getGoalSelector().getAvailableGoals()) {
			if (type.isInstance(wrapped.getGoal())) {
				return (T) wrapped.getGoal();
			}
		}
		return null;
	}

	/**
	 * JOCK-01: with recruitment forced (chance 1, a check every tick), a skeleton walks to the
	 * ordinary spider and mounts it, passing over a cave spider that is nearer.
	 */
	@GameTest(environment = "simple_block-gametest:m09_jockey_recruit_night", skyAccess = true, maxTicks = 7000)
	public void skeletonWalksToASpiderAndMounts(GameTestHelper helper) {
		Arena arena = new Arena(helper, 31, 16);
		arena.player(GameType.CREATIVE, -12, -12);
		Mob[] mobs = new Mob[3];
		Vec3[] start = new Vec3[1];
		long[] times = {-1, -1};
		int recruited = SpiderJockeys.recruited;
		arena.whenLit()
			.thenExecute(() -> {
				ensureClock(helper, true);
				mobs[0] = arena.spawn(EntityTypes.SKELETON, -4, 0);
				mobs[1] = arena.spawn(EntityTypes.SPIDER, 3, 0);
				mobs[2] = arena.spawn(EntityTypes.CAVE_SPIDER, -2, 2);
				mobs[1].setNoAi(true);
				mobs[2].setNoAi(true);
			})
			.thenIdle(5)
			.thenExecute(() -> {
				start[0] = mobs[0].position();
				times[0] = helper.getTick();
				arena.tune("mob.jockey.recruit_chance", 1.0);
				arena.tune("mob.jockey.check_ticks", 1);
			})
			.thenWaitUntil(() -> helper.assertTrue(mobs[0].getVehicle() != null, "waiting for the skeleton to mount"))
			.thenExecute(() -> {
				times[1] = helper.getTick();
				double walked = start[0].distanceTo(mobs[0].getVehicle().position());
				SimpleBlockTestLog.info("JOCK-01 recruitment forced: skeleton mounted a {} after {} ticks, {} blocks from where it stood; cave spider "
					+ "2.8 blocks away carrying anyone {}; recruitments counted {}", BuiltInRegistries.ENTITY_TYPE.getKey(mobs[0].getVehicle().getType()).getPath(),
					times[1] - times[0], String.format("%.1f", walked), mobs[2].isVehicle(), SpiderJockeys.recruited - recruited);
				helper.assertTrue(mobs[0].getVehicle() == mobs[1], "it should mount the ordinary spider");
				helper.assertTrue(!mobs[2].isVehicle(), "the cave spider must not be recruited");
				helper.assertTrue(times[1] - times[0] >= 10 && walked > 4.5, "it should walk there, not mount from afar");
				helper.assertValueEqual(SpiderJockeys.recruited - recruited, 1, "recruitments");
			})
			.thenSucceed();
	}

	/**
	 * JOCK-02, the recruitment check itself (forced chance, then default): a spider beyond the range,
	 * a cave spider and a spider already carrying a rider are never picked; a skeleton with a target
	 * doesn't recruit; strays have no recruitment at all; at the default chance about 1% of checks
	 * succeed.
	 */
	@GameTest(environment = "simple_block-gametest:m09_jockey_rules_night", skyAccess = true, maxTicks = 7000)
	public void recruitmentRules(GameTestHelper helper) {
		Arena arena = new Arena(helper, 32, 20);
		ServerPlayer player = arena.player(GameType.CREATIVE, -15, -15);
		Mob[] mobs = new Mob[6];
		arena.whenLit()
			.thenExecute(() -> {
				ensureClock(helper, true);
				mobs[0] = arena.spawn(EntityTypes.SKELETON, 0, 0);
				mobs[1] = arena.spawn(EntityTypes.SPIDER, 12, 0);
				mobs[2] = arena.spawn(EntityTypes.CAVE_SPIDER, -2, 0);
				mobs[3] = arena.spawn(EntityTypes.SPIDER, 0, 3);
				mobs[4] = arena.spawn(EntityTypes.ZOMBIE, 0, 3);
				mobs[5] = arena.spawn(EntityTypes.STRAY, 0, -8);
				mobs[4].startRiding(mobs[3], true, false);
			})
			.thenIdle(10)
			.thenExecute(() -> {
				arena.tune("mob.jockey.recruit_chance", 1.0);
				arena.tune("mob.jockey.check_ticks", 1);
				Skeleton skeleton = (Skeleton) mobs[0];
				SpiderJockeys.RecruitGoal goal = goal(skeleton, SpiderJockeys.RecruitGoal.class);
				helper.assertTrue(goal != null, "skeletons should have the recruitment goal");
				arena.moveBack(skeleton, 0, 0);
				arena.moveBack(mobs[1], 12, 0);
				arena.moveBack(mobs[2], -2, 0);
				goal.checkNow();
				boolean onlyIneligible = goal.canUse();
				arena.moveBack(mobs[1], 4, 0);
				goal.checkNow();
				boolean near = goal.canUse();
				player.setGameMode(GameType.SURVIVAL);
				skeleton.setTarget(player);
				goal.checkNow();
				boolean withTarget = skeleton.getTarget() == player && goal.canUse();
				skeleton.setTarget(null);
				player.setGameMode(GameType.CREATIVE);
				boolean strayHasGoal = goal(mobs[5], SpiderJockeys.RecruitGoal.class) != null;

				Tuning.reset("mob.jockey.recruit_chance");
				int trials = 20000;
				int successes = 0;
				for (int i = 0; i < trials; i++) {
					goal.checkNow();
					successes += goal.canUse() ? 1 : 0;
				}
				double rate = successes / (double) trials;
				SimpleBlockTestLog.info("JOCK-02 rules (forced chance): spider 12 blocks off + cave spider 2 off + spider with a rider 3 off -> recruits {}; "
					+ "spider moved to 4 blocks -> recruits {}; same with a target -> recruits {}; stray has the goal {}; default chance {}: {} of {} checks "
					+ "chose a spider ({}); with the default 200-tick check that is about one recruitment per {} minutes of idle time next to a spider",
					onlyIneligible, near, withTarget, strayHasGoal, Tuning.getDouble("mob.jockey.recruit_chance"),
					successes, trials, pct(successes, trials), String.format("%.0f", 200 / Math.max(rate, 1e-9) / 20 / 60));
				helper.assertTrue(!onlyIneligible, "distant, cave and mounted spiders must not be recruited");
				helper.assertTrue(near, "an eligible spider in range should be (positive control)");
				helper.assertTrue(!withTarget, "a skeleton with a target doesn't recruit");
				helper.assertTrue(!strayHasGoal, "strays don't recruit");
				helper.assertTrue(rate > 0.007 && rate < 0.013, "default recruitment should be about 1% of checks: " + rate);
			})
			.thenSucceed();
	}

	/** JOCK-03: a skeleton on its way to a spider drops the plan the moment it has a player to fight. */
	@GameTest(environment = "simple_block-gametest:m09_jockey_combat_night", skyAccess = true, maxTicks = 7000)
	public void combatInterruptsRecruitment(GameTestHelper helper) {
		Arena arena = new Arena(helper, 33, 16);
		// The player in a glass cell: in sight, out of reach, arrows stopped.
		for (int dx = -1; dx <= 1; dx++) {
			for (int dz = -1; dz <= 1; dz++) {
				for (int y = 1; y <= 3; y++) {
					if (dx != 0 || dz != 0 || y == 3) {
						arena.set(dx - 6, y, dz - 9, Blocks.GLASS);
					}
				}
			}
		}
		ServerPlayer player = arena.player(GameType.CREATIVE, -6, -9);
		Mob[] mobs = new Mob[2];
		long[] times = {-1};
		arena.whenLit()
			.thenExecute(() -> {
				ensureClock(helper, true);
				mobs[0] = arena.spawn(EntityTypes.SKELETON, -6, 0);
				mobs[1] = arena.spawn(EntityTypes.SPIDER, 1, 0);
				mobs[1].setNoAi(true);
			})
			.thenIdle(5)
			.thenExecute(() -> {
				// Waiting for the light, the skeleton may have wandered up to the spider: start it 7 blocks off again.
				arena.moveBack(mobs[0], -6, 0);
				arena.tune("mob.jockey.recruit_chance", 1.0);
				arena.tune("mob.jockey.check_ticks", 1);
			})
			.thenWaitUntil(() -> helper.assertTrue(running(mobs[0], SpiderJockeys.RecruitGoal.class), "waiting for the skeleton to set off"))
			.thenExecute(() -> helper.assertTrue(!mobs[0].isPassenger() && mobs[0].distanceTo(mobs[1]) > 3, "the skeleton should set off from a distance"))
			.thenExecute(() -> {
				player.setGameMode(GameType.SURVIVAL);
				mobs[0].setTarget(player);
				times[0] = helper.getTick();
			})
			.thenIdle(2)
			.thenExecute(() -> helper.assertTrue(!running(mobs[0], SpiderJockeys.RecruitGoal.class), "recruitment should stop at once"))
			.thenExecuteFor(200, () -> helper.assertTrue(!mobs[0].isPassenger(), "the skeleton mounted while fighting (target " + mobs[0].getTarget()
				+ ", recruiting " + running(mobs[0], SpiderJockeys.RecruitGoal.class) + ")"))
			.thenExecute(() -> {
				SimpleBlockTestLog.info("JOCK-03 combat: skeleton set off for a spider 7 blocks away, given a player target at tick {}: recruitment stopped "
					+ "within 2 ticks, not mounted 200 ticks later, still targeting the player {}", times[0], mobs[0].getTarget() == player);
			})
			.thenSucceed();
	}

	/** Which ledge-grab rig to build: overhang, clear wall, riderless spider, or impassable cap. */
	private enum Rig {
		OVERHANG, CLEAR, RIDERLESS, CAPPED
	}

	/**
	 * JOCK-04..07, the ledge grab. A 10-high wall (z 0..1); a spider at its foot, pushed up the wall
	 * towards its top each tick (its move control: the test stands in for whatever it climbs after),
	 * with a skeleton riding it (no AI of its own, so it doesn't steer). Every tick both must be clear
	 * of blocks.
	 */
	private static void ledgeGrab(GameTestHelper helper, int slot, Rig rig) {
		Arena arena = new Arena(helper, slot, 12);
		for (int x = -3; x <= 3; x++) {
			for (int y = 1; y <= 10; y++) {
				arena.set(x, y, 0, Blocks.STONE);
				arena.set(x, y, 1, Blocks.STONE);
			}
			if (rig == Rig.OVERHANG || rig == Rig.RIDERLESS) {
				arena.set(x, 5, -1, Blocks.STONE);
			}
		}
		if (rig == Rig.CAPPED) {
			// A shaft 2 deep (z -2..-1), 3 wide (x -1..1), capped at y 5.
			for (int y = 1; y <= 5; y++) {
				for (int z = -3; z <= -1; z++) {
					arena.set(-2, y, z, Blocks.STONE);
					arena.set(2, y, z, Blocks.STONE);
				}
				for (int x = -2; x <= 2; x++) {
					arena.set(x, y, -3, Blocks.STONE);
				}
			}
			for (int x = -1; x <= 1; x++) {
				arena.set(x, 5, -1, Blocks.STONE);
				arena.set(x, 5, -2, Blocks.STONE);
			}
		}
		arena.player(GameType.CREATIVE, -10, -10);
		Spider[] spider = new Spider[1];
		Skeleton[] skeleton = new Skeleton[1];
		int started = SpiderJockeys.haulsStarted;
		int cleared = SpiderJockeys.haulsCleared;
		int abandoned = SpiderJockeys.haulsAbandoned;
		int[] grabTicks = {0};
		int[] headInBlock = {0};
		double[] maxY = {0, 0};
		long[] times = {-1, -1};
		Vec3 wallTop = Vec3.atBottomCenterOf(arena.at(0, 11, 0));
		arena.whenLit()
			.thenExecute(() -> {
				ensureClock(helper, true);
				spider[0] = arena.spawn(EntityTypes.SPIDER, 0, -1);
				Vec3 foot = Vec3.atBottomCenterOf(arena.at(0, 1, -1));
				spider[0].snapTo(foot.x, foot.y, foot.z - 0.25, 0.0F, 0.0F);
				if (rig != Rig.RIDERLESS) {
					skeleton[0] = arena.spawn(EntityTypes.SKELETON, 0, -6);
					skeleton[0].setNoAi(true);
					skeleton[0].startRiding(spider[0], true, true);
				}
				times[0] = helper.getTick();
			})
			.thenExecuteFor(400, () -> {
				Spider s = spider[0];
				s.getMoveControl().setWantedPosition(wallTop.x, wallTop.y, wallTop.z, 1.0);
				helper.assertTrue(arena.level.noCollision(s, s.getBoundingBox().deflate(1.0E-3)), "the spider is inside a block at " + s.position());
				maxY[0] = Math.max(maxY[0], s.getY());
				if (skeleton[0] != null) {
					// Under the cap the head left in the ceiling suffocates (vanilla) and the skeleton may die and fall.
					helper.assertTrue(skeleton[0].getVehicle() == s || rig == Rig.CAPPED, "the skeleton fell off");
					boolean clear = arena.level.noCollision(skeleton[0], skeleton[0].getBoundingBox().deflate(1.0E-3));
					// Under a cap the haul is given up, and vanilla climbing then pushes the rider's head into the
					// ceiling (vanilla places a rider without collision, and the head suffocates) before any retry:
					// there only the spider is checked, and that nothing gets past the cap.
					helper.assertTrue(clear || rig == Rig.CAPPED, "the skeleton is inside a block at " + skeleton[0].position());
					headInBlock[0] += clear ? 0 : 1;
					maxY[1] = Math.max(maxY[1], skeleton[0].getBoundingBox().maxY);
					if (((SpiderJockeys.Grabbing) skeleton[0]).simpleBlock$grabbing()) {
						grabTicks[0]++;
						if (times[1] < 0) {
							times[1] = helper.getTick();
						}
					}
				}
			})
			.thenExecute(() -> {
				double ground = arena.at(0, 1, 0).getY();
				int haulsStarted = SpiderJockeys.haulsStarted - started;
				int haulsCleared = SpiderJockeys.haulsCleared - cleared;
				int haulsAbandoned = SpiderJockeys.haulsAbandoned - abandoned;
				SimpleBlockTestLog.info("JOCK {}: 400 ticks pushed up a 10-high wall; hauls started {}, cleared {}, abandoned {}; grab pose shown {} ticks "
					+ "(first at tick {}); spider's highest feet {} blocks above the floor, skeleton's highest head {}; spider never inside a block, "
					+ "skeleton's head in a block {} ticks",
					rig, haulsStarted, haulsCleared, haulsAbandoned, grabTicks[0], times[1], String.format("%.2f", maxY[0] - ground),
					String.format("%.2f", maxY[1] - ground), headInBlock[0]);
				switch (rig) {
					case OVERHANG -> {
						helper.assertTrue(haulsStarted >= 1 && haulsCleared >= 1, "the jockey should be hauled round the overhang");
						helper.assertTrue(grabTicks[0] > 0, "the skeleton should show the grab pose");
						helper.assertTrue(maxY[0] - ground >= 10.0, "the jockey should reach the top of the wall");
					}
					case CLEAR -> {
						helper.assertValueEqual(haulsStarted, 0, "hauls on a clear wall");
						helper.assertValueEqual(grabTicks[0], 0, "grab-pose ticks on a clear wall");
						helper.assertTrue(maxY[0] - ground >= 10.0, "the jockey should climb the clear wall");
					}
					case RIDERLESS -> {
						helper.assertValueEqual(haulsStarted, 0, "hauls without a rider");
					}
					case CAPPED -> {
						helper.assertTrue(haulsStarted >= 1 && haulsAbandoned >= 1 && haulsCleared == 0, "the haul should be tried and given up");
						helper.assertTrue(maxY[0] + 0.9 - ground <= 4.0 + 1.0E-3, "the spider never gets past the cap");
						helper.assertTrue(haulsStarted <= 5, "after giving up it doesn't keep trying every tick: " + haulsStarted);
					}
				}
			})
			.thenSucceed();
	}

	@GameTest(environment = "simple_block-gametest:m09_ledge_overhang_night", skyAccess = true, maxTicks = 7000)
	public void jockeyHaulsRoundAnOverhang(GameTestHelper helper) {
		ledgeGrab(helper, 34, Rig.OVERHANG);
	}

	@GameTest(environment = "simple_block-gametest:m09_ledge_clear_night", skyAccess = true, maxTicks = 7000)
	public void jockeyOnAClearWallJustClimbs(GameTestHelper helper) {
		ledgeGrab(helper, 35, Rig.CLEAR);
	}

	@GameTest(environment = "simple_block-gametest:m09_ledge_riderless_night", skyAccess = true, maxTicks = 7000)
	public void riderlessSpiderClimbsAsUsual(GameTestHelper helper) {
		ledgeGrab(helper, 36, Rig.RIDERLESS);
	}

	@GameTest(environment = "simple_block-gametest:m09_ledge_capped_night", skyAccess = true, maxTicks = 7000)
	public void impassableCapIsNotClipped(GameTestHelper helper) {
		ledgeGrab(helper, 37, Rig.CAPPED);
	}
}
