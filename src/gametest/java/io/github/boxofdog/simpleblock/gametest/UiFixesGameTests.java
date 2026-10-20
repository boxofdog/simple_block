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
				MinecartCargo.forgetPace(player);
				player.interactOn(cart, InteractionHand.MAIN_HAND, Vec3.ZERO);
				helper.assertTrue(player.containerMenu instanceof CargoInspectMenu, "empty main hand with cargo in the off hand: the window");
				player.closeContainer();
				log.add("held cargo loads, sneak unloads, empty main hand with off-hand cargo inspects");

				// Holding use while loading: the stack runs out, the hand is empty, the held use repeats: no window.
				player.getInventory().clearContent();
				player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.RAW_IRON, 4));
				MinecartCargo.forgetPace(player);
				player.interactOn(cart, InteractionHand.MAIN_HAND, Vec3.ZERO);
				helper.assertTrue(player.getMainHandItem().isEmpty(), "the stack loaded");
				player.interactOn(cart, InteractionHand.MAIN_HAND, Vec3.ZERO);
				helper.assertFalse(player.containerMenu instanceof CargoInspectMenu, "a held use right after loading opens no window");
			})
			.thenIdle(4)
			.thenExecute(() -> {
				player.interactOn(cart, InteractionHand.MAIN_HAND, Vec3.ZERO);
				helper.assertFalse(player.containerMenu instanceof CargoInspectMenu, "still held (a repeat 4 ticks on): no window");
			})
			.thenIdle(MinecartCargo.HOLD_TICKS + 5)
			.thenExecute(() -> {
				player.interactOn(cart, InteractionHand.MAIN_HAND, Vec3.ZERO);
				helper.assertTrue(player.containerMenu instanceof CargoInspectMenu, "a fresh press afterwards: the window");
				player.closeContainer();
				log.add("held use after a stack runs out: no window (and on its repeats); a fresh press later: the window");
				SimpleBlockTestLog.info("BUG-014 cargo window: {}", log);
				cart.discard();
				TestPlayers.remove(player);
				TestPlayers.remove(other);
			})
			.thenSucceed();
	}

	/** Opens the chest (single or double, by its block's own menu) at {@code pos} for {@code player}. */
	private static void open(GameTestHelper helper, ServerPlayer player, BlockPos pos) {
		BlockPos abs = helper.absolutePos(pos);
		player.openMenu(helper.getLevel().getBlockState(abs).getMenuProvider(helper.getLevel(), abs));
	}

	private static String contents(net.minecraft.world.Container container) {
		StringBuilder out = new StringBuilder();
		for (int i = 0; i < container.getContainerSize(); i++) {
			ItemStack stack = container.getItem(i);
			if (!stack.isEmpty()) {
				out.append(i).append('=').append(stack.getCount()).append(' ').append(stack.getItem()).append(stack.has(
					net.minecraft.core.component.DataComponents.CUSTOM_NAME) ? "(named)" : "").append("; ");
			}
		}
		return out.toString();
	}

	/**
	 * Chest Sort: the chest's own slots sorted as the inventory's Sort does (compatible stacks merged, a
	 * renamed stack never, unstackables kept apart, registry order, from the top left, empties last), for a
	 * single chest, a double chest (54 slots, both halves) and a trapped chest; the player's inventory and
	 * cursor untouched; item counts conserved; a second click changes nothing. Refused: a barrel (same
	 * screen), a stale screen id, a spectator. A second viewer of the same chest sees the sorted chest.
	 */
	@GameTest(structure = SHAFT, maxTicks = 100)
	public void chestSortButton(GameTestHelper helper) {
		BlockPos single = new BlockPos(3, 1, 3);
		BlockPos left = new BlockPos(6, 1, 3);
		BlockPos trapped = new BlockPos(10, 1, 3);
		BlockPos barrel = new BlockPos(13, 1, 3);
		for (BlockPos pos : List.of(single, left, left.east(), trapped, barrel)) {
			helper.setBlock(pos.below(), Blocks.STONE);
		}
		helper.setBlock(single, Blocks.CHEST);
		helper.setBlock(left, Blocks.CHEST.defaultBlockState().setValue(net.minecraft.world.level.block.ChestBlock.TYPE,
			net.minecraft.world.level.block.state.properties.ChestType.LEFT));
		helper.setBlock(left.east(), Blocks.CHEST.defaultBlockState().setValue(net.minecraft.world.level.block.ChestBlock.TYPE,
			net.minecraft.world.level.block.state.properties.ChestType.RIGHT));
		helper.setBlock(trapped, Blocks.TRAPPED_CHEST);
		helper.setBlock(barrel, Blocks.BARREL);
		ServerPlayer player = TestPlayers.create(helper, GameType.SURVIVAL, new BlockPos(8, 1, 5));
		ServerPlayer viewer = TestPlayers.create(helper, GameType.SURVIVAL, new BlockPos(9, 1, 5));
		List<String> log = new ArrayList<>();
		helper.startSequence()
			.thenIdle(2)
			.thenExecute(() -> {
				ItemStack named = new ItemStack(Items.COBBLESTONE, 10);
				named.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME, net.minecraft.network.chat.Component.literal("keep"));
				for (BlockPos pos : List.of(single, left, trapped, barrel)) {
					var container = (net.minecraft.world.Container) helper.getLevel().getBlockEntity(helper.absolutePos(pos));
					container.setItem(26, new ItemStack(Items.COBBLESTONE, 40));
					container.setItem(5, new ItemStack(Items.DIRT, 3));
					container.setItem(12, new ItemStack(Items.COBBLESTONE, 40));
					container.setItem(20, named.copy());
					container.setItem(8, new ItemStack(Items.IRON_PICKAXE));
					container.setItem(2, new ItemStack(Items.IRON_PICKAXE));
				}
				// The double chest's right half has more.
				var right = (net.minecraft.world.Container) helper.getLevel().getBlockEntity(helper.absolutePos(left.east()));
				right.setItem(0, new ItemStack(Items.DIRT, 60));
				player.getInventory().setItem(18, new ItemStack(Items.DIRT, 7));
				player.getInventory().setItem(19, new ItemStack(Items.COBBLESTONE, 5));
				String inventoryBefore = contents(player.getInventory());

				// A single chest.
				open(helper, player, single);
				var menu = (net.minecraft.world.inventory.ChestMenu) player.containerMenu;
				helper.assertFalse(io.github.boxofdog.simpleblock.feature.ChestSort.handle(player, menu.containerId + 1), "a stale screen id refused");
				helper.assertTrue(io.github.boxofdog.simpleblock.feature.ChestSort.handle(player, menu.containerId), "single chest sorted");
				String sorted = contents(menu.getContainer());
				// Registry order puts dirt before cobblestone; the renamed stack stays apart from plain cobblestone.
				helper.assertTrue(menu.getContainer().getItem(0).is(Items.DIRT) && menu.getContainer().getItem(0).getCount() == 3
					&& menu.getContainer().getItem(1).is(Items.COBBLESTONE) && menu.getContainer().getItem(1).getCount() == 64
					&& menu.getContainer().getItem(2).has(net.minecraft.core.component.DataComponents.CUSTOM_NAME)
					&& menu.getContainer().getItem(3).is(Items.COBBLESTONE) && menu.getContainer().getItem(3).getCount() == 16
					&& menu.getContainer().getItem(4).is(Items.IRON_PICKAXE)
					&& menu.getContainer().getItem(5).is(Items.IRON_PICKAXE) && menu.getContainer().getItem(6).isEmpty(), "single chest order: " + sorted);
				helper.assertTrue(io.github.boxofdog.simpleblock.feature.ChestSort.handle(player, menu.containerId), "sorted again");
				helper.assertValueEqual(contents(menu.getContainer()), sorted, "a second click");
				helper.assertValueEqual(contents(player.getInventory()), inventoryBefore, "the player's inventory");
				player.closeContainer();
				log.add("single: " + sorted);

				// A double chest, with a second viewer.
				open(helper, player, left);
				open(helper, viewer, left);
				var doubleMenu = (net.minecraft.world.inventory.ChestMenu) player.containerMenu;
				helper.assertValueEqual(doubleMenu.getContainer().getContainerSize(), 54, "double chest slots");
				helper.assertTrue(io.github.boxofdog.simpleblock.feature.ChestSort.handle(player, doubleMenu.containerId), "double chest sorted");
				String doubleSorted = contents(doubleMenu.getContainer());
				helper.assertTrue(doubleMenu.getContainer().getItem(0).is(Items.DIRT) && doubleMenu.getContainer().getItem(0).getCount() == 63
					&& doubleMenu.getContainer().getItem(1).is(Items.COBBLESTONE), "double chest: dirt from both halves merged: " + doubleSorted);
				helper.assertValueEqual(contents(((net.minecraft.world.inventory.ChestMenu) viewer.containerMenu).getContainer()), doubleSorted,
					"the second viewer's chest");
				player.closeContainer();
				viewer.closeContainer();
				log.add("double: " + doubleSorted);

				open(helper, player, trapped);
				helper.assertTrue(io.github.boxofdog.simpleblock.feature.ChestSort.handle(player, player.containerMenu.containerId), "trapped chest sorted");
				player.closeContainer();
				open(helper, player, barrel);
				String barrelBefore = contents(((net.minecraft.world.inventory.ChestMenu) player.containerMenu).getContainer());
				helper.assertFalse(io.github.boxofdog.simpleblock.feature.ChestSort.handle(player, player.containerMenu.containerId), "a barrel refused");
				helper.assertValueEqual(contents(((net.minecraft.world.inventory.ChestMenu) player.containerMenu).getContainer()), barrelBefore, "the barrel");
				player.closeContainer();
				open(helper, player, single);
				player.setGameMode(GameType.SPECTATOR);
				helper.assertFalse(io.github.boxofdog.simpleblock.feature.ChestSort.handle(player, player.containerMenu.containerId), "a spectator refused");
				player.setGameMode(GameType.SURVIVAL);
				player.closeContainer();
				log.add("trapped sorted; barrel, stale id and spectator refused; inventory untouched; repeat changes nothing");
				SimpleBlockTestLog.info("M13 chest Sort: {}", log);
				TestPlayers.remove(player);
				TestPlayers.remove(viewer);
			})
			.thenSucceed();
	}
}
