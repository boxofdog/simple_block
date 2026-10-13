package io.github.boxofdog.simpleblock.feature;

import java.util.Set;

import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.loot.v3.LootTableEvents;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.ShulkerBoxBlock;

/**
 * M06: items Simple Block has retired are not in the creative menu, neither in its tabs nor in its
 * search. Their blocks, items and entities stay registered (commands, structures and saved worlds
 * still know them); each feature keeps its own survival rules for getting and using them.
 *
 * <ul>
 *   <li>M01: the elytra ({@link NoElytraFlight}) and every shulker box ({@code feature/ShulkerBoxes}).</li>
 *   <li>M02: granite, diorite, andesite, tuff and every block made from them ({@link RemovedRocks}).</li>
 *   <li>M03: the ender chest ({@link EnderChests}) and the chest minecart ({@link ChestMinecarts}).</li>
 *   <li>M05: the furnace minecart item (the Cobblestone Engine is made by using a furnace on a
 *       minecart; its recipe is disabled).</li>
 *   <li>M06: the powered, detector and activator rails. Their recipes and recipe-book advancements
 *       are disabled by data overrides, and they are taken out of every chest and other non-block
 *       loot (abandoned mineshafts were the only vanilla source; no structure places them). Placed
 *       ones (commands, old structures) keep working. Ordinary rails are untouched.</li>
 *   <li>M08: the vanilla blast furnace. Its recipe and recipe-book advancement are disabled by
 *       data overrides, and new villages build ordinary furnaces instead
 *       ({@code metallurgy/VillageFurnaces}); the pre-industrial blast furnace replaces it.</li>
 * </ul>
 */
public final class RetiredItems {
	/** The redstone rails retired in M06. */
	public static final Set<Item> RAILS = Set.of(Items.POWERED_RAIL, Items.DETECTOR_RAIL, Items.ACTIVATOR_RAIL);

	private static final Set<Item> OTHERS = Set.of(Items.ELYTRA, Items.ENDER_CHEST, Items.CHEST_MINECART, Items.FURNACE_MINECART,
		Items.BLAST_FURNACE);

	private RetiredItems() {
	}

	/** Whether {@code stack} is a retired item. */
	public static boolean isRetired(ItemStack stack) {
		Item item = stack.getItem();
		return RAILS.contains(item) || OTHERS.contains(item) || RemovedRocks.items().contains(item)
			|| item instanceof BlockItem block && block.getBlock() instanceof ShulkerBoxBlock;
	}

	public static void register() {
		CreativeModeTabEvents.MODIFY_OUTPUT_ALL.register((tab, output) -> {
			output.getDisplayStacks().removeIf(RetiredItems::isRetired);
			output.getSearchTabStacks().removeIf(RetiredItems::isRetired);
		});
		LootTableEvents.MODIFY_DROPS.register((table, context, drops) -> {
			boolean blockLoot = table.unwrapKey().map(key -> key.identifier().getPath().startsWith("blocks/")).orElse(false);
			if (!blockLoot) {
				drops.removeIf(stack -> RAILS.contains(stack.getItem()));
			}
		});
	}
}
