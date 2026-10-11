package io.github.boxofdog.simpleblock.mixin.geology;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;

import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.material.rule.OreVeinRule;

import io.github.boxofdog.simpleblock.feature.RemovedRocks;

/**
 * GEO-01: large ore veins keep their ore and raw-ore blocks but lose their tuff (iron) or granite
 * (copper) filler. A null result lets the material rule sequence fall through to the next rule,
 * which is the terrain's ordinary stone or deepslate at that depth, so the vein sits in plain
 * rock with no artificial cut-off.
 */
@Mixin(OreVeinRule.class)
abstract class OreVeinRuleMixin {
	@ModifyExpressionValue(method = "compile", at = @At(value = "FIELD",
		target = "Lnet/minecraft/world/level/levelgen/material/rule/OreVeinRule;fillerBlock:Lnet/minecraft/world/level/block/state/BlockState;"))
	private BlockState simpleBlock$dropRemovedFiller(BlockState filler) {
		return RemovedRocks.isRemoved(filler) ? null : filler;
	}
}
