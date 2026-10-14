package io.github.boxofdog.simpleblock.rope;

import java.util.Map;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * The Spelunker's Rope's anchor ({@link SpelunkerRopes}): {@link #SUPPORT} is the side its support block is
 * on (up: hung under it; a side: fixed to its face). The rope hangs from it straight down.
 */
public class RopeAnchorBlock extends BaseEntityBlock {
	public static final EnumProperty<Direction> SUPPORT = EnumProperty.create("support", Direction.class, Direction.UP, Direction.NORTH,
		Direction.EAST, Direction.SOUTH, Direction.WEST);
	private static final Map<Direction, VoxelShape> SHAPES = Map.of(
		Direction.UP, Block.box(5.0, 10.0, 5.0, 11.0, 16.0, 11.0),
		Direction.NORTH, Block.box(5.0, 8.0, 0.0, 11.0, 14.0, 10.0),
		Direction.SOUTH, Block.box(5.0, 8.0, 6.0, 11.0, 14.0, 16.0),
		Direction.WEST, Block.box(0.0, 8.0, 5.0, 10.0, 14.0, 11.0),
		Direction.EAST, Block.box(6.0, 8.0, 5.0, 16.0, 14.0, 11.0));

	public RopeAnchorBlock(BlockBehaviour.Properties properties) {
		super(properties);
		this.registerDefaultState(this.stateDefinition.any().setValue(SUPPORT, Direction.UP));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(SUPPORT);
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return SHAPES.get(state.getValue(SUPPORT));
	}

	@Override
	protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return Shapes.empty();
	}

	@Override
	protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
		Direction support = state.getValue(SUPPORT);
		BlockPos on = pos.relative(support);
		return level.getBlockState(on).isFaceSturdy(level, on, support.getOpposite());
	}

	@Override
	protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction directionToNeighbour,
		BlockPos neighbourPos, BlockState neighbourState, RandomSource random) {
		return directionToNeighbour == state.getValue(SUPPORT) && !state.canSurvive(level, pos)
			? Blocks.AIR.defaultBlockState()
			: super.updateShape(state, level, ticks, pos, directionToNeighbour, neighbourPos, neighbourState, random);
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
		return RopeLineBlock.takeDown(level, pos, player);
	}

	@Override
	public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new RopeAnchorBlockEntity(pos, state);
	}
}
