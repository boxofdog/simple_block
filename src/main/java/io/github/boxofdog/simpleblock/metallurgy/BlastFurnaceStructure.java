package io.github.boxofdog.simpleblock.metallurgy;

import java.util.ArrayList;
import java.util.List;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;

/**
 * M08: the furnace's shape, one structure 3x3 blocks across and four high. Its controller
 * ({@link BlastFurnaceBlock}, which holds the inventories) is the middle block of the second layer;
 * every other block is a {@link BlastFurnacePartBlock} that knows its offset from it and the furnace's
 * facing. Layer 0 is the heat zone (footing, firebrick hearth, tuyeres, the tap arch) with the spout,
 * the runner and the open casting mold along the front, the mold in the front left corner; layers 1-2
 * the tapering brick shaft (the controller draws layers 0-2); layer 3 the receiving hopper, a flared
 * charging mouth (drawn by its middle block).
 */
public final class BlastFurnaceStructure {
	public static final int LAYERS = 4;
	/** The controller's layer. */
	public static final int CONTROLLER_LAYER = 1;
	/** The hopper's layer: cargo goes in over its rim, from above layer 3. */
	public static final int HOPPER_LAYER = 3;

	private BlastFurnaceStructure() {
	}

	/** The controller of a furnace whose front middle block of the hearth is {@code front}, facing {@code facing}. */
	public static BlockPos controllerFor(BlockPos front, Direction facing) {
		return front.relative(facing.getOpposite()).above(CONTROLLER_LAYER);
	}

	/** Every block of the structure around {@code controller}, the controller first. */
	public static List<BlockPos> blocks(BlockPos controller) {
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
		return blocks;
	}

	/** The structure block at {@code pos}, which belongs to the furnace at {@code controller}. */
	public static BlockState stateAt(BlockPos controller, BlockPos pos, Direction facing) {
		if (pos.equals(controller)) {
			return MetallurgyContent.BLAST_FURNACE.defaultBlockState().setValue(BlastFurnaceBlock.FACING, facing);
		}
		return MetallurgyContent.BLAST_FURNACE_PART.defaultBlockState()
			.setValue(BlastFurnacePartBlock.DX, pos.getX() - controller.getX() + 1)
			.setValue(BlastFurnacePartBlock.LAYER, pos.getY() - controller.getY() + CONTROLLER_LAYER)
			.setValue(BlastFurnacePartBlock.DZ, pos.getZ() - controller.getZ() + 1)
			.setValue(BlastFurnacePartBlock.FACING, facing);
	}

	/** The controller a structure block belongs to (itself for the controller), or null for any other block. */
	public static @Nullable BlockPos controllerOf(BlockGetter level, BlockPos pos) {
		BlockState state = level.getBlockState(pos);
		if (state.is(MetallurgyContent.BLAST_FURNACE)) {
			return pos;
		}
		if (state.is(MetallurgyContent.BLAST_FURNACE_PART)) {
			return pos.offset(1 - state.getValue(BlastFurnacePartBlock.DX), CONTROLLER_LAYER - state.getValue(BlastFurnacePartBlock.LAYER),
				1 - state.getValue(BlastFurnacePartBlock.DZ));
		}
		return null;
	}

	/** The furnace whose hopper layer includes {@code pos}, if {@code pos} is one of its top blocks. */
	public static @Nullable BlastFurnaceBlockEntity hopperAt(BlockGetter level, BlockPos pos) {
		BlockState state = level.getBlockState(pos);
		if (!state.is(MetallurgyContent.BLAST_FURNACE_PART) || state.getValue(BlastFurnacePartBlock.LAYER) != HOPPER_LAYER) {
			return null;
		}
		BlockPos controller = controllerOf(level, pos);
		return controller != null && level.getBlockEntity(controller) instanceof BlastFurnaceBlockEntity furnace ? furnace : null;
	}
}
