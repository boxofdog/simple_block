package io.github.boxofdog.simpleblock.cable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.StringRepresentable;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import net.minecraft.world.phys.Vec3;

import io.github.boxofdog.simpleblock.SimpleBlock;

/**
 * M06: every installed cable line in a level, kept with the level's saved data rather than in its drum,
 * so that a long line keeps working where its drum is far away and unloaded (owner's brief, section 7).
 *
 * <p>A {@link Line} holds what the moving parts need: the working strand's points, whether it is a
 * closed loop, and the drive: the manual crank's input, the coal winch's lever, coal and the exact burn
 * time left in the coal alight, and the strand's current speed. The drum's block entity owns the route
 * and copies its geometry here whenever it changes ({@link CableDrumBlockEntity}); the drive state is
 * owned here (it changes while the drum is unloaded) and copied to the drum for drawing. The grip
 * index ({@link CableIndex}) is built from these lines, so a rod grips a strand whose drum is unloaded.
 * {@link CableDrive} drives each line once a level tick, and carts only ever move while they are
 * themselves in entity-ticking chunks: nothing here loads chunks or moves an unloaded entity.
 *
 * <p>An anchor broken while its drum is unloaded cuts the line here at once ({@link #cut}), so nothing
 * runs past a missing support; the drum applies the cut to its route when it next loads.
 */
public final class CableLines extends SavedData {
	/** The coal winch's lever (M06). NEUTRAL holds; PULL hauls towards the drum; FEED moves the cable away. */
	public enum Lever implements StringRepresentable {
		NEUTRAL("neutral", 0), PULL("pull", -1), FEED("feed", 1);

		private final String name;
		/** Direction of the working strand it drives: +1 away from the drum, -1 towards it. */
		public final int sign;

		Lever(String name, int sign) {
			this.name = name;
			this.sign = sign;
		}

		/** The owner's cycle: NEUTRAL, PULL, FEED, NEUTRAL. */
		public Lever next() {
			return switch (this) {
				case NEUTRAL -> PULL;
				case PULL -> FEED;
				case FEED -> NEUTRAL;
			};
		}

		@Override
		public String getSerializedName() {
			return this.name;
		}

		public static final Codec<Lever> CODEC = StringRepresentable.fromEnum(Lever::values);
	}

	/** One drum's line. */
	public static final class Line {
		/** The route's anchors, in order (so an anchor broken while the drum is unloaded can cut it). */
		List<BlockPos> anchors;
		List<Vec3> points;
		boolean looped;
		@Nullable Direction outward;
		boolean powered;
		/** Not saved: a line always loads with its lever at NEUTRAL, so a saved haul never resumes by itself. */
		Lever lever;
		int coal;
		int burn;
		/** Index into the route where an anchor went while the drum was unloaded, or -1. */
		int cut;

		// Not saved: a line loads standing still.
		float speed;
		int crankTicks;
		int crankDirection;
		double loadForward;
		double loadBackward;
		double lastLoadForward;
		double lastLoadBackward;
		/** Where along the strand each gripping cart (by entity id) was last tick, and this tick so far. */
		Map<Integer, Double> lastAlongs = new HashMap<>();
		Map<Integer, Double> alongs = new HashMap<>();
		/** This tick, reported by gripping carts: one moved, one stopped against something (a physical stop). */
		boolean moved;
		boolean gripped;
		@Nullable String halt;
		int idleTicks;
		int stallTicks;
		/** Why the coal winch's lever last went back to NEUTRAL by itself (for tests and debugging). */
		@Nullable String lastStop;

		Line(List<BlockPos> anchors, List<Vec3> points, boolean looped, Optional<Direction> outward, boolean powered, int coal, int burn, int cut) {
			this.anchors = List.copyOf(anchors);
			this.points = List.copyOf(points);
			this.looped = looped;
			this.outward = outward.orElse(null);
			this.powered = powered;
			this.lever = Lever.NEUTRAL;
			this.coal = coal;
			this.burn = burn;
			this.cut = cut;
		}

		static Line empty() {
			return new Line(List.of(), List.of(), false, Optional.empty(), false, 0, 0, -1);
		}

		public List<Vec3> points() {
			return this.points;
		}

		public boolean looped() {
			return this.looped;
		}

		public @Nullable Direction outward() {
			return this.outward;
		}

		public boolean powered() {
			return this.powered;
		}

		public Lever lever() {
			return this.lever;
		}

		public int coal() {
			return this.coal;
		}

		/** Ticks of driving left in the coal alight (exactly, through stops and saves). */
		public int burn() {
			return this.burn;
		}

		public float speed() {
			return this.speed;
		}

		public @Nullable String lastStop() {
			return this.lastStop;
		}

		static final Codec<Line> CODEC = RecordCodecBuilder.create(i -> i.group(
			BlockPos.CODEC.listOf().optionalFieldOf("anchors", List.of()).forGetter(line -> line.anchors),
			Vec3.CODEC.listOf().fieldOf("points").forGetter(line -> line.points),
			Codec.BOOL.optionalFieldOf("looped", false).forGetter(line -> line.looped),
			Direction.CODEC.optionalFieldOf("outward").forGetter(line -> Optional.ofNullable(line.outward)),
			Codec.BOOL.optionalFieldOf("powered", false).forGetter(line -> line.powered),
			Codec.INT.optionalFieldOf("coal", 0).forGetter(line -> line.coal),
			Codec.INT.optionalFieldOf("burn", 0).forGetter(line -> line.burn),
			Codec.INT.optionalFieldOf("cut", -1).forGetter(line -> line.cut)
		).apply(i, Line::new));
	}

