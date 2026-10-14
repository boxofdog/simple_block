package io.github.boxofdog.simpleblock.feature;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.BreakDoorGoal;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.Vec3;

import io.github.boxofdog.simpleblock.tuning.Tuning;

/**
 * M09: how a light-drawn mob ({@link Entrancement}) gets to the lit ground it is heading for, decided
 * once per wander decision when it is within a step of it. Physical, not omniscient: it only uses
 * what its own path search finds from where it stands.
 * <ol>
 *   <li><b>Reachable:</b> it walks there (open doors are open: vanilla paths pass through them).</li>
 *   <li><b>An open door nearby:</b> if it can't get in but can reach an open door within
 *       {@code mob.light_door.open_notice} blocks, it goes and stands in that doorway; mobs gather at
 *       doors left open.</li>
 *   <li><b>A closed wooden door right in its way:</b> if a path through closed wooden doors would get
 *       it there and the first such door is within {@code mob.light_door.notice_steps} steps, it takes
 *       that route for {@code mob.light_door.route_ticks} ticks: its path goes up to the door, and
 *       when it bumps into the door it breaks it ({@link LightDoorBreakGoal}, the M02 pace and rules:
 *       240 ticks, not on Peaceful, only with {@code mob_griefing}). A door further round the
 *       building is never sought out.</li>
 *   <li><b>Otherwise:</b> it walks as far as it can and loiters along the outside.</li>
 * </ol>
 * A mob with a target (a player it chases) does none of this: combat comes first.
 */
public final class LightRoutes {
	private static final Map<Mob, Long> DOOR_ROUTE = new WeakHashMap<>();
	/** For the tests: route decisions by outcome (reachable, open door nearby, closed wooden door, loiter). */
	public static long reachable;
	public static long openDoors;
	public static long doorRoutes;
	public static long loiters;

	private LightRoutes() {
	}

	/** Whether {@code mob} is taking a route through a closed wooden door now. */
	public static boolean doorRouteActive(Mob mob) {
		Long until = DOOR_ROUTE.get(mob);
		return until != null && mob.level().getGameTime() <= until && mob.getTarget() == null;
	}

	public static void clear(Mob mob) {
		DOOR_ROUTE.remove(mob);
	}

	/** Each server tick of a participating mob that is not of the zombie family (which has its own pursuit rule). */
	public static void tick(Mob mob) {
		mob.getNavigation().setCanOpenDoors(doorRouteActive(mob));
	}

	/** The destination for a mob a step away from the lit ground at {@code ground}, by the rules above. */
	public static @Nullable Vec3 route(PathfinderMob mob, ServerLevel level, BlockPos ground) {
		PathNavigation navigation = mob.getNavigation();
		boolean doors = navigation.getNodeEvaluator().canOpenDoors();
		try {
			navigation.setCanOpenDoors(false);
			Path plain = PathProbe.probe(mob, ground, 0);
			if (plain != null && plain.canReach()) {
				reachable++;
				clear(mob);
				return Vec3.atBottomCenterOf(ground);
			}
			BlockPos open = openDoorNearby(mob, level, plain);
			if (open != null) {
				openDoors++;
				clear(mob);
				return Vec3.atBottomCenterOf(open);
			}
			navigation.setCanOpenDoors(true);
			Path throughDoor = PathProbe.probe(mob, ground, 0);
			if (throughDoor != null && throughDoor.canReach()) {
				int notice = Tuning.getInt("mob.light_door.notice_steps");
				for (int i = 0; i < Math.min(throughDoor.getNodeCount(), notice + 1); i++) {
					Node node = throughDoor.getNode(i);
					if (isClosedWoodenDoor(level.getBlockState(node.asBlockPos())) || isClosedWoodenDoor(level.getBlockState(node.asBlockPos().above()))) {
						DOOR_ROUTE.put(mob, level.getGameTime() + Tuning.getInt("mob.light_door.route_ticks"));
						doorRoutes++;
						doors = true;
						return Vec3.atBottomCenterOf(ground);
					}
				}
			}
			loiters++;
			clear(mob);
			doors = false;
			return Vec3.atBottomCenterOf(ground);
		} finally {
			navigation.setCanOpenDoors(doors);
		}
	}

	private static boolean isClosedWoodenDoor(BlockState state) {
		return ZombieDoors.isWoodenDoor(state) && !state.getValue(DoorBlock.OPEN);
	}

	/**
	 * An open door (its lower half) the mob can reach, within the notice distance of it and near where
	 * its blocked path ends (where it would loiter), nearest that end first; null if none.
	 */
	private static @Nullable BlockPos openDoorNearby(PathfinderMob mob, ServerLevel level, @Nullable Path blocked) {
		BlockPos end = blocked != null && blocked.getEndNode() != null ? blocked.getEndNode().asBlockPos() : mob.blockPosition();
		int notice = Tuning.getInt("mob.light_door.open_notice");
		int r = 6;
		List<BlockPos> doors = new ArrayList<>();
		for (BlockPos pos : BlockPos.betweenClosed(end.offset(-r, -2, -r), end.offset(r, 2, r))) {
			BlockState state = level.getBlockState(pos);
			if (state.getBlock() instanceof DoorBlock && state.getValue(DoorBlock.OPEN) && state.getValue(DoorBlock.HALF) == DoubleBlockHalf.LOWER
				&& pos.closerToCenterThan(mob.position(), notice)) {
				doors.add(pos.immutable());
			}
		}
		doors.sort(Comparator.comparingDouble(pos -> pos.distSqr(end)));
		for (int i = 0; i < Math.min(3, doors.size()); i++) {
			BlockPos door = doors.get(i);
			if (door.closerToCenterThan(mob.position(), 1.5)) {
				// Standing in it already: stay there.
				return door;
			}
			Path path = PathProbe.probe(mob, door, 0);
			if (path != null && path.canReach()) {
				return door;
			}
		}
		return null;
	}

	/**
	 * M09: vanilla's door breaking for a light-drawn mob with no target that has bumped into the closed
	 * wooden door its route goes through. While it works at the door it stays there; when the door
	 * goes, mobs around re-check their routes ({@link Entrancement#reconsider}).
	 */
	public static final class LightDoorBreakGoal extends BreakDoorGoal {
		public LightDoorBreakGoal(Mob mob) {
			super(mob, ZombieDoors.BREAK_TICKS, difficulty -> difficulty != Difficulty.PEACEFUL);
		}

		@Override
		public boolean canUse() {
			return this.mob.getTarget() == null && doorRouteActive(this.mob) && super.canUse()
				&& ZombieDoors.isWoodenDoor(this.mob.level().getBlockState(this.doorPos));
		}

		@Override
		public boolean canContinueToUse() {
			return this.mob.getTarget() == null && super.canContinueToUse();
		}

		@Override
		public void tick() {
			super.tick();
			this.mob.getNavigation().stop();
			if (!(this.mob.level().getBlockState(this.doorPos).getBlock() instanceof DoorBlock) && this.mob.level() instanceof ServerLevel level) {
				clear(this.mob);
				Entrancement.reconsider(level, Vec3.atCenterOf(this.doorPos), Tuning.getInt("mob.breach.radius"));
			}
		}
	}
}
