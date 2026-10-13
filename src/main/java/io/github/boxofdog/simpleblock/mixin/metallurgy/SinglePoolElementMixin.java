package io.github.boxofdog.simpleblock.mixin.metallurgy;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.mojang.datafixers.util.Either;

import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.pools.SinglePoolElement;
import net.minecraft.world.level.levelgen.structure.templatesystem.LiquidSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

import io.github.boxofdog.simpleblock.metallurgy.VillageFurnaces;

/**
 * M08: village (and trail ruin) jigsaw pieces, plain and legacy single elements, which share these
 * settings, get {@link VillageFurnaces}, so new villages build ordinary furnaces instead of blast furnaces.
 */
@Mixin(SinglePoolElement.class)
abstract class SinglePoolElementMixin {
	@Shadow
	@Final
	protected Either<Identifier, StructureTemplate> template;

	@Inject(method = "getSettings", at = @At("RETURN"))
	private void simpleBlock$villageFurnaces(Rotation rotation, BoundingBox chunkBB, LiquidSettings liquidSettings, boolean keepJigsaws,
		CallbackInfoReturnable<StructurePlaceSettings> cir) {
		if (this.template.left().map(id -> VillageFurnaces.appliesTo(id.getPath())).orElse(false)) {
			cir.getReturnValue().addProcessor(VillageFurnaces.INSTANCE);
		}
	}
}
