package io.github.boxofdog.simpleblock.gametest;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.vehicle.minecart.Minecart;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.AbstractFurnaceBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.entity.FurnaceBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.RailShape;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.pools.StructurePoolElement;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;
import net.minecraft.world.level.levelgen.structure.templatesystem.LiquidSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import net.fabricmc.fabric.api.gametest.v1.GameTest;

import io.github.boxofdog.simpleblock.feature.HeavyLoad;
import io.github.boxofdog.simpleblock.feature.MinecartCargo;
import io.github.boxofdog.simpleblock.metallurgy.BlastFurnaceBlock;
import io.github.boxofdog.simpleblock.metallurgy.BlastFurnaceBlockEntity;
import io.github.boxofdog.simpleblock.metallurgy.BlastFurnaceFreight;
import io.github.boxofdog.simpleblock.metallurgy.BlastFurnaceMenu;
import io.github.boxofdog.simpleblock.metallurgy.BlastFurnacePartBlock;
import io.github.boxofdog.simpleblock.metallurgy.BlastFurnaceStructure;
import io.github.boxofdog.simpleblock.metallurgy.MetallurgyContent;
import io.github.boxofdog.simpleblock.skip.OreSkip;
import io.github.boxofdog.simpleblock.skip.SkipStations;
import io.github.boxofdog.simpleblock.train.Couplings;
import io.github.boxofdog.simpleblock.train.LoadingTrackBlock;
import io.github.boxofdog.simpleblock.train.LoadingTracks;
import io.github.boxofdog.simpleblock.train.TrainContent;
import io.github.boxofdog.simpleblock.tuning.Tuning;

/**
 * M08 pre-industrial blast furnace: recipe and mirror, the one-piece 3x3x4 structure, the 516-ore
 * reservoir, fuel and mold, casting, save and load, cart and skip delivery into its hopper with their
 * refusals, the retired vanilla blast furnace in new villages, and the slower ordinary furnace.
 */
public class BlastFurnaceGameTests {
	private static final String SHAFT = SkipGameTests.SHAFT;
	/** A free furnace in the shaft structure, away from the shaft: its front middle bottom block, facing north. */
	private static final BlockPos FRONT = new BlockPos(18, 1, 5);
	private static final BlockPos CONTROLLER = BlastFurnaceStructure.controllerFor(FRONT, Direction.NORTH);

	private static void floor(GameTestHelper helper) {
		for (int x = 0; x < 24; x++) {
			for (int z = 0; z < 12; z++) {
				helper.setBlock(new BlockPos(x, 0, z), Blocks.STONE);
			}
		}
	}

	/** Builds a furnace block by block (as its item does), facing north, around {@code controller} (relative). */
	static BlastFurnaceBlockEntity build(GameTestHelper helper, BlockPos controller) {
		BlockPos absolute = helper.absolutePos(controller);
		for (BlockPos pos : BlastFurnaceStructure.blocks(absolute)) {
			helper.getLevel().setBlockAndUpdate(pos, BlastFurnaceStructure.stateAt(absolute, pos, Direction.NORTH));
		}
		return (BlastFurnaceBlockEntity) helper.getLevel().getBlockEntity(absolute);
	}

	private static ItemStack craft(GameTestHelper helper, Item... grid) {
		List<ItemStack> items = new ArrayList<>();
		for (Item item : grid) {
			items.add(item == Items.AIR ? ItemStack.EMPTY : new ItemStack(item));
		}
		CraftingInput input = CraftingInput.of(3, 3, items);
		ServerLevel level = helper.getLevel();
		return level.getServer().getRecipeManager().getRecipeFor(RecipeType.CRAFTING, input, level)
			.map(holder -> holder.value().assemble(input))
			.orElse(ItemStack.EMPTY);
	}

