package io.github.boxofdog.simpleblock.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.object.cushion.CushionModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MinecartRenderer;
import net.minecraft.client.renderer.entity.state.CushionRenderState;
import net.minecraft.client.renderer.entity.state.MinecartRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.vehicle.minecart.AbstractMinecart;
import net.minecraft.world.entity.vehicle.minecart.Minecart;
import net.minecraft.world.item.DyeColor;

import org.jspecify.annotations.Nullable;

import io.github.boxofdog.simpleblock.feature.MinecartCargo;
import io.github.boxofdog.simpleblock.feature.MinecartCushions;

/**
 * The vanilla minecart renderer plus:
 * <ul>
 *   <li>FRT-08: a cargo label ("Raw Iron 180 / 256") while the player looks at a cargo cart;</li>
 *   <li>a fitted cushion ({@link MinecartCushions}), drawn with vanilla's own cushion model and
 *       texture, squeezed to fit between the cart's walls and resting on its floor. The drawing hook
 *       is {@code mixin/AbstractMinecartRendererMixin}.</li>
 * </ul>
 * The cart's model is unchanged; M04 owns the visible cargo.
 */
public class CargoMinecartRenderer extends MinecartRenderer {
	/** Inside the cart: 16 x 12 pixels between the walls; the cushion is 16 x 16, so it is narrowed. */
	private static final float CUSHION_LENGTH = 15.0F / 16.0F;
	private static final float CUSHION_WIDTH = 11.0F / 16.0F;
	/** The cart floor is 3 pixels below the pose's origin; the cushion model's base is 4 pixels below its own. */
	private static final float CUSHION_DROP = -1.0F / 16.0F;

	private final CushionModel cushionModel;

	public CargoMinecartRenderer(EntityRendererProvider.Context context) {
		super(context, ModelLayers.MINECART);
		this.cushionModel = new CushionModel(context.bakeLayer(ModelLayers.CUSHION));
	}

	/** Replaces the vanilla minecart renderer (same model layer, same look). */
	public static void register() {
		EntityRendererRegistry.register(EntityTypes.MINECART, CargoMinecartRenderer::new);
	}

	/** The minecart's render state plus the fitted cushion's texture (null for none). */
	public static class State extends MinecartRenderState {
		@Nullable Identifier cushion;
		final CushionRenderState cushionState = new CushionRenderState();
	}

	@Override
	public MinecartRenderState createRenderState() {
		return new State();
	}

	@Override
	public void extractRenderState(AbstractMinecart entity, MinecartRenderState state, float partialTicks) {
		super.extractRenderState(entity, state, partialTicks);
		DyeColor cushion = entity instanceof Minecart minecart ? MinecartCushions.fitted(minecart) : null;
		((State) state).cushion = cushion == null ? null : Identifier.withDefaultNamespace("textures/entity/cushion/" + cushion.getName() + "_cushion.png");
	}

	/** Called with the cart's pose (y up, origin above the cart floor), just before the cart body is drawn. */
	public void submitCushion(MinecartRenderState state, PoseStack poseStack, SubmitNodeCollector collector) {
		if (!(state instanceof State cart) || cart.cushion == null) {
			return;
		}
		poseStack.pushPose();
		// Like vanilla's CushionRenderer: flip into model space, then seat the model's base on the cart floor.
		poseStack.rotateDegrees(Axis.XP, 180.0F);
		poseStack.translate(0.0F, CUSHION_DROP, 0.0F);
		poseStack.scale(CUSHION_LENGTH, 1.0F, CUSHION_WIDTH);
		collector.submitModel(this.cushionModel, cart.cushionState, poseStack, this.cushionModel.renderType(cart.cushion), state.lightCoords,
			OverlayTexture.NO_OVERLAY, state.outlineColor);
		poseStack.popPose();
	}

	@Override
	protected boolean shouldShowName(AbstractMinecart entity, double distanceToCameraSq) {
		boolean cargoCart = entity instanceof Minecart minecart && !MinecartCushions.isFitted(minecart);
		return cargoCart && entity == this.entityRenderDispatcher.crosshairPickEntity || super.shouldShowName(entity, distanceToCameraSq);
	}

	@Override
	protected @Nullable Component getNameTag(AbstractMinecart entity) {
		if (!(entity instanceof Minecart minecart) || MinecartCushions.isFitted(minecart)) {
			return super.getNameTag(entity);
		}
		Component status = MinecartCargo.status(MinecartCargo.cart(minecart));
		return minecart.hasCustomName() ? Component.empty().append(minecart.getCustomName()).append(" - ").append(status) : status;
	}
}
