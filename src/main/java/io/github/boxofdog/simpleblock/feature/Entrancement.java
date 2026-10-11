package io.github.boxofdog.simpleblock.feature;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.util.GoalUtils;
import net.minecraft.world.entity.ai.util.LandRandomPos;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.phys.Vec3;

import org.jspecify.annotations.Nullable;

import io.github.boxofdog.simpleblock.tuning.Tuning;

/**
 * MOB-01: hostile mobs within 20 blocks of lit ground drift to it and loiter there, at night in the
 * Overworld and, at any time of day, wherever they get no sky light (caves, under cover).
 *
 * <p>It works on the wander decision that hostile mobs already make ({@code RandomStrollGoal} and
 * its subclasses): {@code mixin/mobs/RandomStrollGoalMixin} hands the chosen destination to
 * {@link #redirect}, which may replace it. Combat, targeting and every other goal are untouched, so
 * a mob that notices a player chases it as usual and goes back to wandering (and so to the light)
 * once it loses the player.
 *
 * <p>Audit (26.3): affected are the {@code Enemy} mobs with such a goal and ground navigation:
 * zombies, husks, zombie villagers, drowned, skeletons, strays, bogged, parched, creepers, spiders,
 * cave spiders, endermen, endermites, silverfish, witches, pillagers, vindicators, evokers,
 * illusioners and ravagers (blazes and zombified piglins too, but Nether mobs only meet it if they
 * are in the Overworld). Not affected: mobs whose AI has no such goal (slimes, magma cubes,
 * phantoms, ghasts, vexes, shulkers, wardens, breezes, creakings, piglins, hoglins, zoglins, the
 * Ender Dragon), and flying or water-bound mobs, whose movement the land-based destinations would
 * break (the Wither, guardians and elder guardians).
 *
 * <p>When: in the Overworld, at night (clock 13000-23000) or where the mob's own position gets no
 * sky light, whatever the time (caves at any depth, roofed spaces); a surface mob in daylight is
 * not affected. Not in the Nether or the End, which have no sky light anywhere and would otherwise
 * attract permanently.
 *
 * <p>What attracts: <em>areas</em> of block light above 0, whatever makes the light. Sunlight never
 * counts. Each wander decision checks a few random positions in the 20-block sphere and keeps a
 * small memory of lit ones (random positions, so no bias towards bright blocks or light sources).
 * <ul>
 *   <li>Outside lit ground, the mob heads for the nearest lit area it knows of: a random one of the
 *       remembered spots that are about as near as the nearest, so mobs coming from one side spread
 *       along that edge instead of queueing for one block. Detection ignores walls; walking does
 *       not: a spot behind a wall gives a path to the nearest reachable point, so mobs loiter along
 *       the outside.</li>
 *   <li>On lit ground, it wanders as aimlessly as vanilla, but prefers destinations that are lit,
 *       so it mills about the area rather than leaving it or homing in on the light source.</li>
 *   <li>Now and then (15%) it ignores the light altogether for one decision.</li>
 * </ul>
 */
public final class Entrancement {
	/** Locked by the design: how far from a mob a lit block can attract it. */
	public static final int RADIUS = 20;
	/** How far a destination may lie below a lit spot (spots are sampled in the air too). */
	private static final int DROP = 12;
	/** A remembered spot counts as part of "the nearest lit area" up to this much farther than the nearest one. */
	private static final double AREA_SLACK = 4.0;

	private static final Map<PathfinderMob, List<BlockPos>> MEMORY = new WeakHashMap<>();
	/**
	 * Decisions handled, decisions where the destination was changed, and time spent in
	 * {@link #redirect}: evidence for the tests (MOB-04, MOB-07).
	 */
	public static long decisions;
	public static long steered;
	public static long nanos;

	private Entrancement() {
	}

	/** Overworld night by the overworld clock (dusk to dawn); weather does not matter. */
	public static boolean isNight(Level level) {
		if (level.dimension() != Level.OVERWORLD) {
			return false;
		}
		long timeOfDay = Math.floorMod(level.getOverworldClockTime(), 24000L);
		return timeOfDay >= 13000L && timeOfDay < 23000L;
	}

	/** Whether the attraction works at {@code pos}: Overworld, and night or no sky light there. */
	public static boolean isActiveAt(Level level, BlockPos pos) {
		return level.dimension() == Level.OVERWORLD && (isNight(level) || level.getBrightness(LightLayer.SKY, pos) == 0);
	}

	/** Lit: any block light at all (in the Overworld any block light also stops monster spawning). */
	public static boolean isLit(Level level, BlockPos pos) {
		return level.isLoaded(pos) && level.getBrightness(LightLayer.BLOCK, pos) > 0;
	}

