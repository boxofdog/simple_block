package io.github.boxofdog.simpleblock.cable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * The cable spans laid out in each loaded level, kept by their drums (updated when a route changes,
 * dropped when the drum unloads or goes). Connecting rods look up the cable above them here
 * ({@link #reachAbove}). Server thread only; emptied when the server stops.
 */
public final class CableIndex {
	/** A straight run of cable between two supports. */
	public record Span(Vec3 from, Vec3 to) {
	}

	private static final Map<ResourceKey<Level>, Map<BlockPos, List<Span>>> SPANS = new HashMap<>();

	private CableIndex() {
	}

	static void register() {
		ServerLifecycleEvents.SERVER_STOPPED.register(server -> SPANS.clear());
	}

	static void update(ServerLevel level, CableDrumBlockEntity drum) {
		List<Span> spans = new ArrayList<>();
		Vec3 previous = CableRoutes.point(drum.getBlockPos(), true);
		for (BlockPos anchor : drum.route()) {
			Vec3 next = CableRoutes.point(anchor, false);
			spans.add(new Span(previous, next));
			previous = next;
		}
		Map<BlockPos, List<Span>> drums = SPANS.computeIfAbsent(level.dimension(), key -> new HashMap<>());
		if (spans.isEmpty()) {
			drums.remove(drum.getBlockPos());
		} else {
			drums.put(drum.getBlockPos().immutable(), List.copyOf(spans));
		}
	}

	static void remove(ServerLevel level, BlockPos drum) {
		Map<BlockPos, List<Span>> drums = SPANS.get(level.dimension());
		if (drums != null) {
			drums.remove(drum);
		}
	}

	/** Every span laid out in the level (for tests and debugging). */
	public static List<Span> spans(ServerLevel level) {
		List<Span> all = new ArrayList<>();
		SPANS.getOrDefault(level.dimension(), Map.of()).values().forEach(all::addAll);
		return all;
	}

	/**
	 * How high above {@code base} the nearest cable runs, among spans that pass within
	 * {@code tolerance} blocks horizontally of it at a height between {@code min} and {@code max}; NaN
	 * if none does. The point used on each span is the one horizontally closest to {@code base}.
	 */
	public static double reachAbove(ServerLevel level, Vec3 base, double tolerance, double min, double max) {
		Map<BlockPos, List<Span>> drums = SPANS.get(level.dimension());
		if (drums == null) {
			return Double.NaN;
		}
		double best = Double.NaN;
		double bestDistance = tolerance * tolerance;
		for (List<Span> spans : drums.values()) {
			for (Span span : spans) {
				Vec3 a = span.from();
				Vec3 b = span.to();
				if (Math.min(a.x, b.x) > base.x + tolerance || Math.max(a.x, b.x) < base.x - tolerance
					|| Math.min(a.z, b.z) > base.z + tolerance || Math.max(a.z, b.z) < base.z - tolerance) {
					continue;
				}
				double dx = b.x - a.x;
				double dz = b.z - a.z;
				double lengthSq = dx * dx + dz * dz;
				double t = lengthSq < 1.0E-6 ? 0.0 : Math.clamp(((base.x - a.x) * dx + (base.z - a.z) * dz) / lengthSq, 0.0, 1.0);
				double px = a.x + t * dx - base.x;
				double pz = a.z + t * dz - base.z;
				double distance = px * px + pz * pz;
				double reach = a.y + t * (b.y - a.y) - base.y;
				if (distance <= bestDistance && reach >= min && reach <= max) {
					bestDistance = distance;
					best = reach;
				}
			}
		}
		return best;
	}
}
