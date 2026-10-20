package io.github.boxofdog.simpleblock.gametest;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.protocol.game.ServerboundAttackPacket;
import net.minecraft.network.protocol.game.ServerboundInteractPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.entity.vehicle.minecart.AbstractMinecart;
import net.minecraft.world.entity.vehicle.minecart.Minecart;
import net.minecraft.world.entity.vehicle.minecart.MinecartFurnace;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ButtonBlock;
import net.minecraft.world.level.block.LeverBlock;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.phys.Vec3;

import net.fabricmc.fabric.api.gametest.v1.GameTest;

import io.github.boxofdog.simpleblock.feature.MinecartCargo;
import io.github.boxofdog.simpleblock.train.CobblestoneEngine;
import io.github.boxofdog.simpleblock.train.Couplings;
import io.github.boxofdog.simpleblock.train.EngineSelector;
import io.github.boxofdog.simpleblock.train.FreightPorts;
import io.github.boxofdog.simpleblock.train.FreightStations;
import io.github.boxofdog.simpleblock.train.LoadingTrackBlock;
import io.github.boxofdog.simpleblock.train.LoadingTracks;

/**
 * M15 Cobblestone Engine selector and multi-wagon freight stations, on {@code cable_yard}: track east-west
 * along z 4 (rails at y 1), Loading Rails two blocks apart, hoppers over the rails and beside them one
 * block down. Engines are started by a player's empty-hand use, as in play.
 */
public class FreightStationGameTests {
	private static final String YARD = "simple_block-gametest:cable_yard";
	private static final int Z = 4;

	private static void station(GameTestHelper helper, int... xs) {
		TrainGameTests.track(helper, Z, 2, 27);
		for (int x : xs) {
			LoadingTrackGameTests.loadingTrack(helper, new BlockPos(x, 1, Z), Direction.EAST);
		}
	}

	private static HopperBlockEntity hopper(GameTestHelper helper, BlockPos pos, ItemStack... contents) {
		return FreightTransferGameTests.hopper(helper, pos, Direction.DOWN, contents);
	}

	private static int count(Minecart cart) {
		return MinecartCargo.cart(cart).simpleBlock$count();
	}

	private static int held(HopperBlockEntity hopper, Item item) {
		int n = 0;
		for (int slot = 0; slot < hopper.getContainerSize(); slot++) {
			if (hopper.getItem(slot).is(item)) {
				n += hopper.getItem(slot).getCount();
			}
		}
		return n;
	}

	private static Minecart wagon(GameTestHelper helper, double x, Item item, int count) {
		Minecart cart = TrainGameTests.cart(helper, x, Z);
		if (count > 0) {
			MinecartCargo.cart(cart).simpleBlock$setCargo(item, count);
		}
		return cart;
	}

	private static Minecart seat(GameTestHelper helper, double x) {
		Minecart cart = TrainGameTests.cart(helper, x, Z);
		MinecartCargo.cart(cart).simpleBlock$setCushion(DyeColor.WHITE);
		return cart;
	}

	private static String where(GameTestHelper helper, List<? extends AbstractMinecart> train) {
		List<String> xs = new ArrayList<>();
		for (AbstractMinecart car : train) {
			xs.add(String.format(java.util.Locale.ROOT, "%.2f", TrainGameTests.x(helper, car)));
		}
		return xs.toString();
	}

	private static void lever(GameTestHelper helper, BlockPos at, Direction facing, boolean on) {
		helper.setBlock(at.relative(facing.getOpposite()), Blocks.STONE);
		helper.setBlock(at, Blocks.LEVER.defaultBlockState().setValue(LeverBlock.FACE, AttachFace.WALL).setValue(LeverBlock.FACING, facing)
			.setValue(LeverBlock.POWERED, on));
	}

	private static void setLever(GameTestHelper helper, BlockPos at, boolean on) {
		helper.setBlock(at, helper.getBlockState(at).setValue(LeverBlock.POWERED, on));
	}