	/**
	 * The wander destination to use instead of {@code wanted} (vanilla's choice, null when it found
	 * none), or {@code wanted} itself; null means no wandering this time.
	 */
	public static @Nullable Vec3 redirect(PathfinderMob mob, @Nullable Vec3 wanted) {
		long start = System.nanoTime();
		Vec3 destination = redirectTimed(mob, wanted);
		decisions++;
		if (destination != wanted) {
			steered++;
		}
		nanos += System.nanoTime() - start;
		return destination;
	}

	private static @Nullable Vec3 redirectTimed(PathfinderMob mob, @Nullable Vec3 wanted) {
		if (!(mob instanceof Enemy) || !mob.getNavigation().canNavigateGround() || !(mob.level() instanceof ServerLevel level)
			|| mob.getTarget() != null || !Tuning.getBoolean("mob.entrancement.enabled") || !isActiveAt(level, mob.blockPosition())) {
			return wanted;
		}
		RandomSource random = mob.getRandom();
		List<BlockPos> lit = MEMORY.computeIfAbsent(mob, m -> new ArrayList<>());
		BlockPos here = mob.blockPosition();
		lit.removeIf(pos -> !pos.closerThan(here, RADIUS) || !isLit(level, pos));

		int samples = Tuning.getInt("mob.entrancement.samples");
		int memory = Math.max(1, Tuning.getInt("mob.entrancement.memory"));
		for (int i = 0; i < samples; i++) {
			BlockPos pos = here.offset(random.nextInt(2 * RADIUS + 1) - RADIUS, random.nextInt(2 * RADIUS + 1) - RADIUS,
				random.nextInt(2 * RADIUS + 1) - RADIUS);
			if (pos.closerThan(here, RADIUS) && isLit(level, pos) && !lit.contains(pos)) {
				// Full memory: forget a random spot, so it stays a random sample of the lit area.
				if (lit.size() >= memory) {
					lit.remove(random.nextInt(lit.size()));
				}
				lit.add(pos);
			}
		}
		if (lit.isEmpty() || random.nextDouble() < Tuning.getDouble("mob.entrancement.lose_interest_chance")) {
			return wanted;
		}
		int step = Math.max(2, Tuning.getInt("mob.entrancement.step"));

		if (isLit(level, here)) {
			// On lit ground: an ordinary random wander, but lit destinations win over dark ones.
			Vec3 roam = LandRandomPos.getPos(mob, step, 7, pos -> isLit(level, pos) ? 1.0 : 0.0);
			if (roam != null && isLit(level, BlockPos.containing(roam))) {
				return roam;
			}
		}

		// Off lit ground (or no lit destination nearby): towards the nearest lit area.
		double nearest = Double.MAX_VALUE;
		for (BlockPos pos : lit) {
			nearest = Math.min(nearest, Math.sqrt(pos.distSqr(here)));
		}
		List<BlockPos> area = new ArrayList<>();
		for (BlockPos pos : lit) {
			if (Math.sqrt(pos.distSqr(here)) <= nearest + AREA_SLACK) {
				area.add(pos);
			}
		}
		BlockPos spot = area.get(random.nextInt(area.size()));
		Vec3 target = Vec3.atBottomCenterOf(spot);
		if (target.subtract(mob.position()).horizontalDistance() > step) {
			// Still out in the dark: one ordinary-length wander step in the area's direction.
			Vec3 towards = LandRandomPos.getPosTowards(mob, step, 7, target);
			return towards != null ? towards : wanted;
		}
		// Within a step: walk to the spot itself. Vanilla's random positions are scored by the mob's
		// own preference, which for monsters is the darkest candidate, and would keep them at the
		// dim edge of the light. When the spot can't be reached (light behind a wall) the path ends
		// at the nearest reachable point, so the mob loiters along the outside.
		BlockPos ground = ground(mob, spot);
		if (ground == null) {
			return wanted;
		}
		// Already there: stand about until the next wander decision.
		return ground.closerToCenterThan(mob.position(), 1.5) ? null : Vec3.atBottomCenterOf(ground);
	}

	/** The standable position at or below a lit spot (spots are sampled in the air too), or null. */
	private static @Nullable BlockPos ground(PathfinderMob mob, BlockPos spot) {
		BlockPos pos = spot;
		for (int i = 0; i < DROP && pos.getY() > mob.level().getMinY() && !GoalUtils.isSolid(mob, pos) && !GoalUtils.isSolid(mob, pos.below()); i++) {
			pos = pos.below();
		}
		pos = LandRandomPos.movePosUpOutOfSolid(mob, pos);
		return pos == null || GoalUtils.isNotStable(mob.getNavigation(), pos) ? null : pos;
	}
}
