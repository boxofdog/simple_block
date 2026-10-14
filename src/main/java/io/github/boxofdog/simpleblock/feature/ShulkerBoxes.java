package io.github.boxofdog.simpleblock.feature;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import org.jspecify.annotations.Nullable;

/**
 * SF-04: shulker boxes (plain and all 16 dyed variants) cannot be obtained or used as storage.
 *
 * <ul>
 *   <li>Acquisition: the 17 vanilla recipes and their recipe-book advancements are removed by
 *       data overrides under {@code data/minecraft/recipe} and
 *       {@code data/minecraft/advancement/recipes/decorations}. No vanilla loot table or trade
 *       contains a shulker box.</li>
 *   <li>Placement (players, dispensers, anything using {@code BlockItem.place}) is refused in
 *       {@code mixin/shulker/BlockItemMixin}.</li>
 *   <li>Boxes that already exist (placed with commands) cannot be opened
 *       ({@code ShulkerBoxBlockMixin}) or filled/emptied by hoppers and droppers
 *       ({@code ShulkerBoxBlockEntityMixin}).</li>
 * </ul>
 *
 * <p>Shulker mobs and shulker shells are untouched. The blocks, items and block entity stay
 * registered.
 */
public final class ShulkerBoxes {
	private ShulkerBoxes() {
	}

	/** Tells a player why nothing happened. Server-side only; a no-op for dispensers and clients. */
	public static void notifyDisabled(@Nullable Player player) {
		if (player instanceof ServerPlayer serverPlayer) {
			Feedback.quiet(serverPlayer, 
					Component.translatableWithFallback("message.simple_block.shulker_box_disabled", "Shulker boxes don't work in this world")
			);
		}
	}
}
