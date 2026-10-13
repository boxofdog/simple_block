package io.github.boxofdog.simpleblock.metallurgy;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;

/**
 * M08: every block of the furnace except its controller. It has no item and drops nothing itself:
 * using it uses the furnace, breaking it breaks the whole furnace ({@link BlastFurnaceBlock#dismantle}),
 * and picking it gives the furnace item. Only the middle of the top layer has a model (the hopper);
 * the controller draws the rest.
 */
public class BlastFurnacePartBlock extends Block {
	/** Offset from the controller across x and z (0..2, the controller at 1), and the layer (0..3, the controller's is 1). */
	public static final IntegerProperty DX = IntegerProperty.create("dx", 0, 2);
	public static final IntegerProperty LAYER = IntegerProperty.create("layer", 0, BlastFurnaceStructure.LAYERS - 1);
	public static final IntegerProperty DZ = IntegerProperty.create("dz", 0, 2);
	/** The furnace's facing (its front): which top block is the hopper, and how the crown is turned. */
	public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;

	public BlastFurnacePartBlock(BlockBehaviour.Properties properties) {
		super(properties);
		this.registerDefaultState(this.stateDefinition.any().setValue(DX, 0).setValue(LAYER, 0).setValue(DZ, 0).setValue(FACING, Direction.NORTH));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(DX, LAYER, DZ, FACING);
	}

	private static @Nullable BlockPos controller(BlockState state, BlockPos pos) {
		return pos.offset(1 - state.getValue(DX), BlastFurnaceStructure.CONTROLLER_LAYER - state.getValue(LAYER), 1 - state.getValue(DZ));
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
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
		return BlastFurnaceBlock.open(level, controller(state, pos), player);
	}

	@Override
	public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
		BlastFurnaceBlock.markBrokenBy(level, controller(state, pos), player);
		return super.playerWillDestroy(level, pos, state, player);
	}

	@Override
	protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
		BlastFurnaceBlock.dismantle(level, controller(state, pos), state.getValue(FACING));
	}

	@Override
	protected ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state, boolean includeData) {
		return new ItemStack(MetallurgyContent.BLAST_FURNACE_ITEM);
	}
}
