package io.github.boxofdog.simpleblock;

import net.fabricmc.api.ModInitializer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.github.boxofdog.simpleblock.feature.NoElytraFlight;

/**
 * Milestone 01 ("Survival Foundation") entrypoint.
 *
 * <p>Each restriction is self-contained so it can be changed or reverted on its own:
 * <ul>
 *   <li>SF-01 sleeping — {@code mixin/sleep}</li>
 *   <li>SF-02 phantoms — {@code mixin/phantom}</li>
 *   <li>SF-03 elytra — {@link NoElytraFlight} and {@code mixin/elytra}</li>
 *   <li>SF-04 shulker boxes — {@code feature/ShulkerBoxes}, {@code mixin/shulker} and the
 *       {@code data/minecraft/recipe} + {@code data/minecraft/advancement/recipes} overrides</li>
 * </ul>
 */
public class SimpleBlock implements ModInitializer {
	public static final String MOD_ID = "simple_block";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		NoElytraFlight.register();
	}
}
