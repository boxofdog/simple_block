package io.github.boxofdog.simpleblock.effigy;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

import org.jspecify.annotations.Nullable;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerBlockEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLevelEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.NeutralMob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.GoalSelector;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.Vec3;

import io.github.boxofdog.simpleblock.mixin.effigy.CreeperAccessor;
import io.github.boxofdog.simpleblock.mixin.mobs.MobAccessor;

/**
 * M12: the fear an effigy puts into its monster.
 *
 * <p><b>The cone.</b> Each effigy's influence is a solid right circular cone: its apex at the middle of
 * the monument (the top of its lower block, centred), its axis level and pointing the way the effigy
 * faces (its angle, {@link EffigyBlockEntity#facing}), a half-angle of 45 degrees and a length of 100
 * blocks along the axis. A point is inside when, with {@code a} its distance along the axis from the
 * apex and {@code r} its distance from the axis, {@code 0 < a <= 100} and {@code r <= a}. So it reaches
 * 100 blocks ahead and, at that distance, 100 blocks to either side, above and below; nothing behind the
 * effigy, nor level with it to its sides, is inside. A mob is inside when the centre of its bounding box
 * is. The same cone, for all four types.
 *
 * <p><b>Fear.</b> Every zombie (baby zombies too), skeleton, creeper and enderman that enters a server
 * world gets {@link FearGoal} at a priority above all its other goals (M09's included). It runs as soon
 * as the mob is inside a loaded effigy's cone of its own type and, while it runs, the mob has no target
 * ({@code mixin/effigy}), gets off anything it rides, puts out a creeper's fuse, and flees along a real
 * path out of every cone that holds it, sprinting (vanilla's sprint: +30% speed and the sprinting dust).
 * It ends once the mob is clear of every such cone by a 4-block margin (so it does not step straight back
 * in); a mob with no way out stays afraid where it is and looks again every second. Nothing else changes:
 * spawning, despawning, and anything coming into a cone from outside it (a skeleton's arrows included).
 *
 * <p><b>Loaded only.</b> The effigies looked at are those whose block entity is loaded (kept in a set as
 * they load and unload, per world); nothing is force-loaded, and only mobs that tick are afraid.
 */
public final class Effigies {
	/** The cone's length along its axis, and its radius at that length. */
	public static final double REACH = 100.0;
	/** How far clear of a cone (beyond its sides and its far end) a fleeing mob goes before fear ends. */
	public static final double MARGIN = 4.0;
	private static final double SQRT2 = Math.sqrt(2.0);
	/** How far ahead an escape is looked for, and how often (ticks) a mob looks again, finding a way out or not. */
	private static final int ESCAPE_DISTANCE = 16;
	private static final int REPATH_TICKS = 10;
	private static final int TRAPPED_RETRY_TICKS = 20;

	private static final Map<Level, Set<EffigyBlockEntity>> LOADED = new WeakHashMap<>();

	/** Counts, for the game tests. */
	public static long frightened;
	public static long trapped;

	/** A mob's fear flag (mixed into every mob, {@code mixin/effigy/MobFearMixin}). */
	public interface Fearful {
		boolean simpleBlock$afraid();

		void simpleBlock$setAfraid(boolean afraid);
	}

	private Effigies() {
	}

	public static boolean afraid(Mob mob) {
		return ((Fearful) mob).simpleBlock$afraid();
	}

	public static void register() {
		ServerBlockEntityEvents.BLOCK_ENTITY_LOAD.register((blockEntity, level) -> {
			if (blockEntity instanceof EffigyBlockEntity effigy) {
				LOADED.computeIfAbsent(level, key -> Collections.newSetFromMap(new IdentityHashMap<>())).add(effigy);
			}
		});
		ServerBlockEntityEvents.BLOCK_ENTITY_UNLOAD.register((blockEntity, level) -> {
			if (blockEntity instanceof EffigyBlockEntity effigy) {
				Set<EffigyBlockEntity> loaded = LOADED.get(level);
				if (loaded != null) {
					loaded.remove(effigy);
				}
			}
		});
		ServerLevelEvents.UNLOAD.register((server, level) -> LOADED.remove(level));
		ServerEntityEvents.ENTITY_LOAD.register((entity, level) -> {
			if (entity instanceof PathfinderMob mob && EffigyType.frightening(mob) != null) {
				GoalSelector goals = ((MobAccessor) mob).simpleBlock$getGoalSelector();
				// A mob is loaded again whenever its chunk is: add the goal once.
				if (goals.getAvailableGoals().stream().noneMatch(goal -> goal.getGoal() instanceof FearGoal)) {
					goals.addGoal(-1, new FearGoal(mob));
				}
			}
		});
	}

