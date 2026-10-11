package io.github.boxofdog.simpleblock.mixin.phantom;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.levelgen.PhantomSpawner;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * SF-02: no phantoms from ordinary survival play.
 *
 * <p>{@link PhantomSpawner} (the insomnia spawner) is the only survival source of phantoms; they
 * are not in any biome spawn list or generated spawner. Its tick is skipped entirely, so not
 * sleeping carries no penalty. The phantom entity itself stays registered, so spawn eggs and
 * {@code /summon} still work for testing.
 */
@Mixin(PhantomSpawner.class)
abstract class PhantomSpawnerMixin {
	@Inject(method = "tick", at = @At("HEAD"), cancellable = true)
	private void simple_block$neverSpawnPhantoms(ServerLevel level, boolean spawnEnemies, CallbackInfo ci) {
		ci.cancel();
	}
}
