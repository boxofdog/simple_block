package io.github.boxofdog.simpleblock.client.train;

import com.mojang.blaze3d.vertex.PoseStack;

import org.jspecify.annotations.Nullable;

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
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.vehicle.minecart.AbstractMinecart;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityTypes;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import io.github.boxofdog.simpleblock.train.FreightModelBlock;
import io.github.boxofdog.simpleblock.train.FreightPorts;
import io.github.boxofdog.simpleblock.train.LoadingTracks;
import io.github.boxofdog.simpleblock.train.TrainContent;

/**
 * M15: draws the lid over a freight hopper's opening while it is capped ({@link FreightPorts}): over the
 * spout of a hopper supplying a Loading Rail below it, over the top of one receiving from a rail beside it.
 * A button-controlled hopper's lid stands open while a wagon's transfer through it, opened by the button,
 * is under way (the wagon's synced port). Vanilla hoppers have no renderer of their own, so this adds
 * nothing to a hopper that is not a freight hopper.
 */
public class FreightHopperLidRenderer implements BlockEntityRenderer<HopperBlockEntity, FreightHopperLidRenderer.State> {
	public static class State extends BlockEntityRenderState {
		final BlockModelRenderState spout = new BlockModelRenderState();
		final BlockModelRenderState top = new BlockModelRenderState();
		boolean capSpout;
		boolean capTop;
	}

	private final BlockModelResolver blockModelResolver;

	public FreightHopperLidRenderer(BlockEntityRendererProvider.Context context) {
		this.blockModelResolver = context.blockModelResolver();
	}

	public static void register() {
		BlockEntityRenderers.register(BlockEntityTypes.HOPPER, FreightHopperLidRenderer::new);
	}

	@Override
	public State createRenderState() {
		return new State();
	}

	@Override
	public void extractRenderState(HopperBlockEntity hopper, State state, float partialTicks, Vec3 cameraPosition,
		ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
		BlockEntityRenderer.super.extractRenderState(hopper, state, partialTicks, cameraPosition, breakProgress);
		state.capSpout = false;
		state.capTop = false;
		Level level = hopper.getLevel();
		if (level == null) {
			return;
		}
		BlockPos pos = hopper.getBlockPos();
		BlockState blockState = hopper.getBlockState();
		boolean supplies = FreightPorts.railBelow(level, pos, blockState) != null;
		boolean receives = FreightPorts.receives(level, pos);
		if (!supplies && !receives) {
			return;
		}
		boolean capped = switch (FreightPorts.control(level, pos)) {
			case OPEN -> false;
			case CAPPED -> true;
			case BUTTON -> !openedByButton(level, pos);
		};
		state.capSpout = capped && supplies;
		state.capTop = capped && receives;
		if (state.capSpout) {
			this.blockModelResolver.update(state.spout, TrainContent.FREIGHT_MODEL.part(FreightModelBlock.Part.CAP_SPOUT),
				AbstractMinecartRenderer.BLOCK_DISPLAY_CONTEXT);
		}
		if (state.capTop) {
			this.blockModelResolver.update(state.top, TrainContent.FREIGHT_MODEL.part(FreightModelBlock.Part.CAP_TOP),
				AbstractMinecartRenderer.BLOCK_DISPLAY_CONTEXT);
		}
	}

	/** Whether a wagon near the hopper has it as its button-opened port. */
	private static boolean openedByButton(Level level, BlockPos pos) {
		AABB near = new AABB(pos).inflate(2.0, FreightPorts.MAX_CLEARANCE + 2.0, 2.0);
		return !level.getEntitiesOfClass(AbstractMinecart.class, near, cart -> pos.equals(LoadingTracks.freight(cart).simpleBlock$port())).isEmpty();
	}

	@Override
	public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
		if (state.capSpout) {
			state.spout.submit(poseStack, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
		}
		if (state.capTop) {
			state.top.submit(poseStack, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
		}
	}
}