	/**
	 * A station of three Loading Rails (x 18, 20, 22) and an engine pulling three wagons east in SERVICE. The
	 * leading car (the engine) stops on the terminal rail (x 22), not the first one it meets, and the cars
	 * behind it are pulled to 2-block berths: A on x 20, B on x 18, C (past the run) on plain rail at x 16.
	 * At once: A loads 40 raw iron from a hopper two clear blocks over it (and, its way latched, never tips
	 * into the empty hopper beside it when its source runs dry), B (the hopper over it holds only iron it
	 * cannot take) unloads 30 raw copper into the hopper beside it, C (no Loading Rail) keeps its gold. Had
	 * the train stopped at the first rail it met, A would have been under that iron. When both are over the lever
	 * goes to NEUTRAL by itself, the train still held; at THROUGH it is let go, the stops fold, and nothing
	 * moves until someone drives it.
	 */
	@GameTest(structure = YARD, maxTicks = 1600)
	public void stationServesWagonsTogetherAtTheTerminalRail(GameTestHelper helper) {
		station(helper, 18, 20, 22);
		ServerPlayer player = TestPlayers.create(helper, GameType.SURVIVAL, new BlockPos(1, 1, 6));
		HopperBlockEntity supplyA = hopper(helper, new BlockPos(20, 4, Z), new ItemStack(Items.RAW_IRON, 40));
		HopperBlockEntity besideA = hopper(helper, new BlockPos(20, 0, Z - 1));
		// Over B: three raw iron B (copper) cannot take; an empty wagon stopped there would load it.
		HopperBlockEntity ironOverB = hopper(helper, new BlockPos(18, 2, Z), new ItemStack(Items.RAW_IRON, 3));
		HopperBlockEntity receiverB = hopper(helper, new BlockPos(18, 0, Z + 1));
		HopperBlockEntity besideC = hopper(helper, new BlockPos(16, 0, Z + 1));
		MinecartFurnace engine = TrainGameTests.engine(helper, 8.7, Z, 4);
		Minecart a = wagon(helper, 7.1, Items.AIR, 0);
		Minecart b = wagon(helper, 5.5, Items.RAW_COPPER, 30);
		Minecart c = wagon(helper, 3.9, Items.RAW_GOLD, 10);
		List<AbstractMinecart> train = List.of(engine, a, b, c);
		List<String> log = new ArrayList<>();
		double[] held = new double[4];
		long[] times = new long[2];
		helper.startSequence()
			.thenIdle(2)
			.thenExecute(() -> {
				TrainGameTests.couple(player, engine, a);
				TrainGameTests.couple(player, a, b);
				TrainGameTests.couple(player, b, c);
				EngineSelector.set(engine, EngineSelector.Mode.SERVICE);
				TrainGameTests.use(player, engine, ItemStack.EMPTY);
				helper.assertTrue(CobblestoneEngine.engine(engine).simpleBlock$running(), "started east");
			})
			.thenWaitUntil(() -> helper.assertTrue(Couplings.docked(engine), "docked: " + where(helper, train)))
			.thenExecute(() -> {
				times[0] = helper.getTick();
				double[] expect = {22.5, 20.5, 18.5, 16.5};
				for (int i = 0; i < 4; i++) {
					held[i] = TrainGameTests.x(helper, train.get(i));
					helper.assertTrue(Math.abs(held[i] - expect[i]) < 0.01, "car " + i + " on its berth: " + where(helper, train));
				}
				helper.assertTrue(Couplings.coupled(a).simpleBlock$dock() != null && Couplings.coupled(b).simpleBlock$dock() != null
					&& Couplings.coupled(c).simpleBlock$dock() == null && Couplings.coupled(engine).simpleBlock$dock() == null,
					"A and B docked on Loading Rails, C (plain rail) and the engine not");
				helper.assertTrue(helper.getBlockState(new BlockPos(20, 1, Z)).getValue(LoadingTrackBlock.RAISED), "stops raised under the wagons");
				log.add("docked with the engine on the terminal rail, berths " + where(helper, train) + " (2.0 apart)");
			})
			.thenWaitUntil(() -> helper.assertValueEqual(EngineSelector.mode(engine), EngineSelector.Mode.NEUTRAL, "service over (A "
				+ count(a) + ", B " + count(b) + ")"))
			.thenExecute(() -> {
				times[1] = helper.getTick();
				helper.assertTrue(count(a) == 40 && MinecartCargo.cart(a).simpleBlock$cargo() == Items.RAW_IRON && held(supplyA, Items.RAW_IRON) == 0,
					"A loaded all 40 from two clear blocks up: " + count(a));
				helper.assertTrue(held(besideA, Items.RAW_IRON) == 0, "A, loading, never tipped into the hopper beside it");
				helper.assertTrue(count(b) == 0 && held(receiverB, Items.RAW_COPPER) == 30, "B unloaded all 30: " + count(b));
				helper.assertTrue(held(ironOverB, Items.RAW_COPPER) == 0 && held(ironOverB, Items.RAW_IRON) == 3,
					"nothing went up, and the iron over B (which an early stop would have put A under) stayed");
				helper.assertTrue(count(c) == 10 && held(besideC, Items.RAW_GOLD) == 0, "C, past the station's rails, untouched");
				helper.assertValueEqual(LoadingTracks.freight(a).simpleBlock$service(), FreightStations.Service.DONE, "A done");
				helper.assertValueEqual(LoadingTracks.freight(b).simpleBlock$service(), FreightStations.Service.DONE, "B done");
				for (int i = 0; i < 4; i++) {
					helper.assertTrue(Math.abs(TrainGameTests.x(helper, train.get(i)) - held[i]) < 0.01, "held through the service");
				}
				log.add("A loaded 40, B unloaded 30, C untouched; NEUTRAL by itself after " + (times[1] - times[0]) + " ticks, still held");
				EngineSelector.set(engine, EngineSelector.Mode.THROUGH);
				for (AbstractMinecart car : train) {
					helper.assertTrue(Couplings.coupled(car).simpleBlock$dock() == null, "released at THROUGH");
				}
			})
			.thenIdle(30)
			.thenExecute(() -> {
				helper.assertFalse(helper.getBlockState(new BlockPos(20, 1, Z)).getValue(LoadingTrackBlock.RAISED), "stops folded");
				for (int i = 0; i < 4; i++) {
					helper.assertTrue(Math.abs(TrainGameTests.x(helper, train.get(i)) - held[i]) < 0.01, "THROUGH does not drive the train");
				}
				log.add("THROUGH: let go, stops folded, nothing moved in 30 ticks");
				SimpleBlockTestLog.info("M15 STATION: {}", log);
				train.forEach(AbstractMinecart::discard);
				TestPlayers.remove(player);
				helper.succeed();
			});
	}

