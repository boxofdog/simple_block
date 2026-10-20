package io.github.boxofdog.simpleblock.mixin.inventory;

import net.minecraft.world.CompoundContainer;
import net.minecraft.world.Container;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** M13: a double chest's two halves, to tell a chest's storage from other containers ({@code feature/ChestSort}). */
@Mixin(CompoundContainer.class)
public interface CompoundContainerAccessor {
	@Accessor("container1")
	Container simpleBlock$first();

	@Accessor("container2")
	Container simpleBlock$second();
}
