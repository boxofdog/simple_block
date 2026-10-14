package io.github.boxofdog.simpleblock.client.effigy;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.block.BlockModelResolver;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.entity.AbstractMinecartRenderer;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.phys.Vec3;

import org.jspecify.annotations.Nullable;

import io.github.boxofdog.simpleblock.effigy.EffigyBlockEntity;
import io.github.boxofdog.simpleblock.effigy.EffigyContent;

/**
 * M12: draws an effigy's figure ({@code block/effigy/<type>}, two blocks tall, modelled facing south)
 * turned about the middle of its block to the effigy's angle, so it faces the way its fear cone points.
 * The effigy blocks' own models are empty (only their particle texture).
 */
public class EffigyRenderer implements BlockEntityRenderer<EffigyBlockEntity, EffigyRenderer.State> {
	public static class State extends BlockEntityRenderState {
		final BlockModelRenderState figure = new BlockModelRenderState();
		float yaw;
	}

	private final BlockModelResolver blockModelResolver;

	public EffigyRenderer(BlockEntityRendererProvider.Context context) {
		this.blockModelResolver = context.blockModelResolver();
	}

	public static void register() {
		BlockEntityRenderers.register(EffigyContent.EFFIGY_ENTITY, EffigyRenderer::new);
	}

	@Override
	public State createRenderState() {
		return new State();
	}

	@Override
	public void extractRenderState(EffigyBlockEntity effigy, State state, float partialTicks, Vec3 cameraPosition,
		ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
		BlockEntityRenderer.super.extractRenderState(effigy, state, partialTicks, cameraPosition, breakProgress);
		state.yaw = effigy.yaw();
		this.blockModelResolver.update(state.figure, EffigyContent.FIGURE_MODEL.figure(effigy.type()), AbstractMinecartRenderer.BLOCK_DISPLAY_CONTEXT);
	}

	@Override
	public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
		poseStack.pushPose();
		poseStack.translate(0.5F, 0.0F, 0.5F);
		// Model south (+z) to the effigy's facing (-sin yaw, 0, cos yaw).
		poseStack.rotateDegrees(Axis.YP, -state.yaw);
		poseStack.translate(-0.5F, 0.0F, -0.5F);
		state.figure.submit(poseStack, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
		poseStack.popPose();
	}

	/** Two blocks tall, wider than its block when turned: drawn whenever its block's chunk is. */
	@Override
	public boolean shouldRenderOffScreen() {
		return true;
	}
}