	/**
	 * Trains that must not stop: SERVICE with nothing to do (no hoppers), THROUGH and NEUTRAL with work
	 * waiting, and SERVICE where the only car that would stand on a Loading Rail is the engine. Each runs the
	 * whole station without docking and without moving an item.
	 */
	@GameTest(structure = YARD, maxTicks = 3000)
	public void trainsWithoutServicePassTheStation(GameTestHelper helper) {
		station(helper, 16, 18);
		ServerPlayer player = TestPlayers.create(helper, GameType.SURVIVAL, new BlockPos(1, 1, 6));
		List<String> log = new ArrayList<>();
		record Run(String name, EngineSelector.Mode mode, boolean hoppers, boolean engineFirstOnly) {
		}
		List<Run> runs = List.of(
			new Run("SERVICE, no work", EngineSelector.Mode.SERVICE, false, false),
			new Run("THROUGH, work waiting", EngineSelector.Mode.THROUGH, true, false),
			new Run("NEUTRAL, work waiting", EngineSelector.Mode.NEUTRAL, true, false),
			new Run("SERVICE, only the engine on a rail", EngineSelector.Mode.SERVICE, true, true));
		var sequence = helper.startSequence();
		for (Run run : runs) {
			MinecartFurnace[] engine = new MinecartFurnace[1];
			Minecart[] wagon = new Minecart[1];
			boolean[] docked = new boolean[1];
			sequence = sequence
				.thenExecute(() -> {
					for (BlockPos pos : List.of(new BlockPos(16, 2, Z), new BlockPos(18, 2, Z), new BlockPos(16, 0, Z + 1), new BlockPos(18, 0, Z + 1))) {
						helper.setBlock(pos, Blocks.AIR);
					}
					if (run.hoppers()) {
						hopper(helper, new BlockPos(16, 2, Z), new ItemStack(Items.RAW_IRON, 20));
						hopper(helper, new BlockPos(18, 2, Z), new ItemStack(Items.RAW_IRON, 20));
						hopper(helper, new BlockPos(16, 0, Z + 1));
						hopper(helper, new BlockPos(18, 0, Z + 1));
					}
					// The engine leads east, a wagon (20 copper) behind it.
					engine[0] = TrainGameTests.engine(helper, 6.7, Z, 4);
					wagon[0] = wagon(helper, 5.1, Items.RAW_COPPER, 20);
					TrainGameTests.couple(player, engine[0], wagon[0]);
					EngineSelector.set(engine[0], run.mode());
					if (run.engineFirstOnly()) {
						// A one-rail station (x 18): lined up, the engine would stand on it and the wagon on plain rail at x 16.
						helper.setBlock(new BlockPos(16, 1, Z), Blocks.RAIL.defaultBlockState().setValue(net.minecraft.world.level.block.RailBlock.SHAPE,
							net.minecraft.world.level.block.state.properties.RailShape.EAST_WEST));
					}
					CableGameTests.teleport(helper, player, new BlockPos(1, 1, 6));
					TrainGameTests.use(player, engine[0], ItemStack.EMPTY);
				})
				.thenWaitUntil(() -> {
					docked[0] |= Couplings.docked(engine[0]);
					helper.assertTrue(TrainGameTests.x(helper, wagon[0]) > 20.0, run.name() + ": waiting for the train to pass the station");
				})
				.thenExecute(() -> {
					helper.assertFalse(docked[0], run.name() + ": never docked");
					helper.assertValueEqual(count(wagon[0]), 20, run.name() + ": no item moved");
					log.add(run.name() + ": passed");
					engine[0].discard();
					wagon[0].discard();
					if (run.engineFirstOnly()) {
						LoadingTrackGameTests.loadingTrack(helper, new BlockPos(16, 1, Z), Direction.EAST);
					}
				});
		}
		sequence.thenExecute(() -> {
			SimpleBlockTestLog.info("M15 PASSING: {}", log);
			TestPlayers.remove(player);
			helper.succeed();
		});
	}

