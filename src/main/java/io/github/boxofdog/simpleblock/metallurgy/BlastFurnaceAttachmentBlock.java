package io.github.boxofdog.simpleblock.metallurgy;

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
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;

/**
 * M08: a furnace block standing out in front of the furnace (the {@link BlastFurnaceChannelBlock
 * channel} and the {@link BlastFurnaceMoldBlock mold}). Like the furnace's other blocks it has no item
 * and drops nothing itself: using it uses the furnace, breaking it breaks the whole furnace, picking it
 * gives the furnace item.
 */
public abstract class BlastFurnaceAttachmentBlock extends Block {
	public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;

	/** How many blocks in front of the body's front row this block is (the channel 1, the mold 2). */
	private final int out;

	protected BlastFurnaceAttachmentBlock(BlockBehaviour.Properties properties, int out) {
		super(properties);
		this.out = out;
	}

	/** The controller of the furnace this block belongs to. */
	BlockPos controller(BlockState state, BlockPos pos) {
		Direction facing = state.getValue(FACING);
		return pos.relative(facing.getOpposite(), this.out + 1).above(BlastFurnaceStructure.CONTROLLER_LAYER);
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
		return BlastFurnaceBlock.open(level, this.controller(state, pos), player);
	}

	@Override
	public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
		BlastFurnaceBlock.markBrokenBy(level, this.controller(state, pos), player);
		return super.playerWillDestroy(level, pos, state, player);
	}

	@Override
	protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
		BlastFurnaceBlock.dismantle(level, this.controller(state, pos), state.getValue(FACING));
	}

	@Override
	protected ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state, boolean includeData) {
		return new ItemStack(MetallurgyContent.BLAST_FURNACE_ITEM);
	}
}
