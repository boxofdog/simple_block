package io.github.boxofdog.simpleblock.cable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import org.jspecify.annotations.Nullable;

/**
 * Cable drum: an empty spool on a timber frame until a cable coil is used on it; then it is a winch
 * ({@link #WOUND}) holding the cable ({@link CableDrumBlockEntity}). {@link #FACING} is the side the
 * cable pays out from; the axle runs across it, with the hand crank on its end to the right of the
 * front (east of a north-facing drum).
 *
 * <p>M05: a drum stands on the ground as before, or hangs under a wooden beam ({@link #HANGING}: placed
 * against a beam's underside, like an anchor, with the same supports), upside down so that its cable
 * leaves at about the anchors' roller height. Clicking the drum on its crank side, near the axle, works the
 * crank ({@link CableDrive}: left-click feeds, right-click pulls); clicking it anywhere else does what it
 * did in M04.
 */
public class CableDrumBlock extends BaseEntityBlock {
	public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
	public static final BooleanProperty WOUND = BooleanProperty.create("wound");
	public static final BooleanProperty HANGING = BooleanProperty.create("hanging");
	/** The crank's reach around the axle, in model pixels, and where the crank side starts. */
	private static final double CRANK_RADIUS = 6.0;
	private static final double CRANK_SIDE_X = 14.5;

	public CableDrumBlock(BlockBehaviour.Properties properties) {
		super(properties);
		this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(WOUND, false).setValue(HANGING, false));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING, WOUND, HANGING);
	}

	@Override
	public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
		BlockState state = this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
		boolean underBeam = context.getClickedFace() == Direction.DOWN && CableAnchorBlock.canHangFrom(context.getLevel(), context.getClickedPos().above());
		return state.setValue(HANGING, underBeam);
	}

	@Override
	protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
		return !state.getValue(HANGING) || CableAnchorBlock.canHangFrom(level, pos.above());
	}

	@Override
	protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction directionToNeighbour,
		BlockPos neighbourPos, BlockState neighbourState, RandomSource random) {
		return directionToNeighbour == Direction.UP && !state.canSurvive(level, pos)
			? Blocks.AIR.defaultBlockState()
			: super.updateShape(state, level, ticks, pos, directionToNeighbour, neighbourPos, neighbourState, random);
	}

	/** The block plus the crank, which sticks out on its side. */
	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		double y0 = state.getValue(HANGING) ? 16.0 - 9.5 - CRANK_RADIUS : 9.5 - CRANK_RADIUS;
		VoxelShape crank = switch (crankSide(state)) {
			case EAST -> Block.box(16.0, y0, 8.0 - CRANK_RADIUS + 1.0, 21.0, y0 + 2 * CRANK_RADIUS, 8.0 + CRANK_RADIUS - 1.0);
			case WEST -> Block.box(-5.0, y0, 8.0 - CRANK_RADIUS + 1.0, 0.0, y0 + 2 * CRANK_RADIUS, 8.0 + CRANK_RADIUS - 1.0);
			case SOUTH -> Block.box(8.0 - CRANK_RADIUS + 1.0, y0, 16.0, 8.0 + CRANK_RADIUS - 1.0, y0 + 2 * CRANK_RADIUS, 21.0);
			default -> Block.box(8.0 - CRANK_RADIUS + 1.0, y0, -5.0, 8.0 + CRANK_RADIUS - 1.0, y0 + 2 * CRANK_RADIUS, 0.0);
		};
		return Shapes.or(Shapes.block(), crank);
	}

	@Override
	protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return Shapes.block();
	}

	/** The world side the crank is on: to the right of the front (east of a north-facing drum). */
	public static Direction crankSide(BlockState state) {
		return state.getValue(FACING).getClockWise();
	}

	/**
	 * Whether a click at {@code hit} (world position) is on the crank: on the crank side's outer part,
	 * within the crank's reach of the axle.
	 */
	public static boolean isCrank(BlockState state, BlockPos pos, Vec3 hit) {
		Direction side = crankSide(state);
		Vec3 local = hit.subtract(pos.getX(), pos.getY(), pos.getZ()).scale(16.0);
		double out = switch (side) {
			case EAST -> local.x;
			case WEST -> 16.0 - local.x;
			case SOUTH -> local.z;
			default -> 16.0 - local.z;
		};
		double across = side.getAxis() == Direction.Axis.X ? local.z : local.x;
		double axleY = state.getValue(HANGING) ? 16.0 - 9.5 : 9.5;
		double dy = local.y - axleY;
		double dc = across - 8.0;
		return out >= CRANK_SIDE_X && dy * dy + dc * dc <= CRANK_RADIUS * CRANK_RADIUS;
	}

	@Override
	protected InteractionResult useItemOn(ItemStack itemStack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
		BlockHitResult hitResult) {
		boolean powered = level.getBlockEntity(pos) instanceof CableDrumBlockEntity drum && drum.powered();
		if (hand == InteractionHand.MAIN_HAND && isCrank(state, pos, hitResult.getLocation())) {
			return powered ? CoalWinch.cycleLever(level, pos, player) : CableDrive.pull(level, pos, player);
		}
		if (itemStack.is(Items.FURNACE) && !powered) {
			return CoalWinch.fitFurnace(level, pos, player, itemStack);
		}
		if (powered && CoalWinch.isCoal(itemStack)) {
			return CoalWinch.addCoal(level, pos, player, itemStack);
		}
		if (itemStack.is(CableContent.CABLE_COIL)) {
			return CableRoutes.windCoil(level, pos, player, itemStack);
		}
		return InteractionResult.TRY_WITH_EMPTY_HAND;
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
		boolean powered = level.getBlockEntity(pos) instanceof CableDrumBlockEntity drum && drum.powered();
		if (isCrank(state, pos, hitResult.getLocation())) {
			return powered ? CoalWinch.cycleLever(level, pos, player) : CableDrive.pull(level, pos, player);
		}
		if (powered && !player.isSecondaryUseActive() && CableRoutes.heldHook(level, player) == null) {
			return CoalWinch.inspect(level, pos, player);
		}
		return CableRoutes.useDrum(level, pos, player);
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new CableDrumBlockEntity(pos, state);
	}

	@Override
	public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
		return level.isClientSide()
			? createTickerHelper(type, CableContent.CABLE_DRUM_ENTITY, CableDrumBlockEntity::clientTick)
			: createTickerHelper(type, CableContent.CABLE_DRUM_ENTITY, CableDrumBlockEntity::serverTick);
	}
}
