package io.github.boxofdog.simpleblock.gametest;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.vehicle.minecart.Minecart;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.RailShape;

import net.fabricmc.fabric.api.gametest.v1.GameTest;

import io.github.boxofdog.simpleblock.feature.MinecartCargo;
import io.github.boxofdog.simpleblock.skip.OreSkip;
import io.github.boxofdog.simpleblock.skip.SkipStations;
import io.github.boxofdog.simpleblock.train.LoadingTrackBlock;
import io.github.boxofdog.simpleblock.train.LoadingTracks;
import io.github.boxofdog.simpleblock.train.TrainContent;

/** M13 freight and cable repairs (BUG-006 to BUG-021): server tests. */
public class FreightFixesGameTests {
	private static final String SHAFT = SkipGameTests.SHAFT;

	private static int cartCount(Minecart cart) {
		return MinecartCargo.cart(cart).simpleBlock$count();
	}

	/**
	 * BUG-006: a docked cart at the skip's bridle height tips its load in over either short end, in front
	 * or behind the skip (the lid's doors open to the sides, and the cart's gate on the skip's side opens);
	 * behind the skip at the chute's height it is never filled (only the front has the chute).
	 */
	@GameTest(structure = SHAFT, maxTicks = 1200)
	public void skipTakesCargoFromEitherEnd(GameTestHelper helper) {
		ServerPlayer player = TestPlayers.create(helper, GameType.SURVIVAL, new BlockPos(8, 1, 6));
		SkipGameTests.layShaft(helper, player);
		OreSkip skip = SkipGameTests.spawnSkip(helper, SkipGameTests.COLUMN);
		Minecart[] cart = new Minecart[1];
		boolean[] lidSeen = new boolean[1];
		boolean[] gateSeen = new boolean[1];
		List<String> log = new ArrayList<>();
		boolean[] failed = {false};
		BlockPos front = SkipGameTests.COLUMN.above(4).north();
		BlockPos rear = SkipGameTests.COLUMN.above(4).south();
		BlockPos rearLow = SkipGameTests.COLUMN.south();
		helper.startSequence()
			.thenIdle(2)
			.thenExecute(CableGameTests.step(failed, () -> {
				SkipGameTests.route(helper, player);
				CableGameTests.teleport(helper, player, new BlockPos(12, 1, 8));
				SkipGameTests.hang(helper, player, skip);
				CableGameTests.teleport(helper, player, new BlockPos(3, 1, 9));
				rail(helper, rear, Direction.NORTH);
				cart[0] = SkipGameTests.cartOn(helper, rear, Items.RAW_IRON, 100);
				SkipGameTests.holdAt(helper, skip, 2.0, Items.AIR, 0);
			}))
			.thenExecuteFor(160, () -> {
				lidSeen[0] |= skip.lidOpen();
				gateSeen[0] |= LoadingTracks.freight(cart[0]).simpleBlock$gateOpen();
			})
			.thenExecute(CableGameTests.step(failed, () -> {
				helper.assertTrue(skip.count() == 100 && cartCount(cart[0]) == 0, "behind the skip: skip " + skip.count() + ", cart " + cartCount(cart[0]));
				helper.assertTrue(lidSeen[0] && gateSeen[0], "behind the skip: lid open " + lidSeen[0] + ", cart gate open " + gateSeen[0]);
				log.add("behind: 100 in, lid and the cart's front gate opened");
				cart[0].discard();
				clear(helper, rear);
				rail(helper, front, Direction.SOUTH);
				cart[0] = SkipGameTests.cartOn(helper, front, Items.RAW_IRON, 50);
			}))
			.thenIdle(160)
			.thenExecute(CableGameTests.step(failed, () -> {
				helper.assertTrue(skip.count() == 150 && cartCount(cart[0]) == 0, "in front: skip " + skip.count() + ", cart " + cartCount(cart[0]));
				log.add("in front: 50 more in");
				cart[0].discard();
				clear(helper, front);
				// Behind at the chute's height: no station (the chute is at the front).
				rail(helper, rearLow, Direction.NORTH);
				cart[0] = SkipGameTests.cartOn(helper, rearLow, Items.AIR, 0);
				SkipGameTests.holdAt(helper, skip, 2.0 + SkipStations.UNLOAD_OFFSET - 1.0, Items.RAW_IRON, 150);
			}))
			.thenIdle(60)
			.thenExecute(CableGameTests.step(failed, () -> {
				helper.assertTrue(skip.count() == 150 && cartCount(cart[0]) == 0, "behind at the chute height: skip " + skip.count() + ", cart "
					+ cartCount(cart[0]));
				log.add("behind at the chute's height: nothing moved");
				SimpleBlockTestLog.info("BUG-006 skip intake from either end: {}", log);
				cart[0].discard();
				skip.discard();
				TestPlayers.remove(player);
			}))
			.thenSucceed();
	}

	static void rail(GameTestHelper helper, BlockPos rail, Direction facing) {
		helper.setBlock(rail.below(), Blocks.STONE);
		helper.setBlock(rail, TrainContent.LOADING_TRACK.defaultBlockState().setValue(LoadingTrackBlock.FACING, facing)
			.setValue(LoadingTrackBlock.SHAPE, facing.getAxis() == Direction.Axis.Z ? RailShape.NORTH_SOUTH : RailShape.EAST_WEST));
	}

	static void clear(GameTestHelper helper, BlockPos rail) {
		helper.setBlock(rail, Blocks.AIR);
		helper.setBlock(rail.below(), rail.getY() == 1 ? Blocks.STONE : Blocks.AIR);
	}
}
