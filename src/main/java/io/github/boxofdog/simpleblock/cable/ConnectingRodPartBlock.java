package io.github.boxofdog.simpleblock.cable;

import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;

/**
 * Not a placeable block: it only carries the connecting rod's models, one per {@link Part}, for the
 * minecart renderer (as vanilla does for a cart's display block). The rod is drawn from its base on the
 * cart, a shaft stretched to the cable, and the grip head, open or closed on the cable.
 */
public class ConnectingRodPartBlock extends Block {
	public enum Part implements StringRepresentable {
		BASE("base"),
		SHAFT("shaft"),
		HEAD_OPEN("head_open"),
		HEAD_CLOSED("head_closed");

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

	public ConnectingRodPartBlock(BlockBehaviour.Properties properties) {
		super(properties);
		this.registerDefaultState(this.stateDefinition.any().setValue(PART, Part.BASE));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(PART);
	}
}
