package io.github.boxofdog.simpleblock.feature;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import org.jspecify.annotations.Nullable;

/**
 * FRT-10: ender chests cannot be obtained or used to move items without hauling them.
 *
 * <ul>
 *   <li>Acquisition: the recipe and its recipe-book advancement are disabled by data overrides; the
 *       block always drops 8 obsidian, silk touch or not ({@code data/minecraft/loot_table/blocks}),
 *       so the ender chest built into one End city floor cannot be taken. No vanilla loot table or
 *       trade contains one. Kept out of the creative menu by {@link RetiredItems}.</li>
 *   <li>Placement (players, and anything else using {@code BlockItem.place}) is refused in
 *       {@code mixin/enderchest/BlockItemMixin}.</li>
 *   <li>Existing ender chests (that End city, or placed with commands) never open
 *       ({@code EnderChestBlockMixin}). Hoppers never had access to them.</li>
 * </ul>
 *
 * <p>Eyes of ender and the rest of End progression are untouched; the block and item stay
 * registered.
 */
public final class EnderChests {
	private EnderChests() {
	}

	/** Tells a player why nothing happened. Server-side only; a no-op for dispensers and clients. */
	public static void notifyDisabled(@Nullable Player player) {
		if (player instanceof ServerPlayer serverPlayer) {
			Feedback.quiet(serverPlayer, 
					Component.translatableWithFallback("message.simple_block.ender_chest_disabled", "Ender chests don't work in this world")
			);
		}
	}
}
