package io.github.boxofdog.simpleblock.train;

import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;

/**
 * Not a placeable block: M15 carries the models the client draws for freight stations, one state per part:
 * the engine's selector lever (its base plate, and the handle the renderer swings) and the lid over a
 * freight hopper's opening (over the spout of a supplying hopper, over the top of a receiving one).
 */
public class FreightModelBlock extends Block {
	public enum Part implements StringRepresentable {
		LEVER_BASE("lever_base"), LEVER_HANDLE("lever_handle"), CAP_SPOUT("cap_spout"), CAP_TOP("cap_top");

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

	public FreightModelBlock(BlockBehaviour.Properties properties) {
		super(properties);
		this.registerDefaultState(this.stateDefinition.any().setValue(PART, Part.LEVER_BASE));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(PART);
	}

	public BlockState part(Part part) {
		return this.defaultBlockState().setValue(PART, part);
	}
}
