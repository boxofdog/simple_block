package io.github.boxofdog.simpleblock.train;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

/**
 * Not a placeable block: it only carries the models of a freight cart's load ({@link CargoLooks}), one
 * per look and fill band, for the minecart renderer (as {@code connecting_rod_part} does for the rod).
 */
public class CargoFillBlock extends Block {
	public static final EnumProperty<CargoLooks.Look> LOOK = EnumProperty.create("look", CargoLooks.Look.class);
	public static final IntegerProperty BAND = IntegerProperty.create("band", 1, 4);

	public CargoFillBlock(BlockBehaviour.Properties properties) {
		super(properties);
		this.registerDefaultState(this.stateDefinition.any().setValue(LOOK, CargoLooks.Look.COVERED).setValue(BAND, 1));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(LOOK, BAND);
	}

	/** The model state for a load of {@code look} at {@code band} (1 to 4). */
	public BlockState fill(CargoLooks.Look look, int band) {
		return this.defaultBlockState().setValue(LOOK, look).setValue(BAND, band);
	}
}