	private record Entry(BlockPos drum, Line line) {
		static final Codec<Entry> CODEC = RecordCodecBuilder.create(i -> i.group(
			BlockPos.CODEC.fieldOf("drum").forGetter(Entry::drum),
			Line.CODEC.fieldOf("line").forGetter(Entry::line)
		).apply(i, Entry::new));
	}

	private static final Codec<CableLines> CODEC = Entry.CODEC.listOf().xmap(CableLines::new,
		lines -> lines.lines.entrySet().stream().map(entry -> new Entry(entry.getKey(), entry.getValue())).toList());

	public static final SavedDataType<CableLines> TYPE = new SavedDataType<>(Identifier.fromNamespaceAndPath(SimpleBlock.MOD_ID, "cable_lines"),
		CableLines::new, CODEC, DataFixTypes.SAVED_DATA_COMMAND_STORAGE);

	private final Map<BlockPos, Line> lines = new HashMap<>();

	private CableLines() {
	}

	private CableLines(List<Entry> entries) {
		for (Entry entry : entries) {
			this.lines.put(entry.drum().immutable(), entry.line());
		}
	}

	public static CableLines get(ServerLevel level) {
		return level.getDataStorage().computeIfAbsent(TYPE);
	}

	public static @Nullable Line line(ServerLevel level, BlockPos drum) {
		return get(level).lines.get(drum);
	}

	/** The line of the drum at {@code drum} in this set of lines, or null. */
	public @Nullable Line lineAt(BlockPos drum) {
		return this.lines.get(drum);
	}

	/** Every line in the level, by drum. Do not modify. */
	Map<BlockPos, Line> all() {
		return this.lines;
	}

	/** The line of the drum at {@code drum}, made empty if there is none yet. */
	Line getOrCreate(BlockPos drum) {
		this.setDirty();
		return this.lines.computeIfAbsent(drum.immutable(), key -> Line.empty());
	}

	/** Copies a loaded drum's route geometry into its line (and the grip index). */
	static void mirror(ServerLevel level, CableDrumBlockEntity drum) {
		CableLines lines = get(level);
		Line line = lines.getOrCreate(drum.getBlockPos());
		line.anchors = drum.route();
		line.points = drum.hasCoil() ? List.copyOf(drum.points()) : List.of();
		line.looped = drum.looped();
		line.outward = drum.looped() ? CableLoops.outward(drum) : null;
		if (!line.looped) {
			line.speed = 0.0F;
		}
		lines.setDirty();
		CableIndex.update(level, drum.getBlockPos(), line);
	}

	/**
	 * A drum's first tick after loading: meets its line (made from the drum's backup if the level lost
	 * it), applies a cut made while it was unloaded, and copies its geometry.
	 */
	static void link(ServerLevel level, CableDrumBlockEntity drum) {
		CableLines lines = get(level);
		Line line = lines.lines.get(drum.getBlockPos());
		if (line == null) {
			line = lines.getOrCreate(drum.getBlockPos());
			drum.restoreDrive(line);
		}
		drum.linked = true;
		applyCut(level, drum, line);
		mirror(level, drum);
	}

	/** Winds the drum's route back from where its line was cut while the drum was unloaded, if it was. */
	static void applyCut(ServerLevel level, CableDrumBlockEntity drum, Line line) {
		int cut = line.cut;
		if (cut < 0) {
			return;
		}
		line.cut = -1;
		get(level).setDirty();
		if (cut < drum.route().size()) {
			CableRoutes.disconnect(level, drum, cut);
		}
	}

	/** The drum is gone (broken, not unloaded): its line goes. */
	static void remove(ServerLevel level, BlockPos drum) {
		CableLines lines = get(level);
		if (lines.lines.remove(drum) != null) {
			lines.setDirty();
		}
		CableIndex.remove(level, drum);
	}

	/**
	 * An anchor of an unloaded drum's route is gone: the line stops at the support before it, at once,
	 * and opens if it was a loop. The drum applies the cut when it loads.
	 */
	static void cut(ServerLevel level, BlockPos drum, BlockPos anchor) {
		CableLines lines = get(level);
		Line line = lines.lines.get(drum);
		int index = line == null ? -1 : line.anchors.indexOf(anchor);
		if (index < 0) {
			return;
		}
		if (line.looped && !line.anchors.isEmpty()) {
			// The loop opens here and now: the wheel's anchor drops at the wheel if loaded, else here.
			BlockPos wheel = line.anchors.getLast();
			Block.popResource(level, level.isLoaded(wheel) ? wheel : anchor, new ItemStack(CableContent.CABLE_ANCHOR_ITEM));
		}
		line.cut = line.cut < 0 ? index : Math.min(line.cut, index);
		// Points: the drum's exit, then one per anchor.
		line.anchors = List.copyOf(new ArrayList<>(line.anchors.subList(0, Math.min(line.anchors.size(), index))));
		line.points = List.copyOf(new ArrayList<>(line.points.subList(0, Math.min(line.points.size(), index + 1))));
		line.looped = false;
		line.outward = null;
		line.speed = 0.0F;
		line.lever = Lever.NEUTRAL;
		lines.setDirty();
		CableIndex.update(level, drum, line);
	}

	void changed() {
		this.setDirty();
	}
}
