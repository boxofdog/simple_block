package io.github.boxofdog.simpleblock.gametest;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.vehicle.minecart.Minecart;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RailBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.RailShape;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import net.fabricmc.fabric.api.gametest.v1.GameTest;

import io.github.boxofdog.simpleblock.cable.CableAnchorBlock;
import io.github.boxofdog.simpleblock.cable.CableContent;
import io.github.boxofdog.simpleblock.cable.CableDrumBlock;
import io.github.boxofdog.simpleblock.cable.CableDrumBlockEntity;
import io.github.boxofdog.simpleblock.cable.CableHook;
import io.github.boxofdog.simpleblock.cable.CableIndex;
import io.github.boxofdog.simpleblock.cable.CableLoops;
import io.github.boxofdog.simpleblock.cable.ConnectingRods;
import io.github.boxofdog.simpleblock.feature.MinecartCargo;

/**
 * M05 work packages A and B: closing a route into a loop, the crank, hauling a cart along the working
 * strand and back, on the level and up a rail climb, the overhead drum, and what happens when the loop
 * is broken, saved or refused. Test ids refer to the matrix in {@code docs/MILESTONE_05_REPORT.md}.
 *
 * <p>The line used by most tests ({@link #line}), in {@code cable_yard}: a track along z 4 from x 2,
 * a drum standing at x 1 and anchors under planks at x 7, 15 and 23, two blocks above the rail, so the
 * cable runs 2.75 blocks above it. The route is 23 blocks; closing it takes 26 more.
 */
public class CableLoopGameTests {
	private static final String YARD = "simple_block-gametest:cable_yard";
	static final BlockPos DRUM = new BlockPos(1, 1, 4);
	static final BlockPos[] ANCHORS = {new BlockPos(7, 3, 4), new BlockPos(15, 3, 4), new BlockPos(23, 3, 4)};

	static void rail(GameTestHelper helper, BlockPos pos, RailShape shape) {
		helper.setBlock(pos.below(), Blocks.STONE);
		helper.setBlock(pos, Blocks.RAIL.defaultBlockState().setValue(RailBlock.SHAPE, shape));
	}

	/** Track, a drum with {@code coils} coils and the three anchors routed from it; the end is put down at the last anchor. */
	static CableDrumBlockEntity line(GameTestHelper helper, ServerPlayer player, int coils) {
		for (int x = 2; x < 30; x++) {
			rail(helper, new BlockPos(x, 1, 4), RailShape.EAST_WEST);
		}
		CableDrumBlockEntity drum = CableGameTests.windDrum(helper, player, DRUM, coils);
		for (BlockPos anchor : ANCHORS) {
			CableGameTests.hangAnchor(helper, player, anchor.above());
		}
		return drum;
	}

	/** Picks up the end and runs it through the anchors; puts it down on the last. */
	static void route(GameTestHelper helper, ServerPlayer player, BlockPos drumPos, BlockPos... anchors) {
		CableHook hook = CableGameTests.hook(helper, drumPos);
		player.interactOn(hook, InteractionHand.MAIN_HAND, hook.position());
		for (BlockPos anchor : anchors) {
			CableGameTests.useBlock(helper, player, anchor, false);
		}
		CableGameTests.useBlock(helper, player, anchors[anchors.length - 1], false);
	}

