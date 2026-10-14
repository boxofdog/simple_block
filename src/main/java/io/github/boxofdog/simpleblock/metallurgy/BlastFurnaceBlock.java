package io.github.boxofdog.simpleblock.metallurgy;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
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

/**
 * M08 blast furnace: the controller block, the middle of the stack's lower layer
 * ({@link BlastFurnaceStructure}). It holds the furnace's inventories ({@link BlastFurnaceBlockEntity})
 * and draws layers 0-2: the brick stack facing {@link #FACING} (the front, with the firebox door and the
 * casting mold), its fire when {@link #LIT} and the molten pour when {@link #POURING}.
 *
 * <p>Using any block of the furnace opens its screen. Breaking any block of it breaks the whole furnace:
 * the furnace item drops (not for a creative player) and so does everything inside, as ordinary
 * stacks.
 */
public class BlastFurnaceBlock extends BaseEntityBlock {
	public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
	public static final BooleanProperty LIT = BlockStateProperties.LIT;
	public static final BooleanProperty POURING = BooleanProperty.create("pouring");
	/** Set while the parts of a broken furnace are being cleared (server thread), so each part removed does not start over. */
	private static boolean dismantling;

	public BlastFurnaceBlock(BlockBehaviour.Properties properties) {
		super(properties);
		this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(LIT, false).setValue(POURING, false));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING, LIT, POURING);
	}

	@Override
	public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
		return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
	}

	@Override
	protected BlockState rotate(BlockState state, Rotation rotation) {
		return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
	}

	@Override
	protected BlockState mirror(BlockState state, Mirror mirror) {
		return state.rotate(mirror.getRotation(state.getValue(FACING)));
	}

	/** Its model is drawn across its neighbours, which are the furnace's own blocks: no shadow from them, sky light passes. */
	@Override
	protected float getShadeBrightness(BlockState state, BlockGetter level, BlockPos pos) {
		return 1.0F;
	}

	@Override
	protected boolean propagatesSkylightDown(BlockState state) {
		return true;
	}

	@Override
	protected RenderShape getRenderShape(BlockState state) {
		return RenderShape.MODEL;
	}

	/** Opens the screen of the furnace whose controller is at {@code controller}. */
	static InteractionResult open(Level level, BlockPos controller, Player player) {
		if (!(level.getBlockEntity(controller) instanceof BlastFurnaceBlockEntity furnace)) {
			return InteractionResult.PASS;
		}
		if (!level.isClientSide()) {
			player.openMenu(new SimpleMenuProvider((id, inventory, p) -> BlastFurnaceMenu.server(id, inventory, furnace),
				Component.translatable("block.simple_block.blast_furnace")));
		}
		return InteractionResult.SUCCESS;
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
		return open(level, pos, player);
	}

	/** Remembers a creative player breaking the furnace, so that it does not drop its item. */
	static void markBrokenBy(Level level, BlockPos controller, Player player) {
		if (player.isCreative() && level.getBlockEntity(controller) instanceof BlastFurnaceBlockEntity furnace) {
			furnace.brokenInCreative = true;
		}
	}

	@Override
	public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
		markBrokenBy(level, pos, player);
		return super.playerWillDestroy(level, pos, state, player);
	}

	/**
	 * One of its blocks went: the rest of the furnace goes too. Any other block gone breaks the controller
	 * (which drops the furnace item, unless broken in creative, and the furnace's contents); the controller
	 * gone clears the rest, which drop nothing.
	 */
	static void dismantle(ServerLevel level, BlockPos controller, Direction facing) {
		if (dismantling) {
			return;
		}
		BlockState state = level.getBlockState(controller);
		if (state.is(MetallurgyContent.BLAST_FURNACE)) {
			boolean creative = level.getBlockEntity(controller) instanceof BlastFurnaceBlockEntity furnace && furnace.brokenInCreative;
			level.destroyBlock(controller, !creative);
			return;
		}
		dismantling = true;
		try {
			for (BlockPos pos : BlastFurnaceStructure.blocks(controller, facing)) {
				BlockState block = level.getBlockState(pos);
				if (block.is(MetallurgyContent.BLAST_FURNACE_PART) || block.getBlock() instanceof BlastFurnaceAttachmentBlock) {
					level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
				}
			}
		} finally {
			dismantling = false;
		}
	}

	@Override
	protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
		dismantle(level, pos, state.getValue(FACING));
		super.affectNeighborsAfterRemoval(state, level, pos, movedByPiston);
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new BlastFurnaceBlockEntity(pos, state);
	}

	@Override
	public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
		return level.isClientSide() ? null : createTickerHelper(type, MetallurgyContent.BLAST_FURNACE_ENTITY, BlastFurnaceBlockEntity::serverTick);
	}
}
