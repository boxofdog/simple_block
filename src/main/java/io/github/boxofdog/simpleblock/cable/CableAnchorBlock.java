package io.github.boxofdog.simpleblock.cable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
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
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import org.jspecify.annotations.Nullable;

/**
 * Copper cable anchor: hangs from the underside of a wooden block and carries a cable on a roller.
 * The support must be in {@code #simple_block:cable_anchor_supports} (logs, planks, wooden slabs,
 * stairs and fences, bamboo blocks) and offer the centre of its bottom face, like a hanging lantern
 * needs: so a full block, a fence post, a bottom slab or upright stairs work, a top slab or upside-down
 * stairs (which leave a gap) do not. Remove the support and the anchor drops.
 *
 * <p>No collision: carts, riders and the connecting rod pass under and beside it. {@link #FACING}
 * (from the player who placed it) is the way the cable runs over the roller, and only its axis
 * matters: the model is the same for north and south, and for east and west. The hanger arm is on one
 * side only, west of a north-south cable and north of an east-west one, so a gripping rod's stem can
 * pass the roller on the other side.
 *
 * <p>M05: on a closed loop an anchor also carries the return strand ({@link #RETURN}): a guide roller
 * on the arm side, or, on the last anchor, the return wheel. Using a copper anchor on the last anchor
 * of a route closes the loop ({@link CableLoops}).
 */
public class CableAnchorBlock extends BaseEntityBlock {
	public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;

	/** M05: what an anchor carries for a closed loop's return strand ({@link CableLoops}). */
	public enum Return implements StringRepresentable {
		NONE("none"),
		/** A small guide roller for the return strand, beside the main roller on the arm side. */
		GUIDE("guide"),
		/** The return wheel at the end of a loop, on the side {@link #FACING} points to. */
		WHEEL("wheel"),
		/**
		 * M07 playtest patch §4: the head anchor of a shaft while a skip hangs on its cable, drawn as a
		 * pulley wheel under the ceiling plate; the cable comes in towards {@link #FACING} over the wheel
		 * and drops on that side ({@link CableHoists}). Only while a skip is hooked on.
		 */
		PULLEY("pulley");

		private final String name;

		Return(String name) {
			this.name = name;
		}

		@Override
		public String getSerializedName() {
			return this.name;
		}
	}

	public static final EnumProperty<Return> RETURN = EnumProperty.create("return", Return.class);
	/** Mounting plate, arm and roller, whichever way the roller faces. */
	private static final VoxelShape SHAPE = Shapes.or(Block.box(4.0, 15.0, 4.0, 12.0, 16.0, 12.0), Block.box(3.5, 7.75, 4.0, 12.5, 15.0, 12.0));

	public CableAnchorBlock(BlockBehaviour.Properties properties) {
		super(properties);
		this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(RETURN, Return.NONE));
	}

	/** Whether the block at {@code above} can carry an anchor hung under it. */
	public static boolean canHangFrom(LevelReader level, BlockPos above) {
		return level.getBlockState(above).is(CableContent.ANCHOR_SUPPORTS) && Block.canSupportCenter(level, above, Direction.DOWN);
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING, RETURN);
	}

	@Override
	public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
		BlockState state = this.defaultBlockState().setValue(FACING, context.getHorizontalDirection());
		return state.canSurvive(context.getLevel(), context.getClickedPos()) ? state : null;
	}

	@Override
	protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
		return canHangFrom(level, pos.above());
	}

	@Override
	protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction directionToNeighbour,
		BlockPos neighbourPos, BlockState neighbourState, RandomSource random) {
		return directionToNeighbour == Direction.UP && !state.canSurvive(level, pos)
			? Blocks.AIR.defaultBlockState()
			: super.updateShape(state, level, ticks, pos, directionToNeighbour, neighbourPos, neighbourState, random);
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return SHAPE;
	}

	@Override
	protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return Shapes.empty();
	}

	@Override
	protected boolean isPathfindable(BlockState state, PathComputationType type) {
		return true;
	}

	/** M05: a copper anchor used on the last anchor of a route closes it into a loop (the anchor becomes its return wheel). */
	@Override
	protected InteractionResult useItemOn(ItemStack itemStack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
		BlockHitResult hitResult) {
		if (itemStack.is(CableContent.CABLE_ANCHOR_ITEM) && CableLoops.routeEndDrum(level, pos) != null) {
			return CableLoops.close(level, pos, player, itemStack);
		}
		return InteractionResult.TRY_WITH_EMPTY_HAND;
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
		return CableRoutes.useAnchor(level, pos, player);
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new CableAnchorBlockEntity(pos, state);
	}

	@Override
	public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
		return level.isClientSide() ? null : createTickerHelper(type, CableContent.CABLE_ANCHOR_ENTITY, CableAnchorBlockEntity::serverTick);
	}
}
