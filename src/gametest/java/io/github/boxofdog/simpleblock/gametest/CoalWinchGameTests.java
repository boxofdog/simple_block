package io.github.boxofdog.simpleblock.gametest;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.vehicle.minecart.Minecart;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import net.fabricmc.fabric.api.gametest.v1.GameTest;

import io.github.boxofdog.simpleblock.cable.CableContent;
import io.github.boxofdog.simpleblock.cable.CableDrive;
import io.github.boxofdog.simpleblock.cable.CableDrumBlock;
import io.github.boxofdog.simpleblock.cable.CableDrumBlockEntity;
import io.github.boxofdog.simpleblock.cable.CableLines;
import io.github.boxofdog.simpleblock.cable.ConnectingRods;
import io.github.boxofdog.simpleblock.train.Couplings;

/**
 * M06 work package 3: the coal winch, a furnace fitted to an existing cable drum. Blocks are used
 * through {@code ServerPlayerGameMode.useItemOn}, the method the server runs for a player's
 * use-on-block packet. Test ids refer to the matrix in {@code docs/MILESTONE_06_REPORT.md}.
 *
 * <p>The line is {@link CableLoopGameTests#line}: a drum at x 1, anchors at x 7, 15 and 23, rails along
 * z 4 at y 1, closed into a loop (the working strand runs east from the drum).
 */
public class CoalWinchGameTests {
	private static final String YARD = "simple_block-gametest:cable_yard";
	private static final BlockPos DRUM = CableLoopGameTests.DRUM;

	private static ServerLevel level(GameTestHelper helper) {
		return helper.getLevel();
	}

	/** A right-click on the drum's lever (where the crank was), with an empty hand. */
	static void lever(GameTestHelper helper, ServerPlayer player) {
		BlockPos pos = helper.absolutePos(DRUM);
		BlockState state = helper.getLevel().getBlockState(pos);
		Direction side = CableDrumBlock.crankSide(state);
		Vec3 hit = Vec3.atLowerCornerOf(pos).add(0.5 + side.getStepX() * 0.55, 9.5 / 16.0, 0.5 + side.getStepZ() * 0.55);
		player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
		player.gameMode.useItemOn(player, helper.getLevel(), ItemStack.EMPTY, InteractionHand.MAIN_HAND, new BlockHitResult(hit, side, pos, false));
	}

	/** A right-click on the drum's body (its top) with {@code held}. */
	static void body(GameTestHelper helper, ServerPlayer player, ItemStack held) {
		player.setItemInHand(InteractionHand.MAIN_HAND, held);
		CableGameTests.useBlock(helper, player, DRUM, Direction.UP);
	}

	static CableLines.Line line(GameTestHelper helper) {
		CableLines.Line line = CableLines.line(helper.getLevel(), helper.absolutePos(DRUM));
		if (line == null) {
			throw helper.assertionException(net.minecraft.network.chat.Component.literal("no cable line"));
		}
		return line;
	}

	/**
	 * The yard line's drum (laid with {@link CableLoopGameTests#line} at least a tick before, so its
	 * loose end is out) closed into a loop, with a furnace fitted and {@code coal} coal in it.
	 */
	static CableDrumBlockEntity winch(GameTestHelper helper, ServerPlayer player, int coal) {
		CableDrumBlockEntity drum = CableGameTests.drum(helper, DRUM);
		CableLoopGameTests.route(helper, player, DRUM, CableLoopGameTests.ANCHORS);
		CableLoopGameTests.close(helper, player, CableLoopGameTests.ANCHORS[2]);
		player.getInventory().clearContent();
		body(helper, player, new ItemStack(Items.FURNACE));
		if (coal > 0) {
			body(helper, player, new ItemStack(Items.COAL, coal));
			for (int i = 1; i < coal; i++) {
				body(helper, player, player.getMainHandItem());
			}
		}
		player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
		return drum;
	}

