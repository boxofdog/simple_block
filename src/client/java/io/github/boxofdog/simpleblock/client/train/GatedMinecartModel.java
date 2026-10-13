package io.github.boxofdog.simpleblock.client.train;

import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.model.object.cart.MinecartModel;
import net.minecraft.client.renderer.entity.state.MinecartRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

import io.github.boxofdog.simpleblock.SimpleBlock;
import io.github.boxofdog.simpleblock.client.CargoMinecartRenderer;

/**
 * Every minecart's body (owner's M07 cart redesign), in vanilla's proportions and on vanilla's
 * {@code textures/entity/minecart.png}: vanilla's floor, four fixed corner columns, a fixed rim along
 * the top of each long side, and four hinged gates filling the frame, so that a closed cart looks like
 * vanilla's (the gates and columns take their faces from the same parts of the texture as vanilla's
 * walls) and one whose gate is open has a real opening there.
 * <ul>
 *   <li>The two <b>end gates</b> (12 px wide, between the columns, no rim over them) are hinged along
 *       their outer bottom edge and fold outwards and down into a ramp
 *       ({@value #END_OPEN_DEGREES} degrees).</li>
 *   <li>The two <b>side gates</b> (16 px wide, the length of the cart between the columns) are hinged
 *       along the side rim and swing outwards and up from the bottom ({@value #SIDE_OPEN_DEGREES}
 *       degrees), so a car can tip sideways out of a coupled train.</li>
 * </ul>
 * Only a freight cart opens a gate ({@link CargoMinecartRenderer}: the one facing the skip it tips
 * into); a cushioned cart, the Cobblestone Engine and every other minecart keep all four shut.
 *
 * <p>Model space is vanilla's: x along the cart (the renderer flips x and y into the world, so model
 * -x is the cart's +x), y down, z across; the floor's top at y 3, the rim's top at y -5. Editable
 * source: {@code art/blockbench/gated_minecart.bbmodel} (Modded Entity format, 64 x 32 UV on vanilla's
 * minecart texture); the geometry here is the one the game uses.
 */
public class GatedMinecartModel extends MinecartModel {
	public static final ModelLayerLocation LAYER = new ModelLayerLocation(Identifier.fromNamespaceAndPath(SimpleBlock.MOD_ID, "gated_minecart"), "main");

	/** How far an end gate folds down: 22.5 degrees past level, a ramp. */
	public static final float END_OPEN_DEGREES = 112.5F;
	/** How far a side gate swings up and out. */
	public static final float SIDE_OPEN_DEGREES = 100.0F;

	/** The gates by the cart's own sides, as {@link CargoMinecartRenderer.State#openGate}: +x end, -x end, +z side, -z side. */
	private final ModelPart[] gates;

	public GatedMinecartModel(ModelPart root) {
		super(root);
		this.gates = new ModelPart[] {root.getChild("gate_end_plus"), root.getChild("gate_end_minus"), root.getChild("gate_side_plus"),
			root.getChild("gate_side_minus")};
	}

	public static LayerDefinition createBodyLayer() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();
		// Vanilla's floor.
		root.addOrReplaceChild("bottom", CubeListBuilder.create().texOffs(0, 10).addBox(-10.0F, -8.0F, -1.0F, 20.0F, 16.0F, 2.0F),
			PartPose.offsetAndRotation(0.0F, 4.0F, 0.0F, Mth.HALF_PI, 0.0F, 0.0F));
		// The frame: corner columns (the full wall height) and the rims along the top of the long sides.
		for (int ix = 0; ix < 2; ix++) {
			for (int iz = 0; iz < 2; iz++) {
				root.addOrReplaceChild("column_" + ix + iz, CubeListBuilder.create().texOffs(iz == 0 ? 0 : 14, 0)
					.addBox(ix == 0 ? -10.0F : 8.0F, -5.0F, iz == 0 ? -8.0F : 6.0F, 2.0F, 8.0F, 2.0F), PartPose.ZERO);
			}
		}
		root.addOrReplaceChild("rim_minus", CubeListBuilder.create().texOffs(0, 0).addBox(-8.0F, -5.0F, -8.0F, 16.0F, 1.0F, 2.0F), PartPose.ZERO);
		root.addOrReplaceChild("rim_plus", CubeListBuilder.create().texOffs(0, 0).addBox(-8.0F, -5.0F, 6.0F, 16.0F, 1.0F, 2.0F), PartPose.ZERO);
		// End gates, between the columns: each part's origin is its hinge, the gate's outer bottom edge.
		CubeListBuilder end = CubeListBuilder.create().texOffs(6, 0).addBox(-6.0F, -8.0F, -2.0F, 12.0F, 8.0F, 2.0F);
		root.addOrReplaceChild("gate_end_plus", end, PartPose.offsetAndRotation(-10.0F, 3.0F, 0.0F, 0.0F, Mth.PI * 1.5F, 0.0F));
		root.addOrReplaceChild("gate_end_minus", end, PartPose.offsetAndRotation(10.0F, 3.0F, 0.0F, 0.0F, Mth.HALF_PI, 0.0F));
		// Side gates, under the rims: each part's origin is its hinge, the gate's outer top edge.
		CubeListBuilder side = CubeListBuilder.create().texOffs(0, 1).addBox(-8.0F, 0.0F, -2.0F, 16.0F, 7.0F, 2.0F);
		root.addOrReplaceChild("gate_side_plus", side, PartPose.offset(0.0F, -4.0F, 8.0F));
		root.addOrReplaceChild("gate_side_minus", side, PartPose.offsetAndRotation(0.0F, -4.0F, -8.0F, 0.0F, Mth.PI, 0.0F));
		return LayerDefinition.create(mesh, 64, 32);
	}

	@Override
	public void setupAnim(MinecartRenderState state) {
		super.setupAnim(state);
		for (ModelPart gate : this.gates) {
			gate.xRot = 0.0F;
			gate.zRot = 0.0F;
		}
		if (state instanceof CargoMinecartRenderer.State cart && cart.openGate >= 0 && cart.gateSwing > 0.0F) {
			float swing = cart.gateSwing;
			switch (cart.openGate) {
				// The cart's +x end is the model's -x one: it folds towards model -x.
				case 0 -> this.gates[0].zRot = -swing * END_OPEN_DEGREES * Mth.DEG_TO_RAD;
				case 1 -> this.gates[1].zRot = swing * END_OPEN_DEGREES * Mth.DEG_TO_RAD;
				// Both sides lift outwards about their own x (the -z one is turned round).
				default -> this.gates[cart.openGate].xRot = swing * SIDE_OPEN_DEGREES * Mth.DEG_TO_RAD;
			}
		}
	}
}
