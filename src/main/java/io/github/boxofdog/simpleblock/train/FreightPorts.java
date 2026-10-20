package io.github.boxofdog.simpleblock.train;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.BaseRailBlock;
import net.minecraft.world.level.block.ButtonBlock;
import net.minecraft.world.level.block.HopperBlock;
import net.minecraft.world.level.block.LeverBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * M15 the hoppers of a Loading Rail and the lids over their freight openings.
 *
 * <ul>
 *   <li><b>Freight hoppers.</b> A supplying hopper points down over the rail with up to {@value #MAX_CLEARANCE}
 *       clear blocks between its spout and the rail (M11's hopper straight over the rail is the case of
 *       none); a receiving hopper sits beside the rail, one block down ({@link FreightHoppers#receiver}).</li>
 *   <li><b>Controls.</b> Ordinary levers and buttons standing in a block face to face with a freight
 *       hopper govern its freight opening: none, open; a lever on, open; a lever off, capped; a button,
 *       capped until pressed, then open for one whole transfer of the wagon it serves. A control that
 *       stands beside two freight hoppers is ambiguous and caps both (fail closed); a lever off caps a
 *       hopper whatever else is there.</li>
 *   <li><b>The lid</b> covers only the freight opening (the spout of a supplying hopper, the top of a
 *       receiving one): the hopper still fills from and empties to its other neighbours, and on a
 *       freight hopper redstone does not lock it ({@code mixin/train/HopperBlockEntityMixin}).</li>
 * </ul>
 * Everything here reads blocks only, so the client draws the same lids the server enforces.
 */
public final class FreightPorts {
	/** Clear blocks allowed between a supplying hopper's spout and the rail. */
	public static final int MAX_CLEARANCE = 3;

	public enum Control {
		/** No control, or levers on: the opening is open. */
		OPEN,
		/** A lever off, or an ambiguous control: the opening is capped. */
		CAPPED,
		/** A button: capped until a press opens it for one transfer. */
		BUTTON
	}

	private FreightPorts() {
	}

	private static boolean isLoadingRail(BlockState state) {
		return state.getBlock() instanceof LoadingTrackBlock;
	}

	/** Whether nothing between a spout and a cart stops items falling through: no collision, not a rail. */
	private static boolean clear(BlockGetter level, BlockPos pos) {
		BlockState state = level.getBlockState(pos);
		return state.getCollisionShape(level, pos).isEmpty() && !(state.getBlock() instanceof BaseRailBlock) && state.getFluidState().isEmpty();
	}

	/** The Loading Rail a down-pointing hopper at {@code hopper} feeds (straight below, through clear blocks), or null. */
	public static @Nullable BlockPos railBelow(BlockGetter level, BlockPos hopper, BlockState state) {
		if (!(state.getBlock() instanceof HopperBlock) || state.getValue(HopperBlock.FACING) != Direction.DOWN) {
			return null;
		}
		for (int depth = 1; depth <= MAX_CLEARANCE + 1; depth++) {
			BlockPos below = hopper.below(depth);
			if (isLoadingRail(level.getBlockState(below))) {
				return below;
			}
			if (!clear(level, below)) {
				return null;
			}
		}
		return null;
	}

	/** The supplying hopper over the Loading Rail at {@code rail} (pointing down at it through clear blocks), or null. */
	public static @Nullable BlockPos hopperAbove(BlockGetter level, BlockPos rail) {
		for (int height = 1; height <= MAX_CLEARANCE + 1; height++) {
			BlockPos above = rail.above(height);
			BlockState state = level.getBlockState(above);
			if (state.getBlock() instanceof HopperBlock) {
				return state.getValue(HopperBlock.FACING) == Direction.DOWN ? above : null;
			}
			if (!clear(level, above)) {
				return null;
			}
		}
		return null;
	}

	/** Whether the hopper at {@code pos} receives from a Loading Rail beside it (one block up and over). */
	public static boolean receives(BlockGetter level, BlockPos pos) {
		for (Direction side : Direction.Plane.HORIZONTAL) {
			if (isLoadingRail(level.getBlockState(pos.above().relative(side)))) {
				return true;
			}
		}
		return false;
	}

	/** Whether the hopper at {@code pos} (state {@code state}) is a freight hopper: supplying or receiving at a Loading Rail. */
	public static boolean isFreightHopper(BlockGetter level, BlockPos pos, BlockState state) {
		return state.getBlock() instanceof HopperBlock && (railBelow(level, pos, state) != null || receives(level, pos));
	}

	/** The control over the freight opening of the hopper at {@code hopper}. */
	public static Control control(BlockGetter level, BlockPos hopper) {
		boolean button = false;
		for (Direction side : Direction.values()) {
			BlockPos at = hopper.relative(side);
			BlockState state = level.getBlockState(at);
			boolean lever = state.getBlock() instanceof LeverBlock;
			if (!lever && !(state.getBlock() instanceof ButtonBlock)) {
				continue;
			}
			if (besideOther(level, at, hopper) || lever && !state.getValue(LeverBlock.POWERED)) {
				return Control.CAPPED;
			}
			button |= !lever;
		}
		return button ? Control.BUTTON : Control.OPEN;
	}

	/** Whether the control at {@code at} also stands face to face with a freight hopper other than {@code hopper}. */
	private static boolean besideOther(BlockGetter level, BlockPos at, BlockPos hopper) {
		for (Direction side : Direction.values()) {
			BlockPos other = at.relative(side);
			if (!other.equals(hopper) && isFreightHopper(level, other, level.getBlockState(other))) {
				return true;
			}
		}
		return false;
	}

	/** Whether the freight opening of the hopper at {@code hopper} is open for a wagon whose button-opened port is {@code port}. */
	public static boolean open(BlockGetter level, BlockPos hopper, @Nullable BlockPos port) {
		return switch (control(level, hopper)) {
			case OPEN -> true;
			case CAPPED -> false;
			case BUTTON -> hopper.equals(port);
		};
	}

	/** Whether a button face to face with the hopper at {@code hopper} is pressed right now (and the hopper is button controlled). */
	public static boolean buttonPressed(BlockGetter level, BlockPos hopper) {
		if (control(level, hopper) != Control.BUTTON) {
			return false;
		}
		for (Direction side : Direction.values()) {
			BlockState state = level.getBlockState(hopper.relative(side));
			if (state.getBlock() instanceof ButtonBlock && state.getValue(ButtonBlock.POWERED)) {
				return true;
			}
		}
		return false;
	}
}
