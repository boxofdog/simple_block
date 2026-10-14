package io.github.boxofdog.simpleblock.feature;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.entity.projectile.arrow.ThrownTrident;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.Vec3;

import io.github.boxofdog.simpleblock.tuning.Tuning;

/**
 * M09: a creeper shot with an arrow by a player it cannot readily walk to goes for what separates
 * them ("fuck your walls, fuck your doors"):
 * <ol>
 *   <li><b>Trigger:</b> an arrow (not a trident) whose owner is a player, and no short complete path
 *       from the creeper to that player ({@code mob.creeper_retaliation.reach_steps}). Melee hits,
 *       arrows from skeletons, dispensers or anything but a player, and arrows from a player it can
 *       reach change nothing: it chases and explodes as usual.</li>
 *   <li><b>Door first:</b> the nearest wooden, iron or copper door or trapdoor within
 *       {@code mob.creeper_retaliation.door_radius} blocks that it can walk up to; it goes there and
 *       ignites against it.</li>
 *   <li><b>Otherwise under the shooter:</b> it walks to the nearest point it can reach to the shooter's
 *       X and Z, whatever the height difference, and ignites there.</li>
 * </ol>
 * It ignites when it gets there (or can get no closer), and gives up after
 * {@code mob.creeper_retaliation.timeout_ticks}. The explosion is vanilla's (block damage only with
 * {@code mob_griefing}); light-drawn mobs around then re-check what they can reach
 * ({@link Entrancement#reconsider}). A creeper dissolving in sunlight ({@link CreeperEvaporation})
 * does nothing.
 */
public final class CreeperRetaliation {
	/** A provoked creeper's goal: the door, or the column under the shooter (at the creeper's height); and until when. */
	public record Grudge(BlockPos target, boolean door, long until) {
	}

	private static final Map<Creeper, Grudge> GRUDGES = new WeakHashMap<>();

	private CreeperRetaliation() {
	}

	public static @Nullable Grudge grudge(Creeper creeper) {
		return GRUDGES.get(creeper);
	}

	/** After a creeper took damage: provoked if it is a player's arrow and the player can't readily be reached. */
	public static void onDamaged(LivingEntity entity, DamageSource source) {
		if (!(entity instanceof Creeper creeper) || !(creeper.level() instanceof ServerLevel level) || !creeper.isAlive()
			|| CreeperEvaporation.isEvaporating(creeper)) {
			return;
		}
		if (!(source.getDirectEntity() instanceof AbstractArrow arrow) || arrow instanceof ThrownTrident
			|| !(source.getEntity() instanceof Player shooter) || shooter.isSpectator()) {
			return;
		}
		if (canReach(creeper, shooter)) {
			return;
		}
		provoke(creeper, level, shooter);
	}

	/** Whether the creeper has a short, complete path to the player. */
	public static boolean canReach(Creeper creeper, Player player) {
		Path path = PathProbe.probe(creeper, navigation -> navigation.createPath(player, 1));
		return path != null && path.canReach() && path.getNodeCount() <= Tuning.getInt("mob.creeper_retaliation.reach_steps");
	}

	static void provoke(Creeper creeper, ServerLevel level, Player shooter) {
		long until = level.getGameTime() + Tuning.getInt("mob.creeper_retaliation.timeout_ticks");
		BlockPos door = nearestDoor(creeper, level);
		if (door != null) {
			GRUDGES.put(creeper, new Grudge(door, true, until));
			return;
		}
		// Under the shooter: its X/Z at the creeper's own height; the march's path search ends at the
		// nearest point it can reach, whatever the height of the shooter.
		GRUDGES.put(creeper, new Grudge(BlockPos.containing(shooter.getX(), creeper.getY(), shooter.getZ()), false, until));
	}

	/** A door (lower half) or trapdoor of any material. */
	public static boolean isDoor(BlockState state) {
		return state.getBlock() instanceof DoorBlock && state.getValue(DoorBlock.HALF) == DoubleBlockHalf.LOWER
			|| state.getBlock() instanceof TrapDoorBlock;
	}