	/**
	 * By hand: a train standing over a two-rail station, every hopper capped (a button by the hopper over
	 * A, a lever off by the hopper beside B). SERVICE chosen by hand lines it up and docks it at once, and it
	 * waits armed (no auto NEUTRAL with nothing begun). The button opens A's hopper for one whole load, then
	 * its lid closes and the lever goes to NEUTRAL. SERVICE again with B's lever on: B unloads; its lever off
	 * stops it at once (not another item), and after the grace the service ends. Leaving SERVICE mid-transfer
	 * (NEUTRAL) stops it at once, held; nothing resumes at NEUTRAL; and SERVICE flicked straight on to THROUGH
	 * (inside the arming delay) moves nothing and lets the train go. Copper is counted throughout.
	 */
	@GameTest(structure = YARD, maxTicks = 3000)
	public void handDockingButtonsLeversAndCancelling(GameTestHelper helper) {
		station(helper, 18, 20);
		ServerPlayer player = TestPlayers.create(helper, GameType.SURVIVAL, new BlockPos(14, 1, 7));
		HopperBlockEntity supplyA = hopper(helper, new BlockPos(20, 2, Z), new ItemStack(Items.RAW_IRON, 20));
		BlockPos button = new BlockPos(20, 2, Z + 1);
		helper.setBlock(button.south(), Blocks.STONE);
		helper.setBlock(button, Blocks.STONE_BUTTON.defaultBlockState().setValue(ButtonBlock.FACE, AttachFace.WALL).setValue(ButtonBlock.FACING, Direction.NORTH));
		HopperBlockEntity receiverB = hopper(helper, new BlockPos(18, 0, Z + 1));
		BlockPos leverB = new BlockPos(18, 0, Z + 2);
		lever(helper, leverB, Direction.NORTH, false);
		Minecart a = wagon(helper, 20.3, Items.AIR, 0);
		Minecart b = wagon(helper, 18.7, Items.RAW_COPPER, 200);
		MinecartFurnace engine = TrainGameTests.engine(helper, 17.1, Z, 4);
		List<AbstractMinecart> train = List.of(a, b, engine);
		List<String> log = new ArrayList<>();
		int[] mark = new int[2];
		helper.startSequence()
			.thenIdle(2)
			.thenExecute(() -> {
				TrainGameTests.couple(player, a, b);
				TrainGameTests.couple(player, b, engine);
				EngineSelector.set(engine, EngineSelector.Mode.SERVICE);
				helper.assertTrue(Couplings.coupled(a).simpleBlock$dock() != null && Couplings.coupled(b).simpleBlock$dock() != null,
					"SERVICE by hand docks the standing train: " + where(helper, train));
				helper.assertTrue(Math.abs(TrainGameTests.x(helper, a) - 20.5) < 0.01 && Math.abs(TrainGameTests.x(helper, b) - 18.5) < 0.01
					&& Math.abs(TrainGameTests.x(helper, engine) - 16.5) < 0.01, "lined up 2 apart: " + where(helper, train));
			})
			.thenIdle(60)
			.thenExecute(() -> {
				helper.assertTrue(count(a) == 0 && count(b) == 200, "all capped: nothing moved (A " + count(a) + ", B " + count(b) + ")");
				helper.assertValueEqual(EngineSelector.mode(engine), EngineSelector.Mode.SERVICE, "armed and waiting, not NEUTRAL");
				log.add("hand docking with every hopper capped: lined up, waits armed");
				BlockState state = helper.getBlockState(button);
				((ButtonBlock) state.getBlock()).press(state, helper.getLevel(), helper.absolutePos(button), player);
			})
			.thenWaitUntil(() -> helper.assertValueEqual(EngineSelector.mode(engine), EngineSelector.Mode.NEUTRAL, "button transfer over (A " + count(a) + ")"))
			.thenExecute(() -> {
				helper.assertTrue(count(a) == 20 && held(supplyA, Items.RAW_IRON) == 0, "the button opened A's hopper for one whole load: " + count(a));
				helper.assertTrue(LoadingTracks.freight(a).simpleBlock$port() == null, "the button's opening closed again");
				helper.assertTrue(count(b) == 200, "B's capped hopper untouched");
				log.add("button: A loaded all 20, lid closed, NEUTRAL");
				setLever(helper, leverB, true);
				EngineSelector.set(engine, EngineSelector.Mode.SERVICE);
			})
			.thenWaitUntil(() -> helper.assertTrue(count(b) <= 150, "B unloading through its opened lever: " + count(b)))
			.thenExecute(() -> {
				setLever(helper, leverB, false);
				mark[0] = count(b);
			})
			.thenIdle(10)
			.thenExecute(() -> {
				helper.assertTrue(count(b) >= mark[0] - 1, "lever off: shut at once (" + mark[0] + " -> " + count(b) + ")");
				helper.assertValueEqual(count(b) + held(receiverB, Items.RAW_COPPER), 200, "copper counted");
				mark[0] = count(b);
			})
			.thenWaitUntil(() -> helper.assertValueEqual(EngineSelector.mode(engine), EngineSelector.Mode.NEUTRAL, "B's transfer over after the grace"))
			.thenExecute(() -> {
				helper.assertValueEqual(count(b), mark[0], "nothing more through a capped hopper");
				log.add("lever on: B unloaded to " + mark[0] + "; lever off shut it at once; NEUTRAL after the grace");
				setLever(helper, leverB, true);
				EngineSelector.set(engine, EngineSelector.Mode.SERVICE);
				mark[1] = count(b);
			})
			.thenWaitUntil(() -> helper.assertTrue(count(b) < mark[1] - 5, "B unloading again"))
			.thenExecute(() -> {
				EngineSelector.set(engine, EngineSelector.Mode.NEUTRAL);
				mark[0] = count(b);
				helper.assertValueEqual(LoadingTracks.freight(b).simpleBlock$service(), FreightStations.Service.DONE, "NEUTRAL stops the transfer");
			})
			.thenIdle(40)
			.thenExecute(() -> {
				helper.assertValueEqual(count(b), mark[0], "NEUTRAL: stopped at once, nothing resumes");
				helper.assertTrue(Couplings.coupled(b).simpleBlock$dock() != null, "NEUTRAL keeps the hold");
				helper.assertValueEqual(count(b) + held(receiverB, Items.RAW_COPPER), 200, "copper counted");
				log.add("NEUTRAL mid-transfer: stopped at " + mark[0] + ", held, nothing resumed in 40 ticks");
				// SERVICE then straight on to THROUGH within the arming delay: nothing moves, the train goes free.
				// (A's hopper, its button gone, is open and offers more iron: a loading wagon would take it at once.)
				helper.setBlock(button, Blocks.AIR);
				supplyA.setItem(0, new ItemStack(Items.RAW_IRON, 10));
				EngineSelector.set(engine, EngineSelector.Mode.SERVICE);
			})
			.thenIdle(10)
			.thenExecute(() -> {
				EngineSelector.set(engine, EngineSelector.Mode.THROUGH);
				helper.assertValueEqual(count(b), mark[0], "a quick pass through SERVICE moved nothing");
				helper.assertTrue(count(a) == 20 && held(supplyA, Items.RAW_IRON) == 10, "nor loaded anything: A " + count(a));
				for (AbstractMinecart car : train) {
					helper.assertTrue(Couplings.coupled(car).simpleBlock$dock() == null, "THROUGH lets go");
				}
				log.add("SERVICE flicked on to THROUGH inside the arming delay: nothing moved, released");
				SimpleBlockTestLog.info("M15 HAND: {}", log);
				train.forEach(AbstractMinecart::discard);
				TestPlayers.remove(player);
				helper.succeed();
			});
	}

