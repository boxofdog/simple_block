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
import net.minecraft.world.level.block.state.properties.IntegerProperty;

/**
 * M08: the casting mold, a whole block at the end of the channel: an iron flask whose cavity is the
 * shape of a full block. M08 playtest (owner): it shows the casting as it happens. While a block is
 * being cast the cavity fills with molten metal from the bottom, eighth by eighth with the cast's real
 * progress ({@link #FILL}); above the molten metal stands the block the mold last cast, if it still
 * holds one ({@link #CONTENT}: its metal), so the next pour takes it over rather than the mold first
 * going empty. A finished cast shows as the solid block (fill 0) for a moment before the next fill
 * shows. Idle, it shows the last block it holds, or its open cavity once emptied. What it shows is only
 * the current cast: the stored count is in the furnace's screen.
 */
public class BlastFurnaceMoldBlock extends BlastFurnaceAttachmentBlock {
	public enum Content implements StringRepresentable {
		/** {@code molten} is no longer shown (the fill is): kept so that molds saved with it still load. */
		EMPTY("empty"), MOLTEN("molten"), IRON("iron"), COPPER("copper"), GOLD("gold");

		private final String name;

		Content(String name) {
			this.name = name;
		}

		@Override
		public String getSerializedName() {
			return this.name;
		}

		/** The metal of the block a mold holding {@code stack} shows (above any molten fill). */
		public static Content of(ItemStack stack) {
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
	/** Eighths of the cavity filled with molten metal (0: none). */
	public static final IntegerProperty FILL = IntegerProperty.create("fill", 0, 8);

	public BlastFurnaceMoldBlock(BlockBehaviour.Properties properties) {
		super(properties, 2);
		this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(CONTENT, Content.EMPTY).setValue(FILL, 0));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING, CONTENT, FILL);
	}
}