	/** The loaded effigies in {@code level} (for the tests). */
	public static int loadedCount(Level level) {
		Set<EffigyBlockEntity> loaded = LOADED.get(level);
		return loaded == null ? 0 : loaded.size();
	}

	/** The cone's apex for an effigy whose lower half is at {@code lower}. */
	public static Vec3 apex(BlockPos lower) {
		return Vec3.atLowerCornerOf(lower).add(0.5, 1.0, 0.5);
	}

	/**
	 * How deep {@code point} is inside the cone of an effigy at {@code lower} facing {@code facing} (a level
	 * unit vector), grown by {@code margin} on its sides and far end: 0 outside, else the shortest distance
	 * to leave it (through the sides or the far end).
	 */
	public static double depth(BlockPos lower, Vec3 facing, Vec3 point, double margin) {
		Vec3 d = point.subtract(apex(lower));
		double a = d.dot(facing);
		double r = d.subtract(facing.scale(a)).length();
		// Growing the sides outwards by the margin moves the apex back by margin * sqrt 2.
		double grown = a + margin * SQRT2;
		if (grown <= 0.0 || a > REACH + margin || r > grown) {
			return 0.0;
		}
		return Math.max(1.0E-6, Math.min((grown - r) / SQRT2, REACH + margin - a));
	}

	/** Whether {@code point} is inside the (ungrown) cone of an effigy at {@code lower} facing {@code facing}. */
	public static boolean inside(BlockPos lower, Vec3 facing, Vec3 point) {
		return depth(lower, facing, point, 0.0) > 0.0;
	}

	/** The loaded effigies of {@code type} in the mob's world whose cone, grown by {@code margin}, holds the mob. */
	static List<EffigyBlockEntity> affecting(Mob mob, EffigyType type, double margin) {
		Set<EffigyBlockEntity> loaded = LOADED.get(mob.level());
		if (loaded == null || loaded.isEmpty()) {
			return List.of();
		}
		Vec3 point = mob.getBoundingBox().getCenter();
		double far = (REACH + 2.0 * margin) * SQRT2;
		List<EffigyBlockEntity> found = new ArrayList<>(1);
		for (EffigyBlockEntity effigy : loaded) {
			if (effigy.isRemoved() || effigy.type() != type || apex(effigy.getBlockPos()).distanceToSqr(point) > far * far) {
				continue;
			}
			if (depth(effigy.getBlockPos(), effigy.facing(), point, margin) > 0.0) {
				found.add(effigy);
			}
		}
		return found;
	}

	private static double totalDepth(List<EffigyBlockEntity> effigies, Vec3 point) {
		double total = 0.0;
		for (EffigyBlockEntity effigy : effigies) {
			total += depth(effigy.getBlockPos(), effigy.facing(), point, MARGIN);
		}
		return total;
	}

	/** The level direction the effigies most push {@code point} out by: to the nearer side of each cone, or on past its far end. */
	private static Vec3 awayFrom(List<EffigyBlockEntity> effigies, Vec3 point) {
		Vec3 sum = Vec3.ZERO;
		for (EffigyBlockEntity effigy : effigies) {
			Vec3 facing = effigy.facing();
			Vec3 d = point.subtract(apex(effigy.getBlockPos()));
			double a = d.dot(facing);
			Vec3 side = new Vec3(-facing.z, 0.0, facing.x);
			double across = d.dot(side);
			double weight = depth(effigy.getBlockPos(), facing, point, MARGIN);
			Vec3 out = REACH - a < a - Math.abs(across) ? facing : side.scale(across < 0.0 ? -1.0 : 1.0);
			sum = sum.add(out.scale(weight));
		}
		return sum.lengthSqr() < 1.0E-8 ? Vec3.ZERO : sum.normalize();
	}

