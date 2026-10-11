package io.github.boxofdog.simpleblock.gametest;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.entity.monster.Shulker;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.structures.EndCityPieces;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import net.minecraft.world.phys.AABB;

import net.fabricmc.fabric.api.gametest.v1.GameTest;

/** SF-03 evidence. */
public class ElytraGameTests {
	/**
	 * ELY-02 (start): a survival or creative player wearing a command-injected elytra in mid-air sends
	 * the same "start gliding" packet a real client sends when jumping; the server refuses.
	 */
	@GameTest(skyAccess = true)
	public void injectedElytraCannotStartGliding(GameTestHelper helper) {
		for (GameType gameType : List.of(GameType.SURVIVAL, GameType.CREATIVE)) {
			ServerPlayer player = airborneWithElytra(helper, gameType);
			player.connection.handlePlayerCommand(new ServerboundPlayerCommandPacket(player, ServerboundPlayerCommandPacket.Action.START_FALL_FLYING));
			helper.assertFalse(player.isFallFlying(), gameType.getName() + " player started gliding");
			TestPlayers.remove(player);
		}

		helper.succeed();
	}

	/**
	 * ELY-02 (maintain): even if a player somehow is in the gliding state (e.g. a modified client),
	 * the next server tick ends it.
	 */
	@GameTest(skyAccess = true)
	public void forcedGlidingIsStoppedByServerTick(GameTestHelper helper) {
		ServerPlayer player = airborneWithElytra(helper, GameType.SURVIVAL);
		player.startFallFlying();
		helper.assertTrue(player.isFallFlying(), "precondition: player is flagged as gliding");
		player.doTick();
		helper.assertFalse(player.isFallFlying(), "server tick should end gliding");
		TestPlayers.remove(player);
		helper.succeed();
	}

	/**
	 * ELY-01: generate a real End ship from the vanilla template. The ship's "Elytra" data marker is
	 * still present in the template, but no item frame with an elytra is created. Placed high above
	 * the test grid in its own batch and removed afterwards.
	 */
	@GameTest(environment = "simple_block-gametest:end_ship", maxTicks = 20)
	public void endShipHasNoElytra(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		StructureTemplateManager templates = level.getServer().getStructureTemplateManager();
		BlockPos origin = helper.absolutePos(new BlockPos(0, 40, 0));

		StructureTemplate template = templates.getOrCreate(Identifier.withDefaultNamespace("end_city/ship"));
		long elytraMarkers = template.filterBlocks(origin, new StructurePlaceSettings(), Blocks.STRUCTURE_BLOCK).stream()
				.filter(info -> info.nbt() != null && info.nbt().getStringOr("metadata", "").startsWith("Elytra"))
				.count();
		helper.assertValueEqual(elytraMarkers, 1L, "Elytra data markers in the vanilla ship template");

		EndCityPieces.EndCityPiece ship = new EndCityPieces.EndCityPiece(templates, "ship", origin, Rotation.NONE, true);
		BoundingBox box = ship.getBoundingBox();
		ship.postProcess(level, level.structureManager(), level.getChunkSource().getGenerator(), RandomSource.create(1), box, ChunkPos.containing(origin), origin);

		AABB area = AABB.of(box).inflate(2);
		long purpur = BlockPos.betweenClosedStream(box).filter(pos -> level.getBlockState(pos).is(Blocks.PURPUR_BLOCK)).count();
		List<ItemFrame> frames = level.getEntitiesOfClass(ItemFrame.class, area, frame -> true);
		List<Shulker> sentries = level.getEntitiesOfClass(Shulker.class, area, shulker -> true);
		SimpleBlockTestLog.info("ELY-01: End ship placed ({} purpur blocks), item frames: {}, frames with elytra: {}, shulker sentries: {}",
				purpur, frames.size(), frames.stream().filter(f -> f.getItem().is(Items.ELYTRA)).count(), sentries.size());

		helper.assertTrue(purpur > 100, "the ship should actually have been generated");
		helper.assertTrue(frames.stream().noneMatch(frame -> frame.getItem().is(Items.ELYTRA)), "an End ship item frame holds an elytra");

		level.getEntitiesOfClass(Entity.class, area, entity -> !(entity instanceof ServerPlayer)).forEach(Entity::discard);
		BlockPos.betweenClosed(box.minX(), box.minY(), box.minZ(), box.maxX(), box.maxY(), box.maxZ())
				.forEach(pos -> level.setBlock(pos, Blocks.AIR.defaultBlockState(), 2 | 16));
		helper.succeed();
	}

	private static ServerPlayer airborneWithElytra(GameTestHelper helper, GameType gameType) {
		ServerPlayer player = TestPlayers.create(helper, gameType, new BlockPos(3, 5, 3));
		player.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.ELYTRA));
		player.setOnGround(false);
		helper.assertTrue(LivingEntity.canGlideUsing(player.getItemBySlot(EquipmentSlot.CHEST), EquipmentSlot.CHEST),
				"precondition: vanilla considers the worn elytra a usable glider");
		helper.assertFalse(player.onGround(), "precondition: player is airborne");
		return player;
	}
}
