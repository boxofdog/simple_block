package io.github.boxofdog.simpleblock.cable;

import java.util.Set;
import java.util.UUID;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;

import io.github.boxofdog.simpleblock.tuning.Tuning;

/**
 * M07: a skip hung on a drum's cable end ({@code skip/OreSkip}). The skip hangs straight under the
 * route's last anchor (the head of the shaft), its bridle's apex made fast to the cable's loose end:
 * one main cable from the apex up over the anchor's roller and along the route to the drum. The same
 * drive that hauls loops ({@link CableDrive}) winds it: PULL (the crank's right-click, or the coal
 * winch's lever back) lifts the skip, FEED lowers it. The hanging length is free, like the rest of the
 * route (one coil, any length).
 *
 * <p>The link is kept in the drum's line ({@link CableLines.Line#hoist()}), so a skip far from its drum
 * still moves while it is loaded. The skip moves itself in its own tick and reports here, as a gripping
 * cart does, so the coal winch's stops and coal rules apply unchanged. Any change to the route (an
 * anchor disconnected or broken, the drum dismantled or broken) lets the skip go.
 */
public final class CableHoists {
	/** The skip's apex stops this far under the anchor's roller. */
	public static final double APEX_GAP = 0.55;
	/** Where the cable leaves the pulley for the skip: this far from the anchor's middle towards its facing, at the axle's height. */
	public static final double PULLEY_DROP = 4.0 / 16.0;
	public static final double PULLEY_AXLE_Y = 8.0 / 16.0;

	/** What a hung skip needs from its line each tick. */
	public record Hoist(BlockPos drum, Vec3 roller, float speed, boolean powered) {
	}

	/** The outcome of trying to hang a skip, and what to tell the player. */
	public record Attach(boolean hung, Component message) {
	}

	private CableHoists() {
	}

	/** Whether a skip hangs on this drum's cable. */
	public static boolean hoisting(ServerLevel level, BlockPos drum) {
		CableLines.Line line = CableLines.line(level, drum);
		return line != null && line.hoist != null;
	}

	/**
	 * A player holding {@code hook} uses it on the skip {@code skip}, standing in the column
	 * {@code column} with its apex at height {@code apexY}: hangs it on the cable if it is straight
	 * under the route's last anchor with nothing in between.
	 */
	public static Attach attach(ServerLevel level, CableHook hook, UUID skip, BlockPos column, double apexY) {
		CableDrumBlockEntity drum = hook.drum(level);
		if (drum == null) {
			return new Attach(false, CableRoutes.message("hoist_no_drum", "This cable end has lost its drum"));
		}
		if (drum.route().isEmpty() || drum.looped()) {
			return new Attach(false, CableRoutes.message("hoist_no_anchor", "Run the cable through an anchor over the shaft first: the skip hangs under the last one"));
		}
		BlockPos anchor = drum.routeEnd();
		Vec3 roller = CableRoutes.rollerPoint(level, anchor);
		if (anchor.getX() != column.getX() || anchor.getZ() != column.getZ()) {
			return new Attach(false, CableRoutes.message("hoist_not_under", "The skip must stand straight under the cable's last anchor, at %s",
				anchor.toShortString()));
		}
		if (apexY > roller.y - APEX_GAP) {
			return new Attach(false, CableRoutes.message("hoist_too_high", "The skip must hang below the anchor"));
		}
		Vec3 apex = new Vec3(roller.x, apexY, roller.z);
		BlockPos obstacle = CableRoutes.obstacle(level, roller, apex, Set.of(anchor, anchor.above()));
		if (obstacle != null) {
			return new Attach(false, CableRoutes.message("blocked", "The cable would run through %s at %s",
				level.getBlockState(obstacle).getBlock().getName(), obstacle.toShortString()));
		}
		CableLines lines = CableLines.get(level);
		CableLines.Line line = lines.getOrCreate(drum.getBlockPos());
		if (line.hoist != null) {
			return new Attach(false, CableRoutes.message("hoist_taken", "This cable already carries a skip"));
		}
		line.hoist = skip;
		line.speed = 0.0F;
		setPulley(level, drum, anchor, true);
		line.lever = CableLines.Lever.NEUTRAL;
		lines.changed();
		drum.copyDrive(line);
		hook.release();
		hook.discard();
		drum.hookId = -1;
		level.playSound(null, column, SoundEvents.CHAIN_PLACE, SoundSource.BLOCKS, 1.0F, 0.9F);
		return new Attach(true, CableRoutes.message("hoist_hung", "Skip hung on the cable: pull to lift it, feed to lower it"));
	}

