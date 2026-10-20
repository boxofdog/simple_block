package io.github.boxofdog.simpleblock.cable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
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
import net.minecraft.world.phys.AABB;
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
	/** M07: a coal winch: the frame model's motor-side stand is replaced by the furnace motor. */
	public static final BooleanProperty POWERED = BooleanProperty.create("powered");
	/** The crank's reach around the axle, in model pixels, and where the crank side starts. */
	private static final double CRANK_RADIUS = 6.0;
	private static final double CRANK_SIDE_X = 14.5;

	public CableDrumBlock(BlockBehaviour.Properties properties) {
		super(properties);
		this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(WOUND, false).setValue(HANGING, false).setValue(POWERED, false));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING, WOUND, HANGING, POWERED);
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

	/**
	 * The handle on the crank side, in model pixels relative to the axle: {@code out} past the block's
	 * side, {@code dy} along the drum's up (flipped when hanging), {@code across} from the middle. The
	 * crank turns in a 6 px circle; the coal winch's lever (M13, BUG-008) stands 9 px up from the axle and
	 * leans 35 degrees either way, so its knob reaches 18 px high and 5.5 px to either side.
	 */
	private static double[] handle(BlockState state) {
		return state.getValue(POWERED)
			? new double[] {16.0, 19.0, -2.0, 9.5, 6.5}
			: new double[] {16.0, 21.0, -CRANK_RADIUS, CRANK_RADIUS, CRANK_RADIUS - 1.0};
	}

	/** The block plus the crank or the lever, which sticks out on its side. */
	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		double[] h = handle(state);
		boolean hanging = state.getValue(HANGING);
		double axleY = hanging ? 16.0 - 9.5 : 9.5;
		double y0 = hanging ? axleY - h[3] : axleY + h[2];
		double y1 = Math.min(hanging ? axleY - h[2] : axleY + h[3], 24.0);
		y0 = Math.max(y0, -8.0);
		double a0 = 8.0 - h[4];
		double a1 = 8.0 + h[4];
		double o0 = h[0];
		double o1 = h[1];
		VoxelShape handle = switch (crankSide(state)) {
			case EAST -> Block.box(o0, y0, a0, o1, y1, a1);
			case WEST -> Block.box(16.0 - o1, y0, a0, 16.0 - o0, y1, a1);
			case SOUTH -> Block.box(a0, y0, o0, a1, y1, o1);
			default -> Block.box(a0, y0, 16.0 - o1, a1, y1, 16.0 - o0);
		};
		return Shapes.or(Shapes.block(), handle);
	}

	/**
	 * M13 (BUG-008): the crank and the lever stick out of the drum's cell (the lever's knob above it), and
	 * the game's look test only tries a block's shape where the look passes through that block's own
	 * cell. This finds the nearest drum within a block of the look whose shape the look hits, for the
	 * client's crosshair to take when it is nearer than what the look hit otherwise. Null if none.
	 */
	public static @Nullable BlockHitResult pickOverhang(BlockGetter level, Vec3 from, Vec3 to) {
		BlockHitResult best = null;
		double bestDistance = Double.MAX_VALUE;
		AABB box = new AABB(from, to).inflate(1.0);
		for (BlockPos pos : BlockPos.betweenClosed(BlockPos.containing(box.minX, box.minY, box.minZ), BlockPos.containing(box.maxX, box.maxY, box.maxZ))) {
			BlockState state = level.getBlockState(pos);
			if (!(state.getBlock() instanceof CableDrumBlock)) {
				continue;
			}
			BlockHitResult hit = state.getShape(level, pos).clip(from, to, pos.immutable());
			if (hit != null && hit.getLocation().distanceToSqr(from) < bestDistance) {
				bestDistance = hit.getLocation().distanceToSqr(from);
				best = hit;
			}
		}
		return best;
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
	 * Whether a click at {@code hit} (world position) is on the crank or lever: on the crank side's outer
	 * part, within the crank's reach of the axle, or (a coal winch, M13 BUG-008) anywhere on the lever's swing.
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
		boolean hanging = state.getValue(HANGING);
		double axleY = hanging ? 16.0 - 9.5 : 9.5;
		double dy = (local.y - axleY) * (hanging ? -1.0 : 1.0);
		double dc = across - 8.0;
		if (state.getValue(POWERED)) {
			double[] h = handle(state);
			return out >= CRANK_SIDE_X && dy >= h[2] - 0.01 && dy <= h[3] + 0.01 && Math.abs(dc) <= h[4] + 0.01;
		}
		return out >= CRANK_SIDE_X && dy * dy + dc * dc <= CRANK_RADIUS * CRANK_RADIUS;
	}

	@Override
	protected InteractionResult useItemOn(ItemStack itemStack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
		BlockHitResult hitResult) {
		if (level.getBlockEntity(pos) instanceof CableDrumBlockEntity far && far.farFrom() != null) {
			// M07 playtest change 2: the far drum of a two-drum line works it from this end; it takes no coal, furnace or cable.
			if (hand == InteractionHand.MAIN_HAND && isCrank(state, pos, hitResult.getLocation())) {
				return level instanceof ServerLevel serverLevel ? FarDrums.work(serverLevel, far, player, true) : InteractionResult.SUCCESS;
			}
			String refused = CoalWinch.isCoal(itemStack) ? "coal" : itemStack.is(Items.FURNACE) ? "furnace" : itemStack.is(CableContent.CABLE_COIL) ? "coil" : null;
			if (refused != null) {
				return level instanceof ServerLevel serverLevel ? FarDrums.refuse(serverLevel, far, player, refused) : InteractionResult.SUCCESS;
			}
			return InteractionResult.TRY_WITH_EMPTY_HAND;
		}
		boolean powered = level.getBlockEntity(pos) instanceof CableDrumBlockEntity drum && drum.powered();
		if (hand == InteractionHand.MAIN_HAND && isCrank(state, pos, hitResult.getLocation())) {
			return powered ? CoalWinch.setLever(level, pos, player, CableLines.Lever.PULL) : CableDrive.pull(level, pos, player);
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
		if (level.getBlockEntity(pos) instanceof CableDrumBlockEntity far && far.farFrom() != null) {
			if (!(level instanceof ServerLevel serverLevel)) {
				return InteractionResult.SUCCESS;
			}
			if (isCrank(state, pos, hitResult.getLocation())) {
				return FarDrums.work(serverLevel, far, player, true);
			}
			return player.isSecondaryUseActive() ? FarDrums.unhook(serverLevel, far, player) : FarDrums.inspect(serverLevel, far, player);
		}
		boolean powered = level.getBlockEntity(pos) instanceof CableDrumBlockEntity drum && drum.powered();
		if (isCrank(state, pos, hitResult.getLocation())) {
			return powered ? CoalWinch.setLever(level, pos, player, CableLines.Lever.PULL) : CableDrive.pull(level, pos, player);
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
