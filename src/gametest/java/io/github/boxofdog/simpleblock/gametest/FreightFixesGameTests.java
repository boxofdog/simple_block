package io.github.boxofdog.simpleblock.gametest;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.vehicle.minecart.Minecart;
import net.minecraft.world.item.ItemStack;
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

	/** Turns {@code player} to look at {@code target} (absolute). */
	static void look(ServerPlayer player, net.minecraft.world.phys.Vec3 target) {
		net.minecraft.world.phys.Vec3 d = target.subtract(player.getEyePosition());
		player.setYRot((float) (Math.toDegrees(Math.atan2(d.z, d.x)) - 90.0));
		// A living entity looks the way its head turns.
		player.setYHeadRot(player.getYRot());
		player.setXRot((float) -Math.toDegrees(Math.atan2(d.y, Math.sqrt(d.x * d.x + d.z * d.z))));
	}

	/** Uses the held item on the face of {@code block} (relative) the player looks at, as the client would send it. */
	static void useOn(GameTestHelper helper, ServerPlayer player, BlockPos block, net.minecraft.world.phys.Vec3 point) {
		look(player, point);
		var hit = new net.minecraft.world.phys.BlockHitResult(point, Direction.UP, helper.absolutePos(block), false);
		player.gameMode.useItemOn(player, helper.getLevel(), player.getMainHandItem(), net.minecraft.world.InteractionHand.MAIN_HAND, hit);
	}

	/**
	 * BUG-007: a chain used on the rail just past a cart, the look grazing the cart's end, couples (first
	 * cart, then the second) instead of placing chain blocks over the rails; a chain used on a rail with
	 * no cart in the look still places a chain block (vanilla).
	 */
	@GameTest(structure = SHAFT, maxTicks = 200)
	public void chainOnTheRailBehindACartCouples(GameTestHelper helper) {
		for (int x = 2; x <= 20; x++) {
			helper.setBlock(new BlockPos(x, 0, 6), Blocks.STONE);
			helper.setBlock(new BlockPos(x, 1, 6), Blocks.RAIL.defaultBlockState().setValue(net.minecraft.world.level.block.RailBlock.SHAPE,
				RailShape.EAST_WEST));
		}
		ServerPlayer player = TestPlayers.create(helper, GameType.SURVIVAL, new BlockPos(8, 1, 9));
		player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, new ItemStack(Items.IRON_CHAIN, 4));
		Minecart a = helper.spawn(net.minecraft.world.entity.EntityTypes.MINECART, new net.minecraft.world.phys.Vec3(7.5, 1.0625, 6.5));
		Minecart b = helper.spawn(net.minecraft.world.entity.EntityTypes.MINECART, new net.minecraft.world.phys.Vec3(9.5, 1.0625, 6.5));
		helper.startSequence()
			.thenIdle(5)
			.thenExecute(() -> {
				// The rail block just beyond each cart's far side, the look passing over the cart's edge.
				useOn(helper, player, new BlockPos(7, 1, 5), helper.absoluteVec(new net.minecraft.world.phys.Vec3(7.5, 1.1, 5.95)));
				useOn(helper, player, new BlockPos(9, 1, 5), helper.absoluteVec(new net.minecraft.world.phys.Vec3(9.5, 1.1, 5.95)));
				helper.assertTrue(b.getUUID().equals(io.github.boxofdog.simpleblock.train.Couplings.coupled(a).simpleBlock$coupling(0))
					|| b.getUUID().equals(io.github.boxofdog.simpleblock.train.Couplings.coupled(a).simpleBlock$coupling(1)), "carts coupled");
				int chains = 0;
				for (BlockPos pos : BlockPos.betweenClosed(helper.absolutePos(new BlockPos(2, 1, 4)), helper.absolutePos(new BlockPos(20, 3, 8)))) {
					if (helper.getLevel().getBlockState(pos).is(Blocks.IRON_CHAIN)) {
						chains++;
					}
				}
				helper.assertValueEqual(chains, 0, "chain blocks placed while coupling");
				helper.assertValueEqual(player.getMainHandItem().getCount(), 3, "chains after one coupling");
				// No cart in the look: an ordinary chain block on the rail's far end.
				useOn(helper, player, new BlockPos(19, 1, 6), helper.absoluteVec(new net.minecraft.world.phys.Vec3(19.5, 1.125, 6.5)));
				helper.assertTrue(helper.getBlockState(new BlockPos(19, 2, 6)).is(Blocks.IRON_CHAIN), "a chain block placed with no cart in the look");
				SimpleBlockTestLog.info("BUG-007: chain used on the blocks just past two carts coupled them, no chain blocks; with no cart, a chain block");
				a.discard();
				b.discard();
				TestPlayers.remove(player);
			})
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
