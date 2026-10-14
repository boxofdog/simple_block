package io.github.boxofdog.simpleblock.feature;

import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.monster.skeleton.Skeleton;
import net.minecraft.world.entity.monster.spider.Spider;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import io.github.boxofdog.simpleblock.tuning.Tuning;

/**
 * M09: occasional skeleton-spider jockeys, formed opportunistically, and the jockey's ledge grab.
 *
 * <p><b>Recruitment ({@link RecruitGoal}):</b> an ordinary skeleton (not strays, bogged, parched or
 * wither skeletons) with no target, wandering, now and then considers a spider: once every
 * {@code mob.jockey.check_ticks} ticks it has a {@code mob.jockey.recruit_chance} chance to pick the
 * nearest ordinary spider (not cave spiders) within {@code mob.jockey.range} blocks that is alive,
 * carries nobody, rides nothing and can be walked to. It walks over and climbs on. Seeing a player,
 * the spider being taken, killed or out of reach, or {@code mob.jockey.walk_ticks} going by, ends the
 * attempt; after any attempt it waits {@code mob.jockey.cooldown_ticks}. There is no tactical
 * recruiting and no quota; vanilla's own jockey spawns are unchanged. Mounted, the pair is a vanilla
 * jockey: the spider moves and climbs, the skeleton shoots.
 *
 * <p><b>Ledge grab ({@link #tick}):</b> while a jockey climbs a wall and the skeleton's head meets an
 * overhang above it, the skeleton raises its arms to the lip (a synced pose the client draws) and
 * hauls: the pair moves out from the wall until the way up is clear, then up until the spider is
 * above the overhang, then ordinary climbing resumes. Every step is an ordinary collision-checked move,
 * so nothing passes through blocks, and while hauled the spider moves only by the haul (no walking,
 * climbing or gravity of its own). If it can go neither out nor up for a few ticks, or the haul takes
 * too long, it lets go and does not try again for {@code mob.jockey.haul_retry_ticks}. Spiders without a rider, and jockeys on a clear wall, never do it.
 */
public final class SpiderJockeys {
	/** Implemented by skeletons (mixin): the synced ledge-grab pose. */
	public interface Grabbing {
		boolean simpleBlock$grabbing();

		void simpleBlock$setGrabbing(boolean grabbing);
	}

	/** A haul in progress: away from which wall, the overhang's top, ticks, stalled ticks. */
	private static final class Haul {
		final Direction out;
		final double top;
		int ticks;
		int stalled;

		Haul(Direction out, double top) {
			this.out = out;
			this.top = top;
		}
	}

	private static final Map<Spider, Haul> HAULS = new WeakHashMap<>();
	/** Spiders that gave up a haul, and the game time until which they don't try again. */
	private static final Map<Spider, Long> GAVE_UP = new WeakHashMap<>();
	/** For tests: completed recruitments, hauls started, hauls finished above the ledge, hauls given up. */
	public static int recruited;
	public static int haulsStarted;
	public static int haulsCleared;
	public static int haulsAbandoned;

	private SpiderJockeys() {
	}

	public static boolean isOrdinarySkeleton(Object entity) {
		return entity instanceof Skeleton skeleton && skeleton.getType() == EntityTypes.SKELETON;
	}

	public static boolean isOrdinarySpider(Object entity) {
		return entity instanceof Spider spider && spider.getType() == EntityTypes.SPIDER;
	}

	/** A spider the skeleton could recruit: ordinary, alive, carrying nobody, riding nothing, in range. */
	static boolean isRecruitable(Skeleton skeleton, Spider spider) {
		return isOrdinarySpider(spider) && spider.isAlive() && !spider.isVehicle() && !spider.isPassenger()
			&& spider.distanceTo(skeleton) <= Tuning.getDouble("mob.jockey.range");
	}

	public static boolean isHauling(Spider spider) {
		return HAULS.containsKey(spider);
	}

	/** Every server tick of a spider (after its own movement). */
	public static void tick(Spider spider) {
		Haul haul = HAULS.get(spider);
		Skeleton rider = isOrdinarySpider(spider) && spider.getFirstPassenger() instanceof Skeleton skeleton && isOrdinarySkeleton(skeleton)
			? skeleton : null;
		if (rider == null || !spider.isAlive()) {
			if (haul != null) {
				end(spider, null, false);
			}
			return;
		}
		if (haul == null) {
			if (!spider.isClimbing() || !headBlocked(spider, rider, 0.3) || spider.level().getGameTime() < GAVE_UP.getOrDefault(spider, Long.MIN_VALUE)) {
				return;
			}
			Direction wall = wallSide(spider);
			if (wall == null) {
				return;
			}
			BlockPos above = BlockPos.containing(rider.getX(), rider.getBoundingBox().maxY + (spider.getY() - spider.yo) + 0.3, rider.getZ());
			haul = new Haul(wall.getOpposite(), above.getY() + 1.0);
			HAULS.put(spider, haul);
			haulsStarted++;
			((Grabbing) rider).simpleBlock$setGrabbing(true);
		}
		haul.ticks++;
		double speed = Tuning.getDouble("mob.jockey.haul_speed");
		Vec3 up = new Vec3(0.0, speed, 0.0);
		Vec3 out = new Vec3(haul.out.getStepX() * speed, 0.0, haul.out.getStepZ() * speed);
		// Up when both have room above, else out from the wall when both have room there; never into a block.
		Vec3 step = free(spider, up) && free(spider, rider, up) ? up : free(spider, rider, out) ? out : Vec3.ZERO;
		Vec3 before = spider.position();
		spider.setDeltaMovement(Vec3.ZERO);
		spider.move(MoverType.SELF, step);
		spider.fallDistance = 0.0F;
		haul.stalled = spider.position().distanceTo(before) < 0.01 ? haul.stalled + 1 : 0;
		if (spider.getY() >= haul.top + 0.05 && !headBlocked(spider, rider, 0.1)) {
			end(spider, rider, true);
		} else if (haul.stalled > 5 || haul.ticks > Tuning.getInt("mob.jockey.haul_ticks")) {
			end(spider, rider, false);
		}
	}

