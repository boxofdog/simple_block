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

import org.jspecify.annotations.Nullable;

/**
 * The cable spans laid out in each loaded level, kept by their drums (updated when a route changes,
 * dropped when the drum unloads or goes). Connecting rods look up the cable above them here
 * ({@link #grip}). Only the working strand is indexed: a loop's return strand (M05) is never gripped.
 * Server thread only; emptied when the server stops.
 */
public final class CableIndex {
	/** A straight run of cable between two supports. */
	public record Span(Vec3 from, Vec3 to) {
	}

	/** A rod's hold on the working strand of the drum at {@code drum}. */
	public record Grip(BlockPos drum, int span, double reach, double along, double length, Vec3 direction) {
	}

	/** One drum's working strand: its spans, and how far along the route (horizontally) each starts. */
	private record Route(List<Span> spans, double[] starts, double length) {
	}

	private static final Map<ResourceKey<Level>, Map<BlockPos, Route>> ROUTES = new HashMap<>();

	private CableIndex() {
	}

	static void register() {
		ServerLifecycleEvents.SERVER_STOPPED.register(server -> ROUTES.clear());
	}

	static void update(ServerLevel level, CableDrumBlockEntity drum) {
		List<Vec3> points = drum.points();
		Map<BlockPos, Route> drums = ROUTES.computeIfAbsent(level.dimension(), key -> new HashMap<>());
		if (points.size() < 2) {
			drums.remove(drum.getBlockPos());
			return;
		}
		List<Span> spans = new ArrayList<>();
		double[] starts = new double[points.size() - 1];
		double along = 0.0;
		for (int i = 1; i < points.size(); i++) {
			Vec3 from = points.get(i - 1);
			Vec3 to = points.get(i);
			spans.add(new Span(from, to));
			starts[i - 1] = along;
			along += Math.sqrt((to.x - from.x) * (to.x - from.x) + (to.z - from.z) * (to.z - from.z));
		}
		drums.put(drum.getBlockPos().immutable(), new Route(List.copyOf(spans), starts, along));
	}

	static void remove(ServerLevel level, BlockPos drum) {
		Map<BlockPos, Route> drums = ROUTES.get(level.dimension());
		if (drums != null) {
			drums.remove(drum);
		}
	}

	/** Every span laid out in the level (for tests and debugging). */
	public static List<Span> spans(ServerLevel level) {
		List<Span> all = new ArrayList<>();
		ROUTES.getOrDefault(level.dimension(), Map.of()).values().forEach(route -> all.addAll(route.spans()));
		return all;
	}

	/**
	 * How high above {@code base} the nearest cable runs, among spans that pass within
	 * {@code tolerance} blocks horizontally of it at a height between {@code min} and {@code max}; NaN
	 * if none does. The point used on each span is the one horizontally closest to {@code base}.
	 */
	public static double reachAbove(ServerLevel level, Vec3 base, double tolerance, double min, double max) {
		Grip grip = grip(level, base, tolerance, min, max);
		return grip == null ? Double.NaN : grip.reach();
	}

	/** The working strand a rod at {@code base} would grip (see {@link #reachAbove}), or null. */
	public static @Nullable Grip grip(ServerLevel level, Vec3 base, double tolerance, double min, double max) {
		Map<BlockPos, Route> drums = ROUTES.get(level.dimension());
		if (drums == null) {
			return null;
		}
		Grip best = null;
		double bestDistance = tolerance * tolerance;
		for (Map.Entry<BlockPos, Route> entry : drums.entrySet()) {
			Route route = entry.getValue();
			for (int i = 0; i < route.spans().size(); i++) {
				Span span = route.spans().get(i);
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
					double horizontal = Math.sqrt(lengthSq);
					Vec3 direction = horizontal < 1.0E-6 ? Vec3.ZERO : new Vec3(dx / horizontal, 0.0, dz / horizontal);
					best = new Grip(entry.getKey(), i, reach, route.starts()[i] + t * horizontal, route.length(), direction);
				}
			}
		}
		return best;
	}
}
