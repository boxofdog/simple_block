package io.github.boxofdog.simpleblock.client.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.client.model.object.cart.MinecartModel;
import net.minecraft.client.renderer.entity.AbstractMinecartRenderer;

/** For the client tests: the body model a minecart renderer draws (which gates it opens). */
@Mixin(AbstractMinecartRenderer.class)
public interface AbstractMinecartRendererAccessor {
	@Accessor("model")
	MinecartModel simpleBlock$getModel();
}
