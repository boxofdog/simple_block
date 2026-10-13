package io.github.boxofdog.simpleblock.client.cable;

import java.util.ArrayList;
import java.util.List;

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
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import org.jspecify.annotations.Nullable;

import io.github.boxofdog.simpleblock.cable.CableContent;
import io.github.boxofdog.simpleblock.cable.CableDrumBlock;
import io.github.boxofdog.simpleblock.cable.CableDrumBlockEntity;
import io.github.boxofdog.simpleblock.cable.CableDrumPartBlock;
import io.github.boxofdog.simpleblock.cable.CableLoops;

/**
 * Draws a drum's turning parts and its laid-out cable.
 *
 * <ul>
 *   <li>The spool (bare or wound) and the hand crank ({@code block/cable_drum_spool*},
 *       {@code block/cable_drum_crank}), turned about the axle by the distance the cable has run;
 *       the block model is only the frame. A coal winch (M06) has, instead of the crank, the lever
 *       ({@code block/cable_drum_lever}), upright at NEUTRAL, leaning back at PULL and forward at
 *       FEED; its furnace motor (M07) is part of the frame's block model, in place of the motor-side
 *       stand ({@code block/cable_drum_frame_powered}).</li>
 *   <li>The working strand: straight spans from the drum's exit through each anchor's roller. On a
 *       closed loop (M05) it carries on round the return wheel and back to the drum as the return
 *       strand, and the cable's texture moves along with the cable.</li>
 * </ul>
 * The loose end from the last support to the hook is drawn by {@link CableHookRenderer}. Drawn whether
 * or not the drum itself is in view, since the route can run far from it.
 */
public class CableDrumRenderer implements BlockEntityRenderer<CableDrumBlockEntity, CableDrumRenderer.State> {
	/** Radius of the wound barrel, in blocks: the cable's run turns the spool by run / radius. */
	private static final float SPOOL_RADIUS = 4.5F / 16.0F;
	private static final float AXLE_Y = 9.5F / 16.0F;
	private static final float AXLE_Z = 8.0F / 16.0F;
	private static final float LEVER_TILT = 35.0F;

	public static class State extends BlockEntityRenderState {
		final List<CableGeometry.Piece> pieces = new ArrayList<>();
		final BlockModelRenderState spool = new BlockModelRenderState();
		final BlockModelRenderState crank = new BlockModelRenderState();
		final BlockModelRenderState lever = new BlockModelRenderState();
		boolean powered;
		/** The lever's tilt about the axle, degrees (positive leans it back, away from the cable). */
		float leverAngle;
		float facingDegrees;
		boolean hanging;
		/** Spool and crank angle, degrees. */
		float angle;
	}

	private final BlockModelResolver blockModelResolver;

	public CableDrumRenderer(BlockEntityRendererProvider.Context context) {
		this.blockModelResolver = context.blockModelResolver();
	}

	public static void register() {
		BlockEntityRenderers.register(CableContent.CABLE_DRUM_ENTITY, CableDrumRenderer::new);
	}

	@Override
	public State createRenderState() {
		return new State();
	}

	private static BlockState part(CableDrumPartBlock.Part part) {
		return CableContent.DRUM_PART_MODEL.defaultBlockState().setValue(CableDrumPartBlock.PART, part);
	}

	@Override
	public void extractRenderState(CableDrumBlockEntity drum, State state, float partialTicks, Vec3 cameraPosition,
		ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
		BlockEntityRenderer.super.extractRenderState(drum, state, partialTicks, cameraPosition, breakProgress);
		BlockState blockState = drum.getBlockState();
		Direction facing = blockState.hasProperty(CableDrumBlock.FACING) ? blockState.getValue(CableDrumBlock.FACING) : Direction.NORTH;
		state.facingDegrees = facing.toYRot() - 180.0F;
		state.hanging = drum.hanging();
		boolean wound = blockState.hasProperty(CableDrumBlock.WOUND) && blockState.getValue(CableDrumBlock.WOUND);
		this.blockModelResolver.update(state.spool, part(wound ? CableDrumPartBlock.Part.SPOOL_WOUND : CableDrumPartBlock.Part.SPOOL),
			AbstractMinecartRenderer.BLOCK_DISPLAY_CONTEXT);
		this.blockModelResolver.update(state.crank, part(CableDrumPartBlock.Part.CRANK), AbstractMinecartRenderer.BLOCK_DISPLAY_CONTEXT);
		this.blockModelResolver.update(state.lever, part(CableDrumPartBlock.Part.LEVER), AbstractMinecartRenderer.BLOCK_DISPLAY_CONTEXT);
		state.powered = drum.powered();
		state.leverAngle = switch (drum.lever()) {
			case NEUTRAL -> 0.0F;
			case PULL -> LEVER_TILT;
			case FEED -> -LEVER_TILT;
		};
		float travel = Mth.lerp(partialTicks, drum.previousTravel, drum.travel);
		state.angle = (float) Math.toDegrees(-travel / SPOOL_RADIUS);

		state.pieces.clear();
		if (drum.getLevel() == null) {
			return;
		}
		Vec3 origin = Vec3.atLowerCornerOf(drum.getBlockPos());
		List<Vec3> path = new ArrayList<>(drum.points());
		Direction outward = drum.looped() ? CableLoops.outward(drum) : null;
		double v = 0.0;
		if (outward != null) {
			List<Vec3> wheel = CableLoops.wheelPath(path.getLast(), outward);
			path.addAll(wheel.subList(1, wheel.size()));
			List<Vec3> back = new ArrayList<>(CableLoops.returnPoints(drum, outward));
			path.addAll(back.reversed().subList(1, back.size()));
			v = -travel;
		}
		for (int i = 1; i < path.size(); i++) {
			v = CableGeometry.addRun(state.pieces, drum.getLevel(), origin, path.get(i - 1), path.get(i), v);
		}
	}

	@Override
	public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
		CableGeometry.submit(state.pieces, poseStack, collector);
		poseStack.pushPose();
		poseStack.translate(0.5F, 0.5F, 0.5F);
		poseStack.rotateDegrees(Axis.YP, -state.facingDegrees);
		if (state.hanging) {
			poseStack.rotateDegrees(Axis.XP, 180.0F);
		}
		poseStack.translate(-0.5F, -0.5F, -0.5F);
		if (state.powered) {
			poseStack.pushPose();
			poseStack.translate(0.0F, AXLE_Y, AXLE_Z);
			poseStack.rotateDegrees(Axis.XP, state.leverAngle);
			poseStack.translate(0.0F, -AXLE_Y, -AXLE_Z);
			state.lever.submit(poseStack, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
			poseStack.popPose();
		}
		poseStack.translate(0.0F, AXLE_Y, AXLE_Z);
		poseStack.rotateDegrees(Axis.XP, state.angle);
		poseStack.translate(0.0F, -AXLE_Y, -AXLE_Z);
		state.spool.submit(poseStack, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
		if (!state.powered) {
			state.crank.submit(poseStack, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
		}
		poseStack.popPose();
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
