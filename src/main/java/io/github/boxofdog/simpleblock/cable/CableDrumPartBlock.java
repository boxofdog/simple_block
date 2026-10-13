package io.github.boxofdog.simpleblock.cable;

import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;

/**
 * Not a placeable block: it only carries the drum's turning parts' models, one per {@link Part}, for
 * the drum renderer, which draws them rotating about the axle: the spool (bare or wound with cable)
 * and the hand crank.
 */
public class CableDrumPartBlock extends Block {
	public enum Part implements StringRepresentable {
		SPOOL("spool"),
		SPOOL_WOUND("spool_wound"),
		CRANK("crank"),
		/** M06 coal winch: the furnace on the drum's free side, and the lever where the crank was. */
		FURNACE("furnace"),
		LEVER("lever");

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

	public CableDrumPartBlock(BlockBehaviour.Properties properties) {
		super(properties);
		this.registerDefaultState(this.stateDefinition.any().setValue(PART, Part.SPOOL));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(PART);
	}
}