	/** Puts out a creeper's fuse (vanilla's own way of standing down, and its flint-and-steel ignition too). */
	private static void defuse(Creeper creeper) {
		if (creeper.isIgnited()) {
			creeper.getEntityData().set(CreeperAccessor.simpleBlock$ignited(), false);
		}
		if (creeper.getSwellDir() > 0) {
			creeper.setSwellDir(-1);
		}
	}

	/** The fear itself: see {@link Effigies}. */
	public static final class FearGoal extends Goal {
		private final PathfinderMob mob;
		private final EffigyType type;
		private int repath;

		FearGoal(PathfinderMob mob) {
			this.mob = mob;
			this.type = EffigyType.frightening(mob);
			this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.JUMP, Goal.Flag.LOOK));
		}

		@Override
		public boolean canUse() {
			return this.mob.isAlive() && !affecting(this.mob, this.type, 0.0).isEmpty();
		}

		@Override
		public boolean canContinueToUse() {
			return this.mob.isAlive() && !affecting(this.mob, this.type, MARGIN).isEmpty();
		}

		@Override
		public boolean requiresUpdateEveryTick() {
			return true;
		}

		@Override
		public void start() {
			frightened++;
			((Fearful) this.mob).simpleBlock$setAfraid(true);
			this.mob.setTarget(null);
			if (this.mob instanceof NeutralMob neutral) {
				neutral.stopBeingAngry();
			}
			this.mob.setAggressive(false);
			this.mob.getNavigation().stop();
			this.repath = 0;
			this.calmDown();
		}

		/** What fear undoes every tick: a target, a mount, a lit fuse. */
		private void calmDown() {
			if (this.mob.getTarget() != null) {
				this.mob.setTarget(null);
			}
			if (this.mob.isPassenger()) {
				this.mob.stopRiding();
			}
			if (this.mob instanceof Creeper creeper) {
				defuse(creeper);
			}
		}

		@Override
		public void tick() {
			this.calmDown();
			// A new way out when the last one has run out (arrived, or given up as stuck), at most every half second.
			if (--this.repath <= 0 && this.mob.getNavigation().isDone()) {
				this.repath = this.flee() ? REPATH_TICKS : TRAPPED_RETRY_TICKS;
			}
			// Vanilla's sprint, only while it runs: a little more speed and the dust at its feet.
			this.mob.setSprinting(this.mob.getNavigation().isInProgress());
		}

		/** Sets off along the best path out of every cone that holds the mob; false if none leads further out. */
		private boolean flee() {
			List<EffigyBlockEntity> effigies = affecting(this.mob, this.type, MARGIN);
			Vec3 here = this.mob.getBoundingBox().getCenter();
			double current = totalDepth(effigies, here);
			Vec3 away = awayFrom(effigies, here);
			if (away == Vec3.ZERO) {
				away = Vec3.directionFromRotation(0.0F, this.mob.getYRot());
			}
			double halfHeight = this.mob.getBbHeight() / 2.0;
			Path best = null;
			double bestDepth = current - 0.5;
			for (int step = 0; step < 8; step++) {
				// Straight away first, then either side of it, wider each time, then straight back.
				double turn = Math.toRadians(45.0 * ((step + 1) / 2) * (step % 2 == 0 ? 1 : -1));
				Vec3 dir = away.yRot((float) turn);
				Vec3 goal = this.mob.position().add(dir.scale(ESCAPE_DISTANCE));
				Path path = this.mob.getNavigation().createPath(BlockPos.containing(goal), 0);
				Node end = path == null ? null : path.getEndNode();
				if (end == null) {
					continue;
				}
				double depth = totalDepth(effigies, new Vec3(end.x + 0.5, end.y + halfHeight, end.z + 0.5));
				if (depth < bestDepth) {
					best = path;
					bestDepth = depth;
					if (depth == 0.0) {
						break;
					}
				}
			}
			if (best == null) {
				trapped++;
				this.mob.getNavigation().stop();
				return false;
			}
			this.mob.getNavigation().moveTo(best, 1.0);
			return true;
		}

		@Override
		public void stop() {
			((Fearful) this.mob).simpleBlock$setAfraid(false);
			this.mob.setSprinting(false);
			this.mob.getNavigation().stop();
		}
	}

	/** Whether {@code mob} is fleeing an effigy now (for the tests). */
	public static boolean fleeing(@Nullable Mob mob) {
		return mob != null && afraid(mob);
	}
}
