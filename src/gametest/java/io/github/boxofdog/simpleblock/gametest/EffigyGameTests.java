package io.github.boxofdog.simpleblock.gametest;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.network.protocol.game.ServerboundContainerButtonClickPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.WrappedGoal;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.chicken.Chicken;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.spider.Spider;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.piston.PistonBaseBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.CrafterBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import net.fabricmc.fabric.api.gametest.v1.GameTest;

import io.github.boxofdog.simpleblock.effigy.Effigies;
import io.github.boxofdog.simpleblock.effigy.EffigyBlock;
import io.github.boxofdog.simpleblock.effigy.EffigyBlockEntity;
import io.github.boxofdog.simpleblock.effigy.EffigyContent;
import io.github.boxofdog.simpleblock.effigy.EffigyItem;
import io.github.boxofdog.simpleblock.effigy.EffigyPlacementMenu;
import io.github.boxofdog.simpleblock.effigy.EffigyType;
import io.github.boxofdog.simpleblock.gametest.MobGameTests.Arena;
import io.github.boxofdog.simpleblock.mixin.mobs.MobAccessor;

/**
 * M12 Monster Effigies: the 576-item (144 for pearls) compression, the effigy recipes, the confirmation
 * screen and placement, permanence, the cone's geometry, and the fear itself in arenas (slots 40 to 58 of
 * {@link MobGameTests}' arena region, three apart: a cone reaches 141 blocks, and each test's arenas are placed from its own grid position). Ids EFF-01.. match the M12 brief's acceptance areas 1-6.
 */
public class EffigyGameTests {
	private static final String SHAFT = SkipGameTests.SHAFT;

	private static Item drop(EffigyType type) {
		return type.drop();
	}

	private static int stack(EffigyType type) {
		return new ItemStack(type.drop()).getMaxStackSize();
	}

	private static CraftingMenu table(GameTestHelper helper, ServerPlayer player) {
		BlockPos table = helper.absolutePos(new BlockPos(2, 1, 1));
		helper.getLevel().setBlockAndUpdate(table, Blocks.CRAFTING_TABLE.defaultBlockState());
		player.openMenu(new SimpleMenuProvider((id, inv, p) -> new CraftingMenu(id, inv, ContainerLevelAccess.create(helper.getLevel(), table)),
			Component.literal("crafting")));
		return (CraftingMenu) player.containerMenu;
	}

	private static void fill(CraftingMenu menu, ItemStack... grid) {
		for (int i = 0; i < 9; i++) {
			menu.getSlot(1 + i).set(grid[i].copy());
		}
	}

	private static ItemStack[] nine(ItemStack stack) {
		ItemStack[] grid = new ItemStack[9];
		for (int i = 0; i < 9; i++) {
			grid[i] = stack.copy();
		}
		return grid;
	}

	private static int gridCount(CraftingMenu menu) {
		int total = 0;
		for (int i = 1; i <= 9; i++) {
			total += menu.getSlot(i).getItem().getCount();
		}
		return total;
	}

	private static void clearGrid(CraftingMenu menu) {
		for (int i = 0; i <= 9; i++) {
			menu.getSlot(i).set(ItemStack.EMPTY);
		}
		menu.setCarried(ItemStack.EMPTY);
	}

	// ---------------------------------------------------------------- 1. compression