	private static void end(Spider spider, @Nullable Skeleton rider, boolean cleared) {
		HAULS.remove(spider);
		if (rider instanceof Grabbing grabbing) {
			grabbing.simpleBlock$setGrabbing(false);
		}
		if (cleared) {
			haulsCleared++;
		} else {
			haulsAbandoned++;
			GAVE_UP.put(spider, spider.level().getGameTime() + Tuning.getInt("mob.jockey.haul_retry_ticks"));
		}
	}

	private static boolean free(Spider spider, Vec3 move) {
		return spider.level().noCollision(spider, spider.getBoundingBox().move(move));
	}

	/**
	 * Whether the rider has room to move by {@code move} from where it is about to be: a rider is
	 * placed on its mount after the mount has moved, so during the mount's tick its box is still where
	 * the mount was at the start of the tick.
	 */
	private static boolean free(Spider spider, Skeleton rider, Vec3 move) {
		Vec3 lag = spider.position().subtract(spider.xo, spider.yo, spider.zo);
		return rider.level().noCollision(rider, rider.getBoundingBox().move(lag).move(move));
	}

	/** Whether the riding skeleton would hit something moving {@code dy} up: its head is against an overhang. */
	static boolean headBlocked(Spider spider, Skeleton rider, double dy) {
		return !free(spider, rider, new Vec3(0.0, dy, 0.0));
	}

	/** The side of the spider where the wall it climbs is, or null. */
	private static @Nullable Direction wallSide(Spider spider) {
		AABB box = spider.getBoundingBox();
		Direction best = null;
		for (Direction direction : Direction.Plane.HORIZONTAL) {
			AABB probe = box.move(direction.getStepX() * 0.1, 0.0, direction.getStepZ() * 0.1);
			if (!spider.level().noCollision(spider, probe)) {
				best = direction;
				break;
			}
		}
		return best;
	}

	/** An ordinary skeleton's opportunistic recruitment of a nearby spider. */
	public static final class RecruitGoal extends Goal {
		private final Skeleton skeleton;
		private @Nullable Spider spider;
		private long nextCheck;
		private int ticks;

		public RecruitGoal(Skeleton skeleton) {
			this.skeleton = skeleton;
			this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
		}

		@Override
		public boolean canUse() {
			if (this.skeleton.getTarget() != null || this.skeleton.isPassenger() || this.skeleton.isVehicle()) {
				return false;
			}
			long now = this.skeleton.level().getGameTime();
			if (now < this.nextCheck) {
				return false;
			}
			this.nextCheck = now + Tuning.getInt("mob.jockey.check_ticks");
			if (this.skeleton.getRandom().nextDouble() >= Tuning.getDouble("mob.jockey.recruit_chance")) {
				return false;
			}
			double range = Tuning.getDouble("mob.jockey.range");
			List<Spider> spiders = this.skeleton.level().getEntitiesOfClass(Spider.class, this.skeleton.getBoundingBox().inflate(range),
				spider -> isRecruitable(this.skeleton, spider));
			spiders.sort(Comparator.comparingDouble(spider -> spider.distanceToSqr(this.skeleton)));
			for (Spider candidate : spiders) {
				Path path = PathProbe.probe(this.skeleton, navigation -> navigation.createPath(candidate, 1));
				if (path != null && path.canReach()) {
					this.spider = candidate;
					return true;
				}
			}
			return false;
		}

		@Override
		public boolean canContinueToUse() {
			return this.spider != null && this.skeleton.getTarget() == null && !this.skeleton.isPassenger()
				&& isRecruitable(this.skeleton, this.spider) && this.ticks < Tuning.getInt("mob.jockey.walk_ticks");
		}

		@Override
		public void start() {
			this.ticks = 0;
		}

		@Override
		public void tick() {
			this.ticks++;
			Spider spider = this.spider;
			// Running goals tick between continuation checks: a target that turned up since then still wins.
			if (spider == null || this.skeleton.getTarget() != null) {
				return;
			}
			this.skeleton.getLookControl().setLookAt(spider, 30.0F, 30.0F);
			if (this.skeleton.distanceTo(spider) < 1.6) {
				if (this.skeleton.startRiding(spider, false, true)) {
					recruited++;
				}
				return;
			}
			if (this.ticks % 10 == 1) {
				this.skeleton.getNavigation().moveTo(spider, 1.0);
			}
		}

		@Override
		public void stop() {
			this.spider = null;
			this.skeleton.getNavigation().stop();
			this.nextCheck = this.skeleton.level().getGameTime() + Tuning.getInt("mob.jockey.cooldown_ticks");
		}

		/** For tests: lets the next check happen now. */
		public void checkNow() {
			this.nextCheck = 0;
		}
	}

	/** Server side only. */
	static boolean server(net.minecraft.world.entity.Entity entity) {
		return entity.level() instanceof ServerLevel;
	}
}
