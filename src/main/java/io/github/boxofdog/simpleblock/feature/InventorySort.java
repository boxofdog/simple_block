package io.github.boxofdog.simpleblock.feature;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import io.github.boxofdog.simpleblock.SimpleBlock;
import io.github.boxofdog.simpleblock.network.SortInventoryPayload;

/**
 * INV-02: sort the 18 storage slots. Hotbar, armor, offhand, crafting grid and cursor are never
 * touched.
 *
 * <p>Compatible stacks are merged first: same item and identical components (so different
 * durability, enchantments, names or contents never merge), up to the stack size limit. Then the
 * stacks are ordered by the item's numeric id in the running game's item registry
 * ({@code BuiltInRegistries.ITEM.getId}, i.e. registration order, independent of language),
 * with the namespaced id as tie-break; stacks of the same item keep their previous relative order.
 * They are laid out left to right along the bottom storage row, then the upper row; empty slots
 * end up at the upper right.
 *
 * <p>The server does the sort in one step on its own thread when the client's Sort button sends
 * {@link SortInventoryPayload}; repeated clicks just sort an already sorted inventory again.
 */
public final class InventorySort {
	private InventorySort() {
	}

	public static void sort(Inventory inventory) {
		List<ItemStack> stacks = new ArrayList<>();
		for (int slot : InventorySlots.SORT_ORDER) {
			ItemStack stack = inventory.getItem(slot);
			if (!stack.isEmpty()) {
				stacks.add(stack.copy());
			}
		}

		List<ItemStack> merged = new ArrayList<>();
		for (ItemStack stack : stacks) {
			for (ItemStack into : merged) {
				if (stack.isEmpty()) {
					break;
				}
				if (into.isStackable() && ItemStack.isSameItemSameComponents(into, stack)) {
					int move = Math.min(stack.getCount(), inventory.getMaxStackSize(into) - into.getCount());
					if (move > 0) {
						into.grow(move);
						stack.shrink(move);
					}
				}
			}
			if (!stack.isEmpty()) {
				merged.add(stack);
			}
		}

		merged.sort(Comparator.<ItemStack>comparingInt(stack -> BuiltInRegistries.ITEM.getId(stack.getItem()))
			.thenComparing(stack -> BuiltInRegistries.ITEM.getKey(stack.getItem()).toString()));

		for (int i = 0; i < InventorySlots.SORT_ORDER.length; i++) {
			inventory.setItem(InventorySlots.SORT_ORDER[i], i < merged.size() ? merged.get(i) : ItemStack.EMPTY);
		}
	}

	static void handle(ServerPlayer player) {
		// Only from the player's own inventory screen, as the button is only there.
		if (player.isSpectator() || player.containerMenu != player.inventoryMenu) {
			return;
		}
		sort(player.getInventory());
		player.inventoryMenu.broadcastChanges();
	}

	public static void register() {
		PayloadTypeRegistry.serverboundPlay().register(SortInventoryPayload.TYPE, SortInventoryPayload.CODEC);
		ServerPlayNetworking.registerGlobalReceiver(SortInventoryPayload.TYPE, (payload, context) -> {
			ServerPlayer player = context.player();
			context.server().execute(() -> handle(player));
		});
		SimpleBlock.LOGGER.debug("Registered inventory sort");
	}
}
