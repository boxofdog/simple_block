package io.github.boxofdog.simpleblock.metallurgy;

import java.util.ArrayList;
import java.util.List;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;

/**
 * M08: the furnace's shape (redesigned after the owner's sketch and notes, 2026-10-13). The furnace
 * itself is 3x3 blocks and four high; its controller ({@link BlastFurnaceBlock}, which holds the
 * inventories) is the middle block of the second layer, and every other block of it is a
 * {@link BlastFurnacePartBlock} that knows its offset from it and the furnace's facing. Layers 0-2 are
 * a large brick body behind an iron front (doors, the tap); layer 3 its brick crown with the receiving
 * hopper in the <b>back middle</b> block, the only place cargo goes in. Out of the front, on the
 * ground, a {@link BlastFurnaceChannelBlock channel} leads from the tap to a full-block
 * {@link BlastFurnaceMoldBlock mold}: 38 blocks in all.
 */
public final class BlastFurnaceStructure {
	public static final int LAYERS = 4;
	/** The controller's layer. */
	public static final int CONTROLLER_LAYER = 1;
	/** The hopper's layer: cargo goes in over its rim, from above layer 3. */
	public static final int HOPPER_LAYER = 3;

	private BlastFurnaceStructure() {
	}

	/** The controller of a furnace whose front middle block of the body's bottom layer is {@code front}, facing {@code facing}. */
	public static BlockPos controllerFor(BlockPos front, Direction facing) {
		return front.relative(facing.getOpposite()).above(CONTROLLER_LAYER);
	}

	/** The controller of a furnace whose mold (the block its item is used on) is {@code mold}, facing {@code facing}. */
	public static BlockPos controllerForMold(BlockPos mold, Direction facing) {
		return controllerFor(mold.relative(facing.getOpposite(), 2), facing);
	}

	public static BlockPos channel(BlockPos controller, Direction facing) {
		return controller.below(CONTROLLER_LAYER).relative(facing, 2);
	}

	public static BlockPos mold(BlockPos controller, Direction facing) {
		return controller.below(CONTROLLER_LAYER).relative(facing, 3);
	}

	/** The receiving hopper: the back middle block of the top layer. */
	public static BlockPos hopper(BlockPos controller, Direction facing) {
		return controller.above(HOPPER_LAYER - CONTROLLER_LAYER).relative(facing.getOpposite());
	}

	/** Every block of the furnace around {@code controller}, the controller first, then the body, the channel and the mold. */
	public static List<BlockPos> blocks(BlockPos controller, Direction facing) {
		List<BlockPos> blocks = new ArrayList<>();
		blocks.add(controller.immutable());
		for (int dy = -CONTROLLER_LAYER; dy < LAYERS - CONTROLLER_LAYER; dy++) {
			for (int dx = -1; dx <= 1; dx++) {
				for (int dz = -1; dz <= 1; dz++) {
					if (dx != 0 || dy != 0 || dz != 0) {
						blocks.add(controller.offset(dx, dy, dz));
					}
				}
			}
		}
		blocks.add(channel(controller, facing));
		blocks.add(mold(controller, facing));
		return blocks;
	}

	/** The furnace block at {@code pos}, which belongs to the furnace at {@code controller}. */
	public static BlockState stateAt(BlockPos controller, BlockPos pos, Direction facing) {
		if (pos.equals(controller)) {
			return MetallurgyContent.BLAST_FURNACE.defaultBlockState().setValue(BlastFurnaceBlock.FACING, facing);
		}
		if (pos.equals(channel(controller, facing))) {
			return MetallurgyContent.BLAST_FURNACE_CHANNEL.defaultBlockState().setValue(BlastFurnaceChannelBlock.FACING, facing);
		}
		if (pos.equals(mold(controller, facing))) {
			return MetallurgyContent.BLAST_FURNACE_MOLD.defaultBlockState().setValue(BlastFurnaceMoldBlock.FACING, facing);
		}
		return MetallurgyContent.BLAST_FURNACE_PART.defaultBlockState()
			.setValue(BlastFurnacePartBlock.DX, pos.getX() - controller.getX() + 1)
			.setValue(BlastFurnacePartBlock.LAYER, pos.getY() - controller.getY() + CONTROLLER_LAYER)
			.setValue(BlastFurnacePartBlock.DZ, pos.getZ() - controller.getZ() + 1)
			.setValue(BlastFurnacePartBlock.FACING, facing);
	}

	/** The controller a furnace block belongs to (itself for the controller), or null for any other block. */
	public static @Nullable BlockPos controllerOf(BlockGetter level, BlockPos pos) {
		BlockState state = level.getBlockState(pos);
		if (state.is(MetallurgyContent.BLAST_FURNACE)) {
			return pos;
		}
		if (state.is(MetallurgyContent.BLAST_FURNACE_PART)) {
			return pos.offset(1 - state.getValue(BlastFurnacePartBlock.DX), CONTROLLER_LAYER - state.getValue(BlastFurnacePartBlock.LAYER),
				1 - state.getValue(BlastFurnacePartBlock.DZ));
		}
		if (state.getBlock() instanceof BlastFurnaceAttachmentBlock attachment) {
			return attachment.controller(state, pos);
		}
		return null;
	}

	/** The furnace whose receiving hopper is the block at {@code pos}, if it is one. */
	public static @Nullable BlastFurnaceBlockEntity hopperAt(BlockGetter level, BlockPos pos) {
		BlockState state = level.getBlockState(pos);
		if (!state.is(MetallurgyContent.BLAST_FURNACE_PART)) {
			return null;
		}
		BlockPos controller = controllerOf(level, pos);
		if (controller == null || !pos.equals(hopper(controller, state.getValue(BlastFurnacePartBlock.FACING)))) {
			return null;
		}
		return level.getBlockEntity(controller) instanceof BlastFurnaceBlockEntity furnace ? furnace : null;
	}
}
