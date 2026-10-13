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

import io.github.boxofdog.simpleblock.cable.CableRoutes;
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

	public static class State extends EntityRenderState {
		final BlockModelRenderState chute = new BlockModelRenderState();
		final BlockModelRenderState bin = new BlockModelRenderState();
		final BlockModelRenderState gate = new BlockModelRenderState();
		final BlockModelRenderState lid = new BlockModelRenderState();
		final BlockModelRenderState bridle = new BlockModelRenderState();
		Direction facing = Direction.NORTH;
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
		this.part(state.gate, skip.gateOpen() ? SkipPartBlock.Part.GATE_OPEN : SkipPartBlock.Part.GATE_CLOSED);
		this.part(state.lid, skip.lidOpen() ? SkipPartBlock.Part.LID_OPEN : SkipPartBlock.Part.LID_CLOSED);
		this.part(state.bridle, SkipPartBlock.Part.BRIDLE);
		state.facing = skip.facing();
		state.pieces.clear();
		BlockPos anchor = skip.anchor();
		if (anchor != null) {
			Vec3 origin = new Vec3(state.x, state.y, state.z);
			CableGeometry.addRun(state.pieces, skip.level(), origin, CableRoutes.point(anchor, false), origin.add(0.0, APEX, 0.0));
		}
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
		state.gate.submit(poseStack, collector, light, OverlayTexture.NO_OVERLAY, state.outlineColor);
		poseStack.translate(0.0F, 2.0F, 0.0F);
		state.lid.submit(poseStack, collector, light, OverlayTexture.NO_OVERLAY, state.outlineColor);
		state.bridle.submit(poseStack, collector, light, OverlayTexture.NO_OVERLAY, state.outlineColor);
		poseStack.popPose();
		super.submit(state, poseStack, collector, camera);
	}
}
