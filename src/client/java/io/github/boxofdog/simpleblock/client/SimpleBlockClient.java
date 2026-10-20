package io.github.boxofdog.simpleblock.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.ModelLayerRegistry;

import io.github.boxofdog.simpleblock.client.workwear.RearWarning;
import io.github.boxofdog.simpleblock.client.workwear.TrailBlazerGhosts;
import io.github.boxofdog.simpleblock.client.cable.CableDrumRenderer;
import io.github.boxofdog.simpleblock.client.cable.CoalWinchScreen;
import io.github.boxofdog.simpleblock.client.cable.CableHookRenderer;
import io.github.boxofdog.simpleblock.client.effigy.EffigyPlacementScreen;
import io.github.boxofdog.simpleblock.client.effigy.EffigyRenderer;
import io.github.boxofdog.simpleblock.client.metallurgy.BlastFurnaceScreen;
import io.github.boxofdog.simpleblock.client.skip.OreSkipRenderer;
import io.github.boxofdog.simpleblock.client.train.CargoInspectScreen;
import io.github.boxofdog.simpleblock.client.train.CobblestoneEngineRenderer;
import io.github.boxofdog.simpleblock.client.train.GatedMinecartModel;

/** Client-only display parts of Simple Block. Every rule is enforced by the server. */
public class SimpleBlockClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		InventoryScreenAddons.register();
		SaturationHud.register();
		TrailBlazerGhosts.register();
		RearWarning.register();
		// The owner's M07 cart redesign: every minecart's body (AbstractMinecartRendererModelMixin).
		ModelLayerRegistry.registerModelLayer(GatedMinecartModel.LAYER, GatedMinecartModel::createBodyLayer);
		CargoMinecartRenderer.register();
		CableDrumRenderer.register();
		CableHookRenderer.register();
		CobblestoneEngineRenderer.register();
		OreSkipRenderer.register();
		io.github.boxofdog.simpleblock.client.coot.OldCootRenderer.register();
		// M13: a rope shot from a bow flies as its item, like a thrown snowball.
		net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry.register(io.github.boxofdog.simpleblock.rope.RopeContent.ROPE_SHOT,
			net.minecraft.client.renderer.entity.ThrownItemRenderer::new);
		BlastFurnaceScreen.register();
		CoalWinchScreen.register();
		CargoInspectScreen.register();
		EffigyPlacementScreen.register();
		EffigyRenderer.register();
	}
}
