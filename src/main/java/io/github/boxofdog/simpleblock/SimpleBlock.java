package io.github.boxofdog.simpleblock;

import net.fabricmc.api.ModInitializer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.github.boxofdog.simpleblock.cable.CableContent;
import io.github.boxofdog.simpleblock.feature.ChestMinecarts;
import io.github.boxofdog.simpleblock.feature.EnderChests;
import io.github.boxofdog.simpleblock.feature.HeavyLoad;
import io.github.boxofdog.simpleblock.feature.InventorySlots;
import io.github.boxofdog.simpleblock.feature.InventorySort;
import io.github.boxofdog.simpleblock.feature.MinecartCargo;
import io.github.boxofdog.simpleblock.feature.NoElytraFlight;
import io.github.boxofdog.simpleblock.feature.RemovedRocks;
import io.github.boxofdog.simpleblock.feature.RetiredItems;
import io.github.boxofdog.simpleblock.feature.SaturationSync;
import io.github.boxofdog.simpleblock.skip.SkipContent;
import io.github.boxofdog.simpleblock.train.TrainContent;
import io.github.boxofdog.simpleblock.tuning.Tuning;
import io.github.boxofdog.simpleblock.worldgen.IronWorldgen;

/**
 * Simple Block entrypoint.
 *
 * <p>Each feature is self-contained so it can be changed or reverted on its own. Provisional
 * balance numbers live in {@link Tuning} ({@code config/simple_block/tuning.json}).
 *
 * <p>Milestone 01, survival foundation:
 * <ul>
 *   <li>SF-01 sleeping — {@code mixin/sleep}</li>
 *   <li>SF-02 phantoms — {@code mixin/phantom}</li>
 *   <li>SF-03 elytra — {@link NoElytraFlight} and {@code mixin/elytra}</li>
 *   <li>SF-04 shulker boxes — {@code feature/ShulkerBoxes}, {@code mixin/shulker} and the
 *       {@code data/minecraft/recipe} + {@code data/minecraft/advancement/recipes} overrides</li>
 * </ul>
 *
 * <p>Milestone 02, world, night and inventory:
 * <ul>
 *   <li>GEO-01 granite/diorite/andesite/tuff removed — {@link RemovedRocks}, {@code mixin/geology},
 *       recipe/advancement overrides and the mason trade tag</li>
 *   <li>MINE-01 deepslate needs an iron pickaxe — {@code feature/DeepslateGate}, {@code mixin/mining}
 *       and {@code #simple_block:requires_iron_pickaxe}</li>
 *   <li>ORE-01/ORE-02 iron and diamonds — {@link IronWorldgen}, {@code worldgen/} and
 *       {@code data/simple_block/worldgen}, diamond placed-feature overrides</li>
 *   <li>INV-01 27 usable slots — {@link InventorySlots}, {@code mixin/inventory/SlotMixin} and
 *       {@code InventoryMixin}</li>
 *   <li>INV-02 sort — {@link InventorySort}, {@code network/SortInventoryPayload}, client button</li>
 *   <li>INV-03 hotbar fills from the left — {@code mixin/inventory/AbstractContainerMenuMixin}</li>
 *   <li>INV-04 64-ingot heavy load — {@link HeavyLoad}, {@code feature/HeavyTransfers},
 *       {@code mixin/inventory/*}, {@code simple_block/heavy_weights.json}, client indicator</li>
 *   <li>LIGHT-01 torches give 12 — {@code feature/LightLevels}, {@code mixin/light}</li>
 *   <li>MOB-01 night attraction to lit ground — {@code feature/Entrancement},
 *       {@code mixin/mobs/RandomStrollGoalMixin}</li>
 *   <li>MOB-02 zombies break wooden doors only in pursuit — {@code feature/ZombieDoors},
 *       {@code mixin/mobs/ZombieMixin}</li>
 *   <li>MOB-03 creepers dissolve in sunlight — {@code feature/CreeperEvaporation},
 *       {@code mixin/mobs/CreeperMixin}</li>
 *   <li>FOOD-01 half activity exhaustion — {@code feature/Exhaustion}, {@code mixin/food};
 *       saturation display — {@link SaturationSync}, {@code network/SaturationPayload}, client
 *       {@code SaturationHud}</li>
 * </ul>
 *
 * <p>Milestone 03, minecart cargo and storage-bypass closure:
 * <ul>
 *   <li>FRT-01..09 the minecart carries 256 of one mined resource and nobody rides it without a cushion —
 *       {@link MinecartCargo}, {@code mixin/minecart}, {@code #simple_block:minecart_cargo}, client
 *       {@code CargoMinecartRenderer}</li>
 *   <li>CUSH-01 a cushion fitted into a minecart makes it a passenger cart —
 *       {@code feature/MinecartCushions}, {@code mixin/minecart}, client {@code CargoMinecartRenderer}
 *       and {@code mixin/AbstractMinecartRendererMixin}</li>
 *   <li>FRT-10 ender chests disabled — {@link EnderChests}, {@code mixin/enderchest}, recipe,
 *       advancement and block loot overrides</li>
 *   <li>FRT-11 chest minecarts disabled — {@link ChestMinecarts}, {@code mixin/chestcart}, recipe
 *       and advancement overrides</li>
 * </ul>
 *
 * <p>Milestone 04, cable infrastructure ({@code cable/}; registrations in {@link CableContent}):
 * <ul>
 *   <li>cable and cable coils, the cable drum (winch), copper cable anchors and routes —
 *       {@code cable/CableRoutes}, {@code cable/CableHook}, {@code #simple_block:cable_anchor_supports},
 *       client {@code CableDrumRenderer} and {@code CableHookRenderer}</li>
 *   <li>the copper connecting rod on a minecart, gripping the cable — {@code cable/ConnectingRods},
 *       {@code mixin/minecart}, client {@code CargoMinecartRenderer}</li>
 * </ul>
 *
 * <p>Milestone 05, haulage and trains ({@code cable/}, {@code train/}; registrations in {@link TrainContent}):
 * <ul>
 *   <li>closed cable loops, the drum's hand crank and cable haulage — {@code cable/CableLoops},
 *       {@code cable/CableDrive}, {@code network/CrankPayload}, client {@code CableDrumRenderer} and
 *       {@code mixin/MultiPlayerGameModeMixin} (left-click feeds); the overhead (hanging) drum —
 *       {@code cable/CableDrumBlock}</li>
 *   <li>the Cobblestone Engine (vanilla's furnace minecart) — {@code train/CobblestoneEngine},
 *       {@code mixin/train/MinecartFurnaceMixin}, {@code mixin/train/VehicleEntityMixin}, recipe and
 *       advancement overrides, client {@code CobblestoneEngineRenderer}</li>
 *   <li>iron-chain couplings — {@code train/Couplings}, {@code mixin/train/AbstractMinecartMixin}, client
 *       {@code CouplingChains}; rail mass and grade — {@code train/TrainPhysics}</li>
 * </ul>
 *
 * <p>Milestone 06, freight station, coal winch and clean-up:
 * <ul>
 *   <li>cable recipes in the middle column and one coil for any length — {@code cable/CenterColumnRecipes},
 *       {@code mixin/cable}, {@code cable/CableRoutes}; long lines kept with the level and driven where
 *       they are loaded — {@code cable/CableLines}, {@code cable/CableDrive}, {@code cable/CableIndex}</li>
 *   <li>the coal winch (a furnace on a drum, a three-position lever) — {@code cable/CoalWinch},
 *       {@code cable/CableDrive}, client {@code CableDrumRenderer}</li>
 *   <li>the Loading Track (docking, holding the train, release, the cart's front and gate) —
 *       {@code train/LoadingTrackBlock}, {@code train/LoadingTracks}, {@code mixin/train/AbstractMinecartMixin},
 *       client {@code CargoMinecartRenderer}</li>
 *   <li>retired redstone rails and no retired items in the creative menu — {@link RetiredItems}, recipe and
 *       advancement overrides</li>
 *   <li>no shoving carts that carry no passenger — {@code feature/CartPushing}, {@code mixin/pushing}</li>
 * </ul>
 *
 * <p>Milestone 07, the upright ore skip ({@code skip/}; registrations in {@link SkipContent}):
 * <ul>
 *   <li>the skip, its 512-item single slot and screen — {@code skip/OreSkip}, {@code skip/SkipCargo},
 *       {@code skip/SkipMenu}, client {@code skip/OreSkipRenderer} and {@code skip/SkipScreen}</li>
 *   <li>hoisting on a drum's cable with the crank or coal winch — {@code cable/CableHoists},
 *       {@code cable/CableDrive}, {@code cable/CoalWinch}</li>
 *   <li>cargo exchange at the Loading Rail by height — {@code skip/SkipStations},
 *       {@code mixin/train/AbstractMinecartMixin} (the cart's ramp), client {@code CargoMinecartRenderer}</li>
 *   <li>visual remodels: the furnace engine inside the vanilla minecart (client
 *       {@code CobblestoneEngineRenderer}, {@code mixin/AbstractMinecartRendererMixin}) and the coal winch's
 *       motor in place of its stand ({@code cable/CableDrumBlock#POWERED})</li>
 * </ul>
 */
public class SimpleBlock implements ModInitializer {
	public static final String MOD_ID = "simple_block";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		Tuning.load();
		CableContent.register();
		TrainContent.register();
		SkipContent.register();
		NoElytraFlight.register();
		RemovedRocks.register();
		IronWorldgen.register();
		HeavyLoad.load();
		InventorySlots.register();
		InventorySort.register();
		SaturationSync.register();
		RetiredItems.register();
	}
}
