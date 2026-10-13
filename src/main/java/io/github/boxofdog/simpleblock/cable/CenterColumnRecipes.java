package io.github.boxofdog.simpleblock.cable;

import java.util.Optional;
import java.util.Set;

import net.minecraft.resources.Identifier;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeHolder;

import io.github.boxofdog.simpleblock.SimpleBlock;

/**
 * M06: the cable and cable-coil recipes only work with their three items down the <b>middle column</b>
 * of a 3 x 3 grid, the other six cells empty (owner's layout). Vanilla shrinks a shaped pattern to the
 * cells it uses and matches a trimmed grid, so a one-column pattern would match any column; the
 * crafting table and the crafter check the column here ({@code mixin/cable}). The recipe book places
 * a one-wide recipe in the middle column, so it still works.
 */
public final class CenterColumnRecipes {
	private static final Set<Identifier> RECIPES = Set.of(Identifier.fromNamespaceAndPath(SimpleBlock.MOD_ID, "cable"),
		Identifier.fromNamespaceAndPath(SimpleBlock.MOD_ID, "cable_coil"));

	private CenterColumnRecipes() {
	}

	/** Whether {@code recipe} may craft from {@code container}'s grid (always, unless it is a middle-column recipe placed elsewhere). */
	public static boolean allowed(RecipeHolder<?> recipe, CraftingContainer container) {
		if (!RECIPES.contains(recipe.id().identifier())) {
			return true;
		}
		CraftingInput.Positioned positioned = container.asPositionedCraftInput();
		return container.getWidth() == 3 && positioned.left() == 1 && positioned.input().width() == 1;
	}

	/** {@code found}, or nothing if it is a middle-column recipe placed elsewhere in {@code container}. */
	public static <T extends RecipeHolder<?>> Optional<T> filter(Optional<T> found, CraftingContainer container) {
		return found.filter(recipe -> allowed(recipe, container));
	}
}
