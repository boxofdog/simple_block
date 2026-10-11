package io.github.boxofdog.simpleblock.client;

import net.fabricmc.api.ClientModInitializer;

/** Client-only display parts of Simple Block. Every rule is enforced by the server. */
public class SimpleBlockClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		InventoryScreenAddons.register();
	}
}
