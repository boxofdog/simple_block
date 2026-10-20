package io.github.boxofdog.simpleblock.gametest;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.vehicle.minecart.AbstractMinecart;
import net.minecraft.world.entity.vehicle.minecart.Minecart;
import net.minecraft.world.entity.vehicle.minecart.MinecartFurnace;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.RailShape;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import net.fabricmc.fabric.api.gametest.v1.GameTest;

import io.github.boxofdog.simpleblock.cable.CableContent;
import io.github.boxofdog.simpleblock.feature.MinecartCargo;
import io.github.boxofdog.simpleblock.train.CobblestoneEngine;
import io.github.boxofdog.simpleblock.train.EngineSelector;
import io.github.boxofdog.simpleblock.train.Couplings;
import io.github.boxofdog.simpleblock.train.LoadingTrackBlock;
import io.github.boxofdog.simpleblock.train.LoadingTracks;
import io.github.boxofdog.simpleblock.train.TrainContent;

/**
 * M06 work package 2: the Loading Track. Carts and engines are used through {@code Player.interactOn}
 * and the track through {@code ServerPlayerGameMode.useItemOn}, the methods the server runs for a
 * player's use packets. Test ids refer to the matrix in {@code docs/MILESTONE_06_REPORT.md}.
 *
 * <p>Tracks run east-west in {@code cable_yard}, rails at y 1.
 */
public class LoadingTrackGameTests {
	private static final String YARD = "simple_block-gametest:cable_yard";

	static void loadingTrack(GameTestHelper helper, BlockPos pos, Direction facing) {
		helper.setBlock(pos.below(), Blocks.STONE);
		helper.setBlock(pos, TrainContent.LOADING_TRACK.defaultBlockState().setValue(LoadingTrackBlock.FACING, facing)
			.setValue(LoadingTrackBlock.SHAPE, facing.getAxis() == Direction.Axis.X ? RailShape.EAST_WEST : RailShape.NORTH_SOUTH));
	}

	/** Uses the track's post with an empty hand, as a player's use-on-block packet does. */
	static void useTrack(GameTestHelper helper, ServerPlayer player, BlockPos pos) {
		BlockPos abs = helper.absolutePos(pos);
		player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
		player.gameMode.useItemOn(player, helper.getLevel(), ItemStack.EMPTY, InteractionHand.MAIN_HAND,
			new BlockHitResult(Vec3.atCenterOf(abs).add(0.0, 0.4, 0.0), Direction.UP, abs, false));
	}

	static boolean docked(AbstractMinecart cart, GameTestHelper helper, BlockPos track) {
		return helper.absolutePos(track).equals(Couplings.coupled(cart).simpleBlock$dock());
	}

	static boolean raised(GameTestHelper helper, BlockPos rail) {
		return helper.getBlockState(rail).getValue(LoadingTrackBlock.RAISED);
	}

	static Direction front(AbstractMinecart cart) {
		return LoadingTracks.freight(cart).simpleBlock$front();
	}

	/** LT-01: one rail and one cable anchor, anywhere in the grid, make one Loading Track. */
	@GameTest
	public void loadingTrackIsCraftedFromARailAndAnAnchor(GameTestHelper helper) {
		ServerPlayer player = TestPlayers.create(helper, GameType.SURVIVAL, new BlockPos(3, 1, 3));
		Item r = Items.RAIL;
		Item a = CableContent.CABLE_ANCHOR_ITEM;
		Item o = Items.AIR;
		List<String> log = new ArrayList<>();
		ItemStack top = RecipeGameTests.craft(helper, player, r, a, o, o, o, o, o, o, o);
		ItemStack apart = RecipeGameTests.craft(helper, player, o, o, a, o, o, o, r, o, o);
		ItemStack powered = RecipeGameTests.craft(helper, player, Items.POWERED_RAIL, a, o, o, o, o, o, o, o);
		helper.assertTrue(top.is(TrainContent.LOADING_TRACK_ITEM) && top.getCount() == 1, "rail + anchor side by side: " + top);
		helper.assertTrue(apart.is(TrainContent.LOADING_TRACK_ITEM) && apart.getCount() == 1, "rail + anchor in far corners: " + apart);
		helper.assertTrue(powered.isEmpty(), "a powered rail is not a rail: " + powered);
		log.add("rail + anchor: " + top + "; apart: " + apart + "; powered rail + anchor: nothing");
		SimpleBlockTestLog.info("LT-01: {}", log);
		TestPlayers.remove(player);
		helper.succeed();
	}

