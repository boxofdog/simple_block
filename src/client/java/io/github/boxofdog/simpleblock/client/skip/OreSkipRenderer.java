package io.github.boxofdog.simpleblock.client.skip;

import java.util.ArrayList;
import java.util.List;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

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
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;

import io.github.boxofdog.simpleblock.cable.CableAnchorBlock;
import io.github.boxofdog.simpleblock.cable.CableHoists;
import io.github.boxofdog.simpleblock.cable.CableRoutes;
import io.github.boxofdog.simpleblock.client.cable.CableDrumRenderer;
import io.github.boxofdog.simpleblock.client.cable.CableGeometry;
import io.github.boxofdog.simpleblock.skip.OreSkip;
import io.github.boxofdog.simpleblock.skip.SkipContent;
import io.github.boxofdog.simpleblock.skip.SkipPartBlock;

/**
 * M07: draws the ore skip from its part models ({@code block/ore_skip_*}), stacked up its column and
 * turned to its facing: the chute (block 1), the bin (blocks 2-3) with its discharge gate closed or
 * open, the intake's lid closed or open, and the bridle (block 4); and, when it hangs on a cable, the
 * cable from the bridle's apex up to the anchor's roller.
 */
public class OreSkipRenderer extends EntityRenderer<OreSkip, OreSkipRenderer.State> {
	/** The bridle's apex, where the cable is made fast, above the skip's bottom. */
	private static final double APEX = OreSkip.HEIGHT - 1.0 / 16.0;
	/** The discharge gate slides this far sideways to open (as {@code block/ore_skip_gate_open}). */
	private static final float GATE_SLIDE = 10.0F / 16.0F;
	private static final float LID_HINGE_Y = 0.75F / 16.0F;
	private static final float LID_HINGE_Z = 14.25F / 16.0F;
	private static final float LID_OPEN_DEGREES = 100.0F;

	public static class State extends EntityRenderState {
		final BlockModelRenderState chute = new BlockModelRenderState();
		final BlockModelRenderState bin = new BlockModelRenderState();
		final BlockModelRenderState gate = new BlockModelRenderState();
		final BlockModelRenderState lid = new BlockModelRenderState();
		final BlockModelRenderState bridle = new BlockModelRenderState();
		Direction facing = Direction.NORTH;
		boolean bridled;
		/** Door positions, 0 shut to 1 open (interpolated between ticks). */
		float lidOpen;
		float gateOpen;
		final List<CableGeometry.Piece> pieces = new ArrayList<>();
	}

	private final BlockModelResolver blockModelResolver;

	public OreSkipRenderer(EntityRendererProvider.Context context) {
		super(context);
		this.blockModelResolver = context.getBlockModelResolver();
	}

	public static void register() {
		EntityRendererRegistry.register(SkipContent.ORE_SKIP, OreSkipRenderer::new);
		SkipScreen.register();
	}

	@Override
	public State createRenderState() {
		return new State();
	}

	/** Drawn whenever it is in range: its cable can cross the view when the skip itself does not. */
	@Override
	protected boolean affectedByCulling(OreSkip entity) {
		return false;
	}

	private void part(BlockModelRenderState state, SkipPartBlock.Part part) {
		this.blockModelResolver.update(state, SkipContent.SKIP_PART_MODEL.defaultBlockState().setValue(SkipPartBlock.PART, part),
			AbstractMinecartRenderer.BLOCK_DISPLAY_CONTEXT);
	}

