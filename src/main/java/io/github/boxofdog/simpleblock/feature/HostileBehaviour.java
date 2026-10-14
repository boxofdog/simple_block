package io.github.boxofdog.simpleblock.feature;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.GoalSelector;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.util.GoalUtils;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.skeleton.Skeleton;

import io.github.boxofdog.simpleblock.mixin.mobs.MobAccessor;

/**
 * M09: hooks the milestone's goals onto mobs as they enter a server world (goals are not saved, so
 * this runs for every load; a mob loaded again with its chunk keeps the goals it has), and creeper
 * retaliation onto damage:
 * <ul>
 *   <li>every light-drawn ground hostile ({@link Entrancement#participates}, with a wander goal) gets
 *       {@link LightRoutes.LightDoorBreakGoal};</li>
 *   <li>creepers get {@link CreeperRetaliation.RetaliationGoal};</li>
 *   <li>ordinary skeletons get {@link SpiderJockeys.RecruitGoal}.</li>
 * </ul>
 */
public final class HostileBehaviour {
	private HostileBehaviour() {
	}

	/** Never runs: marks a mob whose M09 goals are in place. */
	private static final class Marker extends Goal {
		@Override
		public boolean canUse() {
			return false;
		}
	}

	public static void register() {
		ServerEntityEvents.ENTITY_LOAD.register((entity, level) -> {
			if (!(entity instanceof Mob mob)) {
				return;
			}
			GoalSelector goals = ((MobAccessor) mob).simpleBlock$getGoalSelector();
			// A mob is loaded again whenever its chunk is: add the goals once.
			if (goals.getAvailableGoals().stream().anyMatch(goal -> goal.getGoal() instanceof Marker)) {
				return;
			}
			goals.addGoal(8, new Marker());
			if (Entrancement.participates(mob) && GoalUtils.hasGroundPathNavigation(mob)
				&& goals.getAvailableGoals().stream().anyMatch(goal -> goal.getGoal() instanceof RandomStrollGoal)) {
				goals.addGoal(1, new LightRoutes.LightDoorBreakGoal(mob));
			}
			if (mob instanceof Creeper creeper) {
				goals.addGoal(2, new CreeperRetaliation.RetaliationGoal(creeper));
			}
			if (SpiderJockeys.isOrdinarySkeleton(mob)) {
				goals.addGoal(5, new SpiderJockeys.RecruitGoal((Skeleton) mob));
			}
		});
		ServerLivingEntityEvents.AFTER_DAMAGE.register((entity, source, baseDamage, damageTaken, blocked) ->
			CreeperRetaliation.onDamaged(entity, source));
	}
}