	/**
	 * Supplying hoppers' reach and the controls' binding. A four-rail station (x 16 to 22) docked by hand with
	 * the engine at the rear: hoppers over the wagons with 1, 3 and 4 clear blocks under their spouts (the
	 * last too far), and one straight over its rail with a redstone block beside it; the first three load
	 * (redstone does not lock a freight hopper), the far one does not. Then the binding rules on their own: a
	 * lever between two freight hoppers caps both whatever its position (fail closed); a lever beside just
	 * one governs only that one; a button makes its hopper button-controlled.
	 */
	@GameTest(structure = YARD, maxTicks = 1200)
	public void hopperReachRedstoneAndControlBinding(GameTestHelper helper) {
		station(helper, 16, 18, 20, 22);
		ServerPlayer player = TestPlayers.create(helper, GameType.SURVIVAL, new BlockPos(10, 1, 7));
		HopperBlockEntity oneClear = hopper(helper, new BlockPos(22, 3, Z), new ItemStack(Items.RAW_IRON, 5));
		HopperBlockEntity threeClear = hopper(helper, new BlockPos(20, 5, Z), new ItemStack(Items.RAW_IRON, 5));
		HopperBlockEntity fourClear = hopper(helper, new BlockPos(18, 6, Z), new ItemStack(Items.RAW_IRON, 5));
		HopperBlockEntity powered = hopper(helper, new BlockPos(16, 2, Z), new ItemStack(Items.RAW_IRON, 5));
		helper.setBlock(new BlockPos(16, 2, Z + 1), Blocks.REDSTONE_BLOCK);
		Minecart w22 = wagon(helper, 22.3, Items.AIR, 0);
		Minecart w20 = wagon(helper, 20.7, Items.AIR, 0);
		Minecart w18 = wagon(helper, 19.1, Items.AIR, 0);
		Minecart w16 = wagon(helper, 17.5, Items.AIR, 0);
		MinecartFurnace engine = TrainGameTests.engine(helper, 15.9, Z, 1);
		List<AbstractMinecart> train = List.of(w22, w20, w18, w16, engine);
		List<String> log = new ArrayList<>();
		helper.startSequence()
			.thenIdle(2)
			.thenExecute(() -> {
				for (int i = 1; i < train.size(); i++) {
					TrainGameTests.couple(player, train.get(i - 1), train.get(i));
				}
				EngineSelector.set(engine, EngineSelector.Mode.SERVICE);
				helper.assertTrue(Couplings.docked(engine), "docked by hand: " + where(helper, train));
			})
			.thenWaitUntil(() -> helper.assertValueEqual(EngineSelector.mode(engine), EngineSelector.Mode.NEUTRAL, "service over"))
			.thenExecute(() -> {
				helper.assertTrue(count(w22) == 5 && held(oneClear, Items.RAW_IRON) == 0, "one clear block: loaded " + count(w22));
				helper.assertTrue(count(w20) == 5 && held(threeClear, Items.RAW_IRON) == 0, "three clear blocks: loaded " + count(w20));
				helper.assertTrue(count(w18) == 0 && held(fourClear, Items.RAW_IRON) == 5, "four clear blocks: too far, " + count(w18));
				helper.assertTrue(count(w16) == 5 && held(powered, Items.RAW_IRON) == 0, "redstone beside it does not lock a freight hopper: " + count(w16));
				log.add("1 and 3 clear blocks load, 4 do not; a redstone-powered freight hopper still feeds");
				EngineSelector.set(engine, EngineSelector.Mode.THROUGH);
				// The binding rules.
				var level = helper.getLevel();
				BlockPos p = helper.absolutePos(new BlockPos(22, 3, Z));
				BlockPos q = helper.absolutePos(new BlockPos(20, 3, Z));
				helper.setBlock(new BlockPos(20, 5, Z), Blocks.AIR);
				hopper(helper, new BlockPos(20, 3, Z));
				lever(helper, new BlockPos(21, 3, Z), Direction.SOUTH, true);
				helper.assertTrue(FreightPorts.control(level, p) == FreightPorts.Control.CAPPED
					&& FreightPorts.control(level, q) == FreightPorts.Control.CAPPED,
					"a lever between two freight hoppers caps both, even on");
				helper.setBlock(new BlockPos(21, 3, Z), Blocks.AIR);
				helper.setBlock(new BlockPos(21, 3, Z - 1), Blocks.AIR);
				lever(helper, new BlockPos(22, 3, Z + 1), Direction.NORTH, true);
				boolean onOpen = FreightPorts.control(level, p) == FreightPorts.Control.OPEN;
				setLever(helper, new BlockPos(22, 3, Z + 1), false);
				boolean offCapped = FreightPorts.control(level, p) == FreightPorts.Control.CAPPED;
				boolean otherOpen = FreightPorts.control(level, q) == FreightPorts.Control.OPEN;
				helper.assertTrue(onOpen && offCapped && otherOpen, "a lever beside one hopper governs only it: on " + onOpen + ", off " + offCapped + ", other " + otherOpen);
				helper.setBlock(new BlockPos(20, 3, Z + 2), Blocks.STONE);
				helper.setBlock(new BlockPos(20, 3, Z + 1), Blocks.STONE_BUTTON.defaultBlockState().setValue(ButtonBlock.FACE, AttachFace.WALL)
					.setValue(ButtonBlock.FACING, Direction.NORTH));
				helper.assertTrue(FreightPorts.control(level, q) == FreightPorts.Control.BUTTON,
					"a button makes its hopper button-controlled");
				log.add("binding: a shared lever caps both (fail closed); a lever beside one governs only it; a button marks its hopper");
				SimpleBlockTestLog.info("M15 REACH AND BINDING: {}", log);
				train.forEach(AbstractMinecart::discard);
				TestPlayers.remove(player);
				helper.succeed();
			});
	}

