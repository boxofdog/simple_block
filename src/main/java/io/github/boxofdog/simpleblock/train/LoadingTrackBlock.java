package io.github.boxofdog.simpleblock.train;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
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
import net.minecraft.world.level.block.state.properties.BooleanProperty;
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
 * join it). Shown to players as the "Loading Rail" (its id stays {@code simple_block:loading_track}).
 *
 * <p>M06 correction (owner, 2026-10-13): a hinged triangular iron stop on the inside edge of the left
 * rail. Folded flat under a passing cart's floor while nothing is docked ({@link #RAISED} false); raised
 * upright just behind the docked cart's rear wall while a cart is docked (the rear is opposite
 * {@link #FACING}, whatever way the cart came). Using the rail, or the raised stop (which stands clear
 * of the cart's hitbox, so it can be clicked from behind the cart), releases the train held here.
 */
public class LoadingTrackBlock extends BaseRailBlock {
	public static final EnumProperty<RailShape> SHAPE = EnumProperty.create("shape", RailShape.class, RailShape.NORTH_SOUTH, RailShape.EAST_WEST);
	public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
	/** Whether the stop is raised (a cart is docked here). */
	public static final BooleanProperty RAISED = BooleanProperty.create("raised");

	private static final VoxelShape RAIL = Block.column(16.0, 0.0, 2.0);
	/**
	 * The rail plus, when raised, the stop: facing north (front north) it stands on the west rail's
	 * inside edge, x 4 to 6, from z 17 (just behind a docked cart's rear wall at z 18) to z 21, up to
	 * y 10. It reaches past the block's south edge, behind the cart.
	 */
	private static final VoxelShape[] RAISED_SHAPES = new VoxelShape[4];

	static {
		for (Direction facing : Direction.Plane.HORIZONTAL) {
			RAISED_SHAPES[facing.get2DDataValue()] = Shapes.or(RAIL, turned(facing, 4.0, 17.0, 6.0, 21.0, 10.0));
		}
	}

	/** A box modelled for a north-facing rail (x1..x2, z1..z2, y 0..height), turned to {@code facing} as the block model is. */
	private static VoxelShape turned(Direction facing, double x1, double z1, double x2, double z2, double height) {
		return switch (facing) {
			case NORTH -> Block.box(x1, 0.0, z1, x2, height, z2);
			case EAST -> Block.box(16.0 - z2, 0.0, x1, 16.0 - z1, height, x2);
			case SOUTH -> Block.box(16.0 - x2, 0.0, 16.0 - z2, 16.0 - x1, height, 16.0 - z1);
			default -> Block.box(z1, 0.0, 16.0 - x2, z2, height, 16.0 - x1);
		};
	}

	public LoadingTrackBlock(BlockBehaviour.Properties properties) {
		super(true, properties);
		this.registerDefaultState(this.stateDefinition.any().setValue(SHAPE, RailShape.NORTH_SOUTH).setValue(FACING, Direction.NORTH)
			.setValue(WATERLOGGED, false).setValue(RAISED, false));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(SHAPE, FACING, WATERLOGGED, RAISED);
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
		return state.getValue(RAISED) ? RAISED_SHAPES[state.getValue(FACING).get2DDataValue()] : RAIL;
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

	/** While raised: checks now and then that a cart is still docked here, and folds the stop if not (a docked cart broken, say). */
	@Override
	protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
		if (state.getValue(RAISED)) {
			LoadingTracks.checkStop(level, pos, state);
		}
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
