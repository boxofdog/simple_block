package io.github.boxofdog.simpleblock.metallurgy;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * M08: the tapping channel, the block in front of the furnace's middle: an iron trough on a trestle
 * stepping down from the tap in the furnace's iron front to the mold beyond. It shows molten metal
 * running while the furnace {@link #POURING pours} a cast.
 */
public class BlastFurnaceChannelBlock extends BlastFurnaceAttachmentBlock {
	public static final BooleanProperty POURING = BlastFurnaceBlock.POURING;
	private static final VoxelShape NORTH_SOUTH = Block.box(4.0, 0.0, 0.0, 12.0, 16.0, 16.0);
	private static final VoxelShape EAST_WEST = Block.box(0.0, 0.0, 4.0, 16.0, 16.0, 12.0);

	public BlastFurnaceChannelBlock(BlockBehaviour.Properties properties) {
		super(properties, 1);
		this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(POURING, false));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING, POURING);
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return state.getValue(FACING).getAxis() == Direction.Axis.Z ? NORTH_SOUTH : EAST_WEST;
	}
}
