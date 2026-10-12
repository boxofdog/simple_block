package io.github.boxofdog.simpleblock.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.object.cushion.CushionModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.block.BlockModelResolver;
import net.minecraft.client.renderer.entity.AbstractMinecartRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MinecartRenderer;
import net.minecraft.client.renderer.entity.state.CushionRenderState;
import net.minecraft.client.renderer.entity.state.MinecartRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.vehicle.minecart.AbstractMinecart;
import net.minecraft.world.entity.vehicle.minecart.Minecart;
import net.minecraft.world.item.DyeColor;

import org.jspecify.annotations.Nullable;

import io.github.boxofdog.simpleblock.cable.CableContent;
import io.github.boxofdog.simpleblock.cable.ConnectingRodPartBlock;
import io.github.boxofdog.simpleblock.cable.ConnectingRods;
import io.github.boxofdog.simpleblock.feature.MinecartCargo;
import io.github.boxofdog.simpleblock.feature.MinecartCushions;

/**
 * The vanilla minecart renderer plus:
 * <ul>
 *   <li>FRT-08: a cargo label ("Raw Iron 180 / 256") while the player looks at a cargo cart;</li>
 *   <li>a fitted cushion ({@link MinecartCushions}), drawn with vanilla's own cushion model and
 *       texture, squeezed to fit between the cart's walls and resting on its floor. The drawing hook
 *       is {@code mixin/AbstractMinecartRendererMixin}.</li>
 *   <li>M04: a fitted connecting rod ({@link ConnectingRods}), built from the
 *       {@code block/connecting_rod_*} models: the base on the cart, the shaft stretched up to the cable
 *       it grips (or folded down to {@code cable.rod_reach_min} with the head open), and the head. It
 *       stands upright whatever the cart's tilt, with its stem on the side away from the anchors'
 *       hanger arms (east of a north-south track, south of an east-west one).</li>
 * </ul>
 * The cart's model is unchanged; M04 owns the visible cargo.
 */
public class CargoMinecartRenderer extends MinecartRenderer {
	/** Inside the cart: 16 x 12 pixels between the walls; the cushion is 16 x 16, so it is narrowed. */
	private static final float CUSHION_LENGTH = 15.0F / 16.0F;
	private static final float CUSHION_WIDTH = 11.0F / 16.0F;
	/** The cart floor is 3 pixels below the pose's origin; the cushion model's base is 4 pixels below its own. */
	private static final float CUSHION_DROP = -1.0F / 16.0F;

	/** Height of the rod's base above the cart's position on the rail, and of the base model. */
	private static final float ROD_BASE_Y = 6.0F / 16.0F;
	private static final float ROD_BASE_HEIGHT = 5.0F / 16.0F;
	/** Height of the gripped cable's centre in the head model. */
	private static final float ROD_HEAD_GRIP_Y = 4.0F / 16.0F;

	private final CushionModel cushionModel;
	private final BlockModelResolver blockModelResolver;

	public CargoMinecartRenderer(EntityRendererProvider.Context context) {
		super(context, ModelLayers.MINECART);
		this.cushionModel = new CushionModel(context.bakeLayer(ModelLayers.CUSHION));
		this.blockModelResolver = context.getBlockModelResolver();
	}

	/** Replaces the vanilla minecart renderer (same model layer, same look). */
	public static void register() {
		EntityRendererRegistry.register(EntityTypes.MINECART, CargoMinecartRenderer::new);
	}

	/** The minecart's render state plus the fitted cushion's texture (null for none). */
	public static class State extends MinecartRenderState {
		@Nullable Identifier cushion;
		final CushionRenderState cushionState = new CushionRenderState();
		boolean rod;
		/** Height of the rod's grip above the cart: the gripped cable's, or the folded-down minimum. */
		float rodReach;
		/** Turn the rod a quarter (the cart runs east-west). */
		boolean rodAcross;
		final BlockModelRenderState rodBase = new BlockModelRenderState();
		final BlockModelRenderState rodShaft = new BlockModelRenderState();
		final BlockModelRenderState rodHead = new BlockModelRenderState();
	}

	@Override
	public MinecartRenderState createRenderState() {
		return new State();
	}

	@Override
	public void extractRenderState(AbstractMinecart entity, MinecartRenderState state, float partialTicks) {
		super.extractRenderState(entity, state, partialTicks);
		DyeColor cushion = entity instanceof Minecart minecart ? MinecartCushions.fitted(minecart) : null;
		State cart = (State) state;
		cart.cushion = cushion == null ? null : Identifier.withDefaultNamespace("textures/entity/cushion/" + cushion.getName() + "_cushion.png");
		cart.rod = entity instanceof Minecart minecart && ConnectingRods.hasRod(minecart);
		if (cart.rod) {
			boolean gripping = ConnectingRods.isGripping((Minecart) entity);
			cart.rodReach = gripping ? ConnectingRods.fitted((Minecart) entity).simpleBlock$reach() : (float) ConnectingRods.reachMin();
			float yaw = ((state.yRot % 180.0F) + 180.0F) % 180.0F;
			cart.rodAcross = yaw < 45.0F || yaw >= 135.0F;
			this.rodPart(cart.rodBase, ConnectingRodPartBlock.Part.BASE);
			this.rodPart(cart.rodShaft, ConnectingRodPartBlock.Part.SHAFT);
			this.rodPart(cart.rodHead, gripping ? ConnectingRodPartBlock.Part.HEAD_CLOSED : ConnectingRodPartBlock.Part.HEAD_OPEN);
		}
	}

	private void rodPart(BlockModelRenderState model, ConnectingRodPartBlock.Part part) {
		this.blockModelResolver.update(model, CableContent.CONNECTING_ROD_MODEL.defaultBlockState().setValue(ConnectingRodPartBlock.PART, part),
			AbstractMinecartRenderer.BLOCK_DISPLAY_CONTEXT);
	}

	@Override
	public void submit(MinecartRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
		super.submit(state, poseStack, collector, camera);
		if (state instanceof State cart && cart.rod) {
			this.submitRod(cart, poseStack, collector);
		}
	}

	/** The rod upright from the cart's position: base, shaft up to the grip, head. */
	private void submitRod(State cart, PoseStack poseStack, SubmitNodeCollector collector) {
		float baseTop = ROD_BASE_Y + ROD_BASE_HEIGHT;
		float headY = Math.max(baseTop, cart.rodReach - ROD_HEAD_GRIP_Y);
		poseStack.pushPose();
		if (cart.rodAcross) {
			// Model east (the stem's side) to world south.
			poseStack.rotateDegrees(Axis.YP, -90.0F);
		}
		poseStack.translate(-0.5F, 0.0F, -0.5F);
		poseStack.pushPose();
		poseStack.translate(0.0F, ROD_BASE_Y, 0.0F);
		cart.rodBase.submit(poseStack, collector, cart.lightCoords, OverlayTexture.NO_OVERLAY, cart.outlineColor);
		poseStack.popPose();
		poseStack.pushPose();
		poseStack.translate(0.0F, baseTop, 0.0F);
		poseStack.scale(1.0F, headY - baseTop, 1.0F);
		cart.rodShaft.submit(poseStack, collector, cart.lightCoords, OverlayTexture.NO_OVERLAY, cart.outlineColor);
		poseStack.popPose();
		poseStack.translate(0.0F, headY, 0.0F);
		cart.rodHead.submit(poseStack, collector, cart.lightCoords, OverlayTexture.NO_OVERLAY, cart.outlineColor);
		poseStack.popPose();
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
