package io.github.boxofdog.simpleblock.feature;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.util.LandRandomPos;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.phys.Vec3;

import io.github.boxofdog.simpleblock.tuning.Tuning;

/**
 * MOB-01: at night in the Overworld, hostile mobs within 20 blocks of lit ground drift to it and
 * loiter there.
 *
 * <p>It works on the wander decision that hostile mobs already make ({@code RandomStrollGoal}, which
 * zombies, husks, drowned, skeletons, strays, creepers, spiders, endermen, witches and illagers
 * use): {@code mixin/mobs/RandomStrollGoalMixin} hands the chosen destination to {@link #redirect},
 * which may replace it with a walkable spot towards remembered lit ground. Combat, targeting and
 * every other goal are untouched, so a mob that notices a player chases it as usual and goes back to
 * wandering (and so to the light) once it loses the player. Mobs without a wander goal (slimes,
 * phantoms, silverfish, wardens, bosses) are not affected.
 *
 * <p>"Lit" means block light above the dimension's monster-spawn block-light limit (in the
 * Overworld any block light, as any block light stops hostile spawning), so sunlight never counts and
 * it does not matter what makes the light. Detection ignores walls; walking does not, so behind a
 * wall the mobs gather along its outside. Each decision checks a few random positions within
 * 20 blocks and remembers up to a handful of lit spots, choosing among them at random so mobs spread
 * out instead of piling onto one block, and now and then ignores the light altogether.
 *
 * <p><b>Work in progress:</b> not wired in yet (its mixin is not in simple_block.mixins.json); see
 * docs/MILESTONE_02_CHECKPOINT.md.
 */
public final class Entrancement {
	/** Locked by the design: how far from a mob a lit block can attract it. */
	public static final int RADIUS = 20;
	private static final int VERTICAL = 6;

	private static final Map<PathfinderMob, List<BlockPos>> MEMORY = new WeakHashMap<>();

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

	/** The wander destination to use instead of {@code wanted}, or {@code wanted} itself. */
	public static Vec3 redirect(PathfinderMob mob, Vec3 wanted) {
		if (!(mob instanceof Enemy) || !(mob.level() instanceof ServerLevel level) || mob.getTarget() != null || !isNight(level)
			|| !Tuning.getBoolean("mob.entrancement.enabled")) {
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
			if (pos.closerThan(here, RADIUS) && isLit(level, pos)) {
				if (lit.size() >= memory) {
					lit.remove(random.nextInt(lit.size()));
				}
				lit.add(pos);
			}
		}
		if (lit.isEmpty() || random.nextDouble() < Tuning.getDouble("mob.entrancement.lose_interest_chance")) {
			return wanted;
		}

		BlockPos spot = lit.get(random.nextInt(lit.size()));
		int step = Math.max(2, Tuning.getInt("mob.entrancement.step"));
		Vec3 target = Vec3.atBottomCenterOf(spot);
		// Close to the lit spot: mill about near it rather than walking onto the exact block.
		Vec3 destination = mob.position().closerThan(target, 4.0)
			? LandRandomPos.getPosTowards(mob, 4, 2, target)
			: LandRandomPos.getPosTowards(mob, step, VERTICAL, target);
		return destination != null ? destination : wanted;
	}
}
