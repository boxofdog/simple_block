package io.github.boxofdog.simpleblock.client.train;

import com.mojang.blaze3d.vertex.PoseStack;

import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.block.BlockModelResolver;
import net.minecraft.client.renderer.entity.AbstractMinecartRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.MinecartRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.vehicle.minecart.MinecartFurnace;

import io.github.boxofdog.simpleblock.train.TrainContent;

/**
 * M05: draws vanilla's furnace minecart as the Cobblestone Engine. M07 (owner's brief section 5A): the
 * {@code block/cobblestone_engine} model, a furnace engine with its smokestack, sits inside the vanilla
 * minecart, whose own model and texture are drawn unchanged ({@code ModelLayers.MINECART}, the plain
 * minecart's layer); only vanilla's furnace block in the cart is left out. The drawing hook is
 * {@code mixin/AbstractMinecartRendererMixin}, with the cart's pose (following the rails, the slope and
 * the hurt wobble); vanilla's smoke while it burns is kept.
 */
public class CobblestoneEngineRenderer extends AbstractMinecartRenderer<MinecartFurnace, CobblestoneEngineRenderer.State> {
	/** The pose's origin is this far above the rail; the model's y 0 is the rail. */
	private static final float RAIL_DROP = -0.375F;

	public static class State extends MinecartRenderState {
		final BlockModelRenderState engine = new BlockModelRenderState();
	}

	private final BlockModelResolver blockModelResolver;

	public CobblestoneEngineRenderer(EntityRendererProvider.Context context) {
		super(context, ModelLayers.MINECART);
		this.blockModelResolver = context.getBlockModelResolver();
	}

	public static void register() {
		EntityRendererRegistry.register(EntityTypes.FURNACE_MINECART, CobblestoneEngineRenderer::new);
	}

	@Override
	public State createRenderState() {
		return new State();
	}

	@Override
	public void extractRenderState(MinecartFurnace entity, State state, float partialTicks) {
		super.extractRenderState(entity, state, partialTicks);
		state.displayBlockModel.clear();
		this.blockModelResolver.update(state.engine, TrainContent.ENGINE_MODEL.defaultBlockState(), AbstractMinecartRenderer.BLOCK_DISPLAY_CONTEXT);
	}

	/** Called with the cart's pose (y up, the cart's length along x) instead of the cart body. */
	public void submitEngine(MinecartRenderState state, PoseStack poseStack, SubmitNodeCollector collector) {
		if (!(state instanceof State engine)) {
			return;
		}
		poseStack.pushPose();
		poseStack.translate(-0.5F, RAIL_DROP, -0.5F);
		engine.engine.submit(poseStack, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, state.outlineColor);
		poseStack.popPose();
	}
}
