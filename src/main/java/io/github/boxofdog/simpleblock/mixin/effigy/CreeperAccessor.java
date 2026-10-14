package io.github.boxofdog.simpleblock.mixin.effigy;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.world.entity.monster.Creeper;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** M12: a frightened creeper's fuse is put out, flint-and-steel ignition included ({@code Effigies}). */
@Mixin(Creeper.class)
public interface CreeperAccessor {
	@Accessor("DATA_IS_IGNITED")
	static EntityDataAccessor<Boolean> simpleBlock$ignited() {
		throw new AssertionError();
	}
}
