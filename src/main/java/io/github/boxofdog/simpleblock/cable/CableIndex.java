package io.github.boxofdog.simpleblock.cable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import org.jspecify.annotations.Nullable;

/**
 * The working-strand spans of every cable line in a level ({@link CableLines}), for connecting rods to
 * look up the cable above them ({@link #grip}). Built from the level's saved lines, so a span is
 * gripped whether or not its drum is loaded (M06). Spans are filed by the chunks they cross, so a
 * lookup only looks at spans near the rod however long the lines are. Only the working strand is
 * indexed: a loop's return strand is never gripped. Server thread only; emptied when the server stops.
 */
public final class CableIndex {
	/** A straight run of cable between two supports. */
	public record Span(Vec3 from, Vec3 to) {
	}

	/** A rod's hold on the working strand of the drum at {@code drum}. */
	public record Grip(BlockPos drum, int span, double reach, double along, double length, Vec3 direction) {
	}

	/** One line's working strand: its spans, and how far along the route (horizontally) each starts. */
	private record Route(List<Span> spans, double[] starts, double length) {
	}

	private record SpanRef(BlockPos drum, int span) {
	}

	private static final class LevelIndex {
		final Map<BlockPos, Route> routes = new HashMap<>();
		final Map<Long, List<SpanRef>> byChunk = new HashMap<>();
	}

	/** How far round a span its chunks are filed (a rod's sideways tolerance, with room to spare). */
	private static final double MARGIN = 1.0;

	private static final Map<ResourceKey<Level>, LevelIndex> LEVELS = new HashMap<>();

	private CableIndex() {
	}

	static void register() {
		ServerLifecycleEvents.SERVER_STOPPED.register(server -> LEVELS.clear());
	}

	/** The level's index, built from its saved lines the first time it is needed. */
	private static LevelIndex index(ServerLevel level) {
		LevelIndex index = LEVELS.get(level.dimension());
		if (index == null) {
			index = new LevelIndex();
			LEVELS.put(level.dimension(), index);
			for (Map.Entry<BlockPos, CableLines.Line> entry : CableLines.get(level).all().entrySet()) {
				put(index, entry.getKey(), entry.getValue().points());
			}
		}
		return index;
	}

	static void update(ServerLevel level, BlockPos drum, CableLines.Line line) {
		LevelIndex index = index(level);
		unfile(index, drum);
		put(index, drum.immutable(), line.points());
	}

	static void remove(ServerLevel level, BlockPos drum) {
		unfile(index(level), drum);
	}

	private static void unfile(LevelIndex index, BlockPos drum) {
		Route old = index.routes.remove(drum);
		if (old == null) {
			return;
		}
		for (int i = 0; i < old.spans().size(); i++) {
			SpanRef ref = new SpanRef(drum, i);
			for (long chunk : chunks(old.spans().get(i))) {
				List<SpanRef> refs = index.byChunk.get(chunk);
				if (refs != null) {
					refs.remove(ref);
					if (refs.isEmpty()) {
						index.byChunk.remove(chunk);
					}
				}
			}
		}
	}

	private static void put(LevelIndex index, BlockPos drum, List<Vec3> points) {
		if (points.size() < 2) {
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
		Route route = new Route(List.copyOf(spans), starts, along);
		index.routes.put(drum, route);
		for (int i = 0; i < spans.size(); i++) {
			for (long chunk : chunks(spans.get(i))) {
				index.byChunk.computeIfAbsent(chunk, key -> new ArrayList<>()).add(new SpanRef(drum, i));
			}
		}
	}

	/** The chunks a span's horizontal bounds (plus a margin) touch. */
	private static Set<Long> chunks(Span span) {
		Set<Long> chunks = new HashSet<>();
		int minX = Mth.floor(Math.min(span.from().x, span.to().x) - MARGIN) >> 4;
		int maxX = Mth.floor(Math.max(span.from().x, span.to().x) + MARGIN) >> 4;
		int minZ = Mth.floor(Math.min(span.from().z, span.to().z) - MARGIN) >> 4;
		int maxZ = Mth.floor(Math.max(span.from().z, span.to().z) + MARGIN) >> 4;
		for (int x = minX; x <= maxX; x++) {
			for (int z = minZ; z <= maxZ; z++) {
				chunks.add(ChunkPos.pack(x, z));
			}
		}
		return chunks;
	}

	/** Every span laid out in the level (for tests and debugging). */
	public static List<Span> spans(ServerLevel level) {
		List<Span> all = new ArrayList<>();
		index(level).routes.values().forEach(route -> all.addAll(route.spans()));
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
		LevelIndex index = index(level);
		List<SpanRef> near = index.byChunk.get(ChunkPos.pack(Mth.floor(base.x) >> 4, Mth.floor(base.z) >> 4));
		if (near == null) {
			return null;
		}
		Grip best = null;
		double bestDistance = tolerance * tolerance;
		for (SpanRef ref : near) {
			Route route = index.routes.get(ref.drum());
			if (route == null || ref.span() >= route.spans().size()) {
				continue;
			}
			Span span = route.spans().get(ref.span());
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
				best = new Grip(ref.drum(), ref.span(), reach, route.starts()[ref.span()] + t * horizontal, route.length(), direction);
			}
		}
		return best;
	}
}
