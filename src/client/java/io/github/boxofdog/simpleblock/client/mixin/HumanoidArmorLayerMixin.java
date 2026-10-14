package io.github.boxofdog.simpleblock.client.mixin;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.layers.EquipmentLayerRenderer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.client.resources.model.EquipmentClientInfo;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.equipment.Equippable;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import io.github.boxofdog.simpleblock.workwear.WorkwearContent;

/**
 * M10: the Overalls are worn in the chest slot but are trousers too: with them on (and nothing in the
 * legs slot, which they keep empty), their leggings layer is drawn on the legs as leggings would be.
 */
@Mixin(HumanoidArmorLayer.class)
abstract class HumanoidArmorLayerMixin<S extends HumanoidRenderState, M extends HumanoidModel<S>, A extends HumanoidModel<S>> {
	@Shadow
	@Final
	private EquipmentLayerRenderer equipmentRenderer;

	@Shadow
	protected abstract A getArmorModel(S state, EquipmentSlot slot);

	@Inject(method = "submit(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;ILnet/minecraft/client/renderer/entity/state/HumanoidRenderState;FF)V",
		at = @At("TAIL"))
	private void simpleBlock$overallsLegs(PoseStack poseStack, SubmitNodeCollector collector, int lightCoords, S state, float yRot, float xRot, CallbackInfo ci) {
		if (!state.chestEquipment.is(WorkwearContent.OVERALLS) || !state.legsEquipment.isEmpty() || state.isBaby) {
			return;
		}
		Equippable equippable = state.chestEquipment.get(DataComponents.EQUIPPABLE);
		if (equippable == null || equippable.assetId().isEmpty()) {
			return;
		}
		this.equipmentRenderer.renderLayers(EquipmentClientInfo.LayerType.HUMANOID_LEGGINGS, equippable.assetId().get(), this.getArmorModel(state, EquipmentSlot.LEGS),
			state, state.chestEquipment, poseStack, collector, lightCoords, state.outlineColor);
	}
}
