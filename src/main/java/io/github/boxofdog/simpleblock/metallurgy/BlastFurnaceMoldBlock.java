package io.github.boxofdog.simpleblock.metallurgy;

import net.minecraft.core.Direction;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;

/**
 * M08: the casting mold, a whole block at the end of the channel: an iron flask whose cavity is the
 * shape of a full block. It shows what it holds ({@link #CONTENT}): molten metal while a cast is being
 * poured, then the cast block itself (iron, copper or gold) while the mold holds any; empty, its open
 * cavity.
 */
public class BlastFurnaceMoldBlock extends BlastFurnaceAttachmentBlock {
	public enum Content implements StringRepresentable {
		EMPTY("empty"), MOLTEN("molten"), IRON("iron"), COPPER("copper"), GOLD("gold");

		private final String name;

		Content(String name) {
			this.name = name;
		}

		@Override
		public String getSerializedName() {
			return this.name;
		}

		/** What a mold holding {@code stack} shows (molten while pouring). */
		public static Content of(ItemStack stack, boolean pouring) {
			if (pouring) {
				return MOLTEN;
			}
			if (stack.is(Items.IRON_BLOCK)) {
				return IRON;
			}
			if (stack.is(Items.GOLD_BLOCK)) {
				return GOLD;
			}
			return stack.isEmpty() ? EMPTY : COPPER;
		}
	}

	public static final EnumProperty<Content> CONTENT = EnumProperty.create("content", Content.class);

	public BlastFurnaceMoldBlock(BlockBehaviour.Properties properties) {
		super(properties, 2);
		this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(CONTENT, Content.EMPTY));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING, CONTENT);
	}
}
