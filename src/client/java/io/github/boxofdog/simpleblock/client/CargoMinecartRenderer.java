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
import net.minecraft.world.phys.Vec3;
import net.minecraft.core.Direction;

import org.jspecify.annotations.Nullable;

import io.github.boxofdog.simpleblock.cable.CableContent;
import io.github.boxofdog.simpleblock.cable.ConnectingRodPartBlock;
import io.github.boxofdog.simpleblock.cable.ConnectingRods;
import io.github.boxofdog.simpleblock.feature.MinecartCargo;
import io.github.boxofdog.simpleblock.feature.MinecartCushions;
import io.github.boxofdog.simpleblock.train.FreightGatePartBlock;
import io.github.boxofdog.simpleblock.train.LoadingTracks;
import io.github.boxofdog.simpleblock.train.TrainContent;

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
 *   <li>M06: a freight cart's folding front gate ({@code block/freight_gate_closed}; M07: folded down as a
 *       ramp, {@code block/freight_gate_open}, while it tips its load into a skip), on the end wall
 *       at the cart's front ({@link LoadingTracks}): the end that faces the loading track's way while
 *       docked. Always closed in M06.</li>
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
	/** The gate's hinge: outside the end wall, at the underside of the cart floor, one side of the cart. */
	private static final float GATE_X = 10.0F / 16.0F;
	private static final float GATE_Y = -5.0F / 16.0F;
	private static final float GATE_Z = -8.0F / 16.0F;

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
		/** Which end the front gate is on, along the cart's own x axis: +1, -1, or 0 for none. */
		int gate;
		final BlockModelRenderState gateModel = new BlockModelRenderState();
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
		Direction front = LoadingTracks.isFreight(entity) ? LoadingTracks.freight(entity).simpleBlock$front() : null;
		cart.gate = front == null ? 0 : gateEnd(state, front);
		if (cart.gate != 0) {
			this.blockModelResolver.update(cart.gateModel,
				TrainContent.FREIGHT_GATE_MODEL.defaultBlockState().setValue(FreightGatePartBlock.PART,
					LoadingTracks.freight(entity).simpleBlock$gateOpen() ? FreightGatePartBlock.Part.OPEN : FreightGatePartBlock.Part.CLOSED),
				AbstractMinecartRenderer.BLOCK_DISPLAY_CONTEXT);
		}
	}

	/**
	 * Which end of the cart's own x axis points to {@code front} in the world: the cart is turned about
	 * y the way {@link AbstractMinecartRenderer} turns it (new behaviour: its y rotation; old: from the
	 * rail under it).
	 */
	private static int gateEnd(MinecartRenderState state, Direction front) {
		float turn;
		if (state.isNewRender) {
			turn = state.yRot;
		} else {
			float rotation = state.yRot;
			if (state.posOnRail != null && state.frontPos != null && state.backPos != null) {
				Vec3 direction = state.backPos.subtract(state.frontPos);
				if (direction.length() != 0.0) {
					rotation = (float) Math.toDegrees(Math.atan2(direction.z, direction.x));
				}
			}
			turn = 180.0F - rotation;
		}
		double radians = Math.toRadians(turn);
		// The cart's +x in the world after turning by "turn" about y.
		double along = Math.cos(radians) * front.getStepX() - Math.sin(radians) * front.getStepZ();
		return along >= 0.0 ? 1 : -1;
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

	/** M06: the front gate, with the cart's pose (as {@link #submitCushion}). */
	public void submitGate(MinecartRenderState state, PoseStack poseStack, SubmitNodeCollector collector) {
		if (!(state instanceof State cart) || cart.gate == 0) {
			return;
		}
		poseStack.pushPose();
		if (cart.gate < 0) {
			poseStack.rotateDegrees(Axis.YP, 180.0F);
		}
		poseStack.translate(GATE_X, GATE_Y, GATE_Z);
		cart.gateModel.submit(poseStack, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, state.outlineColor);
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
