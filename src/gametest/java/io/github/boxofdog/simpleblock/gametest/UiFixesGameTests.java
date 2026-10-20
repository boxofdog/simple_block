package io.github.boxofdog.simpleblock.gametest;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.vehicle.minecart.Minecart;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

import net.fabricmc.fabric.api.gametest.v1.GameTest;

import io.github.boxofdog.simpleblock.feature.MinecartCargo;
import io.github.boxofdog.simpleblock.train.CargoInspectMenu;

/** M13 inventory and cargo UI (BUG-014, Shift + right-click glide, chest Sort, BUG-010): server tests. */
public class UiFixesGameTests {
	private static final String SHAFT = SkipGameTests.SHAFT;

	/**
	 * BUG-014: an empty main hand on a freight cart opens the read-only cargo window (no slots at all, so
	 * nothing can be moved; the player's inventory is not in it) showing the real item and count, kept
	 * up to date while open (another player loading it). Held cargo still loads, sneak + use still
	 * unloads, an empty main hand with cargo in the off hand inspects, and clicks in the window move
	 * nothing.
	 */
	@GameTest(structure = SHAFT, maxTicks = 100)
	public void emptyHandInspectsTheCargo(GameTestHelper helper) {
		helper.setBlock(new BlockPos(6, 0, 6), Blocks.STONE);
		helper.setBlock(new BlockPos(6, 1, 6), Blocks.RAIL);
		Minecart cart = helper.spawn(EntityTypes.MINECART, new Vec3(6.5, 1.0625, 6.5));
		MinecartCargo.cart(cart).simpleBlock$setCargo(Items.RAW_IRON, 200);
		ServerPlayer player = TestPlayers.create(helper, GameType.SURVIVAL, new BlockPos(6, 1, 8));
		ServerPlayer other = TestPlayers.create(helper, GameType.SURVIVAL, new BlockPos(7, 1, 8));
		List<String> log = new ArrayList<>();
		helper.startSequence()
			.thenIdle(2)
			.thenExecute(() -> {
				player.interactOn(cart, InteractionHand.MAIN_HAND, Vec3.ZERO);
				helper.assertTrue(player.containerMenu instanceof CargoInspectMenu, "empty hand: the cargo window opened");
				CargoInspectMenu menu = (CargoInspectMenu) player.containerMenu;
				menu.broadcastChanges();
				helper.assertTrue(menu.cargo() == Items.RAW_IRON && menu.count() == 200, "the window shows " + menu.count() + " " + menu.cargo());
				helper.assertValueEqual(menu.slots.size(), 0, "slots in the window (none: read-only, no inventory)");
				// A click on "slot 0" is refused by the server's own check (there is none); outside the window, nothing.
				helper.assertFalse(menu.isValidSlotIndex(0), "slot 0 accepted by the server");
				menu.clicked(-999, 0, ContainerInput.PICKUP, player);
				helper.assertTrue(menu.getCarried().isEmpty() && MinecartCargo.cart(cart).simpleBlock$count() == 200, "clicks in the window moved nothing");
				// Another player loads 4 while it is open: the window follows.
				other.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.RAW_IRON, 4));
				MinecartCargo.forgetPace(other);
				other.interactOn(cart, InteractionHand.MAIN_HAND, Vec3.ZERO);
				menu.broadcastChanges();
				helper.assertValueEqual(menu.count(), 204, "the window after another player loaded 4");
				player.closeContainer();
				log.add("empty hand: window with 200 raw iron, no slots, clicks move nothing, follows another player's loading (204)");

				player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.RAW_IRON, 4));
				MinecartCargo.forgetPace(player);
				player.interactOn(cart, InteractionHand.MAIN_HAND, Vec3.ZERO);
				helper.assertTrue(!(player.containerMenu instanceof CargoInspectMenu) && MinecartCargo.cart(cart).simpleBlock$count() == 208,
					"held cargo loads, no window");
				player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
				player.setShiftKeyDown(true);
				MinecartCargo.forgetPace(player);
				player.interactOn(cart, InteractionHand.MAIN_HAND, Vec3.ZERO);
				player.setShiftKeyDown(false);
				helper.assertTrue(!(player.containerMenu instanceof CargoInspectMenu) && MinecartCargo.cart(cart).simpleBlock$count() < 208,
					"sneak + use unloads, no window");
				// (the unloaded iron went to the empty main hand)
				player.getInventory().clearContent();
				player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(Items.RAW_IRON, 4));
				player.interactOn(cart, InteractionHand.MAIN_HAND, Vec3.ZERO);
				helper.assertTrue(player.containerMenu instanceof CargoInspectMenu, "empty main hand with cargo in the off hand: the window");
				player.closeContainer();
				log.add("held cargo loads, sneak unloads, empty main hand with off-hand cargo inspects");
				SimpleBlockTestLog.info("BUG-014 cargo window: {}", log);
				cart.discard();
				TestPlayers.remove(player);
				TestPlayers.remove(other);
			})
			.thenSucceed();
	}
}
