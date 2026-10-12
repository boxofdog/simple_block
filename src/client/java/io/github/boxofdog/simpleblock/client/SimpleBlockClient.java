package io.github.boxofdog.simpleblock.client;

import net.fabricmc.api.ClientModInitializer;

import io.github.boxofdog.simpleblock.client.cable.CableDrumRenderer;
import io.github.boxofdog.simpleblock.client.cable.CableHookRenderer;

/** Client-only display parts of Simple Block. Every rule is enforced by the server. */
public class SimpleBlockClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		InventoryScreenAddons.register();
		SaturationHud.register();
		CargoMinecartRenderer.register();
		CableDrumRenderer.register();
		CableHookRenderer.register();
	}
}
