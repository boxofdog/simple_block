package io.github.boxofdog.simpleblock.client;

import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MinecartRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.vehicle.minecart.AbstractMinecart;
import net.minecraft.world.entity.vehicle.minecart.Minecart;

import org.jspecify.annotations.Nullable;

import io.github.boxofdog.simpleblock.feature.MinecartCargo;

/**
 * FRT-08: the vanilla minecart renderer plus a cargo label ("Raw Iron 180 / 256") while the
 * player looks at the cart. The cart's model is unchanged; M04 owns the visible cargo.
 */
public class CargoMinecartRenderer extends MinecartRenderer {
	public CargoMinecartRenderer(EntityRendererProvider.Context context) {
		super(context, ModelLayers.MINECART);
	}

	/** Replaces the vanilla minecart renderer (same model layer, same look). */
	public static void register() {
		EntityRendererRegistry.register(EntityTypes.MINECART, CargoMinecartRenderer::new);
	}

	@Override
	protected boolean shouldShowName(AbstractMinecart entity, double distanceToCameraSq) {
		return entity == this.entityRenderDispatcher.crosshairPickEntity || super.shouldShowName(entity, distanceToCameraSq);
	}

	@Override
	protected @Nullable Component getNameTag(AbstractMinecart entity) {
		if (!(entity instanceof Minecart minecart)) {
			return super.getNameTag(entity);
		}
		Component status = MinecartCargo.status(MinecartCargo.cart(minecart));
		return minecart.hasCustomName() ? Component.empty().append(minecart.getCustomName()).append(" - ").append(status) : status;
	}
}