	/** Uses a copper anchor on the last anchor, as a player would to close the loop. */
	static void close(GameTestHelper helper, ServerPlayer player, BlockPos lastAnchor) {
		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(CableContent.CABLE_ANCHOR_ITEM));
		CableGameTests.useBlock(helper, player, lastAnchor, Direction.UP);
	}

	/** One use on the drum's crank (a held use key repeats this every 4 ticks); sneaking turns it backwards. */
	static void crank(GameTestHelper helper, ServerPlayer player, BlockPos drumRelative, boolean backwards) {
		ServerLevel level = helper.getLevel();
		BlockPos pos = helper.absolutePos(drumRelative);
		BlockState state = level.getBlockState(pos);
		Direction side = CableDrumBlock.crankSide(state);
		double axleY = state.getValue(CableDrumBlock.HANGING) ? 6.5 / 16.0 : 9.5 / 16.0;
		Vec3 hit = Vec3.atLowerCornerOf(pos).add(0.5 + side.getStepX() * 0.55, axleY, 0.5 + side.getStepZ() * 0.55);
		player.setShiftKeyDown(backwards);
		player.gameMode.useItemOn(player, level, player.getMainHandItem(), InteractionHand.MAIN_HAND, new BlockHitResult(hit, side, pos, false));
		player.setShiftKeyDown(false);
	}

	static Minecart rodCart(GameTestHelper helper, double x) {
		Minecart cart = helper.spawn(EntityTypes.MINECART, new Vec3(x, 1.0625, 4.5));
		cart.setDeltaMovement(Vec3.ZERO);
		ConnectingRods.fitted(cart).simpleBlock$setRod(true);
		return cart;
	}

	static double x(GameTestHelper helper, Minecart cart) {
		return cart.getX() - helper.absoluteVec(Vec3.ZERO).x;
	}

	static void assertCableKept(GameTestHelper helper, CableDrumBlockEntity drum, int coils, String when) {
		helper.assertValueEqual(drum.totalCable(), coils * CableContent.BLOCKS_PER_COIL, "wound + laid out + return cable " + when);
	}

	static CableAnchorBlock.Return returnState(GameTestHelper helper, BlockPos anchor) {
		return helper.getBlockState(anchor).getValue(CableAnchorBlock.RETURN);
	}

	// ---- Closing and opening ----

	/** LOOP-01/02: an anchor used on the last anchor closes the loop with cable off the drum; opening refunds it; breaking an anchor opens it. */
	@GameTest(structure = YARD)
	public void loopClosesWithAnAnchorAndOpensAgain(GameTestHelper helper) {
		ServerPlayer player = TestPlayers.create(helper, GameType.SURVIVAL, new BlockPos(3, 1, 6));
		CableDrumBlockEntity drum = line(helper, player, 2);
		List<String> log = new ArrayList<>();
		boolean[] failed = {false};
		helper.startSequence()
			.thenIdle(2)
			.thenExecute(CableGameTests.step(failed, () -> {
				route(helper, player, DRUM, ANCHORS);
				int laid = drum.laidOut();
				int before = drum.stored();
				close(helper, player, ANCHORS[2]);
				helper.assertTrue(drum.looped(), "the route should be a loop");
				helper.assertTrue(player.getMainHandItem().isEmpty(), "the anchor used should go into the wheel");
				helper.assertValueEqual(before - drum.stored(), drum.returnLength(), "cable taken off the drum for the return");
				assertCableKept(helper, drum, 2, "after closing");
				helper.assertValueEqual(returnState(helper, ANCHORS[2]), CableAnchorBlock.Return.WHEEL, "last anchor");
				helper.assertValueEqual(returnState(helper, ANCHORS[0]), CableAnchorBlock.Return.GUIDE, "first anchor");
				helper.assertValueEqual(helper.getBlockState(ANCHORS[2]).getValue(CableAnchorBlock.FACING), Direction.EAST, "wheel side");
				helper.assertValueEqual(helper.getBlockState(DRUM).getValue(CableDrumBlock.FACING), Direction.EAST, "drum lined up with the route");
				List<Vec3> points = drum.points();
				List<CableIndex.Span> indexed = CableIndex.spans(helper.getLevel());
				for (int i = 1; i < points.size(); i++) {
					helper.assertTrue(indexed.contains(new CableIndex.Span(points.get(i - 1), points.get(i))), "working span " + i + " should stay indexed");
				}
				log.add("closed: " + laid + " laid out, return " + drum.returnLength() + " (" + before + " -> " + drum.stored() + " on the drum)");
			}))
			.thenIdle(2)
			.thenExecute(CableGameTests.step(failed, () -> {
				helper.assertValueEqual(CableGameTests.hooks(helper, DRUM).size(), 0, "loose ends while looped");
				player.getInventory().clearContent();
				CableGameTests.useBlock(helper, player, ANCHORS[1], true);
				helper.assertFalse(drum.looped(), "sneak-use of an anchor should open the loop");
				helper.assertValueEqual(drum.route().size(), 3, "route after opening (not dismantled)");
				helper.assertValueEqual(drum.returnLength(), 0, "return cable after opening");
				assertCableKept(helper, drum, 2, "after opening");
				helper.assertValueEqual(MinecartCargoGameTests.carried(player, CableContent.CABLE_ANCHOR_ITEM), 1, "wheel anchor given back");
				helper.assertValueEqual(returnState(helper, ANCHORS[2]), CableAnchorBlock.Return.NONE, "last anchor after opening");
				log.add("opened by sneak-use: route kept, " + drum.stored() + " on the drum, wheel anchor back");
			}))
			.thenIdle(2)
			.thenExecute(CableGameTests.step(failed, () -> {
				helper.assertValueEqual(CableGameTests.hooks(helper, DRUM).size(), 1, "loose end back after opening");
				close(helper, player, ANCHORS[2]);
				helper.assertTrue(drum.looped(), "closed again");
				player.gameMode.destroyBlock(helper.absolutePos(ANCHORS[2]));
				helper.assertFalse(drum.looped(), "breaking the wheel anchor should open the loop");
				helper.assertValueEqual(drum.route(), CableGameTests.absolute(helper, ANCHORS[0], ANCHORS[1]), "route after breaking the last anchor");
				assertCableKept(helper, drum, 2, "after breaking the wheel anchor");
				List<ItemEntity> items = CableGameTests.itemsNear(helper, ANCHORS[2]);
				helper.assertValueEqual(CableGameTests.count(items, CableContent.CABLE_ANCHOR_ITEM), 2, "anchors dropped (the anchor and its wheel)");
				items.forEach(ItemEntity::discard);
				log.add("closed again, wheel anchor broken: loop open, route back to the second anchor, 2 anchors dropped");
				SimpleBlockTestLog.info("LOOP-01/02: {}", log);
				TestPlayers.remove(player);
			}))
			.thenSucceed();
	}

	/** LOOP-03: closing is refused, with nothing lost, without enough cable, on a turning route, or with the return path blocked. */
	@GameTest(structure = YARD)
	public void loopClosingIsRefusedWhenItCannotWork(GameTestHelper helper) {
		ServerPlayer player = TestPlayers.create(helper, GameType.SURVIVAL, new BlockPos(3, 1, 6));
		CableDrumBlockEntity drum = line(helper, player, 1);
		BlockPos side = CableGameTests.hangAnchor(helper, player, new BlockPos(15, 4, 8));
		List<String> log = new ArrayList<>();
		boolean[] failed = {false};
		helper.startSequence()
			.thenIdle(2)
			.thenExecute(CableGameTests.step(failed, () -> {
				route(helper, player, DRUM, ANCHORS);
				close(helper, player, ANCHORS[2]);
				helper.assertFalse(drum.looped(), "one coil leaves " + drum.stored() + " blocks: too little for the return");
				helper.assertValueEqual(player.getMainHandItem().getCount(), 1, "anchor kept after a refusal");
				assertCableKept(helper, drum, 1, "after the refusal");
				log.add("short of cable: refused with " + drum.stored() + " on the drum, anchor kept");

				player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(CableContent.CABLE_COIL));
				CableGameTests.useBlock(helper, player, DRUM, Direction.UP);
				player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
				helper.setBlock(new BlockPos(11, 3, 4), Blocks.STONE);
				close(helper, player, ANCHORS[2]);
				helper.assertFalse(drum.looped(), "the return strand would run through the stone at (11, 3, 4)");
				helper.setBlock(new BlockPos(11, 3, 4), Blocks.AIR);
				log.add("stone in the return path: refused");

				// A route that turns: back to the second anchor, then sideways to (15, 3, 8). Sneak-use needs empty hands.
				player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
				CableGameTests.useBlock(helper, player, ANCHORS[2], true);
				CableGameTests.useBlock(helper, player, ANCHORS[1], true);
				CableHook hook = CableGameTests.hook(helper, DRUM);
				player.interactOn(hook, InteractionHand.MAIN_HAND, hook.position());
				CableGameTests.useBlock(helper, player, side, false);
				CableGameTests.useBlock(helper, player, side, false);
				helper.assertValueEqual(drum.route().size(), 2, "route with a turn");
				close(helper, player, side);
				helper.assertFalse(drum.looped(), "a route that turns should not close");
				helper.assertValueEqual(player.getMainHandItem().getCount(), 1, "anchor kept after the turn refusal");
				log.add("route turning at the first anchor: refused");
				SimpleBlockTestLog.info("LOOP-03: {}", log);
				TestPlayers.remove(player);
			}))
			.thenSucceed();
	}

	// ---- The crank ----

	/** CRANK-01: the crank never loads a coil or dismantles; a drum with an open route does not haul; an empty one spins. */
	@GameTest(structure = YARD)
	public void crankLeavesTheOtherGesturesAlone(GameTestHelper helper) {
		ServerPlayer player = TestPlayers.create(helper, GameType.SURVIVAL, new BlockPos(3, 1, 6));
		CableDrumBlockEntity drum = line(helper, player, 1);
		List<String> log = new ArrayList<>();
		boolean[] failed = {false};
		helper.startSequence()
			.thenIdle(2)
			.thenExecute(CableGameTests.step(failed, () -> {
				player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(CableContent.CABLE_COIL));
				crank(helper, player, DRUM, false);
				helper.assertValueEqual(drum.stored(), 32, "cable after cranking with a coil in hand");
				helper.assertValueEqual(player.getMainHandItem().getCount(), 1, "coils in hand after cranking");
				player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
				route(helper, player, DRUM, ANCHORS);
				crank(helper, player, DRUM, true);
				helper.assertValueEqual(drum.route().size(), 3, "route after sneak-cranking (not wound back)");
			}))
			.thenIdle(1)
			.thenExecute(CableGameTests.step(failed, () -> {
				helper.assertValueEqual(drum.speed(), 0.0F, "an open route's drum speed while cranked");
				log.add("coil in hand: crank only; sneak on the crank: no dismantling; open route: locked");
				CableGameTests.useBlock(helper, player, DRUM, true);
				CableGameTests.useBlock(helper, player, DRUM, true);
				helper.assertFalse(drum.hasCable(), "sneak-use of the drum body should still dismantle");
				crank(helper, player, DRUM, false);
			}))
			.thenIdle(1)
			.thenExecute(CableGameTests.step(failed, () -> {
				helper.assertTrue(drum.speed() > 0.0F, "an empty drum's spool should turn with the crank");
				log.add("drum body sneak-use still dismantles; empty drum spins at " + drum.speed());
				SimpleBlockTestLog.info("CRANK-01: {}", log);
				TestPlayers.remove(player);
			}))
			.thenSucceed();
	}

	// ---- Hauling ----

	/**
	 * LOOP-04..06: on the level, holding the crank hauls a gripping cart along the working strand to
	 * the end, where it stops short of the wheel; letting go holds it; cranking backwards brings it back
	 * and it stops short of the drum.
	 */
	@GameTest(structure = YARD, maxTicks = 1200)
	public void crankHaulsACartAlongTheLoopAndBack(GameTestHelper helper) {
		ServerPlayer player = TestPlayers.create(helper, GameType.SURVIVAL, new BlockPos(3, 1, 6));
		CableDrumBlockEntity drum = line(helper, player, 2);
		Minecart[] cart = new Minecart[1];
		int[] ticks = {0};
		double[] marks = new double[4];
		List<String> log = new ArrayList<>();
		boolean[] failed = {false};
		helper.startSequence()
			.thenIdle(2)
			.thenExecute(CableGameTests.step(failed, () -> {
				route(helper, player, DRUM, ANCHORS);
				close(helper, player, ANCHORS[2]);
				helper.assertTrue(drum.looped(), "loop closed");
				player.getInventory().clearContent();
				cart[0] = rodCart(helper, 8.5);
			}))
			.thenIdle(5)
			.thenExecute(CableGameTests.step(failed, () -> {
				helper.assertTrue(ConnectingRods.isGripping(cart[0]), "the rod should grip the working strand");
				marks[0] = x(helper, cart[0]);
			}))
			.thenExecuteFor(400, CableGameTests.step(failed, () -> {
				if (ticks[0]++ % 4 == 0) {
					crank(helper, player, DRUM, false);
				}
			}))
			.thenExecute(CableGameTests.step(failed, () -> {
				marks[1] = x(helper, cart[0]);
				helper.assertTrue(marks[1] > 19.0 && marks[1] < 22.5, "hauled to the end, short of the wheel at x 23.5: at x " + marks[1]);
			}))
			.thenIdle(40)
			.thenExecute(CableGameTests.step(failed, () -> {
				double held = x(helper, cart[0]);
				helper.assertTrue(Math.abs(held - marks[1]) < 0.05, "with the crank let go the cart should stay put, moved " + (held - marks[1]));
				ticks[0] = 0;
			}))
			.thenExecuteFor(440, CableGameTests.step(failed, () -> {
				if (ticks[0]++ % 4 == 0) {
					crank(helper, player, DRUM, true);
				}
			}))
			.thenExecute(CableGameTests.step(failed, () -> {
				marks[2] = x(helper, cart[0]);
				helper.assertTrue(marks[2] < 6.0 && marks[2] > 2.5, "brought back, short of the drum at x 1.5: at x " + marks[2]);
			}))
			.thenIdle(40)
			.thenExecute(CableGameTests.step(failed, () -> {
				marks[3] = x(helper, cart[0]);
				helper.assertTrue(Math.abs(marks[3] - marks[2]) < 0.05, "held at the drum end, moved " + (marks[3] - marks[2]));
				log.add(String.format("start x %.2f, hauled to x %.2f (wheel at 23.5), held, back to x %.2f (drum at 1.5), held", marks[0], marks[1],
					marks[2]));
				SimpleBlockTestLog.info("LOOP-04..06: {}", log);
				cart[0].discard();
				TestPlayers.remove(player);
			}))
			.thenSucceed();
	}

	/**
	 * LOOP-07: up a rail climb (4 blocks of 45-degree rail) the haul is slower than on the level, and a
	 * loaded cart slower than an empty one.
	 */
	@GameTest(structure = YARD, maxTicks = 3000)
	public void haulingUphillIsSlowerForHeavierLoads(GameTestHelper helper) {
		ServerPlayer player = TestPlayers.create(helper, GameType.SURVIVAL, new BlockPos(3, 1, 7));
		for (int x = 2; x <= 8; x++) {
			rail(helper, new BlockPos(x, 1, 4), RailShape.EAST_WEST);
		}
		for (int i = 0; i < 4; i++) {
			for (int y = 0; y < i + 1; y++) {
				helper.setBlock(new BlockPos(9 + i, y, 4), Blocks.STONE);
			}
			rail(helper, new BlockPos(9 + i, 1 + i, 4), RailShape.ASCENDING_EAST);
		}
		for (int x = 13; x < 30; x++) {
			for (int y = 0; y < 4; y++) {
				helper.setBlock(new BlockPos(x, y, 4), Blocks.STONE);
			}
			rail(helper, new BlockPos(x, 5, 4), RailShape.EAST_WEST);
		}
		CableDrumBlockEntity drum = CableGameTests.windDrum(helper, player, DRUM, 2);
		// Over the climb's first block and the first level block past its top: the straight cable then
		// keeps 2.2-2.7 blocks above the rails all the way (anchors over the ramp's corners would not).
		BlockPos[] anchors = {new BlockPos(9, 3, 4), new BlockPos(13, 7, 4), new BlockPos(20, 7, 4), new BlockPos(27, 7, 4)};
		for (BlockPos anchor : anchors) {
			CableGameTests.hangAnchor(helper, player, anchor.above());
		}
		Minecart[] cart = new Minecart[1];
		int[] ticks = {0};
		int[] crossing = new int[3];
		int[] started = {-1};
		int[] phase = {0};
		List<String> log = new ArrayList<>();
		boolean[] failed = {false};
		helper.startSequence()
			.thenIdle(2)
			.thenExecute(CableGameTests.step(failed, () -> {
				route(helper, player, DRUM, anchors);
				close(helper, player, anchors[3]);
				helper.assertTrue(drum.looped(), "loop closed over the climb (" + drum.returnLength() + " blocks of return)");
				player.getInventory().clearContent();
				cart[0] = rodCart(helper, 5.5);
			}))
			.thenIdle(5)
			.thenExecuteFor(2400, CableGameTests.step(failed, () -> {
				// Phase 0: level run x 5.5 -> 8.5 timed as reference; phase 1: empty cart up the climb x 9 -> 13;
				// then back down; phase 2: the same climb loaded with 256 cobblestone.
				int tick = ticks[0]++;
				double x = x(helper, cart[0]);
				boolean down = phase[0] == 3;
				if (tick % 4 == 0) {
					crank(helper, player, DRUM, down);
				}
				switch (phase[0]) {
					case 0 -> {
						if (started[0] < 0 && x >= 5.5) {
							started[0] = tick;
						}
						if (x >= 8.5) {
							crossing[0] = tick - started[0];
							started[0] = -1;
							phase[0] = 1;
						}
					}
					case 1, 2 -> {
						if (started[0] < 0 && x >= 9.0) {
							started[0] = tick;
						}
						if (x >= 13.0 && started[0] >= 0) {
							crossing[phase[0]] = tick - started[0];
							started[0] = -1;
							if (phase[0] == 1) {
								phase[0] = 3;
							} else {
								phase[0] = 4;
							}
						}
					}
					case 3 -> {
						if (x <= 7.5) {
							MinecartCargo.cart(cart[0]).simpleBlock$setCargo(Items.COBBLESTONE, 256);
							phase[0] = 2;
						}
					}
					default -> {
					}
				}
			}))
			.thenExecute(CableGameTests.step(failed, () -> {
				helper.assertValueEqual(phase[0], 4, "phases completed (level, empty climb, back down, loaded climb)");
				double level = crossing[0] / 3.0;
				double empty = crossing[1] / 4.0;
				double loaded = crossing[2] / 4.0;
				helper.assertTrue(empty > level * 1.5, "an empty cart should climb clearly slower than it runs on the level: " + empty + " vs " + level
					+ " ticks a block");
				helper.assertTrue(loaded > empty * 1.5, "a loaded cart should climb clearly slower than an empty one: " + loaded + " vs " + empty
					+ " ticks a block");
				log.add(String.format("ticks per block: level %.1f, empty climb %.1f, loaded climb (256 cobblestone) %.1f", level, empty, loaded));
				SimpleBlockTestLog.info("LOOP-07: {}", log);
				cart[0].discard();
				TestPlayers.remove(player);
			}))
			.thenSucceed();
	}

	/** LOOP-08: breaking the loop while a cart is being hauled stops the cart where it is; nothing runs away. */
	@GameTest(structure = YARD, maxTicks = 600)
	public void breakingTheLoopStopsTheCart(GameTestHelper helper) {
		ServerPlayer player = TestPlayers.create(helper, GameType.SURVIVAL, new BlockPos(3, 1, 6));
		CableDrumBlockEntity drum = line(helper, player, 2);
		Minecart[] cart = new Minecart[1];
		int[] ticks = {0};
		double[] at = new double[1];
		boolean[] failed = {false};
		helper.startSequence()
			.thenIdle(2)
			.thenExecute(CableGameTests.step(failed, () -> {
				route(helper, player, DRUM, ANCHORS);
				close(helper, player, ANCHORS[2]);
				player.getInventory().clearContent();
				cart[0] = rodCart(helper, 8.5);
			}))
			.thenExecuteFor(80, CableGameTests.step(failed, () -> {
				if (ticks[0]++ % 4 == 0) {
					crank(helper, player, DRUM, false);
				}
			}))
			.thenExecute(CableGameTests.step(failed, () -> {
				helper.assertTrue(x(helper, cart[0]) > 9.5, "the cart should be moving along, at x " + x(helper, cart[0]));
				crank(helper, player, DRUM, false);
				player.gameMode.destroyBlock(helper.absolutePos(ANCHORS[1]));
				helper.assertFalse(drum.looped(), "breaking an anchor should open the loop");
			}))
			.thenIdle(2)
			.thenExecute(CableGameTests.step(failed, () -> at[0] = x(helper, cart[0])))
			.thenIdle(60)
			.thenExecute(CableGameTests.step(failed, () -> {
				double moved = x(helper, cart[0]) - at[0];
				helper.assertTrue(Math.abs(moved) < 0.1, "the cart should have stopped, it moved " + moved);
				assertCableKept(helper, drum, 2, "after the break");
				SimpleBlockTestLog.info("LOOP-08: anchor broken while hauling at x {}: loop opened, cart stopped (moved {} in 60 ticks), cable kept",
					String.format("%.2f", at[0]), String.format("%.3f", moved));
				CableGameTests.itemsNear(helper, ANCHORS[1]).forEach(ItemEntity::discard);
				cart[0].discard();
				TestPlayers.remove(player);
			}))
			.thenSucceed();
	}

	/**
	 * LOOP-11: a rod cart hauls the carts coupled to it (M05 couplings), a loaded freight cart ahead of
	 * it and an empty one behind, out and back, keeping their spacing; let go, the whole train holds.
	 */
	@GameTest(structure = YARD, maxTicks = 1200)
	public void loopHaulsACoupledTrain(GameTestHelper helper) {
		ServerPlayer player = TestPlayers.create(helper, GameType.SURVIVAL, new BlockPos(3, 1, 6));
		CableDrumBlockEntity drum = line(helper, player, 2);
		Minecart[] train = new Minecart[3];
		int[] ticks = {0};
		double[][] marks = new double[3][3];
		boolean[] failed = {false};
		helper.startSequence()
			.thenIdle(2)
			.thenExecute(CableGameTests.step(failed, () -> {
				route(helper, player, DRUM, ANCHORS);
				close(helper, player, ANCHORS[2]);
				player.getInventory().clearContent();
				train[0] = TrainGameTests.cart(helper, 6.9, 4);
				train[1] = rodCart(helper, 8.5);
				train[2] = TrainGameTests.cart(helper, 10.1, 4);
				MinecartCargo.cart(train[2]).simpleBlock$setCargo(Items.RAW_COPPER, 128);
				TrainGameTests.couple(player, train[0], train[1]);
				TrainGameTests.couple(player, train[1], train[2]);
			}))
			.thenIdle(5)
			.thenExecute(CableGameTests.step(failed, () -> {
				helper.assertTrue(ConnectingRods.isGripping(train[1]), "the rod should grip the working strand");
				for (int i = 0; i < 3; i++) {
					marks[0][i] = x(helper, train[i]);
				}
			}))
			.thenExecuteFor(160, CableGameTests.step(failed, () -> {
				if (ticks[0]++ % 4 == 0) {
					crank(helper, player, DRUM, false);
				}
			}))
			.thenIdle(10)
			.thenExecute(CableGameTests.step(failed, () -> {
				for (int i = 0; i < 3; i++) {
					marks[1][i] = x(helper, train[i]);
					helper.assertTrue(marks[1][i] - marks[0][i] > 3.0, "car " + i + " should be hauled out: x " + marks[0][i] + " -> " + marks[1][i]);
				}
				assertSpacing(helper, train, "hauled out");
			}))
			.thenIdle(30)
			.thenExecute(CableGameTests.step(failed, () -> {
				for (int i = 0; i < 3; i++) {
					helper.assertTrue(Math.abs(x(helper, train[i]) - marks[1][i]) < 0.05, "car " + i + " should hold with the crank let go");
				}
				ticks[0] = 0;
			}))
			.thenExecuteFor(160, CableGameTests.step(failed, () -> {
				if (ticks[0]++ % 4 == 0) {
					crank(helper, player, DRUM, true);
				}
			}))
			.thenIdle(10)
			.thenExecute(CableGameTests.step(failed, () -> {
				for (int i = 0; i < 3; i++) {
					marks[2][i] = x(helper, train[i]);
					helper.assertTrue(marks[1][i] - marks[2][i] > 3.0, "car " + i + " should be hauled back: x " + marks[1][i] + " -> " + marks[2][i]);
				}
				assertSpacing(helper, train, "hauled back");
				SimpleBlockTestLog.info("LOOP-11: coupled train (empty, rod cart, 128 raw copper): out x {} -> {}, held, back -> {}",
					String.format("%.2f/%.2f/%.2f", marks[0][0], marks[0][1], marks[0][2]), String.format("%.2f/%.2f/%.2f", marks[1][0], marks[1][1],
						marks[1][2]), String.format("%.2f/%.2f/%.2f", marks[2][0], marks[2][1], marks[2][2]));
				for (Minecart cart : train) {
					cart.discard();
				}
				TestPlayers.remove(player);
			}))
			.thenSucceed();
	}

	private static void assertSpacing(GameTestHelper helper, Minecart[] train, String when) {
		for (int i = 1; i < train.length; i++) {
			double gap = train[i].distanceTo(train[i - 1]);
			helper.assertTrue(gap > 1.2 && gap < 2.2, "cars " + (i - 1) + "-" + i + " " + when + " are " + gap + " apart");
		}
	}

	/** LOOP-12: breaking a looped drum gives back all its cable (wound, laid out and return) and the wheel's anchor, and frees the anchors. */
	@GameTest(structure = YARD)
	public void breakingALoopedDrumGivesAllTheCableBack(GameTestHelper helper) {
		ServerPlayer player = TestPlayers.create(helper, GameType.SURVIVAL, new BlockPos(3, 1, 6));
		CableDrumBlockEntity drum = line(helper, player, 2);
		boolean[] failed = {false};
		helper.startSequence()
			.thenIdle(2)
			.thenExecute(CableGameTests.step(failed, () -> {
				route(helper, player, DRUM, ANCHORS);
				close(helper, player, ANCHORS[2]);
				helper.assertTrue(drum.looped(), "loop closed");
				player.gameMode.destroyBlock(helper.absolutePos(DRUM));
				helper.assertTrue(helper.getLevel().getBlockEntity(helper.absolutePos(DRUM)) == null, "the drum should be broken");
			}))
			.thenIdle(2)
			.thenExecute(CableGameTests.step(failed, () -> {
				List<ItemEntity> atDrum = CableGameTests.itemsNear(helper, DRUM);
				int blocks = CableGameTests.count(atDrum, CableContent.CABLE_COIL) * CableContent.BLOCKS_PER_COIL
					+ CableGameTests.count(atDrum, CableContent.CABLE) * CableContent.BLOCKS_PER_CABLE;
				helper.assertValueEqual(blocks, 2 * CableContent.BLOCKS_PER_COIL, "blocks of cable dropped by the looped drum");
				List<ItemEntity> atWheel = CableGameTests.itemsNear(helper, ANCHORS[2]);
				helper.assertValueEqual(CableGameTests.count(atWheel, CableContent.CABLE_ANCHOR_ITEM), 1, "the wheel's anchor dropped");
				for (BlockPos anchor : ANCHORS) {
					helper.assertValueEqual(returnState(helper, anchor), CableAnchorBlock.Return.NONE, "return parts on " + anchor);
					helper.assertTrue(CableGameTests.anchor(helper, anchor).drum() == null, "anchor " + anchor + " should be free");
				}
				SimpleBlockTestLog.info("LOOP-12: looped drum broken: {} blocks of cable dropped ({} coils, {} cables), wheel anchor dropped, anchors free",
					blocks, CableGameTests.count(atDrum, CableContent.CABLE_COIL), CableGameTests.count(atDrum, CableContent.CABLE));
				atDrum.forEach(ItemEntity::discard);
				atWheel.forEach(ItemEntity::discard);
				TestPlayers.remove(player);
			}))
			.thenSucceed();
	}

	/** LOOP-09: the loop is saved with the drum: loaded again, it is still closed with its return cable. */
	@GameTest(structure = YARD)
	public void loopIsSavedWithTheDrum(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		ServerPlayer player = TestPlayers.create(helper, GameType.SURVIVAL, new BlockPos(3, 1, 6));
		CableDrumBlockEntity drum = line(helper, player, 2);
		boolean[] failed = {false};
		helper.startSequence()
			.thenIdle(2)
			.thenExecute(CableGameTests.step(failed, () -> {
				route(helper, player, DRUM, ANCHORS);
				close(helper, player, ANCHORS[2]);
				CompoundTag saved = drum.saveWithFullMetadata(level.registryAccess());
				BlockState state = level.getBlockState(helper.absolutePos(DRUM));
				level.setBlock(helper.absolutePos(DRUM), Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL | Block.UPDATE_SKIP_BLOCK_ENTITY_SIDEEFFECTS);
				level.setBlock(helper.absolutePos(DRUM), state, Block.UPDATE_ALL);
				CableGameTests.drum(helper, DRUM).loadWithComponents(TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), saved));
			}))
			.thenIdle(3)
			.thenExecute(CableGameTests.step(failed, () -> {
				CableDrumBlockEntity loaded = CableGameTests.drum(helper, DRUM);
				helper.assertTrue(loaded.looped(), "loop after loading");
				helper.assertValueEqual(loaded.route().size(), 3, "route after loading");
				assertCableKept(helper, loaded, 2, "after loading");
				helper.assertValueEqual(CableGameTests.hooks(helper, DRUM).size(), 0, "loose ends after loading a loop");
				SimpleBlockTestLog.info("LOOP-09: loop saved and loaded: closed, {} return, {} laid out, {} wound, no loose end", loaded.returnLength(),
					loaded.laidOut(), loaded.stored());
				TestPlayers.remove(player);
			}))
			.thenSucceed();
	}

	/** CRANK-02: a drum placed against a plank's underside hangs from it, lets its cable out at roller height, and falls when the plank goes. */
	@GameTest(structure = YARD)
	public void drumHangsUnderABeam(GameTestHelper helper) {
		ServerPlayer player = TestPlayers.create(helper, GameType.SURVIVAL, new BlockPos(3, 1, 6));
		BlockPos beam = new BlockPos(2, 4, 4);
		helper.setBlock(beam, Blocks.OAK_PLANKS);
		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(CableContent.CABLE_DRUM_ITEM));
		CableGameTests.useBlock(helper, player, beam, Direction.DOWN);
		BlockPos at = beam.below();
		helper.assertBlockPresent(CableContent.CABLE_DRUM, at);
		helper.assertTrue(helper.getBlockState(at).getValue(CableDrumBlock.HANGING), "a drum placed under a plank should hang");
		CableDrumBlockEntity drum = CableGameTests.drum(helper, at);
		double exit = drum.exitPoint().y - helper.absolutePos(at).getY();
		helper.assertTrue(Math.abs(exit - 11.0 / 16.0) < 1.0E-6, "a hanging drum's cable should leave at 11/16, was " + exit);
		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(CableContent.CABLE_COIL));
		CableGameTests.useBlock(helper, player, at, Direction.NORTH);
		helper.assertValueEqual(drum.stored(), 32, "cable wound onto the hanging drum");
		player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
		crank(helper, player, at, false);
		helper.setBlock(beam, Blocks.AIR);
		helper.assertBlockNotPresent(CableContent.CABLE_DRUM, at);
		List<ItemEntity> items = CableGameTests.itemsNear(helper, at);
		helper.assertValueEqual(CableGameTests.count(items, CableContent.CABLE_DRUM_ITEM), 1, "drums dropped when the plank went");
		helper.assertValueEqual(CableGameTests.count(items, CableContent.CABLE_COIL), 1, "coils dropped with it");
		SimpleBlockTestLog.info("CRANK-02: hanging drum placed under a plank, cable exit at {}/16, coil wound on, cranked; plank removed: drum and coil dropped",
			exit * 16);
		items.forEach(ItemEntity::discard);
		TestPlayers.remove(player);
		helper.succeed();
	}

	/** LOOP-10: a cart's rod grips the working strand only: the loop's return points are not in the grip index. */
	@GameTest(structure = YARD)
	public void returnStrandIsNeverGripped(GameTestHelper helper) {
		ServerPlayer player = TestPlayers.create(helper, GameType.SURVIVAL, new BlockPos(3, 1, 6));
		CableDrumBlockEntity drum = line(helper, player, 2);
		boolean[] failed = {false};
		helper.startSequence()
			.thenIdle(2)
			.thenExecute(CableGameTests.step(failed, () -> {
				route(helper, player, DRUM, ANCHORS);
				close(helper, player, ANCHORS[2]);
				List<Vec3> returnPoints = CableLoops.returnPoints(drum, Direction.EAST);
				for (CableIndex.Span span : CableIndex.spans(helper.getLevel())) {
					for (Vec3 point : returnPoints) {
						helper.assertFalse(span.from().distanceTo(point) < 1.0E-6 || span.to().distanceTo(point) < 1.0E-6,
							"a return strand point is in the grip index: " + point);
					}
				}
				// Under the return strand (offset to the north), a rod still finds the working strand.
				Vec3 base = helper.absoluteVec(new Vec3(11.5, 1.0625, 4.5 - CableLoops.RETURN_OFFSET));
				CableIndex.Grip grip = CableIndex.grip(helper.getLevel(), base, ConnectingRods.TOLERANCE, ConnectingRods.reachMin(),
					ConnectingRods.reachMax());
				helper.assertTrue(grip != null && grip.direction().x > 0.99, "the grip under the return strand should be on the working strand, east");
				SimpleBlockTestLog.info("LOOP-10: {} spans indexed, none on the return strand; a rod under the return strand grips the working strand",
					CableIndex.spans(helper.getLevel()).size());
				TestPlayers.remove(player);
			}))
			.thenSucceed();
	}
}