	private static void jump(ServerPlayer player, boolean jump) {
		player.setLastClientInput(new Input(false, false, false, false, jump, false, false));
	}

	/**
	 * The selector's controls. Space: a rider in the seat coupled straight to the engine steps it once per
	 * press (held, it does not repeat) round THROUGH, NEUTRAL, SERVICE; a rider in a second seat further down
	 * does nothing; the only seat of a train steps its engine though a wagon is between; a seat between two
	 * engines steps neither. By hand: a hit on the lever steps it left, a use steps it right, the ends stay;
	 * a hit elsewhere on the engine does nothing to it. The setting is saved with the engine.
	 */
	@GameTest(structure = YARD, maxTicks = 400)
	public void selectorSpaceClicksAndSaving(GameTestHelper helper) {
		TrainGameTests.track(helper, Z, 2, 27);
		TrainGameTests.track(helper, Z + 3, 2, 27);
		ServerPlayer rider = TestPlayers.create(helper, GameType.SURVIVAL, new BlockPos(1, 1, 1));
		ServerPlayer hand = TestPlayers.create(helper, GameType.SURVIVAL, new BlockPos(1, 1, 1));
		MinecartFurnace engine = TrainGameTests.engine(helper, 10.5, Z, 1);
		Minecart near = seat(helper, 12.1);
		Minecart far = seat(helper, 13.7);
		MinecartFurnace engine2 = TrainGameTests.engine(helper, 18.5, Z, 1);
		Minecart wagon = TrainGameTests.cart(helper, 20.1, Z);
		Minecart only = seat(helper, 21.7);
		List<String> log = new ArrayList<>();
		helper.startSequence()
			.thenIdle(2)
			.thenExecute(() -> {
				TrainGameTests.couple(rider, engine, near);
				TrainGameTests.couple(rider, near, far);
				TrainGameTests.couple(rider, engine2, wagon);
				TrainGameTests.couple(rider, wagon, only);
				rider.startRiding(far);
				jump(rider, true);
			})
			.thenIdle(3)
			.thenExecute(() -> {
				jump(rider, false);
				helper.assertValueEqual(EngineSelector.mode(engine), EngineSelector.Mode.THROUGH, "a second seat further down has no Space control");
				rider.stopRiding();
				rider.startRiding(near);
			})
			.thenIdle(2)
			.thenExecute(() -> jump(rider, true))
			.thenIdle(5)
			.thenExecute(() -> {
				helper.assertValueEqual(EngineSelector.mode(engine), EngineSelector.Mode.NEUTRAL, "one press, one step (held 5 ticks)");
				jump(rider, false);
			})
			.thenIdle(2)
			.thenExecute(() -> jump(rider, true))
			.thenIdle(2)
			.thenExecute(() -> jump(rider, false))
			.thenIdle(2)
			.thenExecute(() -> {
				helper.assertValueEqual(EngineSelector.mode(engine), EngineSelector.Mode.SERVICE, "second press: SERVICE");
				jump(rider, true);
			})
			.thenIdle(2)
			.thenExecute(() -> {
				jump(rider, false);
				helper.assertValueEqual(EngineSelector.mode(engine), EngineSelector.Mode.THROUGH, "third press: round to THROUGH");
				log.add("Space from the coupled seat: THROUGH -> NEUTRAL -> SERVICE -> THROUGH, one step a press; the far seat none");
				rider.stopRiding();
				rider.startRiding(only);
			})
			.thenIdle(2)
			.thenExecute(() -> jump(rider, true))
			.thenIdle(2)
			.thenExecute(() -> {
				jump(rider, false);
				helper.assertValueEqual(EngineSelector.mode(engine2), EngineSelector.Mode.NEUTRAL, "the only seat works its engine past a wagon");
				rider.stopRiding();
			})
			.thenIdle(2)
			.thenExecute(() -> {
				log.add("the only seat of a train works its engine across a wagon");
				// By hand, from behind the lever.
				Vec3 leverAt = EngineSelector.fromModel(engine, EngineSelector.LEVER_X, EngineSelector.LEVER_Y, EngineSelector.LEVER_Z);
				Vec3 away = leverAt.subtract(engine.position()).multiply(1.0, 0.0, 1.0).normalize();
				Vec3 stand = leverAt.add(away.scale(1.8));
				hand.snapTo(stand.x, engine.getY(), stand.z, 0.0F, 0.0F);
				hand.lookAt(net.minecraft.commands.arguments.EntityAnchorArgument.Anchor.EYES, leverAt);
				hand.connection.handleInteract(new ServerboundInteractPacket(engine.getId(), InteractionHand.MAIN_HAND,
					leverAt.subtract(engine.position()), false));
				helper.assertValueEqual(EngineSelector.mode(engine), EngineSelector.Mode.NEUTRAL, "use on the lever: one step right");
				hand.connection.handleInteract(new ServerboundInteractPacket(engine.getId(), InteractionHand.MAIN_HAND,
					leverAt.subtract(engine.position()), false));
				hand.connection.handleInteract(new ServerboundInteractPacket(engine.getId(), InteractionHand.MAIN_HAND,
					leverAt.subtract(engine.position()), false));
				helper.assertValueEqual(EngineSelector.mode(engine), EngineSelector.Mode.SERVICE, "the right end stays");
				hand.resetAttackStrengthTicker();
				hand.connection.handleAttack(new ServerboundAttackPacket(engine.getId()));
				helper.assertValueEqual(EngineSelector.mode(engine), EngineSelector.Mode.NEUTRAL, "a hit on the lever: one step left");
				// A use at the smokestack end is not on the lever.
				Vec3 front = EngineSelector.fromModel(engine, 2.0, 8.0, 8.0);
				hand.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.STICK));
				hand.connection.handleInteract(new ServerboundInteractPacket(engine.getId(), InteractionHand.MAIN_HAND, front.subtract(engine.position()), false));
				helper.assertValueEqual(EngineSelector.mode(engine), EngineSelector.Mode.NEUTRAL, "a use at the front does not touch the lever");
				log.add("by hand: use steps right (ends stay), hit steps left, the front end does nothing to it");
				// Saved with the engine.
				TagValueOutput out = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, helper.getLevel().registryAccess());
				engine.saveWithoutId(out);
				MinecartFurnace copy = EntityTypes.FURNACE_MINECART.create(helper.getLevel(), EntitySpawnReason.LOAD);
				copy.load(TagValueInput.create(ProblemReporter.DISCARDING, helper.getLevel().registryAccess(), out.buildResult()));
				helper.assertValueEqual(EngineSelector.mode(copy), EngineSelector.Mode.NEUTRAL, "the setting is saved");
				MinecartFurnace fresh = EntityTypes.FURNACE_MINECART.create(helper.getLevel(), EntitySpawnReason.SPAWN_ITEM_USE);
				helper.assertValueEqual(EngineSelector.mode(fresh), EngineSelector.Mode.THROUGH, "a new engine starts at THROUGH");
				log.add("saved and loaded; a new engine starts at THROUGH");
				SimpleBlockTestLog.info("M15 CONTROLS: {}", log);
				for (AbstractMinecart car : List.of(engine, near, far, engine2, wagon, only)) {
					car.discard();
				}
				TestPlayers.remove(rider);
				TestPlayers.remove(hand);
				helper.succeed();
			});
	}

	/** A seat coupled between two engines works neither with Space (it is not clear which is meant). */
	@GameTest(structure = YARD, maxTicks = 100)
	public void seatBetweenTwoEnginesWorksNeither(GameTestHelper helper) {
		TrainGameTests.track(helper, Z, 2, 27);
		ServerPlayer rider = TestPlayers.create(helper, GameType.SURVIVAL, new BlockPos(1, 1, 1));
		MinecartFurnace one = TrainGameTests.engine(helper, 10.5, Z, 1);
		Minecart seat = seat(helper, 12.1);
		MinecartFurnace two = TrainGameTests.engine(helper, 13.7, Z, 1);
		helper.startSequence()
			.thenIdle(2)
			.thenExecute(() -> {
				TrainGameTests.couple(rider, one, seat);
				TrainGameTests.couple(rider, seat, two);
				rider.startRiding(seat);
				jump(rider, true);
			})
			.thenIdle(3)
			.thenExecute(() -> {
				jump(rider, false);
				helper.assertTrue(EngineSelector.seatEngine(seat) == null, "no single engine for the seat");
				helper.assertTrue(EngineSelector.mode(one) == EngineSelector.Mode.THROUGH && EngineSelector.mode(two) == EngineSelector.Mode.THROUGH,
					"neither engine moved");
				SimpleBlockTestLog.info("M15 AMBIGUOUS SEAT: a seat between two engines works neither");
				List.of(one, seat, two).forEach(AbstractMinecart::discard);
				TestPlayers.remove(rider);
				helper.succeed();
			});
	}
}