	/**
	 * A skip taken off (or broken) with its bridle's apex at {@code apexY}: its cable end stays hanging
	 * there, under the head anchor, as the free hook (M07 playtest patch §4/§5A).
	 */
	public static void detach(ServerLevel level, BlockPos drum, double apexY) {
		CableLines.Line line = CableLines.get(level).lineAt(drum);
		boolean hung = line != null && line.hoist != null;
		detach(level, drum);
		if (hung && line.points.size() >= 2) {
			line.drop = Math.max(CableHook.HANG, line.points.getLast().y - apexY);
			CableLines.get(level).changed();
		}
	}

	/** Lets go of the skip hung on this drum's cable, if any; the cable end hangs at the anchor again. */
	public static void detach(ServerLevel level, BlockPos drum) {
		CableLines lines = CableLines.get(level);
		CableLines.Line line = lines.lineAt(drum);
		if (line != null && line.hoist != null) {
			line.hoist = null;
			line.speed = 0.0F;
			if (!line.anchors.isEmpty()) {
				setPulley(level, null, line.anchors.getLast(), false);
			}
			if (line.powered && line.lever != CableLines.Lever.NEUTRAL) {
				CoalWinch.toNeutral(level, drum, line, "open");
			}
			lines.changed();
		}
	}

	/**
	 * Shows the head anchor as a pulley while a skip hangs from it, or as the ordinary anchor again. The
	 * wheel faces the way the cable comes in (from the support before it) and drops it on that side.
	 */
	static void setPulley(ServerLevel level, @Nullable CableDrumBlockEntity drum, BlockPos anchor, boolean pulley) {
		if (!level.isLoaded(anchor)) {
			return;
		}
		net.minecraft.world.level.block.state.BlockState state = level.getBlockState(anchor);
		if (!state.is(CableContent.CABLE_ANCHOR)) {
			return;
		}
		CableAnchorBlock.Return value = pulley ? CableAnchorBlock.Return.PULLEY : CableAnchorBlock.Return.NONE;
		if (!pulley && state.getValue(CableAnchorBlock.RETURN) != CableAnchorBlock.Return.PULLEY) {
			return;
		}
		net.minecraft.core.Direction facing = state.getValue(CableAnchorBlock.FACING);
		if (pulley && drum != null) {
			java.util.List<BlockPos> route = drum.route();
			BlockPos before = route.size() >= 2 ? route.get(route.size() - 2) : drum.getBlockPos();
			int dx = anchor.getX() - before.getX();
			int dz = anchor.getZ() - before.getZ();
			if (dx != 0 || dz != 0) {
				facing = Math.abs(dx) >= Math.abs(dz) ? (dx > 0 ? net.minecraft.core.Direction.EAST : net.minecraft.core.Direction.WEST)
					: (dz > 0 ? net.minecraft.core.Direction.SOUTH : net.minecraft.core.Direction.NORTH);
			}
		}
		level.setBlock(anchor, state.setValue(CableAnchorBlock.RETURN, value).setValue(CableAnchorBlock.FACING, facing),
			net.minecraft.world.level.block.Block.UPDATE_ALL);
	}

	/** The line of the skip {@code skip} hung from {@code drum}, or null if it is not (any more). */
	public static @Nullable Hoist hoist(ServerLevel level, BlockPos drum, UUID skip) {
		CableLines.Line line = CableLines.line(level, drum);
		if (line == null || !skip.equals(line.hoist) || line.points.size() < 2) {
			return null;
		}
		return new Hoist(drum, line.points.getLast(), line.speed, line.powered);
	}

	/**
	 * The skip's report for this tick (from its own tick, before the line's): where it is (its height),
	 * how heavy it is to lift, and why it could not move as driven, if it could not. The coal winch then
	 * burns coal only if it moved, and goes to NEUTRAL on a stop, as for a hauled cart.
	 */
	public static void report(ServerLevel level, BlockPos drum, int skipId, double height, double mass, @Nullable String halt) {
		CableLines.Line line = CableLines.line(level, drum);
		if (line == null) {
			return;
		}
		line.gripped = true;
		// Lifting is the PULL direction: the load slows it as a climb slows a hauled train.
		line.loadBackward += Tuning.getDouble("skip.lift_load") * mass;
		Double before = line.lastAlongs.get(skipId);
		line.alongs.put(skipId, height);
		if (before != null && Math.abs(height - before) > Math.max(0.003, 0.25 * Math.abs(line.speed))) {
			line.moved = true;
		}
		if (halt != null) {
			line.halt = halt;
		}
	}
}
