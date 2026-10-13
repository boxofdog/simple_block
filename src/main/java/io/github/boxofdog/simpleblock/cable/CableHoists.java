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
	public static final double APEX_GAP = 0.3;

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
		Vec3 roller = CableRoutes.point(anchor, false);
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
		line.lever = CableLines.Lever.NEUTRAL;
		lines.changed();
		drum.copyDrive(line);
		hook.release();
		hook.discard();
		drum.hookId = -1;
		level.playSound(null, column, SoundEvents.CHAIN_PLACE, SoundSource.BLOCKS, 1.0F, 0.9F);
		return new Attach(true, CableRoutes.message("hoist_hung", "Skip hung on the cable: pull to lift it, feed to lower it"));
	}

	/** Lets go of the skip hung on this drum's cable, if any; the cable end hangs at the anchor again. */
	public static void detach(ServerLevel level, BlockPos drum) {
		CableLines lines = CableLines.get(level);
		CableLines.Line line = lines.lineAt(drum);
		if (line != null && line.hoist != null) {
			line.hoist = null;
			line.speed = 0.0F;
			if (line.powered && line.lever != CableLines.Lever.NEUTRAL) {
				CoalWinch.toNeutral(level, drum, line, "open");
			}
			lines.changed();
		}
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
