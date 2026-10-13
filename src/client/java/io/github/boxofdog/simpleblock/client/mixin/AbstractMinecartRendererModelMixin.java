package io.github.boxofdog.simpleblock.client.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.object.cart.MinecartModel;
import net.minecraft.client.renderer.entity.AbstractMinecartRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;

import io.github.boxofdog.simpleblock.client.train.GatedMinecartModel;

/**
 * Owner's M07 cart redesign: every minecart renderer draws the same body, {@link GatedMinecartModel}
 * (vanilla's floor and texture, a frame of corner columns and side rims, four hinged gates), instead of
 * vanilla's five-box model. Vanilla builds its model in the constructor with no hook to choose it.
 */
@Mixin(AbstractMinecartRenderer.class)
abstract class AbstractMinecartRendererModelMixin {
	@Shadow
	@Final
	@Mutable
	protected MinecartModel model;

	@Inject(method = "<init>", at = @At("RETURN"))
	private void simpleBlock$gatedBody(EntityRendererProvider.Context context, ModelLayerLocation layer, CallbackInfo ci) {
		this.model = new GatedMinecartModel(context.bakeLayer(GatedMinecartModel.LAYER));
	}
}
