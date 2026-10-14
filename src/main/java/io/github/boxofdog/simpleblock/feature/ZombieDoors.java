package io.github.boxofdog.simpleblock.feature;

import java.util.EnumSet;
import java.util.Map;
import java.util.WeakHashMap;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.BreakDoorGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.LevelEvent;
import net.minecraft.world.level.block.state.BlockState;

/**
 * MOB-02: zombies break wooden doors only while chasing a player, at vanilla's Hard-mode pace
 * (240 ticks) on every difficulty except Peaceful; metal doors get attacked but never damaged.
 *
 * <p>Family: {@code Zombie}, {@code Husk} and {@code ZombieVillager}, which share
 * {@code Zombie.addBehaviourGoals} (see {@code mixin/mobs/ZombieMixin}). Drowned (amphibious
 * navigation) and zombified piglins (neutral Nether mobs) replace that method and are left as in
 * vanilla, where neither breaks doors in practice. Vanilla's own door breaking, given at random to
 * some zombies on Hard and used while wandering through villages, is switched off, so there is no
 * door breaking without a chase. The {@code mob_griefing} game rule still applies.
 *
 * <p>"Wooden" means {@code #minecraft:wooden_doors}. Vanilla's {@code DoorBlock.isWoodenDoor}
 * means "opens by hand", which also includes copper doors; here copper doors count as metal.
 */
public final class ZombieDoors {
	/** Locked: vanilla 26.3's Hard-mode door-breaking time ({@code BreakDoorGoal.DEFAULT_DOOR_BREAK_TIME}). */
	public static final int BREAK_TICKS = 240;

	private ZombieDoors() {
	}

	public static boolean isWoodenDoor(BlockState state) {
		return state.getBlock() instanceof DoorBlock && state.is(BlockTags.WOODEN_DOORS);
	}

	public static boolean isClosedMetalDoor(BlockState state) {
		return state.getBlock() instanceof DoorBlock && !state.is(BlockTags.WOODEN_DOORS) && !state.getValue(DoorBlock.OPEN);
	}

	/**
	 * How long a chase counts as active after the zombie last had a player as its target. Vanilla
	 * zombies forget a player about 3 s after losing sight, which a closed door causes at once; the
	 * chase has to outlast the 240-tick break for a door shut in a zombie's face to be broken.
	 */
	public static final int PURSUIT_MEMORY_TICKS = 300;

	private static final Map<Mob, Long> LAST_PURSUIT = new WeakHashMap<>();

	/** Called every server tick for the zombie family. */
	public static void trackPursuit(Mob mob) {
		if (hasPlayerTarget(mob)) {
			LAST_PURSUIT.put(mob, mob.level().getGameTime());
		}
	}

	private static boolean hasPlayerTarget(Mob mob) {
		return mob.getTarget() instanceof Player player && player.isAlive() && !player.isSpectator() && !player.isCreative();
	}

	/**
	 * Chasing a player it has detected, now or within {@link #PURSUIT_MEMORY_TICKS} (creative and
	 * spectator players are never targets).
	 */
	public static boolean isPursuingPlayer(Mob mob) {
		if (hasPlayerTarget(mob)) {
			return true;
		}
		Long last = LAST_PURSUIT.get(mob);
		return last != null && mob.level().getGameTime() - last <= PURSUIT_MEMORY_TICKS;
	}

	/** Vanilla's door-breaking goal, limited to wooden doors and to active pursuit. */
	public static final class PursuitDoorBreakGoal extends BreakDoorGoal {
		public PursuitDoorBreakGoal(Mob mob) {
			super(mob, BREAK_TICKS, difficulty -> difficulty != Difficulty.PEACEFUL);
		}

		@Override
		public boolean canUse() {
			return isPursuingPlayer(this.mob) && super.canUse() && isWoodenDoor(this.mob.level().getBlockState(this.doorPos));
		}

		@Override
		public boolean canContinueToUse() {
			return isPursuingPlayer(this.mob) && super.canContinueToUse();
		}

		/**
		 * Vanilla's goal claims no movement, so a zombie that lost sight of the player behind the
		 * door it is breaking (it forgets the target after about 3 s) would wander off mid-break.
		 * While it has no target it stays at the door; with a target it moves as the attack goal says.
		 */
		@Override
		public void tick() {
			super.tick();
			if (this.mob.getTarget() == null) {
				this.mob.getNavigation().stop();
			}
			// M09 breach: the door is gone; light-drawn mobs around re-check what they can reach.
			if (!(this.mob.level().getBlockState(this.doorPos).getBlock() instanceof DoorBlock)
				&& this.mob.level() instanceof net.minecraft.server.level.ServerLevel level) {
				Entrancement.reconsider(level, net.minecraft.world.phys.Vec3.atCenterOf(this.doorPos),
					io.github.boxofdog.simpleblock.tuning.Tuning.getInt("mob.breach.radius"));
			}
		}
	}

	/**
	 * Hitting a closed metal door between a pursuing zombie and its player: the zombie faces it,
	 * swings and the iron-door bang plays now and then, exactly like the wooden-door attack, but the
	 * door takes no damage and nothing around it changes.
	 */
	public static final class MetalDoorAttackGoal extends Goal {
		private static final int MAX_TICKS = 200;
		private final Mob mob;
		private BlockPos door;
		private int ticks;

		public MetalDoorAttackGoal(Mob mob) {
			this.mob = mob;
			this.setFlags(EnumSet.of(Goal.Flag.LOOK));
		}

		private BlockPos findDoor() {
			if (!isPursuingPlayer(this.mob) || this.mob.getTarget() != null && this.mob.distanceToSqr(this.mob.getTarget()) < 2.25) {
				return null;
			}
			BlockPos feet = this.mob.blockPosition();
			BlockPos best = null;
			double bestDistance = Double.MAX_VALUE;
			for (int dy = 0; dy <= 1; dy++) {
				for (Direction direction : Direction.Plane.HORIZONTAL) {
					BlockPos pos = feet.relative(direction).above(dy);
					if (isClosedMetalDoor(this.mob.level().getBlockState(pos))) {
						double distance = this.mob.getTarget() == null ? 0.0 : pos.distToCenterSqr(this.mob.getTarget().position());
						if (distance < bestDistance) {
							best = pos;
							bestDistance = distance;
						}
					}
				}
			}
			return best;
		}

		@Override
		public boolean canUse() {
			this.door = this.findDoor();
			return this.door != null;
		}

		@Override
		public void start() {
			this.ticks = 0;
		}

		@Override
		public boolean canContinueToUse() {
			return this.ticks < MAX_TICKS && isPursuingPlayer(this.mob) && isClosedMetalDoor(this.mob.level().getBlockState(this.door))
				&& this.door.closerToCenterThan(this.mob.position(), 2.5);
		}

		@Override
		public void tick() {
			this.ticks++;
			this.mob.getLookControl().setLookAt(this.door.getX() + 0.5, this.door.getY() + 0.5, this.door.getZ() + 0.5);
			if (this.mob.getRandom().nextInt(20) == 0) {
				this.mob.level().levelEvent(LevelEvent.SOUND_ZOMBIE_IRON_DOOR, this.door, 0);
				if (!this.mob.isSwinging()) {
					this.mob.swingForAttack(this.mob.getUsedItemHand());
				}
			}
		}
	}
}