	/**
	 * EFF-01: nine full stacks of the drop make one compressed block, for all four (576 items; ender
	 * pearls, which stack to 16, 144). One stack short by one, an empty cell, a wrong item or a renamed
	 * stack make nothing. Taking the result (a click, and a shift-click) uses up exactly every stack:
	 * the grid is empty and the inventory holds one block, nothing more and nothing back.
	 */
	@GameTest(structure = SHAFT)
	public void compressionTakesNineFullStacks(GameTestHelper helper) {
		ServerPlayer player = TestPlayers.create(helper, GameType.SURVIVAL, new BlockPos(3, 1, 3));
		CraftingMenu menu = table(helper, player);
		List<String> log = new ArrayList<>();
		for (EffigyType type : EffigyType.values()) {
			Item drop = drop(type);
			int full = stack(type);
			Item block = EffigyContent.compressedItem(type);
			fill(menu, nine(new ItemStack(drop, full)));
			helper.assertTrue(menu.getSlot(0).getItem().is(block) && menu.getSlot(0).getItem().getCount() == 1, type + ": nine full stacks offer one block");
			ItemStack[] short1 = nine(new ItemStack(drop, full));
			short1[4] = new ItemStack(drop, full - 1);
			fill(menu, short1);
			helper.assertTrue(menu.getSlot(0).getItem().isEmpty(), type + ": a stack of " + (full - 1) + " makes nothing");
			ItemStack[] gap = nine(new ItemStack(drop, full));
			gap[8] = ItemStack.EMPTY;
			fill(menu, gap);
			helper.assertTrue(menu.getSlot(0).getItem().isEmpty(), type + ": an empty cell makes nothing");
			ItemStack[] wrong = nine(new ItemStack(drop, full));
			wrong[0] = new ItemStack(type == EffigyType.ZOMBIE ? Items.BONE : Items.ROTTEN_FLESH, 64);
			fill(menu, wrong);
			helper.assertTrue(menu.getSlot(0).getItem().isEmpty(), type + ": a wrong material makes nothing");
			ItemStack named = new ItemStack(drop, full);
			named.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME, Component.literal("x"));
			ItemStack[] renamed = nine(new ItemStack(drop, full));
			renamed[3] = named;
			fill(menu, renamed);
			helper.assertTrue(menu.getSlot(0).getItem().isEmpty(), type + ": a renamed stack makes nothing");
			fill(menu, nine(new ItemStack(drop, 1)));
			helper.assertTrue(menu.getSlot(0).getItem().isEmpty(), type + ": nine single items make nothing");

			// A click on the result: one block on the cursor, all nine stacks gone.
			fill(menu, nine(new ItemStack(drop, full)));
			menu.clicked(0, 0, ContainerInput.PICKUP, player);
			helper.assertTrue(menu.getCarried().is(block) && menu.getCarried().getCount() == 1, type + ": one block taken");
			helper.assertValueEqual(gridCount(menu), 0, type + ": items left in the grid after a click");
			menu.setCarried(ItemStack.EMPTY);

			// A shift-click (vanilla repeats it while the recipe still matches): still one block, all stacks gone.
			int before = player.getInventory().countItem(block);
			fill(menu, nine(new ItemStack(drop, full)));
			menu.clicked(0, 0, ContainerInput.QUICK_MOVE, player);
			helper.assertValueEqual(player.getInventory().countItem(block) - before, 1, type + ": blocks from one shift-click");
			helper.assertValueEqual(gridCount(menu), 0, type + ": items left in the grid after a shift-click");
			helper.assertValueEqual(player.getInventory().countItem(drop), 0, type + ": drops returned to the inventory");
			log.add(type + ": " + (9 * full) + " -> 1");
			clearGrid(menu);
			player.getInventory().clearContent();
		}
		player.closeContainer();
		SimpleBlockTestLog.info("EFF-01 compression: {}; short, gap, wrong, renamed and single items refused", log);
		TestPlayers.remove(player);
		helper.succeed();
	}

	/**
	 * EFF-01b: a crafter makes the block from nine full stacks and is left empty (all 576 used, not nine);
	 * with one stack short it makes nothing and keeps everything.
	 */
	@GameTest(structure = SHAFT, maxTicks = 200)
	public void crafterCompressesWholeStacks(GameTestHelper helper) {
		BlockPos crafterPos = new BlockPos(6, 2, 6);
		helper.setBlock(crafterPos.below(), Blocks.STONE);
		helper.setBlock(crafterPos, Blocks.CRAFTER);
		CrafterBlockEntity crafter = (CrafterBlockEntity) helper.getBlockEntity(crafterPos, CrafterBlockEntity.class);
		for (int i = 0; i < 9; i++) {
			crafter.setItem(i, new ItemStack(Items.GUNPOWDER, i == 4 ? 63 : 64));
		}
		BlockPos power = crafterPos.east();
		helper.startSequence()
			.thenExecute(() -> helper.setBlock(power, Blocks.REDSTONE_BLOCK))
			.thenIdle(10)
			.thenExecute(() -> {
				int left = 0;
				for (int i = 0; i < 9; i++) {
					left += crafter.getItem(i).getCount();
				}
				helper.assertValueEqual(left, 8 * 64 + 63, "items kept with one stack of 63");
				helper.assertValueEqual(dropped(helper, crafterPos, EffigyContent.compressedItem(EffigyType.CREEPER)), 0, "blocks made from 575");
				helper.setBlock(power, Blocks.AIR);
				crafter.setItem(4, new ItemStack(Items.GUNPOWDER, 64));
			})
			.thenIdle(4)
			.thenExecute(() -> helper.setBlock(power, Blocks.REDSTONE_BLOCK))
			.thenIdle(10)
			.thenExecute(() -> {
				int left = 0;
				for (int i = 0; i < 9; i++) {
					left += crafter.getItem(i).getCount();
				}
				helper.assertValueEqual(left, 0, "items left in the crafter after crafting");
				helper.assertValueEqual(dropped(helper, crafterPos, EffigyContent.compressedItem(EffigyType.CREEPER)), 1, "creeper blocks dispensed");
				clearDrops(helper, crafterPos);
				// The crafter now remembers the recipe for nine gunpowder: a short stack must still make nothing.
				helper.setBlock(power, Blocks.AIR);
				for (int i = 0; i < 9; i++) {
					crafter.setItem(i, new ItemStack(Items.GUNPOWDER, i == 0 ? 63 : 64));
				}
			})
			.thenIdle(4)
			.thenExecute(() -> helper.setBlock(power, Blocks.REDSTONE_BLOCK))
			.thenIdle(10)
			.thenExecute(() -> {
				int left = 0;
				for (int i = 0; i < 9; i++) {
					left += crafter.getItem(i).getCount();
				}
				helper.assertValueEqual(left, 575, "items kept with one stack of 63 after a craft");
				helper.assertValueEqual(dropped(helper, crafterPos, EffigyContent.compressedItem(EffigyType.CREEPER)), 0, "blocks made from 575 after a craft");
				SimpleBlockTestLog.info("EFF-01b crafter: 575 refused and kept; 576 -> 1 Creeper Block, crafter empty; 575 again (recipe remembered) refused");
			})
			.thenSucceed();
	}

	/**
	 * EFF-02: head (or Eye of Ender) over two of the matching blocks, down the middle column, makes the
	 * effigy; another column, another head, one block, or the blocks of another monster make nothing. The
	 * eight items' ids, the tooltip's text, and the effigies' tags.
	 */
	@GameTest(structure = SHAFT)
	public void effigyRecipesAndIdentities(GameTestHelper helper) {
		ServerPlayer player = TestPlayers.create(helper, GameType.SURVIVAL, new BlockPos(3, 1, 3));
		List<String> log = new ArrayList<>();
		JsonObject lang = lang();
		for (EffigyType type : EffigyType.values()) {
			Item head = type.top();
			Item block = EffigyContent.compressedItem(type);
			Item effigy = EffigyContent.effigyItem(type);
			Item a = Items.AIR;
			ItemStack made = RecipeGameTests.craft(helper, player, a, head, a, a, block, a, a, block, a);
			helper.assertTrue(made.is(effigy) && made.getCount() == 1, type + ": head over two blocks makes the effigy, made " + made);
			helper.assertTrue(RecipeGameTests.craft(helper, player, head, a, a, block, a, a, block, a, a).isEmpty(), type + ": left column");
			helper.assertTrue(RecipeGameTests.craft(helper, player, a, a, head, a, a, block, a, a, block).isEmpty(), type + ": right column");
			helper.assertTrue(RecipeGameTests.craft(helper, player, a, head, a, a, block, a, a, a, a).isEmpty(), type + ": one block");
			helper.assertTrue(RecipeGameTests.craft(helper, player, a, block, a, a, head, a, a, block, a).isEmpty(), type + ": head in the middle");
			EffigyType other = EffigyType.values()[(type.ordinal() + 1) % 4];
			helper.assertTrue(RecipeGameTests.craft(helper, player, a, other.top(), a, a, block, a, a, block, a).isEmpty(), type + ": another head");
			Item otherBlock = EffigyContent.compressedItem(other);
			helper.assertTrue(RecipeGameTests.craft(helper, player, a, head, a, a, otherBlock, a, a, otherBlock, a).isEmpty(), type + ": other blocks");

			String name = type.getSerializedName();
			helper.assertValueEqual(BuiltInRegistries.ITEM.getKey(block).toString(), "simple_block:" + name + "_block", "block item id");
			helper.assertValueEqual(BuiltInRegistries.ITEM.getKey(effigy).toString(), "simple_block:" + name + "_effigy", "effigy item id");
			helper.assertTrue(effigy instanceof EffigyItem item && item.type() == type, "effigy item type");
			String key = ((TranslatableContents) EffigyItem.tooltip(type).getContents()).getKey();
			String plural = switch (type) {
				case ZOMBIE -> "Zombies";
				case SKELETON -> "Skeletons";
				case CREEPER -> "Creepers";
				case ENDERMAN -> "Endermen";
			};
			helper.assertValueEqual(lang.get(key).getAsString(), "Scares away " + plural + " in a 100-block conical radius", "tooltip");
			helper.assertValueEqual(lang.get("block.simple_block." + name + "_effigy").getAsString(),
				switch (type) { case ZOMBIE -> "Zombie"; case SKELETON -> "Skeleton"; case CREEPER -> "Creeper"; case ENDERMAN -> "Enderman"; } + " Effigy",
				"effigy name");
			BlockState state = EffigyContent.effigyBlock(type).defaultBlockState();
			helper.assertTrue(state.is(BlockTags.WITHER_IMMUNE) && state.is(BlockTags.DRAGON_IMMUNE) && state.is(BlockTags.MINEABLE_WITH_PICKAXE)
				&& state.is(BlockTags.NEEDS_DIAMOND_TOOL), type + ": effigy tags");
			log.add(name + ": " + lang.get(key).getAsString());
		}
		helper.assertValueEqual(lang.get("simple_block.effigy.confirm").getAsString(),
			"Are you sure? Monster Effigies cannot be picked back up after being destroyed.", "confirmation warning");
		SimpleBlockTestLog.info("EFF-02 effigy recipes (middle column only, matching head and blocks) and tooltips: {}", log);
		TestPlayers.remove(player);
		helper.succeed();
	}

	private static JsonObject lang() {
		try (InputStream in = EffigyGameTests.class.getResourceAsStream("/assets/simple_block/lang/en_us.json")) {
			return JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
		} catch (Exception e) {
			throw new IllegalStateException(e);
		}
	}

	// ---------------------------------------------------------------- 3. placement

	private static final BlockPos SPOT = new BlockPos(10, 1, 6);

	private static void floor(GameTestHelper helper) {
		for (int x = 0; x < 24; x++) {
			for (int z = 0; z < 12; z++) {
				helper.setBlock(new BlockPos(x, 0, z), Blocks.STONE);
			}
		}
	}

	/** Uses the held effigy on the top of the floor block under {@code spot} (relative). */
	private static void use(GameTestHelper helper, ServerPlayer player, BlockPos spot) {
		BlockPos floor = helper.absolutePos(spot.below());
		BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(floor).add(0.0, 0.5, 0.0), Direction.UP, floor, false);
		player.gameMode.useItemOn(player, helper.getLevel(), player.getMainHandItem(), InteractionHand.MAIN_HAND, hit);
	}

	private static void click(ServerPlayer player, int containerId, int button) {
		player.connection.handleContainerButtonClick(new ServerboundContainerButtonClickPacket(containerId, button));
	}

	/** Removes every effigy in the test's structure (their cones would reach other tests' mobs). */
	private static void removeEffigies(GameTestHelper helper) {
		for (BlockPos pos : BlockPos.betweenClosed(helper.absolutePos(new BlockPos(0, 0, 0)), helper.absolutePos(new BlockPos(23, 15, 11)))) {
			if (helper.getLevel().getBlockState(pos).getBlock() instanceof EffigyBlock) {
				helper.getLevel().setBlock(pos, Blocks.AIR.defaultBlockState(), 2 | 16);
			}
		}
	}

	private static int effigyBlocks(GameTestHelper helper) {
		int count = 0;
		for (BlockPos pos : BlockPos.betweenClosed(helper.absolutePos(new BlockPos(0, 0, 0)), helper.absolutePos(new BlockPos(23, 15, 11)))) {
			if (helper.getLevel().getBlockState(pos).getBlock() instanceof EffigyBlock) {
				count++;
			}
		}
		return count;
	}

	/**
	 * EFF-03: using an effigy opens the confirmation screen and changes nothing. Cancel leaves item and
	 * world as they were. Place puts the effigy where it was aimed, facing the way the player looked at
	 * any angle (the block entity's angle, saved and loaded), and uses up one item, once: a second click
	 * does nothing. Place is refused, with the item kept, when the held item has changed, the spot has
	 * been built over, or the player has walked off; a player who disconnects with the screen open
	 * places nothing. In creative the screen is the same and the item is not used up.
	 */
	@GameTest(structure = SHAFT)
	public void placementNeedsConfirmation(GameTestHelper helper) {
		floor(helper);
		ServerLevel level = helper.getLevel();
		ServerPlayer player = TestPlayers.create(helper, GameType.SURVIVAL, new BlockPos(10, 1, 3));
		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(EffigyContent.effigyItem(EffigyType.ZOMBIE), 3));
		List<String> log = new ArrayList<>();

		// Cancel.
		player.setYRot(37.5F);
		use(helper, player, SPOT);
		helper.assertTrue(player.containerMenu instanceof EffigyPlacementMenu, "confirmation screen opened");
		helper.assertValueEqual(effigyBlocks(helper), 0, "blocks placed before confirming");
		helper.assertValueEqual(player.getMainHandItem().getCount(), 3, "items before confirming");
		click(player, player.containerMenu.containerId, EffigyPlacementMenu.BUTTON_CANCEL);
		helper.assertTrue(!(player.containerMenu instanceof EffigyPlacementMenu), "screen closed on cancel");
		helper.assertValueEqual(effigyBlocks(helper), 0, "blocks placed on cancel");
		helper.assertValueEqual(player.getMainHandItem().getCount(), 3, "items after cancel");
		log.add("cancel: nothing");

		// Held item changed.
		use(helper, player, SPOT);
		int id = player.containerMenu.containerId;
		ItemStack effigies = player.getMainHandItem();
		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.STONE));
		click(player, id, EffigyPlacementMenu.BUTTON_PLACE);
		helper.assertValueEqual(effigyBlocks(helper), 0, "blocks placed with another item held");
		player.setItemInHand(InteractionHand.MAIN_HAND, effigies);
		player.closeContainer();
		log.add("item changed: refused");

		// Built over.
		use(helper, player, SPOT);
		id = player.containerMenu.containerId;
		helper.setBlock(SPOT.above(), Blocks.STONE);
		click(player, id, EffigyPlacementMenu.BUTTON_PLACE);
		helper.assertValueEqual(effigyBlocks(helper), 0, "blocks placed into a built-over spot");
		helper.assertValueEqual(player.getMainHandItem().getCount(), 3, "items after a refused place");
		helper.assertTrue(!(player.containerMenu instanceof EffigyPlacementMenu), "screen closed after a refused place");
		helper.setBlock(SPOT.above(), Blocks.AIR);
		log.add("blocked: refused, item kept");

		// Walked off.
		use(helper, player, SPOT);
		id = player.containerMenu.containerId;
		Vec3 here = player.position();
		player.teleportTo(here.x + 30.0, here.y, here.z);
		click(player, id, EffigyPlacementMenu.BUTTON_PLACE);
		helper.assertValueEqual(effigyBlocks(helper), 0, "blocks placed from 30 blocks away");
		helper.assertValueEqual(player.getMainHandItem().getCount(), 3, "items after walking off");
		player.teleportTo(here.x, here.y, here.z);
		player.closeContainer();
		log.add("walked off: refused");

		// Place, then a second (double) click.
		use(helper, player, SPOT);
		id = player.containerMenu.containerId;
		click(player, id, EffigyPlacementMenu.BUTTON_PLACE);
		click(player, id, EffigyPlacementMenu.BUTTON_PLACE);
		helper.assertValueEqual(effigyBlocks(helper), 2, "effigy blocks (two halves of one effigy)");
		helper.assertValueEqual(player.getMainHandItem().getCount(), 2, "items after placing (a double click used one)");
		BlockState lower = helper.getBlockState(SPOT);
		BlockState upper = helper.getBlockState(SPOT.above());
		helper.assertTrue(lower.is(EffigyContent.effigyBlock(EffigyType.ZOMBIE)) && lower.getValue(EffigyBlock.HALF) == DoubleBlockHalf.LOWER
			&& upper.is(EffigyContent.effigyBlock(EffigyType.ZOMBIE)) && upper.getValue(EffigyBlock.HALF) == DoubleBlockHalf.UPPER, "zombie effigy, both halves");
		EffigyBlockEntity effigy = (EffigyBlockEntity) level.getBlockEntity(helper.absolutePos(SPOT));
		helper.assertTrue(Math.abs(effigy.yaw() - 37.5F) < 1.0E-4, "effigy angle = player's look (37.5): " + effigy.yaw());
		Vec3 look = Vec3.directionFromRotation(0.0F, 37.5F);
		helper.assertTrue(effigy.facing().distanceTo(look) < 1.0E-3, "effigy faces the way the player looked: " + effigy.facing() + " vs " + look);
		log.add("placed once at 37.5 degrees, one item used");

		// The angle is saved and loaded, and sent to clients.
		CompoundTag saved = effigy.saveWithFullMetadata(level.registryAccess());
		BlockEntity loaded = BlockEntity.loadStatic(effigy.getBlockPos(), effigy.getBlockState(), saved, level.registryAccess());
		helper.assertTrue(loaded instanceof EffigyBlockEntity again && again.yaw() == effigy.yaw(), "angle after save and load");
		helper.assertTrue(effigy.getUpdateTag(level.registryAccess()).getFloatOr("yaw", -1.0F) == effigy.yaw(), "angle in the client update");

		// Other angles: the four sides and two in between.
		float[] yaws = {0.0F, 90.0F, 180.0F, 270.0F, -123.25F, 301.0F};
		for (int i = 0; i < yaws.length; i++) {
			BlockPos spot = new BlockPos(2 + 3 * i, 1, 9);
			player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(EffigyContent.effigyItem(EffigyType.values()[i % 4])));
			player.teleportTo(helper.absoluteVec(Vec3.atBottomCenterOf(spot.north(2))).x, here.y, helper.absoluteVec(Vec3.atBottomCenterOf(spot.north(2))).z);
			player.setYRot(yaws[i]);
			use(helper, player, spot);
			click(player, player.containerMenu.containerId, EffigyPlacementMenu.BUTTON_PLACE);
			EffigyBlockEntity placed = (EffigyBlockEntity) level.getBlockEntity(helper.absolutePos(spot));
			helper.assertTrue(placed != null && Math.abs(placed.yaw() - yaws[i]) < 1.0E-4 && placed.type() == EffigyType.values()[i % 4],
				"effigy at " + yaws[i] + ": " + (placed == null ? "none" : placed.yaw() + " " + placed.type()));
			helper.assertTrue(player.getMainHandItem().isEmpty(), "item used at " + yaws[i]);
		}
		log.add("angles 0, 90, 180, 270, -123.25, 301 kept with their types");

		// Disconnect with the screen open.
		player.teleportTo(here.x, here.y, here.z);
		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(EffigyContent.effigyItem(EffigyType.CREEPER)));
		int before = effigyBlocks(helper);
		BlockPos spot = new BlockPos(14, 1, 6);
		use(helper, player, spot);
		helper.assertTrue(player.containerMenu instanceof EffigyPlacementMenu, "screen open before disconnecting");
		TestPlayers.remove(player);
		helper.assertValueEqual(effigyBlocks(helper), before, "blocks placed by a disconnect");
		log.add("disconnect: nothing placed");

		// Creative: the same screen; Place does not use up the item.
		ServerPlayer creative = TestPlayers.create(helper, GameType.CREATIVE, new BlockPos(14, 1, 3));
		creative.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(EffigyContent.effigyItem(EffigyType.CREEPER)));
		use(helper, creative, spot);
		helper.assertTrue(creative.containerMenu instanceof EffigyPlacementMenu, "creative: confirmation screen");
		click(creative, creative.containerMenu.containerId, EffigyPlacementMenu.BUTTON_PLACE);
		helper.assertValueEqual(effigyBlocks(helper), before + 2, "creative: effigy placed");
		helper.assertValueEqual(creative.getMainHandItem().getCount(), 1, "creative: item kept");
		log.add("creative: confirmed, item kept");
		TestPlayers.remove(creative);
		SimpleBlockTestLog.info("EFF-03 placement: {}", log);
		removeEffigies(helper);
		helper.succeed();
	}

	// ---------------------------------------------------------------- 4. permanence

	private static int dropped(GameTestHelper helper, BlockPos relative, Item item) {
		return helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(helper.absolutePos(relative)).inflate(6.0)).stream()
			.filter(entity -> entity.getItem().is(item)).mapToInt(entity -> entity.getItem().getCount()).sum();
	}

	private static int anyDropped(GameTestHelper helper, BlockPos relative) {
		return helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(helper.absolutePos(relative)).inflate(6.0)).size();
	}

	private static void clearDrops(GameTestHelper helper, BlockPos relative) {
		helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(helper.absolutePos(relative)).inflate(8.0)).forEach(ItemEntity::discard);
	}

	private static void placeAt(GameTestHelper helper, BlockPos relative, EffigyType type, float yaw) {
		EffigyPlacementMenu.place(helper.getLevel(), helper.absolutePos(relative), type, yaw, null);
	}

	private static boolean standing(GameTestHelper helper, BlockPos relative) {
		return helper.getBlockState(relative).getBlock() instanceof EffigyBlock && helper.getBlockState(relative.above()).getBlock() instanceof EffigyBlock;
	}

	/**
	 * EFF-04: obsidian's hardness and needs a diamond pickaxe; no explosion moves it (a 6-power blast
	 * beside it); it stands on air after its floor is dug out; pistons cannot push it; water and falling
	 * sand do not dislodge it. Broken by a player (Silk Touch or not, either half, by a pickaxe) it is
	 * gone with both halves and drops nothing at all.
	 */
	@GameTest(structure = SHAFT, maxTicks = 400)
	public void effigiesArePermanentAndDropNothing(GameTestHelper helper) {
		floor(helper);
		ServerLevel level = helper.getLevel();
		BlockPos a = new BlockPos(4, 1, 4);
		BlockPos b = new BlockPos(12, 1, 4);
		BlockPos c = new BlockPos(18, 1, 4);
		placeAt(helper, a, EffigyType.SKELETON, 0.0F);
		placeAt(helper, b, EffigyType.ENDERMAN, 90.0F);
		placeAt(helper, c, EffigyType.CREEPER, 200.0F);
		BlockState state = helper.getBlockState(a);
		helper.assertValueEqual(state.getDestroySpeed(level, helper.absolutePos(a)), Blocks.OBSIDIAN.defaultBlockState().getDestroySpeed(level, BlockPos.ZERO),
			"hardness = obsidian");
		helper.assertTrue(state.getBlock().getExplosionResistance() >= 3600000.0F, "blast resistance");
		helper.assertTrue(state.requiresCorrectToolForDrops() && state.is(BlockTags.NEEDS_DIAMOND_TOOL), "needs a diamond pickaxe");

		// A blast beside it; the floor dug out; a piston pushing it; water and sand.
		Vec3 blast = Vec3.atCenterOf(helper.absolutePos(a.east()));
		level.explode(null, blast.x, blast.y, blast.z, 6.0F, Level.ExplosionInteraction.TNT);
		helper.setBlock(b.below(), Blocks.AIR);
		helper.setBlock(c.west(2), Blocks.PISTON.defaultBlockState().setValue(PistonBaseBlock.FACING, Direction.EAST));
		helper.setBlock(c.east(), Blocks.WATER);
		helper.setBlock(c.above(3), Blocks.SAND);
		helper.startSequence()
			.thenIdle(2)
			.thenExecute(() -> {
				helper.setBlock(c.west(3), Blocks.REDSTONE_BLOCK);
				helper.setBlock(c.west(), Blocks.AIR);
			})
			.thenIdle(40)
			.thenExecute(() -> {
				helper.assertTrue(standing(helper, a), "effigy after a 6-power blast beside it");
				helper.assertTrue(standing(helper, b), "effigy with nothing under it");
				helper.assertTrue(standing(helper, c), "effigy after a piston, water and falling sand: "
					+ helper.getBlockState(c) + " / " + helper.getBlockState(c.above()));
				helper.assertTrue(helper.getBlockState(c.west()).isAir() || helper.getBlockState(c.west()).is(Blocks.PISTON_HEAD)
					? standing(helper, c) : false, "piston did not move it");
				helper.assertValueEqual(dropped(helper, a, EffigyContent.effigyItem(EffigyType.SKELETON)), 0, "effigies dropped by the blast");
				clearDrops(helper, a);
				clearDrops(helper, c);

				ServerPlayer player = TestPlayers.create(helper, GameType.SURVIVAL, new BlockPos(8, 1, 8));
				Holder<Enchantment> silk = level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.SILK_TOUCH);
				ItemStack silky = new ItemStack(Items.DIAMOND_PICKAXE);
				silky.enchant(silk, 1);
				player.setItemInHand(InteractionHand.MAIN_HAND, silky);
				player.gameMode.destroyBlock(helper.absolutePos(a));
				helper.assertTrue(helper.getBlockState(a).isAir() && helper.getBlockState(a.above()).isAir(), "both halves gone (silk touch, lower)");
				player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.DIAMOND_PICKAXE));
				player.gameMode.destroyBlock(helper.absolutePos(b.above()));
				helper.assertTrue(helper.getBlockState(b).isAir() && helper.getBlockState(b.above()).isAir(), "both halves gone (upper)");
				player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.NETHERITE_PICKAXE));
				player.gameMode.destroyBlock(helper.absolutePos(c));
				TestPlayers.remove(player);
			})
			.thenIdle(5)
			.thenExecute(() -> {
				helper.assertValueEqual(anyDropped(helper, a), 0, "items dropped by breaking (silk touch)");
				helper.assertValueEqual(anyDropped(helper, b), 0, "items dropped by breaking the upper half");
				helper.assertValueEqual(dropped(helper, c, EffigyContent.effigyItem(EffigyType.CREEPER)), 0, "effigies dropped (netherite)");
				helper.assertValueEqual(dropped(helper, c, EffigyContent.compressedItem(EffigyType.CREEPER)), 0, "blocks refunded");
				SimpleBlockTestLog.info("EFF-04 permanence: obsidian hardness, blast-proof, needs no support, piston/water/sand safe; "
					+ "broken with silk touch, by its upper half or plainly: gone, nothing dropped");
				clearDrops(helper, c);
			})
			.thenSucceed();
	}

	// ---------------------------------------------------------------- 5. the cone

	/** A point at {@code a} blocks ahead along the facing, {@code side} to its left (positive) and {@code up} above, from the apex. */
	private static Vec3 point(BlockPos lower, Vec3 facing, double a, double side, double up) {
		Vec3 left = new Vec3(facing.z, 0.0, -facing.x);
		return Effigies.apex(lower).add(facing.scale(a)).add(left.scale(side)).add(0.0, up, 0.0);
	}

	/**
	 * EFF-05: the cone, for a south-facing effigy and one at 37.5 degrees: 100 blocks ahead (99.9 in,
	 * 100.1 out), the same up, down and to either side at the far end (98.5 in, 99.5 out at 99 ahead),
	 * the 45-degree edge everywhere, nothing behind or level with the monument at its sides; the 4-block
	 * margin of the retreat. Real mobs: a zombie 99 blocks ahead and one 101 ahead (chunks loaded).
	 */
	@GameTest(structure = SHAFT, maxTicks = 400)
	public void coneGeometry(GameTestHelper helper) {
		BlockPos lower = helper.absolutePos(new BlockPos(2, 1, 2));
		List<String> log = new ArrayList<>();
		for (float yaw : new float[] {0.0F, 37.5F}) {
			Vec3 f = Vec3.directionFromRotation(0.0F, yaw);
			Object[][] cases = {
				{1.0, 0.0, 0.0, true, "1 ahead"}, {50.0, 0.0, 0.0, true, "50 ahead"}, {99.9, 0.0, 0.0, true, "99.9 ahead"},
				{100.1, 0.0, 0.0, false, "100.1 ahead"}, {99.0, 98.5, 0.0, true, "99 ahead, 98.5 left"}, {99.0, -98.5, 0.0, true, "99 ahead, 98.5 right"},
				{99.0, 99.5, 0.0, false, "99 ahead, 99.5 left"}, {99.0, 0.0, 98.5, true, "99 ahead, 98.5 up"}, {99.0, 0.0, -98.5, true, "99 ahead, 98.5 down"},
				{99.0, 0.0, 99.5, false, "99 ahead, 99.5 up"}, {99.0, 0.0, -99.5, false, "99 ahead, 99.5 down"},
				{50.0, 35.0, 35.0, true, "50 ahead, r 49.5"}, {50.0, 36.0, 36.0, false, "50 ahead, r 50.9"},
				{-0.5, 0.0, 0.0, false, "just behind"}, {-30.0, 0.0, 0.0, false, "30 behind"}, {0.0, 5.0, 0.0, false, "level with it, 5 left"},
				{0.0, 0.0, 5.0, false, "5 above it"}, {10.0, 0.0, 9.9, true, "10 ahead, 9.9 up"}, {10.0, 0.0, -10.1, false, "10 ahead, 10.1 down"},
			};
			for (Object[] c : cases) {
				Vec3 p = point(lower, f, (double) c[0], (double) c[1], (double) c[2]);
				boolean inside = Effigies.inside(lower, f, p);
				helper.assertTrue(inside == (boolean) c[3], "yaw " + yaw + ", " + c[4] + ": inside " + inside);
			}
			// The retreat's margin: 2 blocks outside the side is still within it, 6 outside is not; likewise past the far end.
			helper.assertTrue(Effigies.depth(lower, f, point(lower, f, 50.0, 52.0, 0.0), Effigies.MARGIN) > 0.0, "margin: 2 outside the side");
			helper.assertTrue(Effigies.depth(lower, f, point(lower, f, 50.0, 56.0, 0.0), Effigies.MARGIN) == 0.0, "margin: 6 outside the side");
			helper.assertTrue(Effigies.depth(lower, f, point(lower, f, 103.0, 0.0, 0.0), Effigies.MARGIN) > 0.0, "margin: 3 past the end");
			helper.assertTrue(Effigies.depth(lower, f, point(lower, f, 105.0, 0.0, 0.0), Effigies.MARGIN) == 0.0, "margin: 5 past the end");
			log.add("yaw " + yaw + ": " + cases.length + " points");
		}

		// Real mobs at the far end, in an arena region of its own (no other test's cone reaches it): an
		// effigy facing south, zombies 99 and 101 blocks ahead.
		ServerLevel level = helper.getLevel();
		Arena arena = new Arena(helper, 55, 4);
		BlockPos effigyPos = arena.at(0, 1, 0);
		List<Mob> mobs = new ArrayList<>();
		long[] chunks = {(effigyPos.getZ() + 99) >> 4, (effigyPos.getZ() + 102) >> 4};
		for (long cz : chunks) {
			level.setChunkForced(effigyPos.getX() >> 4, (int) cz, true);
		}
		Set<Long> forcedBefore = Set.copyOf(level.getForceLoadedChunks());
		int loadedBefore = Effigies.loadedCount(level);
		EffigyPlacementMenu.place(level, effigyPos, EffigyType.ZOMBIE, 0.0F, null);
		helper.assertValueEqual(Effigies.loadedCount(level), loadedBefore + 1, "loaded effigies after placing one");
		helper.assertTrue(Set.copyOf(level.getForceLoadedChunks()).equals(forcedBefore), "placing an effigy force-loads nothing");
		Vec3 apex = Effigies.apex(effigyPos);
		for (double ahead : new double[] {99.0, 101.0}) {
			Zombie zombie = EntityTypes.ZOMBIE.create(level, EntitySpawnReason.COMMAND);
			// Its bounding box's centre 99 (101) blocks along the axis, on a stone pillar top.
			zombie.snapTo(apex.x, apex.y - zombie.getBbHeight() / 2.0, apex.z + ahead, 0.0F, 0.0F);
			level.setBlockAndUpdate(zombie.blockPosition().below(), Blocks.STONE.defaultBlockState());
			mobs.add(zombie);
		}
		helper.startSequence()
			.thenWaitUntil(() -> {
				for (Mob mob : mobs) {
					helper.assertTrue(level.areEntitiesActuallyLoadedAndTicking(mob.chunkPosition()), "waiting for the far chunks");
				}
			})
			.thenExecute(() -> mobs.forEach(level::addFreshEntity))
			.thenIdle(10)
			.thenExecute(() -> {
				helper.assertTrue(Effigies.frightens(mobs.get(0)), "zombie 99 ahead: inside");
				helper.assertTrue(Effigies.afraid(mobs.get(0)), "zombie 99 ahead: afraid");
				Vec3 far = mobs.get(1).getBoundingBox().getCenter().subtract(apex);
				helper.assertTrue(!Effigies.frightens(mobs.get(1)) && !Effigies.afraid(mobs.get(1)), "zombie 101 ahead: not afraid; offset " + far);
				mobs.forEach(Mob::discard);
				level.setBlockAndUpdate(effigyPos, Blocks.AIR.defaultBlockState());
				for (Mob mob : mobs) {
					level.setBlockAndUpdate(mob.blockPosition().below(), Blocks.AIR.defaultBlockState());
				}
				for (long cz : chunks) {
					level.setChunkForced(effigyPos.getX() >> 4, (int) cz, false);
				}
				helper.assertTrue(helper.getLevel().getBlockState(effigyPos.above()).isAir(), "upper half removed with the lower");
				log.add("zombies at 99 (afraid) and 101 (not)");
				SimpleBlockTestLog.info("EFF-05 cone geometry: {}", log);
			})
			.thenSucceed();
	}

	/**
	 * EFF-05b: natural spawning is untouched: a dark spot in a closed cave passes the zombie's natural
	 * spawn rules, and still does once a zombie effigy's cone covers it.
	 */
	@GameTest(environment = "simple_block-gametest:m09_jockey_combat_night", skyAccess = true, maxTicks = 1000)
	public void spawnRulesUnchangedInsideTheCone(GameTestHelper helper) {
		Arena arena = new Arena(helper, 58, 8);
		arena.enclose();
		arena.whenLit()
			.thenExecute(() -> {
				ServerLevel level = arena.level;
				BlockPos spot = arena.at(2, 1, 0);
				boolean before = SpawnPlacements.checkSpawnRules(EntityTypes.ZOMBIE, level, EntitySpawnReason.NATURAL, spot, level.getRandom());
				BlockPos effigy = effigy(arena, -4, 0, EffigyType.ZOMBIE, 270.0F);
				helper.assertTrue(Effigies.inside(effigy, Vec3.directionFromRotation(0.0F, 270.0F), Vec3.atCenterOf(spot)), "spot inside the cone");
				boolean after = SpawnPlacements.checkSpawnRules(EntityTypes.ZOMBIE, level, EntitySpawnReason.NATURAL, spot, level.getRandom());
				helper.assertTrue(before && after, "spawn rules before " + before + ", after " + after);
				level.setBlockAndUpdate(effigy, Blocks.AIR.defaultBlockState());
				SimpleBlockTestLog.info("EFF-05b natural spawn rules in a dark cave: {} before, {} with a zombie effigy's cone over the spot", before, after);
			})
			.thenSucceed();
	}

	// ---------------------------------------------------------------- 5/6. fear in arenas

	/** The goals running for {@code mob} that hold the MOVE flag. */
	private static List<String> movers(Mob mob) {
		List<String> out = new ArrayList<>();
		for (WrappedGoal goal : ((MobAccessor) mob).simpleBlock$getGoalSelector().getAvailableGoals()) {
			if (goal.isRunning() && goal.getFlags().contains(Goal.Flag.MOVE)) {
				out.add(goal.getGoal().getClass().getSimpleName());
			}
		}
		return out;
	}

	/** An effigy in the arena at (dx, dz), facing {@code yaw}. */
	private static BlockPos effigy(Arena arena, int dx, int dz, EffigyType type, float yaw) {
		BlockPos pos = arena.at(dx, 1, dz);
		EffigyPlacementMenu.place(arena.level, pos, type, yaw, null);
		return pos;
	}

	/**
	 * EFF-06: an effigy facing east at night, a survival player and torches inside its cone. A skeleton
	 * already there when the effigy is placed; a zombie, a baby zombie, a creeper and an enderman spawned
	 * inside it, and a zombie spawned as if by a spawner:
	 * every one is afraid within a few ticks, never targets the player while afraid, runs (sprinting: dust
	 * and vanilla's +30% speed) and leaves the cone, then keeps clear through its retreat. No other goal with
	 * movement runs while afraid (light, doors, wandering and attacks all yield). A husk beside them is
	 * not afraid and goes for the player. The zombie spawned without persistence can still despawn.
	 */
	@GameTest(environment = "simple_block-gametest:m09_jockey_combat_night", skyAccess = true, maxTicks = 3000)
	public void matchingMobsFleeTheCone(GameTestHelper helper) {
		Arena arena = new Arena(helper, 40, 44);
		arena.torches(-28, -12, -2, 2);
		ServerPlayer player = arena.player(GameType.SURVIVAL, -30, 0);
		List<Mob> afraid = new ArrayList<>();
		Mob[] husk = new Mob[1];
		Mob[] early = new Mob[1];
		Mob[] loose = new Mob[1];
		boolean[] sprinted = new boolean[1];
		boolean[] targeted = new boolean[1];
		List<String> problems = new ArrayList<>();
		long[] start = new long[1];
		arena.whenLit()
			.thenExecute(() -> {
				MobGameTests.ensureClock(helper, true);
				// Already there before the effigy: a skeleton.
				early[0] = arena.spawn(EntityTypes.SKELETON, -20, 4);
			})
			.thenIdle(20)
			.thenExecute(() -> {
				helper.assertTrue(!Effigies.afraid((Mob) early[0]), "skeleton not afraid before the effigy");
				effigy(arena, -34, 0, EffigyType.ZOMBIE, 270.0F);
				effigy(arena, -34, 2, EffigyType.SKELETON, 270.0F);
				effigy(arena, -34, -2, EffigyType.CREEPER, 270.0F);
				effigy(arena, -34, 4, EffigyType.ENDERMAN, 270.0F);
				afraid.add(early[0]);
				afraid.add(arena.spawn(EntityTypes.ZOMBIE, -22, 0));
				Zombie baby = arena.spawn(EntityTypes.ZOMBIE, -22, -3);
				baby.setBaby(true);
				afraid.add(baby);
				afraid.add(arena.spawn(EntityTypes.CREEPER, -21, 2));
				afraid.add(arena.spawn(EntityTypes.ENDERMAN, -20, -1));
				// As if from a spawner, and without persistence.
				Zombie spawned = EntityTypes.ZOMBIE.spawn(arena.level, arena.at(-19, 1, 1), EntitySpawnReason.SPAWNER);
				arena.mobs.add(spawned);
				afraid.add(spawned);
				loose[0] = spawned;
				husk[0] = arena.spawn(EntityTypes.HUSK, -24, -1);
				start[0] = helper.getTick();
			})
			.thenIdle(4)
			.thenExecute(() -> {
				for (Mob mob : afraid) {
					helper.assertTrue(Effigies.afraid(mob), mob.getType().toShortString() + " afraid within 4 ticks");
				}
				helper.assertTrue(!Effigies.afraid(husk[0]), "husk not afraid");
			})
			.thenWaitUntil(() -> {
				for (Mob mob : afraid) {
					if (Effigies.afraid(mob) && mob.getTarget() != null) {
						problems.add(mob.getType().toShortString() + " targeted while afraid");
					}
					if (mob.isSprinting()) {
						sprinted[0] = true;
						helper.assertTrue(mob.getAttribute(Attributes.MOVEMENT_SPEED).getModifier(
							net.minecraft.resources.Identifier.withDefaultNamespace("sprinting")) != null, "sprint speed while sprinting");
					}
					if (Effigies.afraid(mob) && !Effigies.retreating(mob)) {
						List<String> movers = movers(mob);
						if (!movers.isEmpty() && !movers.equals(List.of("FearGoal"))) {
							problems.add(mob.getType().toShortString() + " ran " + movers + " while afraid");
						}
					}
				}
				if (husk[0].isAlive() && husk[0].getTarget() == player) {
					// It goes for the player: that is all this needs; it leaves before it does any harm.
					targeted[0] = true;
					husk[0].discard();
				}
				player.setHealth(player.getMaxHealth());
				helper.assertTrue(problems.isEmpty(), "while afraid: " + problems);
				for (Mob mob : afraid) {
					helper.assertTrue(!Effigies.frightens(mob) && Effigies.retreating(mob), mob.getType().toShortString() + " still to leave the cone");
				}
			})
			.thenExecute(() -> {
				helper.assertTrue(sprinted[0], "sprinting seen");
				helper.assertTrue(targeted[0], "husk went for the player");
				helper.assertTrue(!loose[0].isPersistenceRequired() && loose[0].removeWhenFarAway(200.0), "spawned zombie can still despawn");
				SimpleBlockTestLog.info("EFF-06 fear: 6 mobs left the cone in {} ticks, sprinting, no target and no other movement goal while afraid; "
					+ "husk targeted the player; spawner zombie despawnable", helper.getTick() - start[0]);
			})
			.thenIdle(Effigies.RETREAT_TICKS / 2)
			.thenExecute(() -> {
				for (Mob mob : afraid) {
					helper.assertTrue(!Effigies.frightens(mob), mob.getType().toShortString() + " back in the cone during the retreat");
				}
			})
			.thenSucceed();
	}

	/**
	 * EFF-07: a skeleton behind the effigy (outside its cone) still shoots the player standing inside the
	 * cone: the effigy blocks nothing coming in from outside.
	 */
	@GameTest(environment = "simple_block-gametest:m09_jockey_combat_night", skyAccess = true, maxTicks = 1500)
	public void attacksFromOutsideStillReachIn(GameTestHelper helper) {
		Arena arena = new Arena(helper, 43, 24);
		ServerPlayer player = arena.player(GameType.SURVIVAL, 4, 0);
		Mob[] skeleton = new Mob[1];
		arena.whenLit()
			.thenExecute(() -> {
				MobGameTests.ensureClock(helper, true);
				effigy(arena, 0, 0, EffigyType.SKELETON, 270.0F);
				// Behind the effigy, in a fence pen (it shoots over the fence but cannot walk round into the cone).
				for (int dx = -10; dx <= -8; dx++) {
					for (int dz = 3; dz <= 5; dz++) {
						if (dx != -9 || dz != 4) {
							arena.set(dx, 1, dz, Blocks.OAK_FENCE);
						}
					}
				}
				net.minecraft.world.entity.monster.skeleton.Skeleton archer = arena.spawn(EntityTypes.SKELETON, -9, 4);
				archer.setItemSlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND, new ItemStack(Items.BOW));
				archer.reassessWeaponGoal();
				skeleton[0] = archer;
			})
			.thenWaitUntil(() -> {
				helper.assertTrue(!Effigies.afraid(skeleton[0]), "skeleton behind the effigy afraid");
				helper.assertTrue(player.getHealth() < player.getMaxHealth(), "waiting for the skeleton's arrow to hit: target "
					+ skeleton[0].getTarget() + ", sees " + skeleton[0].hasLineOfSight(player) + ", at " + skeleton[0].position() + ", player " + player.position()
					+ " " + player.gameMode.getGameModeForPlayer());
			})
			.thenExecute(() -> SimpleBlockTestLog.info("EFF-07 a skeleton outside the cone shot the player inside it: health {} of {}, arrows {}", player.getHealth(),
				player.getMaxHealth(), arena.level.getEntitiesOfClass(AbstractArrow.class, new AABB(arena.at(0, 1, 0)).inflate(20.0)).size()))
			.thenSucceed();
	}

	/**
	 * EFF-08: a skeleton riding a spider and a baby zombie riding a chicken inside the cone get off and
	 * flee; the spider and the chicken are not afraid. A creeper lit with flint and steel inside the cone
	 * has its fuse put out and does not explode.
	 */
	@GameTest(environment = "simple_block-gametest:m09_jockey_combat_night", skyAccess = true, maxTicks = 1500)
	public void jockeysDismountAndFusesGoOut(GameTestHelper helper) {
		Arena arena = new Arena(helper, 46, 30);
		Mob[] rider = new Mob[2];
		Mob[] mount = new Mob[2];
		Creeper[] creeper = new Creeper[1];
		arena.whenLit()
			.thenExecute(() -> {
				MobGameTests.ensureClock(helper, true);
				Spider spider = arena.spawn(EntityTypes.SPIDER, -10, 0);
				Mob skeleton = arena.spawn(EntityTypes.SKELETON, -10, 0);
				skeleton.startRiding(spider, true, false);
				Chicken chicken = arena.spawn(EntityTypes.CHICKEN, -10, 5);
				Zombie baby = arena.spawn(EntityTypes.ZOMBIE, -10, 5);
				baby.setBaby(true);
				baby.startRiding(chicken, true, false);
				helper.assertTrue(skeleton.getVehicle() == spider && baby.getVehicle() == chicken, "jockeys mounted");
				rider[0] = skeleton;
				rider[1] = baby;
				mount[0] = spider;
				mount[1] = chicken;
				creeper[0] = arena.spawn(EntityTypes.CREEPER, -10, -5);
				effigy(arena, -20, 0, EffigyType.SKELETON, 270.0F);
				effigy(arena, -20, 3, EffigyType.ZOMBIE, 270.0F);
				effigy(arena, -20, -3, EffigyType.CREEPER, 270.0F);
				creeper[0].ignite();
			})
			.thenIdle(5)
			.thenExecute(() -> {
				for (int i = 0; i < 2; i++) {
					helper.assertTrue(!rider[i].isPassenger(), rider[i].getType().toShortString() + " got off");
					helper.assertTrue(Effigies.afraid(rider[i]), rider[i].getType().toShortString() + " afraid");
					helper.assertTrue(!Effigies.afraid(mount[i]) && mount[i].isAlive(), mount[i].getType().toShortString() + " not afraid");
				}
				helper.assertTrue(!creeper[0].isIgnited() && creeper[0].getSwellDir() <= 0, "creeper fuse out");
			})
			.thenIdle(60)
			.thenExecute(() -> {
				helper.assertTrue(creeper[0].isAlive(), "creeper did not explode");
				SimpleBlockTestLog.info("EFF-08 skeleton left its spider, baby zombie its chicken, mounts unafraid; lit creeper's fuse put out, no explosion");
			})
			.thenSucceed();
	}

	/**
	 * EFF-09: a zombie walled into a 1 x 1 pen inside a cone, with the player in reach of its sight: it
	 * stays afraid with no target, does not get out or break anything, and looks for a way out no more
	 * than once a second.
	 */
	@GameTest(environment = "simple_block-gametest:m09_jockey_combat_night", skyAccess = true, maxTicks = 1000)
	public void trappedMobStaysAfraid(GameTestHelper helper) {
		Arena arena = new Arena(helper, 49, 20);
		ServerPlayer player = arena.player(GameType.SURVIVAL, -4, 4);
		player.getAbilities().invulnerable = true;
		Mob[] zombie = new Mob[1];
		int[] trapped = new int[1];
		Vec3[] where = new Vec3[1];
		arena.whenLit()
			.thenExecute(() -> {
				MobGameTests.ensureClock(helper, true);
				for (int dx = -1; dx <= 1; dx++) {
					for (int dz = -1; dz <= 1; dz++) {
						if (dx != 0 || dz != 0) {
							for (int dy = 1; dy <= 3; dy++) {
								arena.set(dx, dy, dz, Blocks.STONE_BRICKS);
							}
						}
					}
				}
				arena.set(0, 3, 0, Blocks.STONE_BRICKS);
				arena.set(-1, 2, 0, Blocks.GLASS);
				zombie[0] = arena.spawn(EntityTypes.ZOMBIE, 0, 0);
				effigy(arena, -10, 0, EffigyType.ZOMBIE, 270.0F);
				where[0] = zombie[0].position();
			})
			.thenIdle(5)
			.thenExecute(() -> trapped[0] = Effigies.searches(zombie[0]))
			.thenIdle(200)
			.thenExecute(() -> {
				helper.assertTrue(Effigies.afraid(zombie[0]) && zombie[0].getTarget() == null, "trapped zombie afraid with no target");
				helper.assertTrue(zombie[0].position().distanceTo(where[0]) < 0.8, "trapped zombie stayed in its pen");
				int tries = Effigies.searches(zombie[0]) - trapped[0];
				helper.assertTrue(tries >= 3 && tries <= 12, "ways out sought in 200 ticks: " + tries);
				for (int dx = -1; dx <= 1; dx++) {
					for (int dz = -1; dz <= 1; dz++) {
						if (dx != 0 || dz != 0) {
							helper.assertTrue(arena.level.getBlockState(arena.at(dx, 1, dz)).is(Blocks.STONE_BRICKS), "pen wall intact");
						}
					}
				}
				SimpleBlockTestLog.info("EFF-09 trapped zombie: afraid, no target, stayed put, {} searches in 200 ticks, pen intact", tries);
			})
			.thenSucceed();
	}

	/**
	 * EFF-10: two zombie effigies facing each other across the arena; zombies in the middle, inside both
	 * cones, leave both (out to the sides) rather than bouncing between them.
	 */
	@GameTest(environment = "simple_block-gametest:m09_jockey_combat_night", skyAccess = true, maxTicks = 3000)
	public void overlappingConesAreLeftTogether(GameTestHelper helper) {
		Arena arena = new Arena(helper, 52, 48);
		List<Mob> zombies = new ArrayList<>();
		long[] start = new long[1];
		arena.whenLit()
			.thenExecute(() -> {
				MobGameTests.ensureClock(helper, true);
				effigy(arena, -30, 0, EffigyType.ZOMBIE, 270.0F);
				effigy(arena, 30, 0, EffigyType.ZOMBIE, 90.0F);
				for (int dz : new int[] {-2, 0, 2}) {
					zombies.add(arena.spawn(EntityTypes.ZOMBIE, 0, dz));
				}
				start[0] = helper.getTick();
			})
			.thenIdle(4)
			.thenExecute(() -> zombies.forEach(z -> helper.assertTrue(Effigies.afraid(z), "zombie afraid")))
			.thenWaitUntil(() -> {
				for (Mob z : zombies) {
					helper.assertTrue(!Effigies.frightens(z) && Effigies.retreating(z), "zombie still in a cone at " + z.position());
				}
			})
			.thenExecute(() -> SimpleBlockTestLog.info("EFF-10 overlapping cones: 3 zombies left both in {} ticks", helper.getTick() - start[0]))
			.thenSucceed();
	}

	// Keeps the EntityType import used where only the raw type is needed.
	@SuppressWarnings("unused")
	private static final EntityType<?> UNUSED = EntityTypes.ZOMBIE;
}
