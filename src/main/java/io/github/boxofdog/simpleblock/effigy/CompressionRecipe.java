package io.github.boxofdog.simpleblock.effigy;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.Holder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

import org.jspecify.annotations.Nullable;

/**
 * M12: nine <b>full stacks</b> of a drop, one in every cell of a 3 x 3 grid, make one compressed
 * monster block ({@code simple_block:compression}; owner rule: every slot a full stack, a full stack
 * used from every slot). For rotten flesh, bones and gunpowder that is 9 x 64 = 576; ender pearls stack
 * to 16, so 9 x 16 = 144 (owner decision 2026-10-14). A grid with any short stack, an empty cell, another
 * item or an item with changed components does not match.
 *
 * <p>Vanilla crafting takes one item from each cell; taking the result here takes whole stacks instead:
 * the crafting table and inventory grid ({@code mixin/effigy/ResultSlotMixin}, covering clicks,
 * shift-click and every repeat, which all go through the result slot) and the crafter
 * ({@code mixin/effigy/CrafterBlockMixin}). It is a special recipe, so not in the recipe book.
 */
public class CompressionRecipe extends CustomRecipe {
	public static final MapCodec<CompressionRecipe> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			Item.CODEC.fieldOf("drop").forGetter(o -> o.drop),
			ItemStackTemplate.CODEC.fieldOf("result").forGetter(o -> o.result))
		.apply(i, CompressionRecipe::new));
	public static final StreamCodec<RegistryFriendlyByteBuf, CompressionRecipe> STREAM_CODEC = StreamCodec.composite(
		Item.STREAM_CODEC, o -> o.drop,
		ItemStackTemplate.STREAM_CODEC, o -> o.result,
		CompressionRecipe::new);
	public static final RecipeSerializer<CompressionRecipe> SERIALIZER = new RecipeSerializer<>(MAP_CODEC, STREAM_CODEC);

	private final Holder<Item> drop;
	private final ItemStackTemplate result;

	public CompressionRecipe(Holder<Item> drop, ItemStackTemplate result) {
		this.drop = drop;
		this.result = result;
	}

	/** Whether {@code stack} is one full, plain stack of this recipe's drop. */
	private boolean full(ItemStack stack) {
		return stack.is(this.drop) && stack.getComponentsPatch().isEmpty() && stack.getCount() == stack.getMaxStackSize();
	}

	@Override
	public boolean matches(CraftingInput input, Level level) {
		if (input.width() != 3 || input.height() != 3 || input.ingredientCount() != 9) {
			return false;
		}
		for (int i = 0; i < input.size(); i++) {
			if (!this.full(input.getItem(i))) {
				return false;
			}
		}
		return true;
	}

	@Override
	public ItemStack assemble(CraftingInput input) {
		return this.result.create();
	}

	@Override
	public RecipeSerializer<CompressionRecipe> getSerializer() {
		return SERIALIZER;
	}

	/** Whether {@code recipe} is a compression recipe, whose craft takes whole stacks. */
	public static boolean takesWholeStacks(@Nullable RecipeHolder<?> recipe) {
		return recipe != null && recipe.value() instanceof CompressionRecipe;
	}
}
