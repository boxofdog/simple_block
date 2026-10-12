package io.github.boxofdog.simpleblock.client.mixin;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.renderer.entity.state.MinecartRenderState;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import io.github.boxofdog.simpleblock.client.cable.CableGeometry;
import io.github.boxofdog.simpleblock.client.train.CouplingChains;

/** M05: every minecart's render state carries its coupling chains ({@link CouplingChains}). */
@Mixin(MinecartRenderState.class)
abstract class MinecartRenderStateMixin implements CouplingChains.Holder {
	@Unique
	private final List<CableGeometry.Piece> simpleBlock$chains = new ArrayList<>();

	@Override
	public List<CableGeometry.Piece> simpleBlock$chains() {
		return this.simpleBlock$chains;
	}
}