	/**
	 * LT-02: a rolling freight cart docks in the middle of a loading track with its front the track's
	 * way, whichever way it came; a cushioned cart and an engine pass over; the dock, release and front
	 * are saved with the cart.
	 */
	@GameTest(structure = YARD, maxTicks = 200)
	public void freightCartsDockFacingTheTrack(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		TrainGameTests.track(helper, 2, 0, 29);
		TrainGameTests.track(helper, 4, 0, 29);
		TrainGameTests.track(helper, 6, 0, 29);
		TrainGameTests.track(helper, 8, 0, 29);
		BlockPos fromWest = new BlockPos(15, 1, 2);
		BlockPos fromEast = new BlockPos(15, 1, 4);
		BlockPos cushioned = new BlockPos(15, 1, 6);
		BlockPos engineLane = new BlockPos(15, 1, 8);
		for (BlockPos pos : List.of(fromWest, fromEast, cushioned, engineLane)) {
			loadingTrack(helper, pos, Direction.EAST);
		}
		Minecart west = TrainGameTests.cart(helper, 10.5, 2);
		Minecart east = TrainGameTests.cart(helper, 20.5, 4);
		Minecart seat = TrainGameTests.cart(helper, 10.5, 6);
		MinecartFurnace engine = TrainGameTests.engine(helper, 5.5, 8, 2);
		List<String> log = new ArrayList<>();
		boolean[] failed = {false};
		helper.startSequence()
			.thenIdle(2)
			.thenExecute(CableGameTests.step(failed, () -> {
				MinecartCargo.cart(west).simpleBlock$setCargo(Items.RAW_IRON, 64);
				MinecartCargo.cart(seat).simpleBlock$setCushion(DyeColor.RED);
				west.setDeltaMovement(0.4, 0.0, 0.0);
				east.setDeltaMovement(-0.4, 0.0, 0.0);
				seat.setDeltaMovement(0.4, 0.0, 0.0);
				CobblestoneEngine.Engine state = CobblestoneEngine.engine(engine);
				state.simpleBlock$setHeading(new Vec3(1.0, 0.0, 0.0));
				state.simpleBlock$setRunning(true);
			}))
			.thenWaitUntil(() -> {
				helper.assertTrue(docked(west, helper, fromWest) && docked(east, helper, fromEast), "both freight carts docked");
				helper.assertTrue(TrainGameTests.x(helper, seat) > 17.0 && TrainGameTests.x(helper, engine) > 17.0,
					"cushioned cart and engine past the track: x " + TrainGameTests.x(helper, seat) + ", " + TrainGameTests.x(helper, engine));
			})
			.thenIdle(10)
			.thenExecute(CableGameTests.step(failed, () -> {
				for (Minecart cart : List.of(west, east)) {
					Vec3 at = cart.position().subtract(helper.absoluteVec(Vec3.ZERO));
					helper.assertTrue(Math.abs(at.x - 15.5) < 1.0E-6, "docked cart in the middle of the track: x " + at.x);
					helper.assertTrue(cart.getDeltaMovement().lengthSqr() < 1.0E-9, "docked cart still");
					helper.assertValueEqual(front(cart), Direction.EAST, "front of the cart that came from the " + (cart == west ? "west" : "east"));
				}
				helper.assertTrue(Couplings.coupled(seat).simpleBlock$dock() == null && Couplings.coupled(engine).simpleBlock$dock() == null,
					"the cushioned cart and the engine never dock");
				helper.assertTrue(raised(helper, fromWest) && raised(helper, fromEast), "the stop is raised behind each docked cart");
				helper.assertFalse(raised(helper, cushioned) || raised(helper, engineLane), "the stop stays folded for a cushioned cart and an engine");
				log.add(String.format("from the west and from the east: docked at x 15.5, front EAST both, stop raised; cushioned cart at x %.1f and engine at x %.1f passed, stop folded",
					TrainGameTests.x(helper, seat), TrainGameTests.x(helper, engine)));

				// Saved and loaded with the cart.
				TagValueOutput output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, level.registryAccess());
				west.saveWithoutId(output);
				Minecart copy = EntityTypes.MINECART.create(level, EntitySpawnReason.LOAD);
				copy.load(TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), output.buildResult()));
				helper.assertTrue(docked(copy, helper, fromWest), "dock after loading");
				helper.assertValueEqual(front(copy), Direction.EAST, "front after loading");
				log.add("dock and front saved and loaded");
				SimpleBlockTestLog.info("LT-02: {}", log);
				List.<Entity>of(west, east, seat, engine).forEach(Entity::discard);
			}))
			.thenSucceed();
	}

	/**
	 * LT-03 (revised by M15: engine trains dock by their selector): an engine pushing two freight carts, its
	 * selector at SERVICE, reaches a one-rail station whose receiving hopper can take the lead cart's copper:
	 * the lead cart docks on the rail, the train is held, the engine switched off and burning nothing, and it
	 * cannot be started. Using the rail lets it go (the selector goes to THROUGH); nothing moves until the
	 * engine is started again, and then the whole train, at THROUGH, passes over the rail. Coming back the
	 * other way at SERVICE, only the engine would stand on the rail (a wagon must, to be served): it passes.
	 */
	@GameTest(structure = YARD, maxTicks = 1200)
	public void trainDocksIsHeldAndReleased(GameTestHelper helper) {
		ServerPlayer player = TestPlayers.create(helper, GameType.SURVIVAL, new BlockPos(2, 1, 6));
		TrainGameTests.track(helper, 4, 0, 29);
		BlockPos station = new BlockPos(16, 1, 4);
		loadingTrack(helper, station, Direction.WEST);
		helper.setBlock(station.north().below(), Blocks.HOPPER);
		MinecartFurnace engine = TrainGameTests.engine(helper, 5.5, 4, 4);
		Minecart rear = TrainGameTests.cart(helper, 7.1, 4);
		Minecart lead = TrainGameTests.cart(helper, 8.7, 4);
		List<AbstractMinecart> train = List.of(engine, rear, lead);
		CobblestoneEngine.Engine state = CobblestoneEngine.engine(engine);
		double[] held = new double[3];
		int[] fuel = new int[1];
		List<String> log = new ArrayList<>();
		boolean[] failed = {false};
		helper.startSequence()
			.thenIdle(2)
			.thenExecute(CableGameTests.step(failed, () -> {
				MinecartCargo.cart(lead).simpleBlock$setCargo(Items.RAW_COPPER, 200);
				TrainGameTests.couple(player, engine, rear);
				TrainGameTests.couple(player, rear, lead);
				EngineSelector.set(engine, EngineSelector.Mode.SERVICE);
				TrainGameTests.use(player, engine, ItemStack.EMPTY);
				helper.assertTrue(state.simpleBlock$running(), "started");
			}))
			.thenWaitUntil(() -> helper.assertTrue(docked(lead, helper, station), "lead freight cart docked"))
			.thenExecute(CableGameTests.step(failed, () -> {
				helper.assertFalse(state.simpleBlock$running(), "the engine is switched off when its train docks");
				for (int i = 0; i < 3; i++) {
					held[i] = TrainGameTests.x(helper, train.get(i));
				}
				fuel[0] = state.simpleBlock$fuel();
				log.add(String.format("docked: lead x %.2f front %s, engine off", held[2], front(lead)));
				// Starting it while held is refused.
				CableGameTests.teleport(helper, player, new BlockPos(2, 1, 6));
				TrainGameTests.use(player, engine, ItemStack.EMPTY);
			}))
			.thenIdle(40)
			.thenExecute(CableGameTests.step(failed, () -> {
				for (int i = 0; i < 3; i++) {
					double x = TrainGameTests.x(helper, train.get(i));
					helper.assertTrue(Math.abs(x - held[i]) < 0.01, "car " + i + " held: x " + held[i] + " -> " + x);
				}
				helper.assertFalse(state.simpleBlock$running(), "an engine in a docked train cannot be started");
				helper.assertValueEqual(state.simpleBlock$fuel(), fuel[0], "fuel while docked");
				helper.assertValueEqual(front(lead), Direction.WEST, "the docked cart's front is the track's way, not the way it came");
				TrainGameTests.assertSpacingPublic(helper, List.of(engine, rear, lead), "docked");
				log.add("40 ticks docked: all three cars held, engine refused to start, no fuel burnt; lead tipped "
					+ (200 - MinecartCargo.cart(lead).simpleBlock$count()) + " copper into the hopper");
				helper.assertTrue(raised(helper, station), "stop raised while the train is held");
				useTrack(helper, player, station);
				for (AbstractMinecart car : train) {
					helper.assertTrue(Couplings.coupled(car).simpleBlock$dock() == null, "released");
				}
				helper.assertValueEqual(EngineSelector.mode(engine), EngineSelector.Mode.THROUGH, "using the rail sets the lever to THROUGH");
				helper.assertFalse(raised(helper, station), "stop folded on release");
			}))
			.thenIdle(40)
			.thenExecute(CableGameTests.step(failed, () -> {
				for (int i = 0; i < 3; i++) {
					double x = TrainGameTests.x(helper, train.get(i));
					helper.assertTrue(Math.abs(x - held[i]) < 0.01, "car " + i + " does not move off by itself: x " + held[i] + " -> " + x);
				}
				helper.assertFalse(state.simpleBlock$running(), "release does not start the engine");
				log.add("released: nothing moved in 40 ticks");
				TrainGameTests.use(player, engine, ItemStack.EMPTY);
				helper.assertTrue(state.simpleBlock$running(), "started again after release");
			}))
			.thenWaitUntil(() -> helper.assertTrue(TrainGameTests.x(helper, engine) > 18.0, "the whole train past the track"))
			.thenExecute(CableGameTests.step(failed, () -> {
				for (AbstractMinecart car : train) {
					helper.assertTrue(Couplings.coupled(car).simpleBlock$dock() == null, "a train at THROUGH passes over the track");
				}
				log.add(String.format("restarted at THROUGH: the whole train passed the track (engine x %.1f) without docking", TrainGameTests.x(helper, engine)));
				TrainGameTests.use(player, engine, ItemStack.EMPTY);
			}))
			.thenWaitUntil(() -> helper.assertTrue(state.simpleBlock$speed() < 1.0E-4, "stopped"))
			.thenExecute(CableGameTests.step(failed, () -> {
				EngineSelector.set(engine, EngineSelector.Mode.SERVICE);
				CableGameTests.teleport(helper, player, new BlockPos(28, 1, 6));
				TrainGameTests.use(player, engine, ItemStack.EMPTY);
				helper.assertTrue(state.simpleBlock$running(), "started back west");
			}))
			.thenWaitUntil(() -> helper.assertTrue(TrainGameTests.x(helper, lead) < 13.0, "the whole train back past the track"))
			.thenExecute(CableGameTests.step(failed, () -> {
				for (AbstractMinecart car : train) {
					helper.assertTrue(Couplings.coupled(car).simpleBlock$dock() == null, "only the engine would stand on the rail: passed");
				}
				log.add("back west at SERVICE: the engine leads, no wagon would stand on the rail: passed");
				SimpleBlockTestLog.info("LT-03: {}", log);
				train.forEach(Entity::discard);
				TestPlayers.remove(player);
			}))
			.thenSucceed();
	}

	/** LT-04: breaking the track lets the train go; a released cart keeps its front round a curve. */
	@GameTest(structure = YARD, maxTicks = 300)
	public void frontFollowsTheCartRoundCurves(GameTestHelper helper) {
		ServerPlayer player = TestPlayers.create(helper, GameType.SURVIVAL, new BlockPos(2, 1, 8));
		// East along z 2 from x 2, a curve at x 20 turning south, then south along x 20.
		TrainGameTests.track(helper, 2, 2, 19);
		CableLoopGameTests.rail(helper, new BlockPos(20, 1, 2), RailShape.SOUTH_WEST);
		for (int z = 3; z <= 9; z++) {
			CableLoopGameTests.rail(helper, new BlockPos(20, 1, z), RailShape.NORTH_SOUTH);
		}
		BlockPos station = new BlockPos(16, 1, 2);
		loadingTrack(helper, station, Direction.WEST);
		Minecart cart = TrainGameTests.cart(helper, 12.5, 2);
		List<String> log = new ArrayList<>();
		boolean[] failed = {false};
		helper.startSequence()
			.thenIdle(2)
			.thenExecute(() -> cart.setDeltaMovement(0.3, 0.0, 0.0))
			.thenWaitUntil(() -> helper.assertTrue(docked(cart, helper, station), "docked"))
			.thenExecute(CableGameTests.step(failed, () -> {
				helper.assertValueEqual(front(cart), Direction.WEST, "front at the station");
				helper.setBlock(station, Blocks.RAIL.defaultBlockState());
			}))
			.thenIdle(2)
			.thenExecute(CableGameTests.step(failed, () -> {
				helper.assertTrue(Couplings.coupled(cart).simpleBlock$dock() == null, "a broken track lets the train go");
				log.add("track replaced by a plain rail: undocked");
				// Rolling east, its front (west) trails; round the curve to the south it should trail north.
				cart.setDeltaMovement(0.4, 0.0, 0.0);
			}))
			.thenWaitUntil(() -> helper.assertTrue(cart.getZ() - helper.absoluteVec(Vec3.ZERO).z > 5.0,
				"round the curve: at " + cart.position().subtract(helper.absoluteVec(Vec3.ZERO))))
			.thenExecute(CableGameTests.step(failed, () -> {
				helper.assertValueEqual(front(cart), Direction.NORTH, "front (trailing) after turning south");
				log.add("rolled east with its front trailing, round the curve south: front NORTH (still trailing)");
				SimpleBlockTestLog.info("LT-04: {}", log);
				cart.discard();
				TestPlayers.remove(player);
			}))
			.thenSucceed();
	}

	/**
	 * LT-05 (M06 correction): the stop is raised behind a docked cart on the rail's rear side (from the
	 * rail's facing, not the way the cart came), is saved with the block, and folds by itself if the
	 * docked cart is destroyed while held. The display name is "Loading Rail"; the id is unchanged.
	 */
	@GameTest(structure = YARD, maxTicks = 300)
	public void stopRisesBehindTheCartAndFolds(GameTestHelper helper) {
		TrainGameTests.track(helper, 4, 0, 29);
		BlockPos station = new BlockPos(15, 1, 4);
		loadingTrack(helper, station, Direction.WEST);
		Minecart cart = TrainGameTests.cart(helper, 10.5, 4);
		List<String> log = new ArrayList<>();
		boolean[] failed = {false};
		helper.startSequence()
			.thenIdle(2)
			.thenExecute(CableGameTests.step(failed, () -> {
				helper.assertFalse(raised(helper, station), "folded while empty");
				helper.assertValueEqual(TrainContent.LOADING_TRACK_ITEM.getName(new ItemStack(TrainContent.LOADING_TRACK_ITEM)).getString(), "Loading Rail",
					"display name");
				helper.assertValueEqual(BuiltInRegistries.ITEM.getKey(TrainContent.LOADING_TRACK_ITEM).toString(), "simple_block:loading_track", "id");
				cart.setDeltaMovement(0.4, 0.0, 0.0);
			}))
			.thenWaitUntil(() -> helper.assertTrue(docked(cart, helper, station), "docked"))
			.thenExecute(CableGameTests.step(failed, () -> {
				helper.assertTrue(raised(helper, station), "raised behind the docked cart");
				// Facing west: the front is west, so the stop stands on the rear (east) side, beyond the block's east edge.
				net.minecraft.world.phys.AABB shape = helper.getBlockState(station).getShape(helper.getLevel(), helper.absolutePos(station)).bounds();
				helper.assertTrue(shape.maxX > 1.0 && shape.minX >= 0.0 && shape.maxY > 0.5, "the raised stop stands past the rail's east (rear) edge: " + shape);
				log.add("cart came from the west onto a west-facing rail: front WEST, stop raised; outline " + shape);
				cart.discard();
			}))
			.thenWaitUntil(() -> helper.assertFalse(raised(helper, station), "the stop folds once its cart is gone"))
			.thenExecute(CableGameTests.step(failed, () -> {
				log.add("docked cart destroyed: stop folded by itself");
				SimpleBlockTestLog.info("LT-05: {}", log);
			}))
			.thenSucceed();
	}
}
