package io.github.boxofdog.simpleblock.mixin.compat;

import java.util.Map;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import net.minecraft.core.Holder;
import net.minecraft.core.MappedRegistry;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;

import io.github.boxofdog.simpleblock.feature.LegacyIds;

/**
 * Looks up a renamed Simple Block identifier ({@link LegacyIds}) as its new name, by name or by key,
 * so blocks in saved chunks, block entities and item stacks saved under the old name load as the
 * renamed content. Only names that are not registered themselves are redirected.
 */
@Mixin(MappedRegistry.class)
abstract class MappedRegistryMixin<T> {
	@Shadow
	@Final
	private Map<Identifier, Holder.Reference<T>> byLocation;

	private Identifier simpleBlock$alias(Identifier id) {
		Identifier renamed = LegacyIds.renamed(id);
		return renamed != null && !this.byLocation.containsKey(id) ? renamed : id;
	}

	private ResourceKey<T> simpleBlock$alias(ResourceKey<T> key) {
		if (key == null) {
			return null;
		}
		Identifier renamed = LegacyIds.renamed(key.identifier());
		return renamed != null && !this.byLocation.containsKey(key.identifier()) ? ResourceKey.create(key.registryKey(), renamed) : key;
	}

	@ModifyVariable(method = "get(Lnet/minecraft/resources/Identifier;)Ljava/util/Optional;", at = @At("HEAD"), argsOnly = true)
	private Identifier simpleBlock$getById(Identifier id) {
		return this.simpleBlock$alias(id);
	}

	@ModifyVariable(method = "getValue(Lnet/minecraft/resources/Identifier;)Ljava/lang/Object;", at = @At("HEAD"), argsOnly = true)
	private Identifier simpleBlock$getValueById(Identifier id) {
		return id == null ? null : this.simpleBlock$alias(id);
	}

	@ModifyVariable(method = "containsKey(Lnet/minecraft/resources/Identifier;)Z", at = @At("HEAD"), argsOnly = true)
	private Identifier simpleBlock$containsId(Identifier id) {
		return this.simpleBlock$alias(id);
	}

	@ModifyVariable(method = "get(Lnet/minecraft/resources/ResourceKey;)Ljava/util/Optional;", at = @At("HEAD"), argsOnly = true)
	private ResourceKey<T> simpleBlock$getByKey(ResourceKey<T> key) {
		return this.simpleBlock$alias(key);
	}

	@ModifyVariable(method = "getValue(Lnet/minecraft/resources/ResourceKey;)Ljava/lang/Object;", at = @At("HEAD"), argsOnly = true)
	private ResourceKey<T> simpleBlock$getValueByKey(ResourceKey<T> key) {
		return this.simpleBlock$alias(key);
	}

	@ModifyVariable(method = "containsKey(Lnet/minecraft/resources/ResourceKey;)Z", at = @At("HEAD"), argsOnly = true)
	private ResourceKey<T> simpleBlock$containsKey(ResourceKey<T> key) {
		return this.simpleBlock$alias(key);
	}
}
