package io.github.boxofdog.simpleblock.gametest;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.vehicle.minecart.Minecart;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.RailShape;
import net.minecraft.world.phys.Vec3;

import net.fabricmc.fabric.api.gametest.v1.GameTest;

import io.github.boxofdog.simpleblock.cable.CableAnchorBlock;
import io.github.boxofdog.simpleblock.cable.CableAnchorBlockEntity;
import io.github.boxofdog.simpleblock.cable.CableContent;
import io.github.boxofdog.simpleblock.cable.CableDrumBlockEntity;
import io.github.boxofdog.simpleblock.cable.CableHook;
import io.github.boxofdog.simpleblock.cable.CableLines;
import io.github.boxofdog.simpleblock.cable.ConnectingRods;

/**
 * M06 section 7: a long cable line, well beyond the old 128-block cable budget, on one coil, with its
 * drum and its far end out of entity-ticking chunks while the carts being hauled are in them.
 *
 * <p>The line runs east along z -8, just outside the test structure ({@code cable_yard}; a test area is
 * walled with barriers, which a cable may not pass), at anchor height y 3 (planks at y 4), anchors every
 * 10 blocks. Its drum stands about 200 blocks west of the structure and its far end about 190 blocks
 * east of it, so with the stand-in player at the structure (simulation distance 10) both
 * are outside entity-ticking chunks: the drum's block entity does not tick at all. The test runs in
 * its own environment ({@code long_line}), so in a batch of its own, with no other test's area near
 * the line. Test ids refer to the matrix in {@code docs/MILESTONE_06_REPORT.md}.
 */
public class LongLineGameTests {
	private static final String YARD = "simple_block-gametest:cable_yard";
	private static final String ENVIRONMENT = "simple_block-gametest:long_line";
	private static final BlockPos DRUM = new BlockPos(-200, 1, -8);
	private static final int FIRST_ANCHOR_X = -194;
	private static final int LAST_ANCHOR_X = 186;
	private static final int Z = -8;

	private static List<BlockPos> anchors() {
		List<BlockPos> anchors = new ArrayList<>();
		for (int x = FIRST_ANCHOR_X; x <= LAST_ANCHOR_X; x += 10) {
			anchors.add(new BlockPos(x, 3, Z));
		}
		return anchors;
	}

	private static boolean ticking(GameTestHelper helper, BlockPos relative) {
		return helper.getLevel().isPositionEntityTicking(helper.absolutePos(relative));
	}

	private static CableLines.Line line(GameTestHelper helper) {
		CableLines.Line line = CableLines.line(helper.getLevel(), helper.absolutePos(DRUM));
		if (line == null) {
			throw new AssertionError("no cable line for the drum");
		}
		return line;
	}

	private static void rails(GameTestHelper helper, int fromX, int toX) {
		for (int x = fromX; x <= toX; x++) {
			CableLoopGameTests.rail(helper, new BlockPos(x, 1, Z), RailShape.EAST_WEST);
		}
	}

	private static Minecart rodCart(GameTestHelper helper, double x) {
		Minecart cart = helper.spawn(EntityTypes.MINECART, new Vec3(x, 1.0625, Z + 0.5));
		cart.setDeltaMovement(Vec3.ZERO);
		ConnectingRods.fitted(cart).simpleBlock$setRod(true);
		return cart;
	}

	/**
	 * The loaded working section: the chunks along the line from x -16 to 48 are kept entity-ticking,
	 * as a player there would keep them (the stand-in player's own chunks do not follow it). Everything
	 * else on the line (the drum 200 blocks west, the far end) is left to the game.
	 */
	private static void loadSection(GameTestHelper helper, boolean load) {
		for (int x = -16; x <= 48; x += 16) {
			BlockPos at = helper.absolutePos(new BlockPos(x, 1, Z));
			helper.getLevel().setChunkForced(at.getX() >> 4, at.getZ() >> 4, load);
		}
	}

	/** One click on the drum's lever (where the crank was): the crank helper's right-click. */
	private static void lever(GameTestHelper helper, ServerPlayer player) {
		CableLoopGameTests.crank(helper, player, DRUM, true);
	}