	/** The nearest door or trapdoor within the radius that the creeper can walk up to, or null. */
	private static @Nullable BlockPos nearestDoor(Creeper creeper, ServerLevel level) {
		int r = Tuning.getInt("mob.creeper_retaliation.door_radius");
		BlockPos here = creeper.blockPosition();
		List<BlockPos> doors = new ArrayList<>();
		for (BlockPos pos : BlockPos.betweenClosed(here.offset(-r, -r, -r), here.offset(r, r, r))) {
			if (pos.closerThan(here, r) && isDoor(level.getBlockState(pos))) {
				doors.add(pos.immutable());
			}
		}
		doors.sort(Comparator.comparingDouble(pos -> pos.distSqr(here)));
		for (int i = 0; i < Math.min(6, doors.size()); i++) {
			Path path = PathProbe.probe(creeper, doors.get(i), 1);
			if (path != null && path.canReach()) {
				return doors.get(i);
			}
		}
		return null;
	}

	/**
	 * The shooter's column and the eight around it: the march under the shooter searches for all of
	 * them, so a reachable spot right beside the column is found (one search for the column alone
	 * sometimes settles on a spot a block or two further off).
	 */
	private static Set<BlockPos> around(BlockPos column) {
		Set<BlockPos> positions = new HashSet<>();
		for (int dx = -1; dx <= 1; dx++) {
			for (int dz = -1; dz <= 1; dz++) {
				positions.add(column.offset(dx, 0, dz));
			}
		}
		return positions;
	}

	/** The provoked creeper's march: to its door or under its shooter, then it ignites. */
	public static final class RetaliationGoal extends Goal {
		/** Paths are searched only so far (a creeper's follow range): the end of a search from close by is the real nearest point. */
		private static final double CLOSE = 8.0;
		private final Creeper creeper;
		private int repath;
		private @Nullable BlockPos end;
		private boolean endFromClose;
		/** Lit: it stands still for the fuse (vanilla only holds a swelling creeper that has a target). */
		private boolean holding;

		public RetaliationGoal(Creeper creeper) {
			this.creeper = creeper;
			this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
		}

		@Override
		public boolean canUse() {
			Grudge grudge = GRUDGES.get(this.creeper);
			if (grudge == null) {
				return false;
			}
			if (this.creeper.level().getGameTime() > grudge.until() || CreeperEvaporation.isEvaporating(this.creeper)) {
				GRUDGES.remove(this.creeper);
				return false;
			}
			return !this.creeper.isIgnited();
		}

		@Override
		public boolean canContinueToUse() {
			return this.holding ? this.creeper.isAlive() : this.canUse();
		}

		@Override
		public void start() {
			this.repath = 0;
			this.end = null;
			this.holding = false;
		}

		@Override
		public void stop() {
			this.creeper.getNavigation().stop();
			this.holding = false;
		}

		@Override
		public void tick() {
			if (this.holding) {
				this.creeper.getNavigation().stop();
				return;
			}
			Grudge grudge = GRUDGES.get(this.creeper);
			if (grudge == null) {
				return;
			}
			Vec3 target = Vec3.atBottomCenterOf(grudge.target());
			this.creeper.getLookControl().setLookAt(target.x, target.y + 0.5, target.z);
			boolean arrived;
			boolean stuck = this.creeper.getNavigation().isDone() && this.repath > 0;
			if (grudge.door()) {
				// Against the door.
				double distance = this.creeper.position().distanceTo(Vec3.atCenterOf(grudge.target()));
				arrived = distance <= 1.8 || stuck && distance <= 2.0;
			} else {
				// Under the shooter: as close to its X/Z as the nearest reachable spot (the end of a path
				// search made from close by; one from far off may end only where the search stopped).
				double here = this.creeper.position().subtract(target).horizontalDistance();
				double best = this.end == null || !this.endFromClose ? -1.0 : Vec3.atBottomCenterOf(this.end).subtract(target).horizontalDistance();
				arrived = best >= 0.0 && (here <= best + 0.5 || stuck && here <= best + 1.0);
			}
			if (arrived) {
				this.creeper.ignite();
				this.creeper.getNavigation().stop();
				GRUDGES.remove(this.creeper);
				this.holding = true;
				return;
			}
			if (this.repath-- <= 0 || stuck) {
				this.repath = 20;
				Set<BlockPos> targets = grudge.door() ? Set.of(grudge.target()) : around(grudge.target());
				Path path = PathProbe.fresh(this.creeper, navigation -> navigation.createPath(targets, grudge.door() ? 1 : 0));
				this.end = path != null && path.getEndNode() != null ? path.getEndNode().asBlockPos() : null;
				this.endFromClose = this.creeper.position().subtract(target).horizontalDistance() <= CLOSE;
				this.creeper.getNavigation().moveTo(path, 1.0);
			}
		}
	}
}