	/**
	 * WIN-01: a furnace used on a drum with a route fits it as a coal winch, keeping the route and the
	 * coil; the lever will not run an open route; on the loop, right-clicks on the lever go NEUTRAL,
	 * PULL, FEED, NEUTRAL; coal used on the body goes in one at a time (charcoal does not); an empty hand
	 * on the body only reports, never moving the lever; the old crank gestures do nothing. Broken, the
	 * drum gives back the furnace and its coal.
	 */
	@GameTest(structure = YARD)
	public void furnaceTurnsADrumIntoACoalWinch(GameTestHelper helper) {
		ServerPlayer player = TestPlayers.create(helper, GameType.SURVIVAL, new BlockPos(3, 1, 6));
		CableDrumBlockEntity drum = CableLoopGameTests.line(helper, player, 1);
		List<String> log = new ArrayList<>();
		boolean[] failed = {false};
		helper.startSequence()
			.thenIdle(2)
			.thenExecute(CableGameTests.step(failed, () -> {
				CableLoopGameTests.route(helper, player, DRUM, CableLoopGameTests.ANCHORS);
				player.getInventory().clearContent();
				int laid = drum.laidOut();
				body(helper, player, new ItemStack(Items.FURNACE));
				helper.assertTrue(drum.powered(), "fitted");
				helper.assertTrue(player.getMainHandItem().isEmpty(), "the furnace is used up");
				helper.assertValueEqual(drum.route().size(), 3, "route kept");
				helper.assertValueEqual(drum.laidOut(), laid, "cable laid out kept");
				helper.assertTrue(drum.hasCoil(), "coil kept");
				body(helper, player, new ItemStack(Items.FURNACE));
				helper.assertValueEqual(player.getMainHandItem().getCount(), 1, "a second furnace is not taken");
				log.add("furnace fitted on a routed drum: route of 3 anchors, " + laid + " blocks and the coil kept");

				lever(helper, player);
				helper.assertValueEqual(line(helper).lever(), CableLines.Lever.NEUTRAL, "lever on an open route");
				log.add("open route: lever stays NEUTRAL");

				CableLoopGameTests.close(helper, player, CableLoopGameTests.ANCHORS[2]);
				player.getInventory().clearContent();
				helper.assertTrue(drum.looped(), "loop closed on the winch");
				List<CableLines.Lever> cycle = new ArrayList<>();
				cycle.add(line(helper).lever());
				for (int i = 0; i < 3; i++) {
					lever(helper, player);
					cycle.add(line(helper).lever());
				}
				helper.assertValueEqual(cycle, List.of(CableLines.Lever.NEUTRAL, CableLines.Lever.PULL, CableLines.Lever.FEED, CableLines.Lever.NEUTRAL),
					"lever cycle");
				log.add("lever: " + cycle);

				body(helper, player, new ItemStack(Items.COAL, 3));
				body(helper, player, player.getMainHandItem());
				helper.assertValueEqual(line(helper).coal(), 2, "two right-clicks with coal");
				helper.assertValueEqual(player.getMainHandItem().getCount(), 1, "one coal each");
				body(helper, player, new ItemStack(Items.CHARCOAL, 2));
				helper.assertValueEqual(line(helper).coal(), 2, "charcoal is not taken");
				helper.assertValueEqual(player.getMainHandItem().getCount(), 2, "charcoal kept");
				log.add("coal: 2 in, one per click; charcoal refused");

				lever(helper, player);
				helper.assertValueEqual(line(helper).lever(), CableLines.Lever.PULL, "lever to PULL");
				body(helper, player, ItemStack.EMPTY);
				helper.assertValueEqual(line(helper).lever(), CableLines.Lever.PULL, "inspecting leaves the lever");
				CableDrive.feed(player, helper.absolutePos(DRUM), Vec3.atCenterOf(helper.absolutePos(DRUM)));
				helper.assertValueEqual(line(helper).lever(), CableLines.Lever.PULL, "the crank's left-click does nothing to a winch");
				lever(helper, player);
				lever(helper, player);
				helper.assertValueEqual(line(helper).lever(), CableLines.Lever.NEUTRAL, "back to NEUTRAL");
				log.add("empty hand on the body: reports, lever stays PULL; left-click does nothing");

				helper.getLevel().destroyBlock(helper.absolutePos(DRUM), true, player);
				List<ItemEntity> drops = helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(helper.absolutePos(DRUM)).inflate(4.0));
				int furnaces = TrainGameTests.total(drops, Items.FURNACE);
				int coal = TrainGameTests.total(drops, Items.COAL);
				int coils = TrainGameTests.total(drops, CableContent.CABLE_COIL);
				helper.assertValueEqual(furnaces, 1, "furnace dropped");
				helper.assertValueEqual(coal, 2, "coal dropped");
				helper.assertValueEqual(coils, 1, "coil dropped");
				log.add("broken: 1 furnace, 2 coal, 1 coil");
				SimpleBlockTestLog.info("WIN-01: {}", log);
				drops.forEach(ItemEntity::discard);
				TestPlayers.remove(player);
			}))
			.thenSucceed();
	}

	/**
	 * WIN-02: the winch hauls a rod cart without anyone holding anything: PULL towards the drum, then
	 * straight to FEED, which slows it, reverses and speeds up again a little at a time; it runs to the
	 * end of the line, where the lever goes back to NEUTRAL by itself. It goes faster than the crank,
	 * and the coal burns one tick for every tick the cart moved, and not while it stands.
	 */
	@GameTest(structure = YARD, maxTicks = 1000)
	public void winchHaulsReversesAndStopsAtTheEnd(GameTestHelper helper) {
		ServerPlayer player = TestPlayers.create(helper, GameType.SURVIVAL, new BlockPos(3, 1, 6));
		CableLoopGameTests.line(helper, player, 1);
		Minecart[] cart = new Minecart[1];
		double[] marks = new double[4];
		int[] moving = {0};
		float[] last = {0.0F};
		float[] maxStep = {0.0F};
		float[] top = {0.0F};
		int[] reversal = {0};
		List<String> log = new ArrayList<>();
		boolean[] failed = {false};
		helper.startSequence()
			.thenIdle(2)
			.thenExecute(CableGameTests.step(failed, () -> {
				winch(helper, player, 2);
				cart[0] = CableLoopGameTests.rodCart(helper, 12.5);
			}))
			.thenIdle(5)
			.thenExecute(CableGameTests.step(failed, () -> {
				helper.assertTrue(ConnectingRods.isGripping(cart[0]), "gripping");
				marks[0] = CableLoopGameTests.x(helper, cart[0]);
				lever(helper, player);
				helper.assertValueEqual(line(helper).lever(), CableLines.Lever.PULL, "PULL");
			}))
			.thenExecuteFor(30, CableGameTests.step(failed, () -> {
				if (cart[0].position().subtract(cart[0].xo, cart[0].yo, cart[0].zo).horizontalDistanceSqr() > 1.0E-8) {
					moving[0]++;
				}
			}))
			.thenExecute(CableGameTests.step(failed, () -> {
				marks[1] = CableLoopGameTests.x(helper, cart[0]);
				helper.assertTrue(marks[1] < marks[0] - 1.0, "PULL hauls towards the drum: x " + marks[0] + " -> " + marks[1]);
				last[0] = line(helper).speed();
				lever(helper, player);
				helper.assertValueEqual(line(helper).lever(), CableLines.Lever.FEED, "straight from PULL to FEED");
			}))
			.thenExecuteFor(60, CableGameTests.step(failed, () -> {
				float speed = line(helper).speed();
				maxStep[0] = Math.max(maxStep[0], Math.abs(speed - last[0]));
				if (last[0] < 0.0F && speed >= 0.0F) {
					reversal[0] = 1;
				}
				last[0] = speed;
				top[0] = Math.max(top[0], Math.abs(speed));
				if (cart[0].position().subtract(cart[0].xo, cart[0].yo, cart[0].zo).horizontalDistanceSqr() > 1.0E-8) {
					moving[0]++;
				}
			}))
			.thenExecute(CableGameTests.step(failed, () -> {
				helper.assertTrue(reversal[0] == 1, "the strand reversed");
				helper.assertTrue(maxStep[0] <= 0.0101F, "the speed changes at most 0.01 a tick through the reversal, changed " + maxStep[0]);
				log.add(String.format("PULL: x %.2f -> %.2f in 30 ticks; FEED straight after: speed changed at most %.3f a tick", marks[0], marks[1],
					maxStep[0]));
			}))
			.thenWaitUntil(() -> {
				if (cart[0].position().subtract(cart[0].xo, cart[0].yo, cart[0].zo).horizontalDistanceSqr() > 1.0E-8) {
					moving[0]++;
				}
				top[0] = Math.max(top[0], Math.abs(line(helper).speed()));
				helper.assertValueEqual(line(helper).lever(), CableLines.Lever.NEUTRAL, "lever back to NEUTRAL at the end");
			})
			.thenExecute(CableGameTests.step(failed, () -> {
				CableLines.Line line = line(helper);
				marks[2] = CableLoopGameTests.x(helper, cart[0]);
				helper.assertValueEqual(line.lastStop(), "end", "why it stopped");
				helper.assertTrue(marks[2] > 19.0 && marks[2] < 22.5, "at the end, short of the wheel at x 23.5: x " + marks[2]);
				helper.assertTrue(top[0] > CableDrive.crankSpeed() * 2.0, "faster than the crank (" + CableDrive.crankSpeed() + "): " + top[0]);
				helper.assertValueEqual(line.coal(), 1, "one coal lit");
				int burnt = 1600 - line.burn();
				helper.assertTrue(Math.abs(burnt - moving[0]) <= 3, "burnt " + burnt + " ticks for " + moving[0] + " ticks moving");
				marks[3] = line.burn();
				log.add(String.format("FEED to x %.2f, lever NEUTRAL (end); top speed %.3f (crank %.3f); burnt %d ticks for %d ticks moving", marks[2],
					top[0], CableDrive.crankSpeed(), burnt, moving[0]));
			}))
			.thenIdle(60)
			.thenExecute(CableGameTests.step(failed, () -> {
				CableLines.Line line = line(helper);
				helper.assertValueEqual(line.lever(), CableLines.Lever.NEUTRAL, "nothing restarts");
				helper.assertValueEqual((double) line.burn(), marks[3], "no coal burns while it stands");
				helper.assertTrue(Math.abs(CableLoopGameTests.x(helper, cart[0]) - marks[2]) < 0.02, "held at the end");
				log.add("60 ticks later: held, still NEUTRAL, burn unchanged at " + line.burn());
				SimpleBlockTestLog.info("WIN-02: {}", log);
				cart[0].discard();
				TestPlayers.remove(player);
			}))
			.thenSucceed();
	}

	/**
	 * WIN-03: a cart hauled onto a loading track docks; the lever goes to NEUTRAL and the coal stops
	 * burning; releasing the track starts nothing; set going again, the cart leaves without docking.
	 * The rails ending ahead of a hauled cart stop it the same way, still on the rails.
	 */
	@GameTest(structure = YARD, maxTicks = 1000)
	public void dockAndTrackEndPutTheLeverToNeutral(GameTestHelper helper) {
		ServerPlayer player = TestPlayers.create(helper, GameType.SURVIVAL, new BlockPos(3, 1, 6));
		CableLoopGameTests.line(helper, player, 1);
		BlockPos station = new BlockPos(17, 1, 4);
		Minecart[] cart = new Minecart[1];
		double[] marks = new double[3];
		List<String> log = new ArrayList<>();
		boolean[] failed = {false};
		helper.startSequence()
			.thenIdle(2)
			.thenExecute(CableGameTests.step(failed, () -> {
				winch(helper, player, 1);
				LoadingTrackGameTests.loadingTrack(helper, station, Direction.EAST);
				cart[0] = CableLoopGameTests.rodCart(helper, 10.5);
			}))
			.thenIdle(5)
			.thenExecute(CableGameTests.step(failed, () -> {
				lever(helper, player);
				lever(helper, player);
				helper.assertValueEqual(line(helper).lever(), CableLines.Lever.FEED, "FEED");
			}))
			.thenWaitUntil(() -> helper.assertValueEqual(line(helper).lever(), CableLines.Lever.NEUTRAL, "lever back to NEUTRAL at the dock"))
			.thenExecute(CableGameTests.step(failed, () -> {
				helper.assertValueEqual(line(helper).lastStop(), "docked", "why it stopped");
				helper.assertTrue(LoadingTrackGameTests.docked(cart[0], helper, station), "the cart docked");
				marks[0] = CableLoopGameTests.x(helper, cart[0]);
				marks[1] = line(helper).burn();
				log.add(String.format("FEED onto the loading track: docked at x %.2f, lever NEUTRAL (docked), burn %d", marks[0], (int) marks[1]));
			}))
			.thenIdle(40)
			.thenExecute(CableGameTests.step(failed, () -> {
				helper.assertValueEqual((double) line(helper).burn(), marks[1], "no burn while docked");
				LoadingTrackGameTests.useTrack(helper, player, station);
				helper.assertTrue(Couplings.coupled(cart[0]).simpleBlock$dock() == null, "released");
			}))
			.thenIdle(40)
			.thenExecute(CableGameTests.step(failed, () -> {
				helper.assertTrue(Math.abs(CableLoopGameTests.x(helper, cart[0]) - marks[0]) < 0.01, "release moves nothing");
				helper.assertValueEqual(line(helper).lever(), CableLines.Lever.NEUTRAL, "release leaves the lever");
				helper.assertValueEqual((double) line(helper).burn(), marks[1], "no burn after release");
				log.add("40 ticks docked and 40 released: no movement, no burn, lever NEUTRAL");
				lever(helper, player);
			}))
			.thenIdle(40)
			.thenExecute(CableGameTests.step(failed, () -> {
				double x = CableLoopGameTests.x(helper, cart[0]);
				helper.assertTrue(x < marks[0] - 1.0, "PULL takes it back off the track: x " + x);
				helper.assertTrue(Couplings.coupled(cart[0]).simpleBlock$dock() == null, "no relock leaving");
				log.add(String.format("PULL: left the track to x %.2f without docking again", x));
				lever(helper, player);
				lever(helper, player);
				helper.assertValueEqual(line(helper).lever(), CableLines.Lever.NEUTRAL, "stopped");
				// The rails end at x 13: FEED runs it to the end of the track.
				for (int x2 = 13; x2 < 30; x2++) {
					helper.setBlock(new BlockPos(x2, 1, 4), Blocks.AIR);
				}
				lever(helper, player);
				lever(helper, player);
			}))
			.thenWaitUntil(() -> helper.assertValueEqual(line(helper).lever(), CableLines.Lever.NEUTRAL, "lever back to NEUTRAL at the end of the track"))
			.thenExecute(CableGameTests.step(failed, () -> {
				helper.assertValueEqual(line(helper).lastStop(), "track_end", "why it stopped");
				double x = CableLoopGameTests.x(helper, cart[0]);
				helper.assertTrue(x < 13.0 && x > 12.0, "stopped at the end of the rails at x 13: x " + x);
				helper.assertTrue(io.github.boxofdog.simpleblock.train.TrainPhysics.onRail(cart[0]), "still on the rails");
				log.add(String.format("rails ending at x 13: FEED stopped it at x %.2f on the rails, lever NEUTRAL (track_end)", x));
				SimpleBlockTestLog.info("WIN-03: {}", log);
				cart[0].discard();
				TestPlayers.remove(player);
			}))
			.thenSucceed();
	}

	/**
	 * WIN-04: hauled into a cart held at a loading track ahead, the hauled cart is blocked: within a
	 * moment the lever goes to NEUTRAL, and no coal burns while it is pressed against the obstacle.
	 */
	@GameTest(structure = YARD, maxTicks = 600)
	public void blockedHaulBurnsNoCoal(GameTestHelper helper) {
		ServerPlayer player = TestPlayers.create(helper, GameType.SURVIVAL, new BlockPos(3, 1, 6));
		CableLoopGameTests.line(helper, player, 1);
		BlockPos station = new BlockPos(16, 1, 4);
		Minecart[] carts = new Minecart[2];
		int[] burn = {-1};
		int[] stuck = {0};
		boolean[] started = {false};
		List<String> log = new ArrayList<>();
		boolean[] failed = {false};
		helper.startSequence()
			.thenIdle(2)
			.thenExecute(CableGameTests.step(failed, () -> {
				winch(helper, player, 1);
				LoadingTrackGameTests.loadingTrack(helper, station, Direction.EAST);
				carts[1] = TrainGameTests.cart(helper, 16.5, 4);
				carts[0] = CableLoopGameTests.rodCart(helper, 10.5);
			}))
			.thenWaitUntil(() -> helper.assertTrue(carts[1] != null && LoadingTrackGameTests.docked(carts[1], helper, station), "the obstacle docked"))
			.thenExecute(CableGameTests.step(failed, () -> {
				lever(helper, player);
				lever(helper, player);
			}))
			.thenWaitUntil(() -> {
				CableLines.Line line = line(helper);
				// From the first tick the hauled cart (once under way) stands still against the obstacle, no more coal may burn.
				boolean still = carts[0].position().subtract(carts[0].xo, carts[0].yo, carts[0].zo).horizontalDistanceSqr() < 1.0E-6;
				if (!still) {
					started[0] = true;
				} else if (started[0] && line.burn() > 0) {
					if (burn[0] < 0) {
						burn[0] = line.burn();
					}
					stuck[0]++;
				}
				helper.assertValueEqual(line.lever(), CableLines.Lever.NEUTRAL, "lever back to NEUTRAL when blocked");
			})
			.thenExecute(CableGameTests.step(failed, () -> {
				CableLines.Line line = line(helper);
				helper.assertValueEqual(line.lastStop(), "blocked", "why it stopped");
				helper.assertTrue(burn[0] > 0, "the hauled cart was stopped by the obstacle");
				helper.assertTrue(burn[0] - line.burn() <= 2, "burnt " + (burn[0] - line.burn()) + " ticks while blocked");
				double gap = carts[1].getX() - carts[0].getX();
				log.add(String.format("hauled into a docked cart: stopped %.2f blocks behind it, lever NEUTRAL (blocked) after %d ticks still, %d ticks burnt meanwhile",
					gap, stuck[0], burn[0] - line.burn()));
				SimpleBlockTestLog.info("WIN-04: {}", log);
				for (Minecart cart : carts) {
					cart.discard();
				}
				TestPlayers.remove(player);
			}))
			.thenSucceed();
	}

	/**
	 * WIN-05: the winch's coal, partly burnt coal and lever are saved, in the level's cable lines and
	 * in the drum: loaded again, the burn time left is exact and the lever is at NEUTRAL.
	 */
	@GameTest(structure = YARD, maxTicks = 400)
	public void partlyBurntCoalIsSavedExactly(GameTestHelper helper) {
		ServerLevel level = level(helper);
		ServerPlayer player = TestPlayers.create(helper, GameType.SURVIVAL, new BlockPos(3, 1, 6));
		CableLoopGameTests.line(helper, player, 1);
		Minecart[] cart = new Minecart[1];
		List<String> log = new ArrayList<>();
		boolean[] failed = {false};
		helper.startSequence()
			.thenIdle(2)
			.thenExecute(CableGameTests.step(failed, () -> {
				winch(helper, player, 3);
				cart[0] = CableLoopGameTests.rodCart(helper, 8.5);
			}))
			.thenIdle(5)
			.thenExecute(CableGameTests.step(failed, () -> {
				lever(helper, player);
				lever(helper, player);
			}))
			.thenIdle(37)
			.thenExecute(CableGameTests.step(failed, () -> {
				CableLines.Line line = line(helper);
				helper.assertValueEqual(line.lever(), CableLines.Lever.FEED, "still running");
				int coal = line.coal();
				int burn = line.burn();
				helper.assertTrue(burn > 0 && burn < 1600, "a coal partly burnt: " + burn);

				Tag saved = CableLines.TYPE.codec().encodeStart(NbtOps.INSTANCE, CableLines.get(level)).getOrThrow();
				CableLines loaded = CableLines.TYPE.codec().parse(NbtOps.INSTANCE, saved).getOrThrow();
				CableLines.Line copy = loadedLine(loaded, helper.absolutePos(DRUM));
				helper.assertValueEqual(copy.coal(), coal, "coal after loading the level's lines");
				helper.assertValueEqual(copy.burn(), burn, "burn time after loading the level's lines");
				helper.assertValueEqual(copy.lever(), CableLines.Lever.NEUTRAL, "lever after loading the level's lines");
				helper.assertTrue(copy.powered() && copy.looped(), "powered loop after loading");

				CableDrumBlockEntity drum = CableGameTests.drum(helper, DRUM);
				CompoundTag tag = drum.saveWithFullMetadata(level.registryAccess());
				BlockState state = level.getBlockState(helper.absolutePos(DRUM));
				level.setBlock(helper.absolutePos(DRUM), Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL | Block.UPDATE_SKIP_BLOCK_ENTITY_SIDEEFFECTS);
				level.setBlock(helper.absolutePos(DRUM), state, Block.UPDATE_ALL);
				CableDrumBlockEntity reloaded = CableGameTests.drum(helper, DRUM);
				reloaded.loadWithComponents(TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), tag));
				helper.assertTrue(reloaded.powered(), "the drum is still a coal winch");
				helper.assertValueEqual(reloaded.coal(), coal, "coal in the drum's copy");
				helper.assertValueEqual(reloaded.burn(), burn, "burn time in the drum's copy");
				log.add(String.format("running FEED: %d coal and %d of 1600 ticks left; saved and loaded: %d coal, %d ticks, lever %s", coal, burn,
					copy.coal(), copy.burn(), copy.lever()));
				SimpleBlockTestLog.info("WIN-05: {}", log);
				cart[0].discard();
				TestPlayers.remove(player);
			}))
			.thenSucceed();
	}

	/**
	 * WIN-06 (BUG-002): PULL brings the cart to the drum's end of the line, where it stops and the lever
	 * drops back to NEUTRAL; the next click goes straight to FEED (not PULL again, which could never
	 * move it) and FEED takes the cart away. Once it has moved, the cycle is NEUTRAL, PULL, FEED as usual. At the far end, FEED is
	 * stopped the same way and the next click gives PULL, which brings it back.
	 */
	@GameTest(structure = YARD, maxTicks = 1200)
	public void blockedEndLetsTheLeverChooseTheWayOut(GameTestHelper helper) {
		ServerPlayer player = TestPlayers.create(helper, GameType.SURVIVAL, new BlockPos(3, 1, 6));
		CableLoopGameTests.line(helper, player, 1);
		Minecart[] cart = new Minecart[1];
		double[] marks = new double[4];
		List<String> log = new ArrayList<>();
		boolean[] failed = {false};
		helper.startSequence()
			.thenIdle(2)
			.thenExecute(CableGameTests.step(failed, () -> {
				winch(helper, player, 2);
				cart[0] = CableLoopGameTests.rodCart(helper, 5.5);
			}))
			.thenIdle(5)
			.thenExecute(CableGameTests.step(failed, () -> {
				helper.assertTrue(ConnectingRods.isGripping(cart[0]), "gripping at the drum's end");
				marks[0] = CableLoopGameTests.x(helper, cart[0]);
				lever(helper, player);
				helper.assertValueEqual(line(helper).lever(), CableLines.Lever.PULL, "first click");
			}))
			.thenWaitUntil(() -> helper.assertValueEqual(line(helper).lever(), CableLines.Lever.NEUTRAL, "PULL at the drum's end stops"))
			.thenExecute(CableGameTests.step(failed, () -> {
				helper.assertValueEqual(line(helper).lastStop(), "end", "why it stopped");
				// The owner's case: the cart now stands at the drum's end, where PULL can't move it.
				marks[0] = CableLoopGameTests.x(helper, cart[0]);
				lever(helper, player);
				helper.assertValueEqual(line(helper).lever(), CableLines.Lever.FEED, "the next click skips the blocked PULL");
				log.add(String.format("PULL to the drum's end: stopped at x %.2f (end); next click: FEED", marks[0]));
			}))
			.thenIdle(60)
			.thenExecute(CableGameTests.step(failed, () -> {
				marks[1] = CableLoopGameTests.x(helper, cart[0]);
				helper.assertTrue(marks[1] - marks[0] > 3.0, "FEED takes the cart away from the drum's end: x " + marks[0] + " -> " + marks[1]);
				helper.assertValueEqual(line(helper).blockedSign(), 0, "moving clears the blocked way");
				// Repeated clicks now cycle as usual: FEED -> NEUTRAL -> PULL -> FEED.
				List<CableLines.Lever> cycle = new ArrayList<>();
				for (int i = 0; i < 3; i++) {
					lever(helper, player);
					cycle.add(line(helper).lever());
				}
				helper.assertValueEqual(cycle, List.of(CableLines.Lever.NEUTRAL, CableLines.Lever.PULL, CableLines.Lever.FEED), "cycle after moving");
				log.add(String.format("FEED 60 ticks: x %.2f -> %.2f; then clicks %s", marks[0], marks[1], cycle));
			}))
			.thenWaitUntil(() -> helper.assertValueEqual(line(helper).lever(), CableLines.Lever.NEUTRAL, "FEED to the far end stops"))
			.thenExecute(CableGameTests.step(failed, () -> {
				helper.assertValueEqual(line(helper).lastStop(), "end", "why it stopped at the far end");
				marks[2] = CableLoopGameTests.x(helper, cart[0]);
				lever(helper, player);
				helper.assertValueEqual(line(helper).lever(), CableLines.Lever.PULL, "at the far end the next click is PULL");
			}))
			.thenIdle(60)
			.thenExecute(CableGameTests.step(failed, () -> {
				marks[3] = CableLoopGameTests.x(helper, cart[0]);
				helper.assertTrue(marks[2] - marks[3] > 3.0, "PULL brings it back from the far end: x " + marks[2] + " -> " + marks[3]);
				lever(helper, player);
				lever(helper, player);
				helper.assertValueEqual(line(helper).lever(), CableLines.Lever.NEUTRAL, "stopped");
				log.add(String.format("far end at x %.2f: FEED stopped (end), next click PULL, back to x %.2f", marks[2], marks[3]));
				SimpleBlockTestLog.info("WIN-06 (BUG-002): {}", log);
				cart[0].discard();
				TestPlayers.remove(player);
			}))
			.thenSucceed();
	}

	private static CableLines.Line loadedLine(CableLines lines, BlockPos drum) {
		return lines.lineAt(drum);
	}
}