	/**
	 * LONG-01..05: one coil lays a 386-block line through 39 anchors and closes it into a loop for
	 * nothing more; with the drum and the far end in chunks that do not tick, a coal winch (lever set
	 * from a distance) hauls a rod cart in the loaded part and stops it at the end of its rails, and a
	 * second cart at the edge of the loaded world, each time putting the lever back to NEUTRAL and
	 * burning coal only while a cart moved; nothing restarts by itself. Dismantling gives back the one coil.
	 */
	@GameTest(structure = YARD, environment = ENVIRONMENT, maxTicks = 1600)
	public void longLineWorksWhereItIsLoaded(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		ServerPlayer player = TestPlayers.create(helper, GameType.SURVIVAL, new BlockPos(3, 1, -6));
		List<BlockPos> anchors = anchors();
		Minecart[] carts = new Minecart[2];
		int[] marks = new int[4];
		double[] xs = new double[6];
		int[] boundary = {0};
		int[] ticks = {0};
		List<String> log = new ArrayList<>();
		boolean[] failed = {false};
		helper.startSequence()
			.thenExecute(CableGameTests.step(failed, () -> {
				// Setup only: the test forces the drum's chunk so the drum ticks long enough to put out its loose
				// end (the stand-in player's chunks follow it only at spawn). It is unforced before any haul.
				BlockPos drumAt = helper.absolutePos(DRUM);
				level.setChunkForced(drumAt.getX() >> 4, drumAt.getZ() >> 4, true);
				player.getInventory().clearContent();
				CableDrumBlockEntity drum = CableGameTests.windDrum(helper, player, DRUM, 1);
				helper.assertTrue(drum.hasCoil(), "one coil fitted");
				for (BlockPos anchor : anchors) {
					CableGameTests.hangAnchor(helper, player, anchor.above());
				}
			}))
			.thenIdle(40)
			.thenExecute(CableGameTests.step(failed, () -> {
				CableDrumBlockEntity drum = CableGameTests.drum(helper, DRUM);
				// Stand by the drum to carry its end (a held end is found near its holder).
				CableGameTests.teleport(helper, player, DRUM.east(2));
				CableHook hook = CableGameTests.hook(helper, DRUM);
				player.interactOn(hook, InteractionHand.MAIN_HAND, hook.position());
				for (BlockPos anchor : anchors) {
					CableGameTests.useBlock(helper, player, anchor, false);
				}
				helper.assertValueEqual(drum.route().size(), anchors.size(), "anchors routed on one coil");
				helper.assertTrue(drum.laidOut() > 3 * 128, "route length " + drum.laidOut() + " blocks: well beyond the old 128-block drum");
				CableLoopGameTests.close(helper, player, anchors.getLast());
				helper.assertTrue(drum.looped(), "the long route should close into a loop");
				helper.assertTrue(player.getInventory().isEmpty(), "closing used only the anchor: no cable, no coil");
				helper.assertTrue(drum.hasCoil(), "still the one coil");
				log.add(String.format("one coil: %d anchors, %d blocks, closed into a loop for one anchor", anchors.size(), drum.laidOut()));

				// The coal winch: furnace and two coal, from a distance later is fine (the drum need not tick).
				player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.FURNACE));
				CableGameTests.useBlock(helper, player, DRUM, Direction.UP);
				player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.COAL, 2));
				CableGameTests.useBlock(helper, player, DRUM, Direction.UP);
				CableGameTests.useBlock(helper, player, DRUM, Direction.UP);
				player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
				helper.assertTrue(drum.powered() && line(helper).coal() == 2, "coal winch with 2 coal: powered " + drum.powered() + ", coal " + line(helper).coal());

