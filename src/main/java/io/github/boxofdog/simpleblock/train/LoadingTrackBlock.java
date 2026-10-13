package io.github.boxofdog.simpleblock.train;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseRailBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.block.state.properties.RailShape;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * M06 Loading Track ({@link LoadingTracks}): a straight, flat rail with a station post on its left.
 * {@link #FACING} is the way the player faced when placing it: the front of a cart docked here points
 * that way, whichever way it came in. It never bends or slopes to meet other rails (other rails still
 * join it). Using it (best: its post, which stands above a docked cart) releases the train held here.
 */
public class LoadingTrackBlock extends BaseRailBlock {
	public static final EnumProperty<RailShape> SHAPE = EnumProperty.create("shape", RailShape.class, RailShape.NORTH_SOUTH, RailShape.EAST_WEST);
	public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;

	private static final VoxelShape RAIL = Block.column(16.0, 0.0, 2.0);
	/** The post on the track's left (facing north: the west side), up past a docked cart's sides. */
	private static final VoxelShape[] SHAPES = new VoxelShape[4];

	static {
		for (Direction facing : Direction.Plane.HORIZONTAL) {
			VoxelShape post = switch (facing) {
				case NORTH -> Block.box(0.0, 0.0, 6.5, 3.0, 16.0, 9.5);
				case SOUTH -> Block.box(13.0, 0.0, 6.5, 16.0, 16.0, 9.5);
				case EAST -> Block.box(6.5, 0.0, 0.0, 9.5, 16.0, 3.0);
				default -> Block.box(6.5, 0.0, 13.0, 9.5, 16.0, 16.0);
			};
			SHAPES[facing.get2DDataValue()] = Shapes.or(RAIL, post);
		}
	}

	public LoadingTrackBlock(BlockBehaviour.Properties properties) {
		super(true, properties);
		this.registerDefaultState(this.stateDefinition.any().setValue(SHAPE, RailShape.NORTH_SOUTH).setValue(FACING, Direction.NORTH)
			.setValue(WATERLOGGED, false));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(SHAPE, FACING, WATERLOGGED);
	}

	@Override
	public Property<RailShape> getShapeProperty() {
		return SHAPE;
	}

	static RailShape shapeFor(Direction facing) {
		return facing.getAxis() == Direction.Axis.X ? RailShape.EAST_WEST : RailShape.NORTH_SOUTH;
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		Direction facing = context.getHorizontalDirection();
		return super.getStateForPlacement(context).setValue(FACING, facing).setValue(SHAPE, shapeFor(facing));
	}

	/** Keeps its own straight, level shape: it does not turn to meet neighbours. */
	@Override
	protected BlockState updateDir(Level level, BlockPos pos, BlockState state, boolean first) {
		return state;
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return SHAPES[state.getValue(FACING).get2DDataValue()];
	}

	@Override
	protected BlockState rotate(BlockState state, Rotation rotation) {
		Direction facing = rotation.rotate(state.getValue(FACING));
		return state.setValue(FACING, facing).setValue(SHAPE, shapeFor(facing));
	}

	@Override
	protected BlockState mirror(BlockState state, Mirror mirror) {
		return this.rotate(state, mirror.getRotation(state.getValue(FACING)));
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
		return LoadingTracks.release(level, pos, player);
	}

	/** The way a cart docked at {@code state} has its front, or null if it is not a loading track. */
	public static @Nullable Direction front(BlockState state) {
		return state.getBlock() instanceof LoadingTrackBlock ? state.getValue(FACING) : null;
	}
}
