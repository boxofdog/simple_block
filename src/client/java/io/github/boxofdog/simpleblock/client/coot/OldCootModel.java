package io.github.boxofdog.simpleblock.client.coot;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.model.AnimationUtils;
import net.minecraft.client.model.ArmedModel;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HeadedModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.renderer.entity.state.ArmedEntityRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;

import io.github.boxofdog.simpleblock.SimpleBlock;

/**
 * M14 the Old Coot's model ({@link OldCootGeometry}, generated with its Blockbench project): short, broad and
 * hunched, villager nosed, scraggly bearded, prospector's hat on. Walking, his short arms and legs swing;
 * whenever he swings (a pickaxe blow on stone or at a player) his pickaxe arm comes down from overhead, as a
 * vindicator swings its axe, timed by the entity's own swing ({@link ArmedEntityRenderState#swingAnimation}).
 */
public class OldCootModel extends EntityModel<ArmedEntityRenderState> implements ArmedModel<ArmedEntityRenderState>, HeadedModel {
	public static final ModelLayerLocation LAYER = new ModelLayerLocation(Identifier.fromNamespaceAndPath(SimpleBlock.MOD_ID, "old_coot"), "main");
	/** The hand is 6.5 pixels below the shoulder pivot, where the held item is drawn 10 down a humanoid arm: lift it 3.5. */
	private static final float HAND_LIFT = 3.5F / 16.0F;

	private final ModelPart head;
	private final ModelPart rightArm;
	private final ModelPart leftArm;
	private final ModelPart rightLeg;
	private final ModelPart leftLeg;

	public OldCootModel(ModelPart root) {
		super(root);
		this.head = root.getChild("head");
		this.rightArm = root.getChild("right_arm");
		this.leftArm = root.getChild("left_arm");
		this.rightLeg = root.getChild("right_leg");
		this.leftLeg = root.getChild("left_leg");
	}

	public static LayerDefinition createBodyLayer() {
		return OldCootGeometry.create();
	}

	@Override
	public void setupAnim(ArmedEntityRenderState state) {
		super.setupAnim(state);
		this.head.yRot = state.yRot * Mth.DEG_TO_RAD;
		this.head.xRot = state.xRot * Mth.DEG_TO_RAD;
		float pos = state.walkAnimationPos;
		float speed = state.walkAnimationSpeed;
		this.rightLeg.xRot = Mth.cos(pos * 0.6662F) * 1.4F * speed * 0.5F;
		this.leftLeg.xRot = Mth.cos(pos * 0.6662F + Mth.PI) * 1.4F * speed * 0.5F;
		if (state.swingAnimation > 0.0F) {
			AnimationUtils.swingWeaponDown(this.rightArm, this.leftArm, state.mainArm, state.swingAnimation, state.ageInTicks);
		} else {
			// Arms hanging forward from the hunch (their splay is the pose's), the pickaxe a little further
			// forward; the other arm swings with his stride.
			this.rightArm.xRot = -0.45F + Mth.cos(pos * 0.6662F + Mth.PI) * speed * 0.6F;
			this.leftArm.xRot = -0.3F + Mth.cos(pos * 0.6662F) * speed;
		}
	}

	@Override
	public ModelPart getHead() {
		return this.head;
	}

	@Override
	public void translateToHand(ArmedEntityRenderState state, HumanoidArm arm, PoseStack poseStack) {
		this.root.translateAndRotate(poseStack);
		(arm == HumanoidArm.LEFT ? this.leftArm : this.rightArm).translateAndRotate(poseStack);
		poseStack.translate(0.0F, -HAND_LIFT, 0.0F);
	}
}
