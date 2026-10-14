package io.github.boxofdog.simpleblock.gametest;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.piglin.Piglin;
import net.minecraft.world.entity.monster.skeleton.Skeleton;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LightBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import net.fabricmc.fabric.api.gametest.v1.GameTest;

import io.github.boxofdog.simpleblock.feature.HeavyLoad;
import io.github.boxofdog.simpleblock.feature.InventorySlots;
import io.github.boxofdog.simpleblock.feature.RetiredItems;
import io.github.boxofdog.simpleblock.tuning.Tuning;
import io.github.boxofdog.simpleblock.workwear.Headlamps;
import io.github.boxofdog.simpleblock.workwear.Workwear;
import io.github.boxofdog.simpleblock.workwear.WorkwearContent;

/**
 * M10 workwear and armor retirement. Test ids refer to the evidence in
 * {@code docs/evidence/milestone-10/}.
 */
public class WorkwearGameTests {
	private static final String YARD = "simple_block-gametest:cable_yard";
	private static final Item A = Items.AIR;
	/** Inventory menu slots: head, chest, legs, feet. */
	private static final int HEAD = 5;
	private static final int CHEST = 6;
	private static final int LEGS = 7;
	private static final int FEET = 8;

	private static void wear(LivingEntity entity, Item head, Item chest, Item feet) {
		entity.setItemSlot(EquipmentSlot.HEAD, head == A ? ItemStack.EMPTY : new ItemStack(head));
		entity.setItemSlot(EquipmentSlot.CHEST, chest == A ? ItemStack.EMPTY : new ItemStack(chest));
		entity.setItemSlot(EquipmentSlot.FEET, feet == A ? ItemStack.EMPTY : new ItemStack(feet));
	}

	private static int count(ServerPlayer player, Item item) {
		int total = player.containerMenu.getCarried().is(item) ? player.containerMenu.getCarried().getCount() : 0;
		for (ItemStack stack : player.getInventory().getNonEquipmentItems()) {
			if (stack.is(item)) {
				total += stack.getCount();
			}
		}
		for (EquipmentSlot slot : List.of(EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET, EquipmentSlot.OFFHAND)) {
			if (player.getItemBySlot(slot).is(item)) {
				total += player.getItemBySlot(slot).getCount();
			}
		}
		return total;
	}