	private static int dropped(GameTestHelper helper, BlockPos around, Item item) {
		return helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(helper.absolutePos(around)).inflate(4.0),
			entity -> entity.getItem().is(item)).stream().mapToInt(entity -> entity.getItem().getCount()).sum();
	}

	private static void clearDrops(GameTestHelper helper, BlockPos around) {
		helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(helper.absolutePos(around)).inflate(6.0)).forEach(ItemEntity::discard);
	}

	private static int structureBlocks(GameTestHelper helper, BlockPos controller) {
		int n = 0;
		for (BlockPos pos : BlastFurnaceStructure.blocks(helper.absolutePos(controller))) {
			BlockState state = helper.getLevel().getBlockState(pos);
			if (state.is(MetallurgyContent.BLAST_FURNACE) || state.is(MetallurgyContent.BLAST_FURNACE_PART)) {
				n++;
			}
		}
		return n;
	}

	/** Runs the furnace's own server tick {@code ticks} times at once (casting takes hundreds). */
	private static void run(GameTestHelper helper, BlastFurnaceBlockEntity furnace, int ticks) {
		for (int i = 0; i < ticks; i++) {
			BlastFurnaceBlockEntity.serverTick(helper.getLevel(), furnace.getBlockPos(), helper.getLevel().getBlockState(furnace.getBlockPos()), furnace);
		}
	}

	// ---- Recipes and retirement ----

	/**
	 * M08-F1: the owner's shaped recipe (hopper top middle; Bricks block and iron block; Bricks block,
	 * furnace and cauldron; the rest empty) and its horizontal mirror each make one furnace. A rotation,
	 * swapped ingredients, an empty slot filled, Brick items or iron ingots for the Bricks blocks (the
	 * superseded recipe) make nothing. Nine charcoal make a Block of Charcoal; the vanilla blast furnace
	 * recipe is gone.
	 */
	@GameTest
	public void furnaceRecipeAndMirrorOnly(GameTestHelper helper) {
		Item a = Items.AIR;
		Item h = Items.HOPPER;
		Item b = Items.BRICKS;
		Item i = Items.IRON_BLOCK;
		Item f = Items.FURNACE;
		Item c = Items.CAULDRON;
		ItemStack made = craft(helper, a, h, a, b, i, a, b, f, c);
		helper.assertTrue(made.is(MetallurgyContent.BLAST_FURNACE_ITEM) && made.getCount() == 1, "owner's recipe: " + made);
		ItemStack mirrored = craft(helper, a, h, a, a, i, b, c, f, b);
		helper.assertTrue(mirrored.is(MetallurgyContent.BLAST_FURNACE_ITEM) && mirrored.getCount() == 1, "mirror: " + mirrored);
		List<Item[]> wrong = List.of(
			new Item[] {b, b, a, f, i, h, c, a, a},          // rotated 90 degrees
			new Item[] {c, f, b, a, i, b, a, h, a},          // rotated 180 degrees
			new Item[] {a, h, a, b, i, a, b, c, f},          // furnace and cauldron swapped
			new Item[] {Items.STICK, h, a, b, i, a, b, f, c}, // an empty slot filled
			new Item[] {a, h, a, Items.BRICK, i, a, Items.BRICK, f, c}, // brick items, not Bricks blocks
			new Item[] {a, h, a, Items.IRON_INGOT, i, a, Items.IRON_INGOT, f, c}); // the superseded recipe's ingots
		for (Item[] grid : wrong) {
			helper.assertTrue(craft(helper, grid).isEmpty(), "should make nothing: " + java.util.Arrays.toString(grid));
		}
		Item ch = Items.CHARCOAL;
		ItemStack charcoal = craft(helper, ch, ch, ch, ch, ch, ch, ch, ch, ch);
		helper.assertTrue(charcoal.is(MetallurgyContent.CHARCOAL_BLOCK_ITEM) && charcoal.getCount() == 1, "nine charcoal: " + charcoal);
		Item ingot = Items.IRON_INGOT;
		Item stone = Items.SMOOTH_STONE;
		helper.assertTrue(craft(helper, ingot, ingot, ingot, ingot, f, ingot, stone, stone, stone).isEmpty(), "vanilla blast furnace recipe is retired");
		helper.assertTrue(helper.getLevel().getServer().getRecipeManager().byKey(net.minecraft.resources.ResourceKey.create(
			net.minecraft.core.registries.Registries.RECIPE, net.minecraft.resources.Identifier.withDefaultNamespace("blast_furnace"))).isEmpty(),
			"no blast_furnace recipe loaded");
		helper.assertTrue(HeavyLoad.unitWeight(new ItemStack(MetallurgyContent.BLAST_FURNACE_ITEM)) == 21 * HeavyLoad.UNITS_PER_INGOT
			&& !HeavyLoad.isCommodity(new ItemStack(MetallurgyContent.BLAST_FURNACE_ITEM)), "the furnace item weighs its 21 iron, as equipment");
		SimpleBlockTestLog.info("M08-F1: owner's furnace recipe and its mirror make one; 2 rotations, swapped, filled, brick-item and ingot "
			+ "variants make nothing; 9 charcoal -> Block of Charcoal; vanilla blast furnace recipe gone; furnace item weighs 21 (equipment)");
		helper.succeed();
	}

	// ---- Structure ----

	/**
	 * M08-F2: the item places the whole 3x3x4 furnace facing the player, the clicked block its front middle
	 * bottom; it is used up. A block in the way stops it, item kept. Breaking any block of it breaks it all:
	 * the furnace item drops with everything inside (reservoir ore as stacks, fuel, cast blocks). Broken
	 * in creative, only the contents drop.
	 */
	@GameTest(structure = SHAFT)
	public void furnacePlacesWholeAndBreaksWhole(GameTestHelper helper) {
		floor(helper);
		ServerLevel level = helper.getLevel();
		ServerPlayer player = TestPlayers.create(helper, GameType.SURVIVAL, new BlockPos(18, 1, 1));
		player.setYRot(0.0F);
		// A block in the way at the top back corner: nothing is placed.
		helper.setBlock(FRONT.offset(1, 3, 2), Blocks.STONE);
		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(MetallurgyContent.BLAST_FURNACE_ITEM));
		BlockPos floor = helper.absolutePos(FRONT.below());
		BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(floor).add(0.0, 0.5, 0.0), Direction.UP, floor, false);
		player.gameMode.useItemOn(player, level, player.getMainHandItem(), InteractionHand.MAIN_HAND, hit);
		helper.assertValueEqual(structureBlocks(helper, CONTROLLER), 0, "blocks placed with one in the way");
		helper.assertValueEqual(player.getMainHandItem().getCount(), 1, "item kept");
		helper.setBlock(FRONT.offset(1, 3, 2), Blocks.AIR);
		player.gameMode.useItemOn(player, level, player.getMainHandItem(), InteractionHand.MAIN_HAND, hit);
		helper.assertValueEqual(structureBlocks(helper, CONTROLLER), 36, "furnace blocks placed");
		helper.assertTrue(player.getMainHandItem().isEmpty(), "item used up");
		BlockState controller = level.getBlockState(helper.absolutePos(CONTROLLER));
		helper.assertTrue(controller.is(MetallurgyContent.BLAST_FURNACE) && controller.getValue(BlastFurnaceBlock.FACING) == Direction.NORTH,
			"controller in the middle of the second layer, facing the player: " + controller);
		BlockState hopper = level.getBlockState(helper.absolutePos(CONTROLLER.above(2)));
		helper.assertTrue(hopper.is(MetallurgyContent.BLAST_FURNACE_PART) && hopper.getValue(BlastFurnacePartBlock.LAYER) == 3, "hopper on top");
		helper.assertTrue(level.getBlockState(helper.absolutePos(FRONT)).is(MetallurgyContent.BLAST_FURNACE_PART), "front middle bottom block");

		BlastFurnaceBlockEntity furnace = (BlastFurnaceBlockEntity) level.getBlockEntity(helper.absolutePos(CONTROLLER));
		furnace.setOre(Items.RAW_IRON, 150);
		furnace.items().setItem(BlastFurnaceBlockEntity.FUEL_SLOT, new ItemStack(Items.COAL_BLOCK, 5));
		furnace.items().setItem(BlastFurnaceBlockEntity.MOLD_SLOT, new ItemStack(Items.IRON_BLOCK, 7));
		player.gameMode.destroyBlock(helper.absolutePos(FRONT.offset(-1, 2, 1)));
		helper.assertValueEqual(structureBlocks(helper, CONTROLLER), 0, "blocks left after breaking one part");
		helper.assertValueEqual(dropped(helper, CONTROLLER, MetallurgyContent.BLAST_FURNACE_ITEM), 1, "furnace item dropped");
		helper.assertValueEqual(dropped(helper, CONTROLLER, Items.RAW_IRON), 150, "reservoir ore dropped");
		helper.assertValueEqual(dropped(helper, CONTROLLER, Items.COAL_BLOCK), 5, "fuel dropped");
		helper.assertValueEqual(dropped(helper, CONTROLLER, Items.IRON_BLOCK), 7, "cast blocks dropped");
		boolean stacksOk = helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(helper.absolutePos(CONTROLLER)).inflate(4.0))
			.stream().allMatch(entity -> entity.getItem().getCount() <= entity.getItem().getMaxStackSize());
		helper.assertTrue(stacksOk, "every dropped stack within its stack size");
		clearDrops(helper, CONTROLLER);

		player.setGameMode(GameType.CREATIVE);
		build(helper, CONTROLLER).setOre(Items.RAW_GOLD, 9);
		player.gameMode.destroyBlock(helper.absolutePos(CONTROLLER));
		helper.assertValueEqual(structureBlocks(helper, CONTROLLER), 0, "blocks left after a creative break");
		helper.assertValueEqual(dropped(helper, CONTROLLER, MetallurgyContent.BLAST_FURNACE_ITEM), 0, "no furnace item in creative");
		helper.assertValueEqual(dropped(helper, CONTROLLER, Items.RAW_GOLD), 9, "contents still dropped in creative");
		clearDrops(helper, CONTROLLER);
		SimpleBlockTestLog.info("M08-F2: blocked placement refused (item kept); placed 36 blocks facing the player; breaking one part dropped "
			+ "the furnace item, 150 raw iron, 5 coal blocks and 7 iron blocks and cleared all 36; creative break dropped only the contents");
		TestPlayers.remove(player);
		helper.succeed();
	}

	// ---- Reservoir, fuel, mold ----

	/**
	 * M08-F3: the reservoir takes raw iron, copper or gold only, one kind at a time, exactly 516: the
	 * 516th goes in, the 517th does not. By hand through the screen: shift-click deposits what fits;
	 * fuel slot takes only the three fuel blocks; the mold takes nothing put in.
	 */
	@GameTest(structure = SHAFT)
	public void reservoirTakes516OfOneRawMetal(GameTestHelper helper) {
		floor(helper);
		BlastFurnaceBlockEntity furnace = build(helper, CONTROLLER);
		helper.assertValueEqual(furnace.room(Items.IRON_ORE), 0, "iron ore blocks are not raw metal");
		helper.assertValueEqual(furnace.room(Items.COAL), 0, "coal is not ore");
		helper.assertValueEqual(furnace.deposit(Items.RAW_IRON, 515), 515, "515 raw iron");
		helper.assertValueEqual(furnace.deposit(Items.RAW_IRON, 1), 1, "the 516th");
		helper.assertValueEqual(furnace.deposit(Items.RAW_IRON, 1), 0, "the 517th");
		furnace.setOre(Items.RAW_IRON, 10);
		helper.assertValueEqual(furnace.room(Items.RAW_COPPER), 0, "no copper while iron is in");

		ServerPlayer player = TestPlayers.create(helper, GameType.SURVIVAL, new BlockPos(18, 1, 2));
		Inventory inventory = player.getInventory();
		player.openMenu(new net.minecraft.world.SimpleMenuProvider((id, inv, p) -> BlastFurnaceMenu.server(id, inv, furnace), net.minecraft.network.chat.Component.literal("furnace")));
		BlastFurnaceMenu menu = (BlastFurnaceMenu) player.containerMenu;
		inventory.setItem(18, new ItemStack(Items.RAW_IRON, 64));
		inventory.setItem(19, new ItemStack(Items.RAW_COPPER, 64));
		inventory.setItem(20, new ItemStack(Items.COAL_BLOCK, 3));
		inventory.setItem(21, new ItemStack(Items.COAL, 3));
		furnace.setOre(Items.RAW_IRON, 500);
		menu.clicked(InventoryGameTests.menuIndex(menu, inventory, 18), 0, ContainerInput.QUICK_MOVE, player);
		helper.assertValueEqual(furnace.count(), 516, "shift-click filled the reservoir to 516");
		helper.assertValueEqual(inventory.getItem(18).getCount(), 48, "the rest stays in the inventory");
		menu.clicked(InventoryGameTests.menuIndex(menu, inventory, 19), 0, ContainerInput.QUICK_MOVE, player);
		helper.assertValueEqual(inventory.getItem(19).getCount(), 64, "copper not taken into an iron furnace");
		menu.clicked(InventoryGameTests.menuIndex(menu, inventory, 20), 0, ContainerInput.QUICK_MOVE, player);
		helper.assertTrue(furnace.fuel().is(Items.COAL_BLOCK) && furnace.fuel().getCount() == 3, "coal blocks into the fuel slot");
		menu.clicked(InventoryGameTests.menuIndex(menu, inventory, 21), 0, ContainerInput.QUICK_MOVE, player);
		helper.assertValueEqual(inventory.getItem(21).getCount(), 3, "loose coal is no fuel here");
		helper.assertTrue(menu.getSlot(0).mayPlace(new ItemStack(MetallurgyContent.CHARCOAL_BLOCK_ITEM))
			&& menu.getSlot(0).mayPlace(new ItemStack(Items.DRIED_KELP_BLOCK)) && !menu.getSlot(0).mayPlace(new ItemStack(Items.CHARCOAL))
			&& !menu.getSlot(0).mayPlace(new ItemStack(Items.DRIED_KELP)) && !menu.getSlot(0).mayPlace(new ItemStack(Items.LAVA_BUCKET)),
			"fuel slot: coal, charcoal and dried kelp blocks only");
		helper.assertFalse(menu.getSlot(1).mayPlace(new ItemStack(Items.IRON_BLOCK)), "nothing is put into the mold");
		// Taking from the reservoir by hand: a stack, within the heavy-load cap (nothing heavy carried now).
		inventory.setItem(18, ItemStack.EMPTY);
		inventory.setItem(19, ItemStack.EMPTY);
		menu.clickMenuButton(player, BlastFurnaceMenu.BUTTON_SHIFT);
		helper.assertValueEqual(furnace.count(), 516 - 64, "a stack taken out by shift-click");
		player.closeContainer();
		SimpleBlockTestLog.info("M08-F3: reservoir 516th accepted, 517th refused; ore blocks, coal and a second metal refused; screen shift-click "
			+ "filled to 516 and kept 48; fuel slot only coal/charcoal/kelp blocks; mold takes nothing; reservoir gives a stack back");
		TestPlayers.remove(player);
		helper.succeed();
	}

	/**
	 * M08-F4: casting. 20 raw iron and a Block of Coal: after 2 x cast_ticks of burning, two iron blocks in
	 * the mold, two raw iron left (a fractional batch waits), one fuel block used. With no fuel the furnace
	 * waits, keeping its progress. A mold of 64, or of another metal, stops it: nothing burns, nothing is
	 * lost. Copper and gold cast into their blocks.
	 */
	@GameTest(structure = SHAFT)
	public void castsNineRawIntoOneBlock(GameTestHelper helper) {
		floor(helper);
		BlastFurnaceBlockEntity furnace = build(helper, CONTROLLER);
		int cast = Tuning.getInt("blast_furnace.cast_ticks");
		int burn = Tuning.getInt("blast_furnace.coal_block_burn_ticks");
		furnace.setOre(Items.RAW_IRON, 20);
		run(helper, furnace, cast / 2);
		helper.assertValueEqual(furnace.progress(), 0, "no fuel: no progress");
		furnace.items().setItem(BlastFurnaceBlockEntity.FUEL_SLOT, new ItemStack(Items.COAL_BLOCK, 2));
		run(helper, furnace, cast / 2);
		helper.assertTrue(helper.getLevel().getBlockState(furnace.getBlockPos()).getValue(BlastFurnaceBlock.LIT), "lit while casting");
		run(helper, furnace, cast * 2 - cast / 2);
		helper.assertTrue(furnace.mold().is(Items.IRON_BLOCK) && furnace.mold().getCount() == 2, "two iron blocks cast: " + furnace.mold());
		helper.assertValueEqual(furnace.count(), 2, "two raw iron wait (a batch needs nine)");
		helper.assertValueEqual(furnace.fuel().getCount(), 1, "one coal block burned for 600 ticks");
		helper.assertValueEqual(furnace.burn(), burn - cast * 2, "burn left in that block");
		run(helper, furnace, cast);
		helper.assertValueEqual(furnace.mold().getCount(), 2, "two raw iron cast nothing more");
		helper.assertValueEqual(furnace.burn(), burn - cast * 2, "nothing burns while waiting");

		// A full mold, or another metal in it, stops the furnace with nothing lost.
		furnace.setOre(Items.RAW_IRON, 30);
		furnace.items().setItem(BlastFurnaceBlockEntity.MOLD_SLOT, new ItemStack(Items.IRON_BLOCK, 64));
		run(helper, furnace, cast * 2);
		helper.assertTrue(furnace.count() == 30 && furnace.mold().getCount() == 64, "full mold: waits");
		furnace.items().setItem(BlastFurnaceBlockEntity.MOLD_SLOT, new ItemStack(Items.GOLD_BLOCK, 3));
		run(helper, furnace, cast * 2);
		helper.assertTrue(furnace.count() == 30 && furnace.mold().is(Items.GOLD_BLOCK) && furnace.mold().getCount() == 3, "gold in the mold: waits");
		helper.assertValueEqual(furnace.burn(), burn - cast * 2, "no fuel burned while stopped");

		furnace.items().setItem(BlastFurnaceBlockEntity.MOLD_SLOT, ItemStack.EMPTY);
		furnace.setOre(Items.RAW_COPPER, 9);
		run(helper, furnace, cast);
		helper.assertTrue(furnace.mold().is(Items.COPPER_BLOCK.weathering().unaffected()) && furnace.count() == 0, "copper casts a copper block");
		furnace.items().setItem(BlastFurnaceBlockEntity.MOLD_SLOT, ItemStack.EMPTY);
		furnace.setOre(Items.RAW_GOLD, 9);
		run(helper, furnace, cast);
		helper.assertTrue(furnace.mold().is(Items.GOLD_BLOCK), "gold casts a gold block");
		SimpleBlockTestLog.info("M08-F4: cast_ticks {} and {} burn per coal block: no fuel no progress; 20 raw iron -> 2 iron blocks + 2 raw "
			+ "waiting, 1 coal block used; full or gold mold waits with nothing burned; copper and gold cast (burn left {})", cast, burn, furnace.burn());
		helper.succeed();
	}

	/** M08-F5: save and load keep the reservoir, the burning fuel, the progress, the fuel stack and the mold exactly. */
	@GameTest(structure = SHAFT)
	public void saveAndLoadKeepEverything(GameTestHelper helper) {
		floor(helper);
		BlastFurnaceBlockEntity furnace = build(helper, CONTROLLER);
		furnace.setOre(Items.RAW_GOLD, 517 - 1);
		furnace.items().setItem(BlastFurnaceBlockEntity.FUEL_SLOT, new ItemStack(Items.DRIED_KELP_BLOCK, 12));
		furnace.items().setItem(BlastFurnaceBlockEntity.MOLD_SLOT, new ItemStack(Items.GOLD_BLOCK, 5));
		run(helper, furnace, 123);
		ServerLevel level = helper.getLevel();
		CompoundTag saved = furnace.saveWithFullMetadata(level.registryAccess());
		BlastFurnaceBlockEntity copy = new BlastFurnaceBlockEntity(furnace.getBlockPos(), furnace.getBlockState());
		copy.loadWithComponents(TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), saved));
		helper.assertTrue(copy.ore() == Items.RAW_GOLD && copy.count() == 516, "reservoir: " + copy);
		helper.assertTrue(copy.burn() == furnace.burn() && copy.burnTotal() == furnace.burnTotal() && copy.progress() == 123, "burn and progress");
		helper.assertTrue(ItemStack.matches(copy.fuel(), furnace.fuel()) && ItemStack.matches(copy.mold(), furnace.mold()), "fuel and mold");
		SimpleBlockTestLog.info("M08-F5: saved and loaded 516 raw gold, {} kelp blocks, 5 gold blocks, burn {}/{} and progress 123 exactly",
			copy.fuel().getCount(), copy.burn(), copy.burnTotal());
		helper.succeed();
	}

	// ---- Freight ----

	private static void rail(GameTestHelper helper, BlockPos rail, RailShape shape) {
		helper.setBlock(rail.below(), Blocks.STONE);
		Direction facing = shape == RailShape.NORTH_SOUTH ? Direction.SOUTH : Direction.EAST;
		helper.setBlock(rail, TrainContent.LOADING_TRACK.defaultBlockState().setValue(LoadingTrackBlock.FACING, facing)
			.setValue(LoadingTrackBlock.SHAPE, shape));
	}

	/**
	 * M08-F6: a cart docked on a Loading Rail beside the furnace, level with the top of the hopper's rim,
	 * tips its raw iron in through its gate on the furnace's side: side-on (east, a north-south rail) and
	 * end-on (north, an east-west... here a north-south rail pointing at the furnace). A partial load fills
	 * the furnace to 516 and the rest stays in the cart. Refused, with the cart's gate shut and its cargo
	 * unchanged: another metal than the furnace holds, coal, a full furnace, a block over the rim, and a
	 * rail one level too low.
	 */
	@GameTest(structure = SHAFT, maxTicks = 1200)
	public void cartsTipIntoTheHopper(GameTestHelper helper) {
		floor(helper);
		BlastFurnaceBlockEntity furnace = build(helper, CONTROLLER);
		int rimY = CONTROLLER.getY() + 3;
		BlockPos sideOn = new BlockPos(CONTROLLER.getX() + 2, rimY, CONTROLLER.getZ());
		BlockPos endOn = new BlockPos(CONTROLLER.getX(), rimY, CONTROLLER.getZ() - 2);
		boolean[] failed = {false};
		Minecart[] cart = new Minecart[1];
		List<String> log = new ArrayList<>();
		var sequence = helper.startSequence()
			.thenExecute(CableGameTests.step(failed, () -> {
				rail(helper, sideOn, RailShape.NORTH_SOUTH);
				cart[0] = SkipGameTests.cartOn(helper, sideOn, Items.RAW_IRON, 100);
			}))
			.thenIdle(8)
			.thenExecute(CableGameTests.step(failed, () -> {
				helper.assertTrue(LoadingTracks.freight(cart[0]).simpleBlock$gateSide() == Direction.WEST, "side gate open towards the furnace");
			}))
			.thenIdle(40)
			.thenExecute(CableGameTests.step(failed, () -> {
				helper.assertTrue(furnace.count() == 100 && MinecartCargo.cart(cart[0]).simpleBlock$count() == 0, "side-on: all 100 in: " + furnace);
				log.add("side-on 100 raw iron tipped in");
				cart[0].discard();
				helper.setBlock(sideOn, Blocks.AIR);
				rail(helper, endOn, RailShape.NORTH_SOUTH);
				furnace.setOre(Items.RAW_IRON, 500);
				cart[0] = SkipGameTests.cartOn(helper, endOn, Items.RAW_IRON, 50);
			}))
			.thenIdle(40)
			.thenExecute(CableGameTests.step(failed, () -> {
				helper.assertTrue(furnace.count() == 516 && MinecartCargo.cart(cart[0]).simpleBlock$count() == 34, "end-on partial: furnace "
					+ furnace.count() + ", cart " + MinecartCargo.cart(cart[0]).simpleBlock$count());
				log.add("end-on partial: 16 of 50 to fill 516, 34 kept");
				cart[0].discard();
			}));
		record Refusal(String name, Item furnaceOre, int furnaceCount, Item cartItem, int cartCount, boolean block, boolean low) {
		}
		List<Refusal> refusals = List.of(
			new Refusal("copper cart, iron furnace", Items.RAW_IRON, 100, Items.RAW_COPPER, 100, false, false),
			new Refusal("coal cart", Items.AIR, 0, Items.COAL, 100, false, false),
			new Refusal("full furnace", Items.RAW_IRON, 516, Items.RAW_IRON, 100, false, false),
			new Refusal("block over the rim", Items.AIR, 0, Items.RAW_IRON, 100, true, false),
			new Refusal("rail one level low", Items.AIR, 0, Items.RAW_IRON, 100, false, true));
		for (Refusal r : refusals) {
			BlockPos at = r.low() ? endOn.below() : endOn;
			sequence = sequence
				.thenExecute(CableGameTests.step(failed, () -> {
					helper.setBlock(endOn, Blocks.AIR);
					helper.setBlock(endOn.below(), Blocks.AIR);
					rail(helper, at, RailShape.NORTH_SOUTH);
					if (r.block()) {
						helper.setBlock(endOn.south(), Blocks.STONE);
					}
					furnace.setOre(r.furnaceOre(), r.furnaceCount());
					cart[0] = SkipGameTests.cartOn(helper, at, r.cartItem(), r.cartCount());
				}))
				.thenIdle(30)
				.thenExecute(CableGameTests.step(failed, () -> {
					helper.assertTrue(Couplings.coupled(cart[0]).simpleBlock$dock() != null, r.name() + ": docked");
					helper.assertTrue(MinecartCargo.cart(cart[0]).simpleBlock$count() == r.cartCount() && furnace.count() == r.furnaceCount(),
						r.name() + ": cart " + MinecartCargo.cart(cart[0]).simpleBlock$count() + ", furnace " + furnace.count());
					helper.assertFalse(LoadingTracks.freight(cart[0]).simpleBlock$gateOpen(), r.name() + ": gate shut");
					log.add(r.name() + ": refused");
					cart[0].discard();
					helper.setBlock(endOn.south(), Blocks.AIR);
					helper.setBlock(at, Blocks.AIR);
				}));
		}
		sequence.thenExecute(CableGameTests.step(failed, () -> SimpleBlockTestLog.info("M08-F6 cart -> hopper: {}; moved {} in total", log,
			BlastFurnaceFreight.moved(furnace)))).thenSucceed();
	}

	/**
	 * M08-F7: a hung skip whose bottom is level with the top of the hopper's rim, the hopper in front of
	 * it, is a furnace station: it empties its chute into the furnace (its discharge gate open). A copper
	 * skip at an iron furnace, a coal skip and a full furnace move nothing; an unhung skip there has no
	 * station.
	 */
	@GameTest(structure = SHAFT, maxTicks = 1200)
	public void skipEmptiesItsChuteIntoTheHopper(GameTestHelper helper) {
		ServerPlayer player = TestPlayers.create(helper, GameType.SURVIVAL, new BlockPos(8, 1, 6));
		SkipGameTests.layShaft(helper, player);
		// The furnace in front of (north of) the shaft column, its top layer at y 4: front middle bottom (12, 1, 2).
		BlockPos controller = BlastFurnaceStructure.controllerFor(new BlockPos(12, 1, 2), Direction.NORTH);
		BlastFurnaceBlockEntity furnace = build(helper, controller);
		OreSkip skip = SkipGameTests.spawnSkip(helper, SkipGameTests.COLUMN);
		double furnaceStop = 5.0;
		boolean[] failed = {false};
		List<String> log = new ArrayList<>();
		helper.startSequence()
			.thenIdle(2)
			.thenExecute(CableGameTests.step(failed, () -> {
				OreSkip loose = SkipGameTests.spawnSkip(helper, new BlockPos(20, 1, 9));
				Vec3 at = helper.absoluteVec(new Vec3(12.5, furnaceStop, 5.5));
				loose.setPos(at.x, at.y, at.z);
				helper.assertTrue(SkipStations.station(helper.getLevel(), loose, loose.getY()) == null, "no station for an unhung skip");
				loose.discard();
				SkipGameTests.route(helper, player);
				CableGameTests.teleport(helper, player, new BlockPos(12, 1, 8));
				SkipGameTests.hang(helper, player, skip);
				helper.assertTrue(skip.bridled(), "hung");
				SkipGameTests.holdAt(helper, skip, furnaceStop, Items.RAW_IRON, 200);
				SkipStations.Station station = SkipStations.station(helper.getLevel(), skip, skip.getY());
				helper.assertTrue(station != null && station.mode() == SkipStations.Mode.FURNACE, "furnace station: " + station);
					}))
			.thenIdle(8)
			.thenExecute(CableGameTests.step(failed, () -> helper.assertTrue(skip.gateOpen(), "discharge gate open")))
			.thenIdle(70)
			.thenExecute(CableGameTests.step(failed, () -> {
				helper.assertTrue(furnace.count() == 200 && skip.count() == 0, "skip emptied into the furnace: " + furnace + ", skip " + skip.count());
				log.add("200 raw iron down the chute");
				furnace.setOre(Items.RAW_IRON, 100);
				SkipGameTests.holdAt(helper, skip, furnaceStop, Items.RAW_COPPER, 50);
			}))
			.thenIdle(30)
			.thenExecute(CableGameTests.step(failed, () -> {
				helper.assertTrue(furnace.count() == 100 && skip.count() == 50 && !skip.gateOpen(), "copper refused by an iron furnace");
				log.add("copper skip at iron furnace refused");
				SkipGameTests.holdAt(helper, skip, furnaceStop, Items.COAL, 50);
				furnace.setOre(Items.AIR, 0);
			}))
			.thenIdle(30)
			.thenExecute(CableGameTests.step(failed, () -> {
				helper.assertTrue(furnace.count() == 0 && skip.count() == 50, "coal refused");
				log.add("coal skip refused");
				furnace.setOre(Items.RAW_IRON, 516);
				SkipGameTests.holdAt(helper, skip, furnaceStop, Items.RAW_IRON, 50);
			}))
			.thenIdle(30)
			.thenExecute(CableGameTests.step(failed, () -> {
				helper.assertTrue(furnace.count() == 516 && skip.count() == 50, "full furnace refused");
				log.add("full furnace refused");
				SimpleBlockTestLog.info("M08-F7 skip -> hopper: {}", log);
				skip.discard();
				TestPlayers.remove(player);
			}))
			.thenSucceed();
	}

	// ---- Retirement and the ordinary furnace ----

	/**
	 * M08-F8: every vanilla village piece with a blast furnace (seven armorer houses) and the two trail
	 * ruin rooms, placed as new structures place them, have an ordinary furnace facing the same way at
	 * each blast furnace's place, and no blast furnace. (Villager professions, AI and trades are not
	 * touched by M08.)
	 */
	@GameTest(structure = SHAFT, maxTicks = 200)
	public void newVillagesBuildFurnacesNotBlastFurnaces(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		var templates = level.getServer().getStructureTemplateManager();
		List<String> pieces = List.of("village/desert/houses/desert_armorer_1", "village/plains/houses/plains_armorer_house_1",
			"village/savanna/houses/savanna_armorer_1", "village/snowy/houses/snowy_armorer_house_1", "village/snowy/houses/snowy_armorer_house_2",
			"village/taiga/houses/taiga_armorer_2", "village/taiga/houses/taiga_armorer_house_1", "trail_ruins/buildings/large_room_1",
			"trail_ruins/buildings/large_room_5");
		// High above the test grid, so that no piece reaches another test's area.
		BlockPos origin = helper.absolutePos(new BlockPos(1, 60, 0));
		List<String> log = new ArrayList<>();
		for (String piece : pieces) {
			StructureTemplate template = templates.getOrCreate(net.minecraft.resources.Identifier.withDefaultNamespace(piece));
			List<StructureTemplate.StructureBlockInfo> blast = template.filterBlocks(origin, new StructurePlaceSettings(), Blocks.BLAST_FURNACE);
			helper.assertFalse(blast.isEmpty(), piece + " has a blast furnace in its template");
			StructurePoolElement element = piece.startsWith("village/")
				? StructurePoolElement.legacy("minecraft:" + piece).apply(StructureTemplatePool.Projection.RIGID)
				: StructurePoolElement.single("minecraft:" + piece).apply(StructureTemplatePool.Projection.RIGID);
			BoundingBox box = BoundingBox.fromCorners(origin, origin.offset(template.getSize()).offset(-1, -1, -1));
			element.place(templates, level, level.structureManager(), level.getChunkSource().getGenerator(), origin, origin, Rotation.NONE,
				new BoundingBox(origin.getX() - 32, origin.getY() - 32, origin.getZ() - 32, origin.getX() + 64, origin.getY() + 64, origin.getZ() + 64),
				level.getRandom(), LiquidSettings.IGNORE_WATERLOGGING, false);
			int furnaces = 0;
			for (StructureTemplate.StructureBlockInfo info : blast) {
				BlockState placed = level.getBlockState(info.pos());
				helper.assertTrue(placed.is(Blocks.FURNACE) && placed.getValue(AbstractFurnaceBlock.FACING) == info.state().getValue(AbstractFurnaceBlock.FACING),
					piece + ": furnace facing " + info.state().getValue(AbstractFurnaceBlock.FACING) + " expected at " + info.pos() + ", found " + placed);
				furnaces++;
			}
			for (BlockPos pos : BlockPos.betweenClosed(box.minX(), box.minY(), box.minZ(), box.maxX(), box.maxY(), box.maxZ())) {
				helper.assertFalse(level.getBlockState(pos).is(Blocks.BLAST_FURNACE), piece + ": a blast furnace was placed at " + pos);
			}
			for (BlockPos pos : BlockPos.betweenClosed(box.minX(), box.minY(), box.minZ(), box.maxX(), box.maxY(), box.maxZ())) {
				level.setBlock(pos, Blocks.AIR.defaultBlockState(), 2 | 16);
			}
			level.getEntitiesOfClass(net.minecraft.world.entity.Entity.class, AABB.of(box).inflate(2.0), entity -> !(entity instanceof ServerPlayer))
				.forEach(net.minecraft.world.entity.Entity::discard);
			log.add(piece.substring(piece.lastIndexOf('/') + 1) + " " + furnaces);
		}
		SimpleBlockTestLog.info("M08-F8: new structure pieces built ordinary furnaces (same facing) for every blast furnace: {}", log);
		helper.succeed();
	}

	/**
	 * M08-F9: an ordinary furnace smelts raw iron twice as slowly (furnace.raw_ore_cook_multiplier 2:
	 * 400 ticks), while raw beef still takes the vanilla 200. The Block of Charcoal burns in it like a
	 * Block of Coal.
	 */
	@GameTest(structure = SHAFT)
	public void ordinaryFurnaceSmeltsRawOreSlowly(GameTestHelper helper) {
		floor(helper);
		ServerLevel level = helper.getLevel();
		BlockPos pos = helper.absolutePos(new BlockPos(3, 1, 3));
		level.setBlockAndUpdate(pos, Blocks.FURNACE.defaultBlockState());
		FurnaceBlockEntity furnace = (FurnaceBlockEntity) level.getBlockEntity(pos);
		furnace.setItem(0, new ItemStack(Items.RAW_IRON, 1));
		furnace.setItem(1, new ItemStack(MetallurgyContent.CHARCOAL_BLOCK_ITEM, 1));
		int base = 200;
		int slow = (int) Math.ceil(base * Tuning.getDouble("furnace.raw_ore_cook_multiplier"));
		for (int i = 0; i < slow - 1; i++) {
			AbstractFurnaceBlockEntity.serverTick(level, pos, level.getBlockState(pos), furnace);
		}
		helper.assertTrue(furnace.getItem(2).isEmpty(), "raw iron not smelted after " + (slow - 1) + " ticks");
		AbstractFurnaceBlockEntity.serverTick(level, pos, level.getBlockState(pos), furnace);
		AbstractFurnaceBlockEntity.serverTick(level, pos, level.getBlockState(pos), furnace);
		helper.assertTrue(furnace.getItem(2).is(Items.IRON_INGOT), "raw iron smelted at " + slow + " ticks: " + furnace.getItem(2));
		helper.assertTrue(furnace.getItem(1).isEmpty(), "the Block of Charcoal burned as fuel");
		furnace.setItem(2, ItemStack.EMPTY);
		furnace.setItem(0, new ItemStack(Items.BEEF, 1));
		for (int i = 0; i < base + 1; i++) {
			AbstractFurnaceBlockEntity.serverTick(level, pos, level.getBlockState(pos), furnace);
		}
		helper.assertTrue(furnace.getItem(2).is(Items.COOKED_BEEF), "beef at the vanilla 200: " + furnace.getItem(2));
		SimpleBlockTestLog.info("M08-F9: ordinary furnace: raw iron {} ticks (x{}), beef 200; charcoal block burned as fuel", slow,
			Tuning.getDouble("furnace.raw_ore_cook_multiplier"));
		helper.succeed();
	}

	/** M08-F10: taking cast blocks from the mold is commodity intake: at the heavy-load cap it is refused; with room it is taken. */
	@GameTest(structure = SHAFT)
	public void moldBlocksCountAgainstTheCap(GameTestHelper helper) {
		floor(helper);
		BlastFurnaceBlockEntity furnace = build(helper, CONTROLLER);
		furnace.items().setItem(BlastFurnaceBlockEntity.MOLD_SLOT, new ItemStack(Items.IRON_BLOCK, 10));
		ServerPlayer player = TestPlayers.create(helper, GameType.SURVIVAL, new BlockPos(18, 1, 2));
		Inventory inventory = player.getInventory();
		inventory.setItem(18, new ItemStack(Items.IRON_INGOT, 50));
		player.openMenu(new net.minecraft.world.SimpleMenuProvider((id, inv, p) -> BlastFurnaceMenu.server(id, inv, furnace), net.minecraft.network.chat.Component.literal("furnace")));
		BlastFurnaceMenu menu = (BlastFurnaceMenu) player.containerMenu;
		menu.clicked(1, 0, ContainerInput.QUICK_MOVE, player);
		helper.assertValueEqual(inventory.countItem(Items.IRON_BLOCK), 1, "one iron block (9) fits into 14 of room");
		helper.assertValueEqual(furnace.mold().getCount(), 9, "the rest stays in the mold");
		player.closeContainer();
		SimpleBlockTestLog.info("M08-F10: at 50 ingots, shift-click on a mold of 10 iron blocks took 1 (cap), 9 stayed");
		TestPlayers.remove(player);
		helper.succeed();
	}
}
