package io.github.boxofdog.simpleblock.feature;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.entity.monster.Creeper;

import io.github.boxofdog.simpleblock.tuning.Tuning;

/**
 * MOB-03: a creeper in direct sunlight dissolves in a short puff of smoke and ash instead of
 * staying dangerous. No explosion, no damage, no drops: after the effect it is simply removed.
 *
 * <p>"Direct sunlight" is exactly the test vanilla uses to set zombies and skeletons on fire
 * ({@code Mob.isSunBurnTick}): the {@code monsters_burn} environment attribute is on at its position
 * (Overworld daytime), its light-based brightness is above 0.5, it is not in water, rain or powder
 * snow, it can see the sky from eye height, and the same small random chance per tick, so creepers
 * coming into the sun go one by one rather than all at once. Rain and thunderstorms therefore
 * protect them, as they protect zombies. Caves, roofs, night, torches and lava never trigger it.
 *
 * <p>The effect lasts {@code mob.creeper.evaporation_ticks} ticks (10 = 0.5 s): the creeper stops
 * (its fuse is put out), fizzes and gives off smoke and ash from its body, then vanishes with a
 * final puff. The server sends the particles, so every client sees the same thing once.
 * Applied in {@code mixin/mobs/CreeperMixin}.
 */
public final class CreeperEvaporation {
	/** Implemented by creepers (mixin): ticks into the effect, or -1 while not evaporating. */
	public interface Evaporating {
		int simpleBlock$evaporationTicks();
	}

	private CreeperEvaporation() {
	}

	public static boolean isEvaporating(Creeper creeper) {
		return creeper instanceof Evaporating evaporating && evaporating.simpleBlock$evaporationTicks() >= 0;
	}

	public static boolean inDirectSunlight(Creeper creeper, ServerLevel level) {
		if (!level.environmentAttributes().getValue(EnvironmentAttributes.MONSTERS_BURN, creeper.position())) {
			return false;
		}
		float brightness = creeper.getLightLevelDependentMagicValue();
		boolean sheltered = creeper.isInWaterOrRain() || creeper.isInPowderSnow || creeper.wasInPowderSnow;
		BlockPos eyes = BlockPos.containing(creeper.getX(), creeper.getEyeY(), creeper.getZ());
		return brightness > 0.5F && creeper.getRandom().nextFloat() * 30.0F < (brightness - 0.4F) * 2.0F && !sheltered && level.canSeeSky(eyes);
	}

	public static int duration() {
		return Math.max(1, Tuning.getInt("mob.creeper.evaporation_ticks"));
	}

	public static void start(Creeper creeper, ServerLevel level) {
		creeper.setSwellDir(-1);
		creeper.getNavigation().stop();
		level.playSound(null, creeper.getX(), creeper.getY(), creeper.getZ(), SoundEvents.FIRE_EXTINGUISH, SoundSource.HOSTILE, 0.6F, 1.4F);
	}

	public static void smoke(Creeper creeper, ServerLevel level) {
		double width = creeper.getBbWidth();
		double height = creeper.getBbHeight();
		level.sendParticles(ParticleTypes.LARGE_SMOKE, creeper.getX(), creeper.getY() + height * 0.5, creeper.getZ(), 3,
			width * 0.35, height * 0.3, width * 0.35, 0.01);
		level.sendParticles(ParticleTypes.WHITE_ASH, creeper.getX(), creeper.getY() + height * 0.5, creeper.getZ(), 8,
			width * 0.4, height * 0.35, width * 0.4, 0.02);
	}

	public static void finish(Creeper creeper, ServerLevel level) {
		level.sendParticles(ParticleTypes.POOF, creeper.getX(), creeper.getY() + creeper.getBbHeight() * 0.5, creeper.getZ(), 16,
			creeper.getBbWidth() * 0.4, creeper.getBbHeight() * 0.35, creeper.getBbWidth() * 0.4, 0.03);
		creeper.discard();
	}
}
