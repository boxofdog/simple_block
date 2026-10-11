package io.github.boxofdog.simpleblock.gametest;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.entity.vehicle.minecart.Minecart;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.phys.Vec3;

import net.fabricmc.fabric.api.gametest.v1.GameTest;

import io.github.boxofdog.simpleblock.feature.MinecartCushions;

/**
 * A cushion fitted into a minecart makes it a passenger cart ({@link MinecartCushions}). Every use
 * goes through {@code Player.interactOn}, the server's handler for a use-entity packet.
 */
public class MinecartCushionGameTests {
	private static void use(ServerPlayer player, Minecart cart, boolean sneaking) {
		player.setShiftKeyDown(sneaking);
		MinecartCargoGameTests.use(player, cart, InteractionHand.MAIN_HAND);
		player.setShiftKeyDown(false);
	}

	/** Fit a cushion, ride, can't take it out from under a rider, take it out, and the cart is a cargo cart again. */
	@GameTest
	public void cushionMakesAMinecartRideableAndComesBackOut(GameTestHelper helper) {
		Minecart cart = MinecartCargoGameTests.cartOnRail(helper, new BlockPos(2, 2, 2));
		ServerPlayer player = TestPlayers.create(helper, GameType.SURVIVAL, new BlockPos(4, 2, 2));
		ServerPlayer other = TestPlayers.create(helper, GameType.SURVIVAL, new BlockPos(0, 2, 2));
		player.getInventory().clearContent();

		use(player, cart, false);
		helper.assertTrue(player.getVehicle() == null, "a cart without a cushion should not seat the player");

		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.CUSHION.pick(DyeColor.RED), 2));
		use(player, cart, false);
		helper.assertValueEqual(MinecartCushions.fitted(cart), DyeColor.RED, "fitted cushion");
		helper.assertValueEqual(player.getMainHandItem().getCount(), 1, "cushions left in hand after fitting one");

		// Using it now seats the player, even with a cushion or cargo in hand; nothing is loaded.
		use(player, cart, false);
		helper.assertTrue(player.getVehicle() == cart, "the player should be sitting in the cushioned cart");
		helper.assertValueEqual(player.getMainHandItem().getCount(), 1, "cushions left in hand after getting in");
		player.stopRiding();
		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.RAW_IRON, 32));
		use(player, cart, false);
		helper.assertTrue(player.getVehicle() == cart, "raw iron in hand: the player should get in, not load");
		helper.assertValueEqual(MinecartCargoGameTests.cargo(cart).simpleBlock$count(), 0, "cargo in the cushioned cart");
		helper.assertValueEqual(player.getMainHandItem().getCount(), 32, "raw iron left in hand");
		player.stopRiding();
		player.getInventory().clearContent();

		// Not from under someone.
		other.setShiftKeyDown(false);
		MinecartCargoGameTests.use(other, cart, InteractionHand.MAIN_HAND);
		helper.assertTrue(other.getVehicle() == cart, "the other player should be sitting in the cart");
		use(player, cart, true);
		helper.assertValueEqual(MinecartCushions.fitted(cart), DyeColor.RED, "cushion after trying to take it from under a rider");
		other.stopRiding();

		use(player, cart, true);
		helper.assertTrue(MinecartCushions.fitted(cart) == null, "the cushion should be out");
		helper.assertValueEqual(player.getInventory().countItem(Items.CUSHION.pick(DyeColor.RED)), 1, "red cushions given back");
		// It lands in the empty hand; put it away, or the next use fits it again.
		player.getInventory().clearContent();
		use(player, cart, false);
		helper.assertTrue(player.getVehicle() == null, "without its cushion the cart should not seat the player again");
		helper.assertValueEqual(MinecartCargoGameTests.load(player, cart, Items.RAW_IRON, 10), 0, "raw iron left in hand: the cart takes cargo again");
		SimpleBlockTestLog.info("CUSH-01: no seat without a cushion; red cushion fitted (1 of 2 used), player got in with a cushion and with raw iron in "
			+ "hand (nothing loaded); refused while another player sat in it; taken out and given back; cart then took 10 raw iron");
		cart.discard();
		TestPlayers.remove(player);
		TestPlayers.remove(other);
		helper.succeed();
	}

	/** A cushion goes only into an empty cart. */
	@GameTest
	public void cushionNeedsAnEmptyCart(GameTestHelper helper) {
		Minecart cart = MinecartCargoGameTests.cartOnRail(helper, new BlockPos(2, 2, 2));
		ServerPlayer player = TestPlayers.create(helper, GameType.SURVIVAL, new BlockPos(4, 2, 2));
		MinecartCargoGameTests.cargo(cart).simpleBlock$setCargo(Items.COAL, 10);
		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.CUSHION.pick(DyeColor.BLUE)));
		use(player, cart, false);
		helper.assertTrue(MinecartCushions.fitted(cart) == null, "a cushion was fitted over cargo");
		helper.assertValueEqual(player.getMainHandItem().getCount(), 1, "cushion left in hand");
		helper.assertValueEqual(MinecartCargoGameTests.cargo(cart).simpleBlock$count(), 10, "coal left in the cart");
		cart.discard();
		TestPlayers.remove(player);
		helper.succeed();
	}

	/** With a cushion, a rolling cart picks up a mob in its path again, as vanilla does. */
	@GameTest(maxTicks = 120)
	public void cushionedCartPicksUpMobsAgain(GameTestHelper helper) {
		for (int x = 0; x < 8; x++) {
			helper.setBlock(new BlockPos(x, 1, 1), Blocks.STONE);
			helper.setBlock(new BlockPos(x, 2, 1), Blocks.RAIL);
		}
		Minecart cart = helper.spawn(EntityTypes.MINECART, Vec3.atBottomCenterOf(new BlockPos(1, 2, 1)).add(0, 0.0625, 0));
		MinecartCargoGameTests.cargo(cart).simpleBlock$setCushion(DyeColor.GREEN);
		Zombie zombie = helper.spawn(EntityTypes.ZOMBIE, new BlockPos(5, 2, 1));
		zombie.setNoAi(true);
		cart.setDeltaMovement(0.4, 0.0, 0.0);
		helper.runAfterDelay(60, () -> {
			helper.assertTrue(zombie.getVehicle() == cart, "the rolling cushioned cart should have picked up the zombie");
			SimpleBlockTestLog.info("CUSH-01: a rolling cushioned cart picked up the zombie in its path");
			zombie.discard();
			cart.discard();
			helper.succeed();
		});
	}

	/** The cushion is saved with the cart, and comes out as an item when the cart is broken. */
	@GameTest
	public void cushionIsSavedAndDroppedWithTheCart(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		Minecart cart = MinecartCargoGameTests.cartOnRail(helper, new BlockPos(2, 2, 2));
		MinecartCargoGameTests.cargo(cart).simpleBlock$setCushion(DyeColor.BLUE);
		TagValueOutput output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, level.registryAccess());
		cart.saveWithoutId(output);
		CompoundTag tag = output.buildResult();
		Minecart copy = EntityTypes.MINECART.create(level, EntitySpawnReason.LOAD);
		copy.load(TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), tag));
		helper.assertValueEqual(MinecartCushions.fitted(copy), DyeColor.BLUE, "cushion after saving and loading");

		ServerPlayer player = TestPlayers.create(helper, GameType.SURVIVAL, new BlockPos(4, 2, 2));
		Vec3 at = cart.position();
		cart.hurtServer(level, level.damageSources().playerAttack(player), 10.0F);
		List<ItemEntity> items = MinecartCargoGameTests.itemsAround(level, at);
		helper.assertValueEqual(MinecartCargoGameTests.total(items, Items.CUSHION.pick(DyeColor.BLUE)), 1, "blue cushions dropped");
		helper.assertValueEqual(MinecartCargoGameTests.total(items, Items.MINECART), 1, "minecarts dropped");
		SimpleBlockTestLog.info("CUSH-01: saved {} -> loaded {}; broken: {}", tag.get("simple_block:cushion"), MinecartCushions.fitted(copy),
			items.stream().map(item -> item.getItem().toString()).toList());
		items.forEach(ItemEntity::discard);
		TestPlayers.remove(player);
		helper.succeed();
	}
}