				rails(helper, 2, 28);
				loadSection(helper, true);
				BlockPos drumAt = helper.absolutePos(DRUM);
				level.setChunkForced(drumAt.getX() >> 4, drumAt.getZ() >> 4, false);
				CableGameTests.teleport(helper, player, new BlockPos(3, 1, -6));
			}))
			.thenIdle(40)
			.thenWaitUntil(() -> {
				// Forcing again is harmless and keeps the section loaded should anything else release its chunks.
				loadSection(helper, true);
				helper.assertTrue(ticking(helper, new BlockPos(8, 1, Z)) && ticking(helper, new BlockPos(28, 1, Z)), "section loading: x 8 "
				+ level.isLoaded(helper.absolutePos(new BlockPos(8, 1, Z))) + "/" + ticking(helper, new BlockPos(8, 1, Z)) + ", x 28 "
				+ level.isLoaded(helper.absolutePos(new BlockPos(28, 1, Z))) + "/" + ticking(helper, new BlockPos(28, 1, Z)));
			})
			.thenExecute(CableGameTests.step(failed, () -> {
				helper.assertFalse(ticking(helper, DRUM), "the drum's chunk should not be entity-ticking with the player 200 blocks away");
				helper.assertFalse(ticking(helper, anchors.getLast()), "the far end should not be entity-ticking");
				carts[0] = rodCart(helper, 8.5);
			}))
			.thenIdle(5)
			.thenExecute(CableGameTests.step(failed, () -> {
				helper.assertTrue(ConnectingRods.isGripping(carts[0]), "the rod should grip the working strand of a line whose drum is not ticking");
				xs[0] = CableLoopGameTests.x(helper, carts[0]);
				// NEUTRAL -> PULL -> FEED: the second click reverses straight away.
				lever(helper, player);
				helper.assertValueEqual(line(helper).lever(), CableLines.Lever.PULL, "lever after one click");
				lever(helper, player);
				helper.assertValueEqual(line(helper).lever(), CableLines.Lever.FEED, "lever after two clicks");
			}))
			.thenExecuteFor(300, CableGameTests.step(failed, () -> {
				if (line(helper).lever() == CableLines.Lever.NEUTRAL && marks[0] == 0) {
					marks[0] = ++ticks[0];
				} else if (marks[0] == 0) {
					ticks[0]++;
				}
			}))
			.thenExecute(CableGameTests.step(failed, () -> {
				CableLines.Line line = line(helper);
				xs[1] = CableLoopGameTests.x(helper, carts[0]);
				helper.assertValueEqual(line.lever(), CableLines.Lever.NEUTRAL, "the lever should go back to NEUTRAL by itself at the end of the rails");
				helper.assertTrue(xs[1] - xs[0] > 10.0, "fed east with the drum unloaded: x " + xs[0] + " -> " + xs[1]);
				helper.assertValueEqual(line.coal(), 1, "coal left (one lit)");
				marks[1] = line.burn();
				helper.assertTrue(marks[1] < 1600 && marks[1] > 1600 - ticks[0], "burn left " + marks[1] + " after " + ticks[0] + " ticks running");
				log.add(String.format("drum and far end not ticking; FEED: cart x %.2f -> %.2f, lever back to NEUTRAL (%s) after %d ticks, burnt %d of 1600",
					xs[0], xs[1], line.lastStop(), ticks[0], 1600 - marks[1]));
			}))
			.thenIdle(40)
			.thenExecute(CableGameTests.step(failed, () -> {
				helper.assertValueEqual(line(helper).burn(), marks[1], "no coal burns while stopped");
				helper.assertTrue(Math.abs(CableLoopGameTests.x(helper, carts[0]) - xs[1]) < 0.05, "a stopped cart stays put");
				helper.assertValueEqual(line(helper).lever(), CableLines.Lever.NEUTRAL, "nothing restarts by itself");
				carts[0].discard();
				// The edge of the loaded world, east of the structure.
				for (int x = 30; x < 400; x++) {
					if (!ticking(helper, new BlockPos(x, 1, Z))) {
						boundary[0] = x;
						break;
					}
				}
				helper.assertTrue(boundary[0] > 20 && boundary[0] < LAST_ANCHOR_X - 10, "entity-ticking edge at x " + boundary[0]);
				rails(helper, boundary[0] - 16, boundary[0] + 4);
				carts[1] = rodCart(helper, boundary[0] - 13.5);
			}))
			.thenIdle(5)
			.thenExecute(CableGameTests.step(failed, () -> {
				helper.assertTrue(ConnectingRods.isGripping(carts[1]), "the second rod cart should grip");
				xs[2] = CableLoopGameTests.x(helper, carts[1]);
				marks[2] = line(helper).burn();
				lever(helper, player);
				lever(helper, player);
				helper.assertValueEqual(line(helper).lever(), CableLines.Lever.FEED, "FEED again");
			}))
			.thenIdle(200)
			.thenExecute(CableGameTests.step(failed, () -> {
				CableLines.Line line = line(helper);
				xs[3] = CableLoopGameTests.x(helper, carts[1]);
				helper.assertValueEqual(line.lever(), CableLines.Lever.NEUTRAL, "stopped at the edge of the loaded world");
				helper.assertValueEqual(line.lastStop(), "unloaded", "why the lever went back");
				helper.assertTrue(xs[3] - xs[2] > 5.0 && xs[3] < boundary[0], "hauled up to the edge (x " + boundary[0] + "): x " + xs[2] + " -> " + xs[3]);
				marks[3] = line.burn();
				log.add(String.format("edge of entity-ticking chunks at x %d: cart x %.2f -> %.2f, lever NEUTRAL (unloaded)", boundary[0], xs[2], xs[3]));
			}))
			.thenIdle(40)
			.thenExecute(CableGameTests.step(failed, () -> {
				helper.assertTrue(Math.abs(CableLoopGameTests.x(helper, carts[1]) - xs[3]) < 0.05, "held at the edge");
				helper.assertValueEqual(line(helper).burn(), marks[3], "no coal burns while held at the edge");
				carts[1].discard();

				// Dismantle from a distance: open the loop, wind the route back, take the coil off.
				player.getInventory().clearContent();
				CableGameTests.useBlock(helper, player, DRUM, true);
				int wheels = MinecartCargoGameTests.carried(player, CableContent.CABLE_ANCHOR_ITEM);
				// The wheel's anchor came back into the hand; sneak-use with something in hand would not reach the drum.
				player.getInventory().clearContent();
				CableGameTests.useBlock(helper, player, DRUM, true);
				CableGameTests.useBlock(helper, player, DRUM, true);
				CableDrumBlockEntity drum = CableGameTests.drum(helper, DRUM);
				helper.assertFalse(drum.hasCoil() || drum.looped() || !drum.route().isEmpty(), "dismantled: coil " + drum.hasCoil() + ", looped "
					+ drum.looped() + ", route " + drum.route().size());
				helper.assertValueEqual(MinecartCargoGameTests.carried(player, CableContent.CABLE_COIL), 1, "coils back from a 386-block line");
				helper.assertValueEqual(MinecartCargoGameTests.carried(player, CableContent.CABLE), 0, "cables back (never made from length)");
				helper.assertValueEqual(wheels, 1, "the wheel's anchor back (the wheel is far away and unloaded)");
				helper.assertTrue(line(helper).points().isEmpty(), "the line is empty");
				helper.assertTrue(CableGameTests.anchor(helper, anchors.get(20)).drum() == null, "a loaded anchor is freed at once");
				// Far anchors were not loaded: each clears its stale record when it next ticks (here, called directly).
				for (BlockPos anchor : List.of(anchors.getFirst(), anchors.getLast())) {
					BlockPos at = helper.absolutePos(anchor);
					CableAnchorBlockEntity entity = CableGameTests.anchor(helper, anchor);
					entity.reconcile(level);
					helper.assertTrue(entity.drum() == null && !entity.wheel(), "far anchor " + anchor + " reconciled on loading");
					helper.assertValueEqual(level.getBlockState(at).getValue(CableAnchorBlock.RETURN), CableAnchorBlock.Return.NONE, "return parts on " + anchor);
				}
				log.add("dismantled: 1 coil, 0 cable, the wheel's anchor; anchors freed near and far");
				loadSection(helper, false);
				SimpleBlockTestLog.info("LONG-01..05: {}", log);
				TestPlayers.remove(player);
			}))
			.thenSucceed();
	}
}