	@Override
	public void extractRenderState(OreSkip skip, State state, float partialTicks) {
		super.extractRenderState(skip, state, partialTicks);
		this.part(state.chute, SkipPartBlock.Part.CHUTE);
		this.part(state.bin, SkipPartBlock.Part.BIN);
		// The closed models, moved by the animation: the gate slides sideways, the lid swings up on its hinge.
		this.part(state.gate, SkipPartBlock.Part.GATE_CLOSED);
		this.part(state.lid, SkipPartBlock.Part.LID_CLOSED);
		state.lidOpen = net.minecraft.util.Mth.lerp(partialTicks, skip.lidAnimO, skip.lidAnim);
		state.gateOpen = net.minecraft.util.Mth.lerp(partialTicks, skip.gateAnimO, skip.gateAnim);
		state.bridled = skip.bridled();
		this.part(state.bridle, SkipPartBlock.Part.BRIDLE);
		state.facing = skip.facing();
		state.pieces.clear();
		BlockPos anchor = skip.anchor();
		if (anchor != null) {
			Vec3 origin = new Vec3(state.x, state.y, state.z);
			Vec3 drop = dropPoint(skip.level().getBlockState(anchor), anchor);
			// The texture carries on from the routed cable's, so the whole line moves together (M07 patch §9).
			BlockPos drumPos = skip.drum();
			CableDrumRenderer.RouteEnd end = drumPos == null ? null : CableDrumRenderer.routeEnd(skip.level(), drumPos, partialTicks);
			CableGeometry.addRun(state.pieces, skip.level(), origin, drop, origin.add(0.0, APEX, 0.0), end == null ? 0.0 : end.vAt(drop));
		}
	}

	/** Where the hoisting cable leaves the head anchor: the pulley's far side at its axle (or the roller, before the block state arrives). */
	private static Vec3 dropPoint(net.minecraft.world.level.block.state.BlockState state, BlockPos anchor) {
		if (state.hasProperty(CableAnchorBlock.RETURN) && state.getValue(CableAnchorBlock.RETURN) == CableAnchorBlock.Return.PULLEY) {
			Direction facing = state.getValue(CableAnchorBlock.FACING);
			return new Vec3(anchor.getX() + 0.5 + facing.getStepX() * CableHoists.PULLEY_DROP, anchor.getY() + CableHoists.PULLEY_AXLE_Y,
				anchor.getZ() + 0.5 + facing.getStepZ() * CableHoists.PULLEY_DROP);
		}
		return CableRoutes.point(anchor, false);
	}

	@Override
	public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
		CableGeometry.submit(state.pieces, poseStack, collector);
		poseStack.pushPose();
		// The models face north; a block state's y rotation turns clockwise seen from above.
		poseStack.rotateDegrees(Axis.YP, 180.0F - state.facing.toYRot());
		poseStack.translate(-0.5F, 0.0F, -0.5F);
		int light = state.lightCoords;
		state.chute.submit(poseStack, collector, light, OverlayTexture.NO_OVERLAY, state.outlineColor);
		poseStack.translate(0.0F, 1.0F, 0.0F);
		state.bin.submit(poseStack, collector, light, OverlayTexture.NO_OVERLAY, state.outlineColor);
		poseStack.pushPose();
		poseStack.translate(state.gateOpen * GATE_SLIDE, 0.0F, 0.0F);
		state.gate.submit(poseStack, collector, light, OverlayTexture.NO_OVERLAY, state.outlineColor);
		poseStack.popPose();
		poseStack.translate(0.0F, 2.0F, 0.0F);
		poseStack.pushPose();
		// The lid's hinge runs across the back of the intake (model y 0.75, z 14.25).
		poseStack.translate(0.0F, LID_HINGE_Y, LID_HINGE_Z);
		poseStack.rotateDegrees(Axis.XP, state.lidOpen * LID_OPEN_DEGREES);
		poseStack.translate(0.0F, -LID_HINGE_Y, -LID_HINGE_Z);
		state.lid.submit(poseStack, collector, light, OverlayTexture.NO_OVERLAY, state.outlineColor);
		poseStack.popPose();
		if (state.bridled) {
			// Owner's playtest patch §3: the bridle exists only while the skip hangs on its cable.
			state.bridle.submit(poseStack, collector, light, OverlayTexture.NO_OVERLAY, state.outlineColor);
		}
		poseStack.popPose();
		super.submit(state, poseStack, collector, camera);
	}
}