	private static int onGround(GameTestHelper helper, Item item) {
		return TrainGameTests.total(helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(helper.absolutePos(BlockPos.ZERO)).inflate(40.0)), item);
	}

	// ---- M10-A: golden and chainmail armor retired ----

	/**
	 * ARM-01: the golden armor recipes are gone (chainmail has none); random mob armor never rolls a
	 * golden or chainmail piece (those tiers are copper now); piglins spawn without golden armor; ruined
	 * portal and bastion chests never give either set (gold itself still drops: control); armorers offer
	 * no chainmail at any level.
	 */
	@GameTest(maxTicks = 40)
	public void goldAndChainmailArmorAreRetired(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		List<String> log = new ArrayList<>();
		for (String piece : List.of("golden_helmet", "golden_chestplate", "golden_leggings", "golden_boots")) {
			helper.assertTrue(level.getServer().getRecipeManager().byKey(ResourceKey.create(Registries.RECIPE, Identifier.withDefaultNamespace(piece))).isEmpty(),
				"no " + piece + " recipe");
		}
		Item g = Items.GOLD_INGOT;
		ServerPlayer crafter = TestPlayers.create(helper, GameType.SURVIVAL, new BlockPos(2, 1, 2));
		helper.assertTrue(RecipeGameTests.craft(helper, crafter, g, A, g, g, g, g, g, g, g).isEmpty(), "the golden chestplate grid makes nothing");
		log.add("golden armor recipes: none loaded, the chestplate grid makes nothing");

		List<Item> tiers = new ArrayList<>();
		for (int type = 0; type <= 5; type++) {
			tiers.add(Mob.getEquipmentForSlot(EquipmentSlot.HEAD, type));
		}
		helper.assertValueEqual(tiers, List.of(Items.LEATHER_HELMET, Items.COPPER_HELMET, Items.COPPER_HELMET, Items.COPPER_HELMET, Items.IRON_HELMET,
			Items.DIAMOND_HELMET), "mob helmet tiers");
		helper.assertTrue(Mob.getEquipmentForSlot(EquipmentSlot.CHEST, 3) == Items.COPPER_CHESTPLATE, "chainmail tier chestplate is copper");
		log.add("mob armor tiers: " + tiers.stream().map(item -> item.toString().replace("minecraft:", "")).toList());

		int piglins = 60;
		int golden = 0;
		int weapons = 0;
		for (int i = 0; i < piglins; i++) {
			Piglin piglin = EntityTypes.PIGLIN.create(level, EntitySpawnReason.NATURAL);
			piglin.snapTo(helper.absoluteVec(new Vec3(4.5, 1, 4.5)), 0.0F, 0.0F);
			piglin.finalizeSpawn(level, level.getCurrentDifficultyAt(piglin.blockPosition()), EntitySpawnReason.NATURAL, null);
			for (EquipmentSlot slot : List.of(EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET)) {
				if (RetiredItems.isRetired(piglin.getItemBySlot(slot))) {
					golden++;
				}
			}
			if (!piglin.getMainHandItem().isEmpty()) {
				weapons++;
			}
			piglin.discard();
		}
		helper.assertValueEqual(golden, 0, "golden armor pieces on spawned piglins");
		helper.assertTrue(weapons > 0, "piglins still spawn armed (control)");
		log.add(piglins + " piglins spawned: " + golden + " golden armor pieces, " + weapons + " with weapons");

		int retiredLoot = 0;
		int gold = 0;
		for (ResourceKey<LootTable> key : List.of(BuiltInLootTables.RUINED_PORTAL, BuiltInLootTables.BASTION_TREASURE, BuiltInLootTables.BASTION_OTHER)) {
			LootTable table = level.getServer().reloadableRegistries().getLootTable(key);
			for (int i = 0; i < 150; i++) {
				LootParams params = new LootParams.Builder(level).withParameter(LootContextParams.ORIGIN, helper.absoluteVec(Vec3.ZERO))
					.create(LootContextParamSets.CHEST);
				for (ItemStack stack : table.getRandomItems(params)) {
					if (RetiredItems.ARMOR.contains(stack.getItem())) {
						retiredLoot++;
					}
					if (stack.is(Items.GOLD_INGOT) || stack.is(Items.GOLD_NUGGET) || stack.is(Items.GOLD_BLOCK) || stack.is(Items.GOLDEN_APPLE)) {
						gold++;
					}
				}
			}
		}
		helper.assertValueEqual(retiredLoot, 0, "golden or chainmail armor in 450 portal and bastion chests");
		helper.assertTrue(gold > 0, "gold itself still drops (control)");
		log.add("450 ruined portal and bastion chests: " + retiredLoot + " retired armor, " + gold + " gold stacks");

		Holder<VillagerProfession> armorer = level.registryAccess().lookupOrThrow(Registries.VILLAGER_PROFESSION).getOrThrow(VillagerProfession.ARMORER);
		int offers = 0;
		int chain = 0;
		for (int villagers = 0; villagers < 10; villagers++) {
			Villager villager = EntityTypes.VILLAGER.create(level, EntitySpawnReason.EVENT);
			villager.snapTo(helper.absoluteVec(new Vec3(4.5, 1, 4.5)), 0.0F, 0.0F);
			for (int career = 1; career <= 5; career++) {
				villager.setVillagerData(villager.getVillagerData().withProfession(armorer).withLevel(career));
				try {
					Method update = Villager.class.getDeclaredMethod("updateTrades", ServerLevel.class);
					update.setAccessible(true);
					update.invoke(villager, level);
				} catch (ReflectiveOperationException e) {
					throw new IllegalStateException(e);
				}
			}
			for (MerchantOffer offer : villager.getOffers()) {
				offers++;
				if (RetiredItems.isRetired(offer.getResult())) {
					chain++;
				}
			}
			villager.discard();
		}
		helper.assertValueEqual(chain, 0, "chainmail offers from armorers");
		helper.assertTrue(offers > 0, "armorers still trade (control)");
		log.add("10 armorers raised to master: " + offers + " offers, " + chain + " chainmail");
		SimpleBlockTestLog.info("ARM-01: {}", log);
		TestPlayers.remove(crafter);
		helper.succeed();
	}

	// ---- M10-B: recipes ----

	/** WW-01: the four workwear and two rope recipes, as owner-settled; misplaced ingredients make nothing. */
	@GameTest(maxTicks = 40)
	public void workwearAndRopeRecipes(GameTestHelper helper) {
		ServerPlayer player = TestPlayers.create(helper, GameType.SURVIVAL, new BlockPos(2, 1, 2));
		Item c = Items.COPPER_INGOT;
		Item l = Items.LANTERN;
		Item s = Items.STRING;
		Item h = Items.CHEST;
		Item lc = Items.LEATHER_CHESTPLATE;
		Item ll = Items.LEATHER_LEGGINGS;
		Item lb = Items.LEATHER_BOOTS;
		Item d = Items.POINTED_DRIPSTONE;
		Item i = Items.IRON_INGOT;
		Item an = io.github.boxofdog.simpleblock.cable.CableContent.CABLE_ANCHOR_ITEM;
		Item rope = io.github.boxofdog.simpleblock.rope.RopeContent.ROPE;
		List<String> log = new ArrayList<>();
		Object[][] makes = {
			{"headlamp", new Item[] {c, c, c, c, l, c, A, A, A}, WorkwearContent.HEADLAMP},
			{"overalls", new Item[] {s, h, s, A, lc, A, A, ll, A}, WorkwearContent.OVERALLS},
			{"spiked boots", new Item[] {A, A, A, A, lb, A, A, d, A}, WorkwearContent.SPIKED_BOOTS},
			{"mining boots", new Item[] {A, A, A, A, lb, A, A, i, A}, WorkwearContent.MINING_BOOTS},
			{"rope", new Item[] {A, s, A, A, s, A, A, s, A}, rope},
			{"spelunker's rope", new Item[] {A, A, A, A, an, A, A, rope, A}, io.github.boxofdog.simpleblock.rope.RopeContent.SPELUNKER_ROPE},
		};
		for (Object[] row : makes) {
			ItemStack result = RecipeGameTests.craft(helper, player, (Item[]) row[1]);
			helper.assertTrue(result.is((Item) row[2]) && result.getCount() == 1, row[0] + " should make one, made " + result);
			log.add(row[0] + ": " + result);
		}
		Item[][] nothing = {
			{c, l, c, c, c, c, A, A, A},
			{c, c, c, c, c, c, A, l, A},
			{h, s, s, A, lc, A, A, ll, A},
			{s, h, s, A, ll, A, A, lc, A},
			{A, A, A, A, d, A, A, lb, A},
			{A, A, A, A, i, A, A, lb, A},
			{A, A, A, s, s, s, A, A, A},
			{A, A, A, A, rope, A, A, an, A},
		};
		for (Item[] grid : nothing) {
			helper.assertTrue(RecipeGameTests.craft(helper, player, grid).isEmpty(), "should make nothing: " + java.util.Arrays.toString(grid));
		}
		log.add(nothing.length + " misplaced grids make nothing (lantern on top, lantern under the ring, chest off-centre, chestplate and leggings swapped, "
			+ "dripstone or iron over the boots, string across, rope over the anchor)");
		SimpleBlockTestLog.info("WW-01: {}", log);
		TestPlayers.remove(player);
		helper.succeed();
	}

	// ---- M10-B: the Headlamp's light ----

	private static BlockState at(GameTestHelper helper, BlockPos absolute) {
		return helper.getLevel().getBlockState(absolute);
	}

	/**
	 * HL-01: worn, the Headlamp lights the block at the head (level 13), follows the player and leaves no
	 * light behind; with the head in a block it lights the feet; it goes when taken off, on death and when
	 * the player leaves; nothing is left recorded.
	 */
	@GameTest(structure = YARD, maxTicks = 200)
	public void headlampLightsAndLeavesNothing(GameTestHelper helper) {
		ServerPlayer player = TestPlayers.create(helper, GameType.SURVIVAL, new BlockPos(5, 1, 5));
		ServerLevel level = helper.getLevel();
		BlockPos[] marks = new BlockPos[4];
		List<String> log = new ArrayList<>();
		boolean[] failed = {false};
		helper.startSequence()
			.thenExecute(CableGameTests.step(failed, () -> player.setItemSlot(EquipmentSlot.HEAD, new ItemStack(WorkwearContent.HEADLAMP))))
			.thenIdle(2)
			.thenExecute(CableGameTests.step(failed, () -> {
				marks[0] = BlockPos.containing(player.getEyePosition());
				BlockState light = at(helper, marks[0]);
				helper.assertTrue(light.is(Blocks.LIGHT) && light.getValue(LightBlock.LEVEL) == 13, "light at the head: " + light);
				helper.assertTrue(level.getBrightness(net.minecraft.world.level.LightLayer.BLOCK, marks[0]) == 13, "block light 13 at the head");
				log.add("worn: light 13 at the head " + marks[0].subtract(helper.absolutePos(BlockPos.ZERO)).toShortString());
				Vec3 to = helper.absoluteVec(new Vec3(9.5, 1, 5.5));
				player.teleportTo(to.x, to.y, to.z);
			}))
			.thenIdle(2)
			.thenExecute(CableGameTests.step(failed, () -> {
				marks[1] = BlockPos.containing(player.getEyePosition());
				helper.assertTrue(at(helper, marks[0]).isAir(), "the old light is gone: " + at(helper, marks[0]));
				helper.assertTrue(at(helper, marks[1]).is(Blocks.LIGHT), "the light followed");
				log.add("moved 4 blocks: old light gone, new one at the head");
				helper.getLevel().setBlockAndUpdate(marks[1], Blocks.GLASS.defaultBlockState());
			}))
			.thenIdle(2)
			.thenExecute(CableGameTests.step(failed, () -> {
				helper.assertTrue(at(helper, marks[1]).is(Blocks.GLASS), "a block at the head is never replaced");
				helper.assertTrue(at(helper, player.blockPosition()).is(Blocks.LIGHT), "with the head in glass, the light is at the feet");
				log.add("head in glass: glass kept, light at the feet");
				marks[2] = player.blockPosition();
				helper.getLevel().setBlockAndUpdate(marks[1], Blocks.AIR.defaultBlockState());
				player.setItemSlot(EquipmentSlot.HEAD, ItemStack.EMPTY);
			}))
			.thenIdle(2)
			.thenExecute(CableGameTests.step(failed, () -> {
				helper.assertTrue(at(helper, marks[2]).isAir() && at(helper, marks[1]).isAir(), "taken off: no light left");
				helper.assertTrue(Headlamps.lightOf(player.getUUID()) == null, "nothing held for the player");
				log.add("taken off: no light left");
				player.setItemSlot(EquipmentSlot.HEAD, new ItemStack(WorkwearContent.HEADLAMP));
			}))
			.thenIdle(2)
			.thenExecute(CableGameTests.step(failed, () -> {
				marks[3] = BlockPos.containing(player.getEyePosition());
				helper.assertTrue(at(helper, marks[3]).is(Blocks.LIGHT), "worn again: lit");
				player.kill(level);
			}))
			.thenIdle(2)
			.thenExecute(CableGameTests.step(failed, () -> {
				helper.assertTrue(at(helper, marks[3]).isAir(), "dead: no light left");
				log.add("died: no light left");
			}))
			.thenExecute(CableGameTests.step(failed, () -> {
				ServerPlayer second = TestPlayers.create(helper, GameType.SURVIVAL, new BlockPos(14, 1, 5));
				second.setItemSlot(EquipmentSlot.HEAD, new ItemStack(WorkwearContent.HEADLAMP));
				marks[0] = BlockPos.containing(second.getEyePosition());
				TestPlayers.remove(second);
			}))
			.thenIdle(1)
			.thenExecute(CableGameTests.step(failed, () -> {
				// It was lit for a tick before leaving, at most; now it must be gone.
				helper.assertTrue(at(helper, marks[0]).isAir(), "left the game: no light left");
				log.add("left the game: no light left");
				SimpleBlockTestLog.info("HL-01: {}", log);
				TestPlayers.remove(player);
			}))
			.thenSucceed();
	}

	// ---- M10-B: Overalls take two slots and open the backpack ----

	/**
	 * OV-01: put on over leggings, the Overalls move the leggings into the inventory; leggings can then
	 * not go on (slot or right-click); the backpack row takes items (36 slots); taken off with the row
	 * full, its items move to free slots or drop, nothing lost or made.
	 */
	@GameTest(structure = YARD, maxTicks = 100)
	public void overallsTakeTwoSlotsAndOpenTheBackpack(GameTestHelper helper) {
		ServerPlayer player = TestPlayers.create(helper, GameType.SURVIVAL, new BlockPos(5, 1, 5));
		List<String> log = new ArrayList<>();
		boolean[] failed = {false};
		helper.startSequence()
			.thenExecute(CableGameTests.step(failed, () -> {
				player.setItemSlot(EquipmentSlot.LEGS, new ItemStack(Items.LEATHER_LEGGINGS));
				player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(WorkwearContent.OVERALLS));
				player.gameMode.useItem(player, helper.getLevel(), player.getMainHandItem(), InteractionHand.MAIN_HAND);
				helper.assertTrue(Workwear.overalls(player), "overalls on");
			}))
			.thenIdle(1)
			.thenExecute(CableGameTests.step(failed, () -> {
				helper.assertTrue(player.getItemBySlot(EquipmentSlot.LEGS).isEmpty(), "the legs slot is cleared");
				helper.assertValueEqual(count(player, Items.LEATHER_LEGGINGS), 1, "the leggings are in the inventory");
				log.add("overalls on over leggings: leggings moved into the inventory");

				player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.IRON_LEGGINGS));
				player.gameMode.useItem(player, helper.getLevel(), player.getMainHandItem(), InteractionHand.MAIN_HAND);
				helper.assertTrue(player.getItemBySlot(EquipmentSlot.LEGS).isEmpty() && player.getMainHandItem().is(Items.IRON_LEGGINGS),
					"right-clicking leggings on is refused");
				player.inventoryMenu.setCarried(player.getMainHandItem().copy());
				player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
				player.inventoryMenu.clicked(LEGS, 0, ContainerInput.PICKUP, player);
				helper.assertTrue(player.getItemBySlot(EquipmentSlot.LEGS).isEmpty() && player.inventoryMenu.getCarried().is(Items.IRON_LEGGINGS),
					"clicking leggings into the slot is refused");
				player.inventoryMenu.setCarried(ItemStack.EMPTY);
				log.add("leggings refused while the overalls are on: right-click and slot click");

				player.getInventory().clearContent();
				player.setItemSlot(EquipmentSlot.CHEST, new ItemStack(WorkwearContent.OVERALLS));
				for (int slot = 0; slot < 36; slot++) {
					if (InventorySlots.isDisabled(slot)) {
						continue;
					}
					player.getInventory().setItem(slot, new ItemStack(Items.COBBLESTONE, 64));
				}
				boolean taken = player.getInventory().add(new ItemStack(Items.DIRT, 64 * 9));
				int inRow = 0;
				for (int slot = InventorySlots.FIRST_DISABLED; slot <= InventorySlots.LAST_DISABLED; slot++) {
					inRow += player.getInventory().getItem(slot).is(Items.DIRT) ? player.getInventory().getItem(slot).getCount() : 0;
				}
				helper.assertTrue(taken && inRow == 576, "the backpack row took 9 stacks: " + inRow);
				helper.assertTrue(player.inventoryMenu.slots.stream().filter(slot -> slot.container == player.getInventory() && InventorySlots.isRow(slot))
					.allMatch(slot -> slot.isActive() && slot.mayPickup(player)), "the row's slots are usable");
				log.add("with the overalls: 9 stacks went into the backpack row (36 slots)");
				// Take the overalls off with everything full.
				player.inventoryMenu.clicked(CHEST, 0, ContainerInput.PICKUP, player);
				helper.assertTrue(player.inventoryMenu.getCarried().is(WorkwearContent.OVERALLS), "overalls taken off");
			}))
			.thenIdle(2)
			.thenExecute(CableGameTests.step(failed, () -> {
				int dirt = count(player, Items.DIRT) + onGround(helper, Items.DIRT);
				helper.assertValueEqual(dirt, 576, "every item of the row is kept (inventory or dropped)");
				for (int slot = InventorySlots.FIRST_DISABLED; slot <= InventorySlots.LAST_DISABLED; slot++) {
					helper.assertTrue(player.getInventory().getItem(slot).isEmpty(), "the row is empty again");
				}
				log.add("taken off with the inventory full: the row's 576 dirt dropped at the feet, none lost, none made; row empty");
				SimpleBlockTestLog.info("OV-01: {}", log);
				helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(helper.absolutePos(BlockPos.ZERO)).inflate(40.0)).forEach(ItemEntity::discard);
				TestPlayers.remove(player);
			}))
			.thenSucceed();
	}

	// ---- M10-C: the named bonuses ----

	/**
	 * BON-01: Spiked Boots alone show Spelunker's Speed, with the Headlamp and Overalls Trail Blazer
	 * (only); Mining Boots alone Mule, with the outfit Old Pro (only); a headlamp and overalls without
	 * workwear boots show nothing. Climbing: 0.2 lift without, scaled with Spiked Boots (also on a zombie
	 * wearing them).
	 */
	@GameTest(structure = YARD, maxTicks = 100)
	public void bonusesAndClimbing(GameTestHelper helper) {
		ServerPlayer player = TestPlayers.create(helper, GameType.SURVIVAL, new BlockPos(5, 1, 5));
		Object[][] outfits = {
			{A, A, WorkwearContent.SPIKED_BOOTS, WorkwearContent.SPELUNKERS_SPEED},
			{WorkwearContent.HEADLAMP, WorkwearContent.OVERALLS, WorkwearContent.SPIKED_BOOTS, WorkwearContent.TRAIL_BLAZER},
			{A, A, WorkwearContent.MINING_BOOTS, WorkwearContent.MULE},
			{WorkwearContent.HEADLAMP, WorkwearContent.OVERALLS, WorkwearContent.MINING_BOOTS, WorkwearContent.OLD_PRO},
			{WorkwearContent.HEADLAMP, WorkwearContent.OVERALLS, Items.LEATHER_BOOTS, null},
		};
		List<Holder<MobEffect>> all = List.of(WorkwearContent.SPELUNKERS_SPEED, WorkwearContent.TRAIL_BLAZER, WorkwearContent.MULE, WorkwearContent.OLD_PRO);
		List<String> log = new ArrayList<>();
		int[] step = {0};
		boolean[] failed = {false};
		var sequence = helper.startSequence();
		for (Object[] outfit : outfits) {
			sequence = sequence.thenExecute(CableGameTests.step(failed, () -> wear(player, (Item) outfit[0], (Item) outfit[1], (Item) outfit[2])))
				.thenIdle(2)
				.thenExecute(CableGameTests.step(failed, () -> {
					@SuppressWarnings("unchecked")
					Holder<MobEffect> expected = (Holder<MobEffect>) outfit[3];
					List<String> shown = new ArrayList<>();
					for (Holder<MobEffect> effect : all) {
						if (player.hasEffect(effect)) {
							shown.add(effect.unwrapKey().orElseThrow().identifier().getPath());
						}
					}
					helper.assertTrue(expected == null ? shown.isEmpty() : shown.size() == 1 && player.hasEffect(expected),
						"outfit " + step[0] + " shows " + shown);
					log.add(java.util.Arrays.stream(outfit, 0, 3).map(item -> item.toString().replaceAll("^.*:", "")).toList() + " -> " + shown);
					step[0]++;
				}));
		}
		sequence.thenExecute(CableGameTests.step(failed, () -> {
			ServerLevel level = helper.getLevel();
			BlockPos ladder = helper.absolutePos(new BlockPos(20, 1, 8));
			level.setBlockAndUpdate(ladder.south(), Blocks.STONE.defaultBlockState());
			level.setBlockAndUpdate(ladder, Blocks.LADDER.defaultBlockState());
			double[] lift = new double[3];
			Zombie zombie = EntityTypes.ZOMBIE.create(level, EntitySpawnReason.EVENT);
			zombie.setNoAi(true);
			zombie.snapTo(Vec3.atBottomCenterOf(ladder), 0.0F, 0.0F);
			level.addFreshEntity(zombie);
			try {
				Method move = LivingEntity.class.getDeclaredMethod("handleRelativeFrictionAndCalculateMovement", Vec3.class, float.class);
				move.setAccessible(true);
				for (int i = 0; i < 2; i++) {
					zombie.setItemSlot(EquipmentSlot.FEET, i == 0 ? ItemStack.EMPTY : new ItemStack(WorkwearContent.SPIKED_BOOTS));
					zombie.setDeltaMovement(Vec3.ZERO);
					zombie.snapTo(Vec3.atBottomCenterOf(ladder), 0.0F, 0.0F);
					zombie.setJumping(true);
					lift[i] = ((Vec3) move.invoke(zombie, Vec3.ZERO, 0.6F)).y;
				}
			} catch (ReflectiveOperationException e) {
				throw new IllegalStateException(e);
			}
			lift[2] = Workwear.climbMultiplier(zombie);
			helper.assertTrue(zombie.onClimbable(), "the zombie is on the ladder");
			helper.assertTrue(Math.abs(lift[0] - 0.2) < 1.0E-9, "vanilla climbing lift 0.2: " + lift[0]);
			double expected = 0.08 + 0.12 * Tuning.getDouble("workwear.climb_speed");
			helper.assertTrue(Math.abs(lift[1] - expected) < 1.0E-9, "Spiked Boots climbing lift " + expected + ": " + lift[1]);
			log.add(String.format("ladder lift: %.3f without, %.3f with Spiked Boots (net climb x%.2f)", lift[0], lift[1], lift[2]));
			zombie.discard();
			SimpleBlockTestLog.info("BON-01: {}", log);
			TestPlayers.remove(player);
		})).thenSucceed();
	}

	// ---- M10-C: Mule, Old Pro's lock ----

	/** Whether clicking (or shift-clicking, or number-key swapping) the armor slot left everything as it was. */
	private static boolean refuses(ServerPlayer player, int menuSlot, EquipmentSlot slot) {
		ItemStack before = player.getItemBySlot(slot).copy();
		int total = count(player, before.getItem());
		player.inventoryMenu.clicked(menuSlot, 0, ContainerInput.PICKUP, player);
		boolean pickup = ItemStack.matches(before, player.getItemBySlot(slot)) && player.inventoryMenu.getCarried().isEmpty();
		player.inventoryMenu.clicked(menuSlot, 0, ContainerInput.QUICK_MOVE, player);
		boolean quick = ItemStack.matches(before, player.getItemBySlot(slot));
		// The last hotbar slot, which the tests keep empty (overwriting carried commodities would end the overload).
		int key = 8;
		player.getInventory().setItem(key, new ItemStack(slot == EquipmentSlot.FEET ? Items.LEATHER_BOOTS : slot == EquipmentSlot.HEAD ? Items.LEATHER_HELMET
			: slot == EquipmentSlot.CHEST ? Items.LEATHER_CHESTPLATE : Items.LEATHER_LEGGINGS));
		player.inventoryMenu.clicked(menuSlot, key, ContainerInput.SWAP, player);
		boolean swap = ItemStack.matches(before, player.getItemBySlot(slot));
		player.getInventory().setSelectedSlot(key);
		player.gameMode.useItem(player, player.level(), player.getMainHandItem(), InteractionHand.MAIN_HAND);
		boolean rightClick = ItemStack.matches(before, player.getItemBySlot(slot));
		player.getInventory().setItem(key, ItemStack.EMPTY);
		player.getInventory().setSelectedSlot(0);
		boolean kept = count(player, before.getItem()) == total;
		if (!(pickup && quick && swap && rightClick && kept)) {
			SimpleBlockTestLog.info("lock check on {} let something through: pickup {} quick {} swap {} rightClick {} kept {}", slot, pickup, quick, swap, rightClick, kept);
		}
		return pickup && quick && swap && rightClick && kept;
	}

	/**
	 * MULE-01: Mining Boots carry commodities past 64 (to 128) with a slowdown growing with the overload;
	 * while overloaded the boots are locked (click, shift-click, number key, right-click); equipment alone
	 * over 64 locks nothing; with Old Pro all four slots lock; carrying less unlocks; nothing is lost.
	 */
	@GameTest(structure = YARD, maxTicks = 100)
	public void muleCarriesAndTheOverloadLocks(GameTestHelper helper) {
		ServerPlayer player = TestPlayers.create(helper, GameType.SURVIVAL, new BlockPos(5, 1, 5));
		List<String> log = new ArrayList<>();
		boolean[] failed = {false};
		helper.startSequence()
			.thenExecute(CableGameTests.step(failed, () -> {
				player.getInventory().add(new ItemStack(Items.RAW_IRON, 100));
				helper.assertValueEqual(count(player, Items.RAW_IRON), 64, "without Mining Boots, 64 raw iron");
				player.getInventory().clearContent();
				wear(player, A, A, WorkwearContent.MINING_BOOTS);
				player.getInventory().add(new ItemStack(Items.RAW_IRON, 100));
				player.getInventory().add(new ItemStack(Items.RAW_IRON, 64));
				helper.assertValueEqual(count(player, Items.RAW_IRON), 128, "with Mining Boots, up to 128 raw iron");
				log.add("raw iron taken in: 64 without, 128 with Mining Boots");
			}))
			.thenIdle(2)
			.thenExecute(CableGameTests.step(failed, () -> {
				AttributeModifier slow = player.getAttribute(Attributes.MOVEMENT_SPEED).getModifier(Identifier.fromNamespaceAndPath("simple_block", "mule_slowdown"));
				helper.assertTrue(slow != null && Math.abs(slow.amount() + 0.5) < 1.0E-6, "full overload: -50% speed, " + slow);
				helper.assertTrue(Workwear.locked(player, EquipmentSlot.FEET) && !Workwear.locked(player, EquipmentSlot.HEAD), "only the boots lock for Mule");
				helper.assertTrue(refuses(player, FEET, EquipmentSlot.FEET), "the boots cannot be taken off or swapped");
				log.add("128 carried: speed -50%; Mining Boots locked against click, shift-click, number key and right-click; nothing lost");
				// Half the overload.
				for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
					ItemStack stack = player.getInventory().getItem(i);
					if (stack.is(Items.RAW_IRON)) {
						player.getInventory().setItem(i, ItemStack.EMPTY);
					}
				}
				player.getInventory().add(new ItemStack(Items.RAW_IRON, 96));
			}))
			.thenIdle(2)
			.thenExecute(CableGameTests.step(failed, () -> {
				AttributeModifier slow = player.getAttribute(Attributes.MOVEMENT_SPEED).getModifier(Identifier.fromNamespaceAndPath("simple_block", "mule_slowdown"));
				helper.assertTrue(slow != null && Math.abs(slow.amount() + 0.25) < 1.0E-6, "half overload: -25% speed, " + slow);
				log.add("96 carried: speed -25%");
				player.getInventory().clearContent();
				wear(player, A, A, WorkwearContent.MINING_BOOTS);
				player.getInventory().add(new ItemStack(Items.ANVIL, 3));
				helper.assertTrue(HeavyLoad.carried(player) > HeavyLoad.CAPACITY && Workwear.commodityOverload(player) == 0, "equipment alone over 64");
				helper.assertTrue(Workwear.lockedSlots(player).isEmpty(), "equipment alone locks nothing");
				player.inventoryMenu.clicked(FEET, 0, ContainerInput.PICKUP, player);
				helper.assertTrue(player.inventoryMenu.getCarried().is(WorkwearContent.MINING_BOOTS), "the boots come off");
				player.inventoryMenu.clicked(FEET, 0, ContainerInput.PICKUP, player);
				helper.assertTrue(player.getItemBySlot(EquipmentSlot.FEET).is(WorkwearContent.MINING_BOOTS), "and go back on");
				log.add("3 anvils (93, equipment): not locked, the boots come off and back on");

				player.getInventory().clearContent();
				wear(player, WorkwearContent.HEADLAMP, WorkwearContent.OVERALLS, WorkwearContent.MINING_BOOTS);
				player.getInventory().add(new ItemStack(Items.RAW_IRON, 100));
				helper.assertValueEqual(Workwear.bonus(player), Workwear.Bonus.OLD_PRO, "Old Pro");
				helper.assertValueEqual(Workwear.lockedSlots(player).size(), 4, "Old Pro overload locks all four slots");
				helper.assertTrue(refuses(player, HEAD, EquipmentSlot.HEAD) && refuses(player, CHEST, EquipmentSlot.CHEST) && refuses(player, FEET, EquipmentSlot.FEET),
					"headlamp, overalls and boots locked");
				player.inventoryMenu.setCarried(new ItemStack(Items.LEATHER_LEGGINGS));
				player.inventoryMenu.clicked(LEGS, 0, ContainerInput.PICKUP, player);
				helper.assertTrue(player.getItemBySlot(EquipmentSlot.LEGS).isEmpty(), "the overalls' legs slot stays empty");
				player.inventoryMenu.setCarried(ItemStack.EMPTY);
				log.add("Old Pro with 100 raw iron: all four slots locked; nothing lost");
				for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
					ItemStack stack = player.getInventory().getItem(i);
					if (stack.is(Items.RAW_IRON)) {
						stack.shrink(stack.getCount());
					}
				}
				player.getInventory().add(new ItemStack(Items.RAW_IRON, 60));
				helper.assertTrue(Workwear.lockedSlots(player).isEmpty(), "under 64 again: unlocked");
				player.inventoryMenu.clicked(HEAD, 0, ContainerInput.PICKUP, player);
				helper.assertTrue(player.inventoryMenu.getCarried().is(WorkwearContent.HEADLAMP), "the headlamp comes off once unlocked");
				player.inventoryMenu.clicked(HEAD, 0, ContainerInput.PICKUP, player);
				log.add("down to 60 raw iron: unlocked, the headlamp comes off");
				player.getInventory().add(new ItemStack(Items.RAW_IRON, 60));
				helper.assertTrue(!Workwear.lockedSlots(player).isEmpty(), "locked again");
				player.kill(helper.getLevel());
			}))
			.thenIdle(2)
			.thenExecute(CableGameTests.step(failed, () -> {
				helper.assertValueEqual(onGround(helper, Items.RAW_IRON), 120, "died locked: the raw iron drops");
				helper.assertValueEqual(onGround(helper, WorkwearContent.MINING_BOOTS) + onGround(helper, WorkwearContent.OVERALLS)
					+ onGround(helper, WorkwearContent.HEADLAMP), 3, "and the three pieces");
				log.add("died locked: 120 raw iron and the three pieces dropped");
				SimpleBlockTestLog.info("MULE-01: {}", log);
				helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(helper.absolutePos(BlockPos.ZERO)).inflate(40.0)).forEach(ItemEntity::discard);
				TestPlayers.remove(player);
			}))
			.thenSucceed();
	}

	/**
	 * OLD-01: with Old Pro, an iron pickaxe in the offhand mines stone and iron ore at its speed and with
	 * its drops while a sword is held: the pickaxe wears, the sword does not. Mule alone: the sword's speed
	 * and no ore drop. The main hand's own tool wins where it is right.
	 */
	@GameTest(structure = YARD, maxTicks = 60)
	public void oldProMinesWithTheOffhandPickaxe(GameTestHelper helper) {
		ServerPlayer player = TestPlayers.create(helper, GameType.SURVIVAL, new BlockPos(5, 1, 5));
		ServerLevel level = helper.getLevel();
		List<String> log = new ArrayList<>();
		BlockPos ore = helper.absolutePos(new BlockPos(6, 1, 6));
		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.IRON_SWORD));
		player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(Items.IRON_PICKAXE));
		BlockState stone = Blocks.STONE.defaultBlockState();
		wear(player, A, A, WorkwearContent.MINING_BOOTS);
		float muleSpeed = player.getDestroySpeed(stone);
		boolean muleDrops = player.hasCorrectToolForDrops(Blocks.IRON_ORE.defaultBlockState());
		wear(player, WorkwearContent.HEADLAMP, WorkwearContent.OVERALLS, WorkwearContent.MINING_BOOTS);
		float oldProSpeed = player.getDestroySpeed(stone);
		boolean oldProDrops = player.hasCorrectToolForDrops(Blocks.IRON_ORE.defaultBlockState());
		// (The test player is not on the ground, so every speed is a fifth: the ratio is what counts.)
		helper.assertTrue(!muleDrops, "Mule alone: no ore drop with the sword");
		helper.assertTrue(Workwear.miningTool(player, stone) == player.getOffhandItem() && oldProSpeed >= muleSpeed * 5.0F && oldProDrops,
			"Old Pro: the pickaxe's speed (x6 the sword's) and drops: " + oldProSpeed + " vs " + muleSpeed);
		level.setBlockAndUpdate(ore, Blocks.IRON_ORE.defaultBlockState());
		player.gameMode.destroyBlock(ore);
		List<ItemEntity> drops = level.getEntitiesOfClass(ItemEntity.class, new AABB(ore).inflate(2.0));
		int raw = TrainGameTests.total(drops, Items.RAW_IRON);
		helper.assertTrue(raw >= 1, "the ore drops raw iron");
		helper.assertTrue(player.getOffhandItem().getDamageValue() == 1 && player.getMainHandItem().getDamageValue() == 0,
			"the pickaxe wears, not the sword: " + player.getOffhandItem().getDamageValue() + "/" + player.getMainHandItem().getDamageValue());
		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.IRON_SHOVEL));
		float dirtSpeed = player.getDestroySpeed(Blocks.DIRT.defaultBlockState());
		helper.assertTrue(Workwear.miningTool(player, Blocks.DIRT.defaultBlockState()).is(Items.IRON_SHOVEL)
			&& Workwear.miningTool(player, stone).is(Items.IRON_PICKAXE), "a shovel digs dirt itself; stone goes to the pickaxe");
		log.add(String.format("sword in hand, iron pickaxe in offhand: Mule alone stone speed %.2f, ore drops %s; Old Pro %.2f (the pickaxe), %s; iron ore broken: %d raw iron, "
			+ "pickaxe damage 1, sword 0; shovel in hand digs dirt itself (%.2f)", muleSpeed, muleDrops, oldProSpeed, oldProDrops, raw, dirtSpeed));
		SimpleBlockTestLog.info("OLD-01: {}", log);
		drops.forEach(ItemEntity::discard);
		TestPlayers.remove(player);
		helper.succeed();
	}

	/**
	 * TB-01: a skeleton's arrows scatter more at a target with the full spelunking outfit (Trail Blazer)
	 * than at the same target without it: mean angle off the line, 60 shots each.
	 */
	@GameTest(structure = YARD, maxTicks = 60)
	public void trailBlazerSpoilsSkeletonAim(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		Skeleton skeleton = EntityTypes.SKELETON.create(level, EntitySpawnReason.EVENT);
		skeleton.setNoAi(true);
		skeleton.snapTo(helper.absoluteVec(new Vec3(3.5, 1, 5.5)), -90.0F, 0.0F);
		skeleton.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.BOW));
		level.addFreshEntity(skeleton);
		Zombie target = EntityTypes.ZOMBIE.create(level, EntitySpawnReason.EVENT);
		target.setNoAi(true);
		target.snapTo(helper.absoluteVec(new Vec3(17.5, 1, 5.5)), 0.0F, 0.0F);
		level.addFreshEntity(target);
		double[] spread = new double[2];
		for (int run = 0; run < 2; run++) {
			wear(target, run == 0 ? A : WorkwearContent.HEADLAMP, run == 0 ? A : WorkwearContent.OVERALLS, run == 0 ? A : WorkwearContent.SPIKED_BOOTS);
			double total = 0.0;
			int shots = 60;
			for (int i = 0; i < shots; i++) {
				skeleton.performRangedAttack(target, 1.0F);
				List<AbstractArrow> arrows = level.getEntitiesOfClass(AbstractArrow.class, skeleton.getBoundingBox().inflate(3.0));
				for (AbstractArrow arrow : arrows) {
					Vec3 v = arrow.getDeltaMovement().normalize();
					Vec3 ideal = target.position().subtract(arrow.position());
					// Horizontal spread only: the arc is the skeleton's own.
					double angle = Math.abs(Math.atan2(v.z, v.x) - Math.atan2(ideal.z, ideal.x));
					total += Math.toDegrees(Math.min(angle, 2 * Math.PI - angle));
					arrow.discard();
				}
			}
			spread[run] = total / shots;
		}
		helper.assertTrue(Workwear.bonus(target) == Workwear.Bonus.TRAIL_BLAZER, "the target is a Trail Blazer");
		helper.assertTrue(spread[1] > spread[0] * 1.5, String.format("arrows should scatter more at a Trail Blazer: %.2f vs %.2f degrees", spread[1], spread[0]));
		SimpleBlockTestLog.info("TB-01: skeleton at 14 blocks, 60 shots each: mean horizontal miss {} degrees at a plain target, {} at a Trail Blazer "
			+ "(aim penalty +{})", String.format("%.2f", spread[0]), String.format("%.2f", spread[1]), Tuning.getDouble("workwear.trail_blazer_aim_penalty"));
		skeleton.discard();
		target.discard();
		helper.succeed();
	}
}
