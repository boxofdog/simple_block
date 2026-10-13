package io.github.boxofdog.simpleblock.client.cable;

import java.util.ArrayList;
import java.util.List;

import com.mojang.blaze3d.vertex.PoseStack;

import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.block.BlockModelResolver;
import net.minecraft.client.renderer.entity.AbstractMinecartRenderer;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import io.github.boxofdog.simpleblock.cable.CableContent;
import io.github.boxofdog.simpleblock.cable.CableHook;

/**
 * The loose end: the hook model ({@code block/cable_hook}) and the cable from the last support down
 * to it. A held hook is drawn in the holder's hand, following it smoothly between ticks.
 */
public class CableHookRenderer extends EntityRenderer<CableHook, CableHookRenderer.State> {
	public static class State extends EntityRenderState {
		final BlockModelRenderState hook = new BlockModelRenderState();
		/** Where the hook is drawn, relative to the entity's position. */
		Vec3 hookOffset = Vec3.ZERO;
		int hookLight;
		final List<CableGeometry.Piece> pieces = new ArrayList<>();
	}

	private final BlockModelResolver blockModelResolver;

	public CableHookRenderer(EntityRendererProvider.Context context) {
		super(context);
		this.blockModelResolver = context.getBlockModelResolver();
	}

	public static void register() {
		EntityRendererRegistry.register(CableContent.CABLE_HOOK, CableHookRenderer::new);
	}

	@Override
	public State createRenderState() {
		return new State();
	}

	/** Drawn whenever it is in range: the cable to it can cross the view when the hook itself does not. */
	@Override
	protected boolean affectedByCulling(CableHook entity) {
		return false;
	}

	@Override
	public void extractRenderState(CableHook hook, State state, float partialTicks) {
		super.extractRenderState(hook, state, partialTicks);
		this.blockModelResolver.update(state.hook, CableContent.CABLE_HOOK_MODEL.defaultBlockState(), AbstractMinecartRenderer.BLOCK_DISPLAY_CONTEXT);
		Vec3 origin = new Vec3(state.x, state.y, state.z);
		Player holder = hook.holder();
		Vec3 at = holder != null ? hook.heldPosition(holder, partialTicks) : origin;
		state.hookOffset = at.subtract(origin);
		state.hookLight = LightCoordsUtil.getLightCoords(hook.level(), BlockPos.containing(at.add(0.0, 0.25, 0.0)));
		state.pieces.clear();
		// The texture carries on from the routed cable's: a free hook winched up or down moves its cable (M07 patch §9).
		CableDrumRenderer.RouteEnd end = CableDrumRenderer.routeEnd(hook.level(), hook.drumPos(), partialTicks);
		Vec3 from = hook.endPoint();
		CableGeometry.addRun(state.pieces, hook.level(), origin, from, hook.eyePoint(at), end == null ? 0.0 : end.vAt(from));
	}

	@Override
	public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
		CableGeometry.submit(state.pieces, poseStack, collector);
		poseStack.pushPose();
		poseStack.translate(state.hookOffset.x - 0.5, state.hookOffset.y, state.hookOffset.z - 0.5);
		state.hook.submit(poseStack, collector, state.hookLight, OverlayTexture.NO_OVERLAY, state.outlineColor);
		poseStack.popPose();
		super.submit(state, poseStack, collector, camera);
	}
}
