package io.github.boxofdog.simpleblock.gametest;

import java.util.ArrayList;
import java.util.List;

import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.trading.VillagerTrade;
import net.minecraft.world.level.storage.loot.LootTable;

import net.fabricmc.fabric.api.gametest.v1.GameTest;

/**
 * ELY-01 / SHU-02: audits the data the running game actually loaded (vanilla plus any data packs),
 * so the check does not depend on remembering every vanilla source.
 */
public class DataAuditGameTests {
	@GameTest
	public void noLootOrTradeGivesElytraOrShulkerBoxes(GameTestHelper helper) {
		MinecraftServer server = helper.getLevel().getServer();
		HolderLookup.Provider lookup = server.reloadableRegistries().lookup();
		RegistryOps<JsonElement> ops = RegistryOps.create(JsonOps.INSTANCE, lookup);
		List<String> problems = new ArrayList<>();

		int lootTables = 0;
		for (var table : lookup.lookupOrThrow(Registries.LOOT_TABLE).listElements().toList()) {
			String id = table.key().identifier().toString();
			String json = LootTable.DIRECT_CODEC.encodeStart(ops, table.value()).getOrThrow().toString();
			lootTables++;

			if (json.contains("\"minecraft:elytra\"")) {
				problems.add("loot table " + id + " contains elytra");
			}

			// A shulker box block's own table only drops the box that was already there.
			if (json.contains("shulker_box\"") && !id.matches("minecraft:blocks/([a-z_]+_)?shulker_box")) {
				problems.add("loot table " + id + " contains a shulker box");
			}
		}

		RegistryOps<JsonElement> worldOps = RegistryOps.create(JsonOps.INSTANCE, server.registryAccess());
		int trades = 0;
		for (var trade : server.registryAccess().lookupOrThrow(Registries.VILLAGER_TRADE).listElements().toList()) {
			String json = VillagerTrade.CODEC.encodeStart(worldOps, trade.value()).getOrThrow().toString();
			trades++;

			if (json.contains("\"minecraft:elytra\"") || json.contains("shulker_box\"")) {
				problems.add("villager trade " + trade.key().identifier() + " offers elytra or a shulker box");
			}
		}

		SimpleBlockTestLog.info("ELY-01/SHU-02 audit: {} loot tables and {} villager trades scanned, problems: {}", lootTables, trades, problems);
		helper.assertTrue(lootTables > 100 && trades > 50, "audit should have scanned the vanilla data (" + lootTables + " loot tables, " + trades + " trades)");
		helper.assertTrue(problems.isEmpty(), String.join("; ", problems));
		helper.succeed();
	}

	static String lootTableJson(MinecraftServer server, ResourceKey<LootTable> key) {
		HolderLookup.Provider lookup = server.reloadableRegistries().lookup();
		LootTable table = lookup.lookupOrThrow(Registries.LOOT_TABLE).getOrThrow(key).value();
		return LootTable.DIRECT_CODEC.encodeStart(RegistryOps.create(JsonOps.INSTANCE, lookup), table).getOrThrow().toString();
	}
}
