package io.github.boxofdog.simpleblock.train;

import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;

/**
 * Not a placeable block: it only carries the freight cart's folding front gate models for the cart
 * renderer (M06): closed (upright over the cart's front wall) and open (folded down flat as a ramp).
 * M06 always draws it closed; opening it onto a skip is M07.
 */
public class FreightGatePartBlock extends Block {
	public enum Part implements StringRepresentable {
		CLOSED("closed"),
		OPEN("open");

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

	public FreightGatePartBlock(BlockBehaviour.Properties properties) {
		super(properties);
		this.registerDefaultState(this.stateDefinition.any().setValue(PART, Part.CLOSED));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(PART);
	}
}
