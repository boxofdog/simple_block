package io.github.boxofdog.simpleblock.feature;

import java.util.ArrayList;
import java.util.List;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.CompoundContainer;
import net.minecraft.world.Container;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.ChestBlockEntity;

import io.github.boxofdog.simpleblock.mixin.inventory.CompoundContainerAccessor;
import io.github.boxofdog.simpleblock.network.SortChestPayload;
import io.github.boxofdog.simpleblock.network.SortableChestPayload;

/**
 * M13: the Sort button on a chest's screen (single and double chests, trapped chests too). It sorts only
 * the chest's own 27 or 54 slots, exactly as the inventory's Sort does ({@link InventorySort}: compatible
 * stacks merged, never ones with different components, then by the item registry's order, filling rows
 * from the top left); never the player's inventory, the cursor or anything else.
 *
 * <p>The server sorts in one step, on its own thread, the chest in the menu the player has open now, and
 * only if it is that menu ({@code containerId}), still valid, and a chest's: barrels, ender chests and
 * every other container sharing the chest screen are refused, and so are spectators. Every viewer sees the
 * result through the menu's normal syncing; a repeated click sorts a sorted chest again (nothing changes).
 * The client shows the button only for a screen the server said is a chest's ({@link SortableChestPayload},
 * sent as the chest's menu is made, before its screen opens).
 */
public final class ChestSort {
	private ChestSort() {
	}

	/** Whether {@code container} is a chest's own storage: a chest (or trapped chest) or the two halves of a double one. */
	public static boolean isChest(Container container) {
		if (container instanceof ChestBlockEntity) {
			return true;
		}
		return container instanceof CompoundContainer compound && ((CompoundContainerAccessor) compound).simpleBlock$first() instanceof ChestBlockEntity
			&& ((CompoundContainerAccessor) compound).simpleBlock$second() instanceof ChestBlockEntity;
	}

	/** Sorts {@code container}'s slots in place. */
	public static void sort(Container container) {
		List<ItemStack> stacks = new ArrayList<>();
		for (int slot = 0; slot < container.getContainerSize(); slot++) {
			ItemStack stack = container.getItem(slot);
			if (!stack.isEmpty()) {
				stacks.add(stack.copy());
			}
		}
		List<ItemStack> merged = InventorySort.sorted(stacks, container);
		for (int slot = 0; slot < container.getContainerSize(); slot++) {
			container.setItem(slot, slot < merged.size() ? merged.get(slot) : ItemStack.EMPTY);
		}
		container.setChanged();
	}

	/** The server's handling of a Sort click: only the chest in the menu the player has open now. */
	public static boolean handle(ServerPlayer player, int containerId) {
		if (player.isSpectator() || !(player.containerMenu instanceof ChestMenu menu) || menu.containerId != containerId || !menu.stillValid(player)
			|| !isChest(menu.getContainer())) {
			return false;
		}
		sort(menu.getContainer());
		menu.broadcastChanges();
		return true;
	}

	/** Tells {@code player}'s client that the chest menu {@code containerId} about to open can be sorted. */
	public static void announce(ServerPlayer player, int containerId) {
		if (ServerPlayNetworking.canSend(player, SortableChestPayload.TYPE)) {
			ServerPlayNetworking.send(player, new SortableChestPayload(containerId));
		}
	}

	public static void register() {
		PayloadTypeRegistry.serverboundPlay().register(SortChestPayload.TYPE, SortChestPayload.CODEC);
		PayloadTypeRegistry.clientboundPlay().register(SortableChestPayload.TYPE, SortableChestPayload.CODEC);
		ServerPlayNetworking.registerGlobalReceiver(SortChestPayload.TYPE, (payload, context) -> {
			ServerPlayer player = context.player();
			context.server().execute(() -> handle(player, payload.containerId()));
		});
	}
}
