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
 * MOB-01: at night in the Overworld, hostile mobs within 20 blocks of lit ground drift to it and
 * loiter there.
 *
 * <p>It works on the wander decision that hostile mobs already make ({@code RandomStrollGoal} and
 * its subclasses): {@code mixin/mobs/RandomStrollGoalMixin} hands the chosen destination to
 * {@link #redirect}, which may replace it with a walkable spot towards remembered lit ground.
 * Combat, targeting and every other goal are untouched, so a mob that notices a player chases it as
 * usual and goes back to wandering (and so to the light) once it loses the player.
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
 * <p>"Lit" means block light above the dimension's monster-spawn block-light limit (in the
 * Overworld any block light, as any block light stops hostile spawning), so sunlight never counts and
 * it does not matter what makes the light. Detection ignores walls; walking does not, so behind a
 * wall the mobs gather along its outside. Each decision checks a few random positions within
 * 20 blocks and remembers up to a handful of lit spots (drifting towards brighter ones), choosing
 * among them at random so mobs spread out and mill about instead of piling onto one block, and now
 * and then ignores the light altogether.
 */
public final class Entrancement {
	/** Locked by the design: how far from a mob a lit block can attract it. */
	public static final int RADIUS = 20;
	private static final int VERTICAL = 6;

	private static final Map<PathfinderMob, List<BlockPos>> MEMORY = new WeakHashMap<>();
	/** Decisions handled and time spent in {@link #redirect}, for the performance evidence (MOB-07). */
	public static long decisions;
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

	public static boolean isLit(Level level, BlockPos pos) {
		return level.isLoaded(pos) && level.getBrightness(LightLayer.BLOCK, pos) > level.dimensionType().monsterSpawnBlockLightLimit();
	}

	/**
	 * The wander destination to use instead of {@code wanted} (vanilla's choice, null when it found
	 * none), or {@code wanted} itself; null means no wandering this time.
	 */
	public static @Nullable Vec3 redirect(PathfinderMob mob, @Nullable Vec3 wanted) {
		long start = System.nanoTime();
		try {
			return redirectTimed(mob, wanted);
		} finally {
			decisions++;
			nanos += System.nanoTime() - start;
		}
	}

	private static @Nullable Vec3 redirectTimed(PathfinderMob mob, @Nullable Vec3 wanted) {
		if (!(mob instanceof Enemy) || !mob.getNavigation().canNavigateGround() || !(mob.level() instanceof ServerLevel level)
			|| mob.getTarget() != null || !isNight(level) || !Tuning.getBoolean("mob.entrancement.enabled")) {
			return wanted;
		}
		RandomSource random = mob.getRandom();
		List<BlockPos> lit = MEMORY.computeIfAbsent(mob, m -> new ArrayList<>());
		BlockPos here = mob.blockPosition();
		lit.removeIf(pos -> !pos.closerThan(here, RADIUS) || !isLit(level, pos));

		int samples = Tuning.getInt("mob.entrancement.samples");
		int memory = Math.max(1, Tuning.getInt("mob.entrancement.memory"));
		for (int i = 0; i < samples; i++) {
			BlockPos pos = here.offset(random.nextInt(2 * RADIUS + 1) - RADIUS, random.nextInt(2 * VERTICAL + 1) - VERTICAL,
				random.nextInt(2 * RADIUS + 1) - RADIUS);
			if (pos.closerThan(here, RADIUS) && isLit(level, pos) && !lit.contains(pos)) {
				remember(level, lit, pos, memory, random);
			}
		}
		if (lit.isEmpty() || random.nextDouble() < Tuning.getDouble("mob.entrancement.lose_interest_chance")) {
			return wanted;
		}

		BlockPos spot = lit.get(random.nextInt(lit.size()));
		Vec3 target = Vec3.atBottomCenterOf(spot);
		int step = Math.max(2, Tuning.getInt("mob.entrancement.step"));
		if (target.subtract(mob.position()).horizontalDistance() > step) {
			// Still out in the dark: one ordinary-length wander step in the spot's direction.
			Vec3 towards = LandRandomPos.getPosTowards(mob, step, VERTICAL, target);
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

	/**
	 * Adds a lit spot to the mob's memory. When the memory is full the new spot replaces a random
	 * remembered one that is no brighter, so the memory drifts into the lit area without collapsing
	 * onto the single brightest block.
	 */
	private static void remember(ServerLevel level, List<BlockPos> lit, BlockPos pos, int memory, RandomSource random) {
		if (lit.size() < memory) {
			lit.add(pos);
			return;
		}
		int index = random.nextInt(lit.size());
		if (level.getBrightness(LightLayer.BLOCK, pos) >= level.getBrightness(LightLayer.BLOCK, lit.get(index))) {
			lit.set(index, pos);
		}
	}

	/** The standable position at or below a lit spot (spots are sampled in the air too), or null. */
	private static @Nullable BlockPos ground(PathfinderMob mob, BlockPos spot) {
		BlockPos pos = spot;
		for (int i = 0; i < 2 * VERTICAL && pos.getY() > mob.level().getMinY() && !GoalUtils.isSolid(mob, pos) && !GoalUtils.isSolid(mob, pos.below()); i++) {
			pos = pos.below();
		}
		pos = LandRandomPos.movePosUpOutOfSolid(mob, pos);
		return pos == null || GoalUtils.isNotStable(mob.getNavigation(), pos) ? null : pos;
	}
}
