package io.github.boxofdog.simpleblock.client.cable;

import java.util.ArrayList;
import java.util.List;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

import org.jspecify.annotations.Nullable;

import io.github.boxofdog.simpleblock.cable.CableContent;
import io.github.boxofdog.simpleblock.cable.CableDrumBlockEntity;
import io.github.boxofdog.simpleblock.cable.CableRoutes;

/**
 * Draws a drum's laid-out cable: straight spans from the drum's exit through each anchor's roller.
 * The loose end from the last support to the hook is drawn by {@link CableHookRenderer}. Drawn
 * whether or not the drum itself is in view, since the route can run far from it.
 */
public class CableDrumRenderer implements BlockEntityRenderer<CableDrumBlockEntity, CableDrumRenderer.State> {
	public static class State extends BlockEntityRenderState {
		final List<CableGeometry.Piece> pieces = new ArrayList<>();
	}

	public CableDrumRenderer(BlockEntityRendererProvider.Context context) {
	}

	public static void register() {
		BlockEntityRenderers.register(CableContent.CABLE_DRUM_ENTITY, CableDrumRenderer::new);
	}

	@Override
	public State createRenderState() {
		return new State();
	}

	@Override
	public void extractRenderState(CableDrumBlockEntity drum, State state, float partialTicks, Vec3 cameraPosition,
		ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
		BlockEntityRenderer.super.extractRenderState(drum, state, partialTicks, cameraPosition, breakProgress);
		state.pieces.clear();
		if (drum.getLevel() == null) {
			return;
		}
		Vec3 origin = Vec3.atLowerCornerOf(drum.getBlockPos());
		Vec3 previous = CableRoutes.point(drum.getBlockPos(), true);
		for (BlockPos anchor : drum.route()) {
			Vec3 next = CableRoutes.point(anchor, false);
			CableGeometry.addRun(state.pieces, drum.getLevel(), origin, previous, next);
			previous = next;
		}
	}

	@Override
	public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
		CableGeometry.submit(state.pieces, poseStack, collector);
	}

	@Override
	public boolean shouldRenderOffScreen() {
		return true;
	}

	@Override
	public int getViewDistance() {
		return 160;
	}
}
