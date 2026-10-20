package io.github.boxofdog.simpleblock.skip;

import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;

/**
 * Not a placeable block: it only carries the ore skip's part models, one per {@link Part}, for the
 * skip's renderer, which stacks them: the chute, the bin, its discharge gate (closed or open), the lid
 * of its top intake (closed or open) and the bridle.
 */
public class SkipPartBlock extends Block {
	public enum Part implements StringRepresentable {
		CHUTE("chute"),
		BIN("bin"),
		GATE_CLOSED("gate_closed"),
		GATE_OPEN("gate_open"),
		LID_CLOSED("lid_closed"),
		LID_OPEN("lid_open"),
		/** M13 (BUG-006): the lid as two doors opening to the sides, and the fixed rim at the back. */
		LID_LEFT("lid_left"),
		LID_RIGHT("lid_right"),
		LID_RIM("lid_rim"),
		BRIDLE("bridle");

		private final String name;

		Part(String name) {
			this.name = name;
		}

		@Override
		public String getSerializedName() {
			return this.name;
		}
	}

	public static final EnumProperty<Part> PART = EnumProperty.create("part", Part.class);

	public SkipPartBlock(BlockBehaviour.Properties properties) {
		super(properties);
		this.registerDefaultState(this.stateDefinition.any().setValue(PART, Part.BIN));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(PART);
	}
}
