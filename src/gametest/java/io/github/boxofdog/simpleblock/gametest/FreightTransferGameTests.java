package io.github.boxofdog.simpleblock.gametest;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.vehicle.minecart.Minecart;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HopperBlock;
import net.minecraft.world.level.block.RailBlock;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.state.properties.RailShape;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import net.fabricmc.fabric.api.gametest.v1.GameTest;

import io.github.boxofdog.simpleblock.feature.MinecartCargo;
import io.github.boxofdog.simpleblock.feature.MinecartCushions;
import io.github.boxofdog.simpleblock.train.CargoLooks;
import io.github.boxofdog.simpleblock.train.Couplings;
import io.github.boxofdog.simpleblock.train.FreightHoppers;
import io.github.boxofdog.simpleblock.train.LoadingTrackBlock;
import io.github.boxofdog.simpleblock.train.LoadingTracks;
import io.github.boxofdog.simpleblock.train.TrainContent;

/**
 * M11: freight carts filled and emptied gradually, by hand and by vanilla hoppers, their load's look
 * worked out from their cargo alone, and no text from any of it. Ids FILL-, HAND-, HOP- and TEXT-
 * refer to the M11 brief's deliverables 4-6.
 */
public class FreightTransferGameTests {
	static final String YARD = "simple_block-gametest:cable_yard";
	static final BlockPos RAIL = new BlockPos(3, 2, 3);

	static int count(Minecart cart) {
		return MinecartCargo.cart(cart).simpleBlock$count();
	}

	/** A Loading Rail at {@code rail} facing {@code facing}, on stone. */
	static void station(GameTestHelper helper, BlockPos rail, Direction facing) {
		helper.setBlock(rail.below(), Blocks.STONE);
		helper.setBlock(rail, TrainContent.LOADING_TRACK.defaultBlockState().setValue(LoadingTrackBlock.FACING, facing)
			.setValue(LoadingTrackBlock.SHAPE, facing.getAxis() == Direction.Axis.Z ? RailShape.NORTH_SOUTH : RailShape.EAST_WEST));
	}

	static HopperBlockEntity hopper(GameTestHelper helper, BlockPos pos, Direction facing, ItemStack... contents) {
		helper.setBlock(pos, Blocks.HOPPER.defaultBlockState().setValue(HopperBlock.FACING, facing));
		HopperBlockEntity hopper = (HopperBlockEntity) helper.getLevel().getBlockEntity(helper.absolutePos(pos));
		for (int i = 0; i < contents.length; i++) {
			hopper.setItem(i, contents[i]);
		}
		return hopper;
	}

	/** Takes a cart away without spilling its cargo (a discarded cart counts as destroyed and spills it). */
	static void remove(Minecart cart) {
		MinecartCargo.cart(cart).simpleBlock$setCargo(Items.AIR, 0);
		cart.discard();
	}

	/** Removes a block, emptying it first if it is a container (so a removed hopper drops nothing for the next one to take in). */
	static void clear(GameTestHelper helper, BlockPos pos) {
		if (helper.getLevel().getBlockEntity(helper.absolutePos(pos)) instanceof net.minecraft.world.Container container) {
			container.clearContent();
		}
		helper.setBlock(pos, Blocks.AIR);
	}

	/** For failure messages: the carts and dropped items near the rail. */
	static String surroundings(GameTestHelper helper) {
		AABB box = new AABB(helper.absolutePos(RAIL)).inflate(6.0);
		List<String> seen = new ArrayList<>();
		for (Minecart cart : helper.getLevel().getEntitiesOfClass(Minecart.class, box)) {
			seen.add("cart " + count(cart) + " " + MinecartCargo.cart(cart).simpleBlock$cargo() + " at " + cart.blockPosition().subtract(helper.absolutePos(BlockPos.ZERO))
				+ " gate " + LoadingTracks.freight(cart).simpleBlock$gateSide() + " docked " + docked(cart));
		}
		for (ItemEntity item : helper.getLevel().getEntitiesOfClass(ItemEntity.class, box)) {
			seen.add("item " + item.getItem() + " at " + item.blockPosition().subtract(helper.absolutePos(BlockPos.ZERO)));
		}
		return seen.toString();
	}

	static List<String> slots(HopperBlockEntity hopper) {
		List<String> slots = new ArrayList<>();
		for (int i = 0; i < hopper.getContainerSize(); i++) {
			slots.add(hopper.getItem(i).toString());
		}
		return slots;
	}

	static int held(HopperBlockEntity hopper, Item item) {
		int total = 0;
		for (int i = 0; i < hopper.getContainerSize(); i++) {
			total += hopper.getItem(i).is(item) ? hopper.getItem(i).getCount() : 0;
		}
		return total;
	}

	static boolean docked(Minecart cart) {
		return Couplings.coupled(cart).simpleBlock$dock() != null;
	}

	/** FILL-01: every cargo item has its own look, the 17 of the brief in the right forms; the bands. */
	@GameTest(structure = YARD)
	public void everyCargoHasItsOwnLookAndBand(GameTestHelper helper) {
		Map<Item, CargoLooks.Form> brief = new java.util.LinkedHashMap<>();
		brief.put(Items.GOLD_INGOT, CargoLooks.Form.INGOTS);
		brief.put(Items.COPPER_INGOT, CargoLooks.Form.INGOTS);
		brief.put(Items.IRON_INGOT, CargoLooks.Form.INGOTS);
		brief.put(Items.NETHERITE_INGOT, CargoLooks.Form.INGOTS);
		brief.put(Items.DIAMOND, CargoLooks.Form.GEMS);
		brief.put(Items.GOLD_BLOCK, CargoLooks.Form.BLOCKS);
		brief.put(BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("copper_block")), CargoLooks.Form.BLOCKS);
		brief.put(Items.IRON_BLOCK, CargoLooks.Form.BLOCKS);
		brief.put(Items.DIAMOND_BLOCK, CargoLooks.Form.BLOCKS);
		brief.put(Items.NETHERITE_BLOCK, CargoLooks.Form.BLOCKS);
		brief.put(Items.GOLD_ORE, CargoLooks.Form.ROCKS);
		brief.put(Items.COPPER_ORE, CargoLooks.Form.ROCKS);
		brief.put(Items.IRON_ORE, CargoLooks.Form.ROCKS);
		brief.put(Items.SAND, CargoLooks.Form.HEAP);
		brief.put(Items.GRAVEL, CargoLooks.Form.HEAP);
		brief.put(Items.COBBLESTONE, CargoLooks.Form.HEAP);
		brief.put(Items.DIRT, CargoLooks.Form.HEAP);
		for (Map.Entry<Item, CargoLooks.Form> entry : brief.entrySet()) {
			helper.assertTrue(MinecartCargo.isCargo(new ItemStack(entry.getKey())), entry.getKey() + " should be cart cargo");
			helper.assertValueEqual(CargoLooks.look(entry.getKey()).form(), entry.getValue(), "form of " + entry.getKey());
		}
		// Every item in the cargo tag has a look of its own, and a different one from every other item.
		List<String> looks = new ArrayList<>();
		java.util.Set<CargoLooks.Look> used = new java.util.HashSet<>();
		for (var holder : BuiltInRegistries.ITEM.getTagOrEmpty(MinecartCargo.CARGO)) {
			CargoLooks.Look look = CargoLooks.look(holder.value());
			helper.assertTrue(look != CargoLooks.Look.COVERED, holder.value() + " has no look of its own");
			helper.assertTrue(used.add(look), holder.value() + " shares the look " + look);
			looks.add(holder.value() + "=" + look.form().name().toLowerCase() + "/" + look.getSerializedName());
		}
		// And every look is for a cargo item, but the covered fallback, which is what anything else gets.
		for (CargoLooks.Look look : CargoLooks.Look.values()) {
			if (look != CargoLooks.Look.COVERED) {
				helper.assertTrue(MinecartCargo.isCargo(new ItemStack(BuiltInRegistries.ITEM.getValue(look.item()))), look + " is for a cargo item");
			}
		}
		helper.assertValueEqual(CargoLooks.look(Items.STICK), CargoLooks.Look.COVERED, "look of a non-cargo item");
		helper.assertValueEqual(used.size(), CargoLooks.Look.values().length - 1, "looks in use");
		// Deepslate ores and raw metals are not drawn as the plain ores.
		helper.assertTrue(CargoLooks.look(Items.DEEPSLATE_IRON_ORE) != CargoLooks.look(Items.IRON_ORE)
			&& CargoLooks.look(Items.RAW_IRON) != CargoLooks.look(Items.IRON_ORE), "deepslate and raw iron have their own looks");
		int[][] bands = {{0, 0}, {1, 1}, {64, 1}, {65, 2}, {128, 2}, {129, 3}, {192, 3}, {193, 4}, {256, 4}};
		for (int[] band : bands) {
			helper.assertValueEqual(CargoLooks.band(band[0]), band[1], "band at " + band[0]);
		}
		helper.assertValueEqual(TrainContent.CARGO_FILL_MODEL.getStateDefinition().getPossibleStates().size(), CargoLooks.Look.values().length * 4,
			"load model states");
		// A cushioned cart carries nothing, so it never shows a load.
		Minecart cushioned = SkipGameTests.cartOn(helper, new BlockPos(1, 2, 1), Items.AIR, 0);
		MinecartCargo.cart(cushioned).simpleBlock$setCushion(net.minecraft.world.item.DyeColor.RED);
		helper.assertTrue(MinecartCushions.isFitted(cushioned) && CargoLooks.band(count(cushioned)) == 0, "cushioned cart shows no load");
		remove(cushioned);
		SimpleBlockTestLog.info("FILL-01: {} cargo items, each its own look: {}; bands 0/1/64/65/128/129/192/193/256 -> 0/1/1/2/2/3/3/4/4; anything "
			+ "else covered", looks.size(), String.join(", ", looks));
		helper.succeed();
	}

	/**
	 * HAND-01, TEXT-01: loading by hand is gradual: held down (a use every tick), 4 items go in every 4
	 * ticks, the stack in hand going down by exactly as many; letting go stops it. Sneak-unloading
	 * likewise, within the heavy-load cap. A cart that holds something else takes nothing. Nothing
	 * the cart or the Loading Rail does sends the player any text.
	 */
	@GameTest(structure = YARD, maxTicks = 400)
	public void handLoadingIsGradualAndSilent(GameTestHelper helper) {
		station(helper, RAIL, Direction.NORTH);
		Minecart cart = SkipGameTests.cartOn(helper, RAIL, Items.AIR, 0);
		ServerPlayer player = TestPlayers.create(helper, GameType.SURVIVAL, new BlockPos(5, 2, 3));
		player.getInventory().clearContent();
		List<Integer> fills = new ArrayList<>();
		List<Integer> bands = new ArrayList<>();
		int[] start = new int[1];
		helper.startSequence()
			.thenIdle(2)
			.thenExecute(() -> {
				player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.RAW_IRON, 64));
				MinecartCargo.forgetPace(player);
			})
			.thenExecuteFor(40, () -> {
				player.interactOn(cart, InteractionHand.MAIN_HAND, cart.position());
				fills.add(count(cart));
				helper.assertValueEqual(count(cart) + player.getMainHandItem().getCount(), 64, "raw iron in hand and cart");
			})
			.thenExecute(() -> {
				helper.assertValueEqual(count(cart), 40, "after 40 ticks of holding use (10 batches of 4)");
				for (int i = 1; i < fills.size(); i++) {
					helper.assertTrue(fills.get(i) - fills.get(i - 1) == 0 || fills.get(i) - fills.get(i - 1) == 4, "steps of 4: " + fills);
				}
				start[0] = count(cart);
			})
			.thenIdle(40)
			.thenExecute(() -> {
				helper.assertValueEqual(count(cart), start[0], "let go: nothing more moves");
				helper.assertValueEqual(player.getMainHandItem().getCount(), 24, "left in hand");
				// To the next band and beyond: another stack, held.
				player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.RAW_IRON, 64));
			})
			.thenExecuteFor(120, () -> {
				player.interactOn(cart, InteractionHand.MAIN_HAND, cart.position());
				bands.add(CargoLooks.band(count(cart)));
			})
			.thenExecute(() -> {
				helper.assertValueEqual(count(cart), 104, "after the second stack");
				helper.assertTrue(bands.contains(1) && bands.contains(2) && bands.indexOf(2) > bands.lastIndexOf(1), "band 1 then 2: " + bands);
				// Another kind is refused, and leaves the hand as it was.
				player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.RAW_COPPER, 10));
				MinecartCargo.forgetPace(player);
				player.interactOn(cart, InteractionHand.MAIN_HAND, cart.position());
				helper.assertTrue(count(cart) == 104 && player.getMainHandItem().getCount() == 10, "raw copper refused");
				player.getInventory().clearContent();
				player.setShiftKeyDown(true);
				MinecartCargo.forgetPace(player);
			})
			.thenExecuteFor(40, () -> player.interactOn(cart, InteractionHand.MAIN_HAND, cart.position()))
			.thenExecute(() -> {
				player.setShiftKeyDown(false);
				helper.assertValueEqual(MinecartCargoGameTests.carried(player, Items.RAW_IRON), 40, "unloaded in 40 ticks (10 batches)");
				helper.assertValueEqual(count(cart), 64, "left in the cart");
				// The rail's release, by an empty hand: no text either.
				player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
				player.gameMode.useItemOn(player, helper.getLevel(), ItemStack.EMPTY, InteractionHand.MAIN_HAND,
					new net.minecraft.world.phys.BlockHitResult(Vec3.atCenterOf(helper.absolutePos(RAIL)), Direction.UP, helper.absolutePos(RAIL), false));
				helper.assertFalse(docked(cart), "released by using the rail");
				helper.assertValueEqual(ActionBarWatch.sent(player).size(), 0, "action-bar messages sent to the player");
				SimpleBlockTestLog.info("HAND-01/TEXT-01: held use: 40 ticks -> 40 in (steps of 4: {}), let go 40 ticks -> no change, second stack "
					+ "-> 104 (band 1 then 2), raw copper refused, sneak-held 40 ticks -> 40 out; {} action-bar messages", fills.subList(0, 12),
					ActionBarWatch.sent(player).size());
				remove(cart);
				TestPlayers.remove(player);
			})
			.thenSucceed();
	}

	/**
	 * HOP-01: a hopper pointing down over a cart docked at a Loading Rail feeds it, one item every 8
	 * ticks (the hopper's own pace), through the open top, gates shut; full, it is let go. No Loading
	 * Rail, a hopper locked by redstone, a hopper pointing sideways or two blocks up, another kind of
	 * cargo, a non-cargo item and a full cart all move nothing.
	 */
	@GameTest(structure = YARD, maxTicks = 1200)
	public void hopperAboveFeedsADockedCart(GameTestHelper helper) {
		BlockPos above = RAIL.above();
		Minecart[] cart = new Minecart[1];
		HopperBlockEntity[] hopper = new HopperBlockEntity[1];
		List<Integer> fills = new ArrayList<>();
		List<String> log = new ArrayList<>();
		var sequence = helper.startSequence()
			.thenExecute(() -> {
				station(helper, RAIL, Direction.NORTH);
				cart[0] = SkipGameTests.cartOn(helper, RAIL, Items.AIR, 0);
				hopper[0] = hopper(helper, above, Direction.DOWN, new ItemStack(Items.RAW_IRON, 20));
			})
			.thenExecuteFor(100, () -> {
				fills.add(count(cart[0]));
				helper.assertFalse(LoadingTracks.freight(cart[0]).simpleBlock$gateOpen(), "gates stay shut for top loading");
				helper.assertValueEqual(count(cart[0]) + held(hopper[0], Items.RAW_IRON), 20, "raw iron in hopper and cart");
			})
			.thenExecute(() -> {
				int n = count(cart[0]);
				helper.assertTrue(n >= 11 && n <= 13, "about one item every 8 ticks: " + n + " in 100 ticks");
				for (int i = 1; i < fills.size(); i++) {
					helper.assertTrue(fills.get(i) - fills.get(i - 1) <= 1, "one at a time: " + fills);
				}
				log.add("fed " + n + " in 100 ticks, one at a time");
			})
			.thenWaitUntil(() -> helper.assertValueEqual(held(hopper[0], Items.RAW_IRON), 0, "hopper emptied"))
			.thenExecute(() -> {
				helper.assertValueEqual(count(cart[0]), 20, "cart after the hopper emptied");
				helper.assertTrue(docked(cart[0]), "not full: still docked");
				// Topping up to 256 from above lets the car go.
				MinecartCargo.cart(cart[0]).simpleBlock$setCargo(Items.RAW_IRON, 254);
				hopper[0].setItem(0, new ItemStack(Items.RAW_IRON, 5));
			})
			.thenIdle(40)
			.thenExecute(() -> {
				helper.assertTrue(count(cart[0]) == 256 && held(hopper[0], Items.RAW_IRON) == 3, "full at 256, the hopper keeps 3");
				helper.assertFalse(docked(cart[0]), "a cart filled from above is let go");
				log.add("filled to 256 (hopper keeps 3), let go");
				remove(cart[0]);
			});
		record Refusal(String name, Runnable setUp, Item cartItem, int cartCount, ItemStack offered) {
		}
		List<Refusal> refusals = List.of(
			new Refusal("plain rail", () -> helper.setBlock(RAIL, Blocks.RAIL.defaultBlockState().setValue(RailBlock.SHAPE, RailShape.NORTH_SOUTH)),
				Items.AIR, 0, new ItemStack(Items.RAW_IRON, 10)),
			new Refusal("locked hopper", () -> helper.setBlock(above.east(), Blocks.REDSTONE_BLOCK), Items.AIR, 0, new ItemStack(Items.RAW_IRON, 10)),
			new Refusal("hopper pointing east", () -> hopper(helper, above, Direction.EAST), Items.AIR, 0, new ItemStack(Items.RAW_IRON, 10)),
			new Refusal("hopper two blocks up", () -> {
				helper.setBlock(above, Blocks.AIR);
				hopper(helper, above.above(), Direction.DOWN);
			}, Items.AIR, 0, new ItemStack(Items.RAW_IRON, 10)),
			new Refusal("other cargo", () -> {
			}, Items.RAW_COPPER, 30, new ItemStack(Items.RAW_IRON, 10)),
			new Refusal("not cargo", () -> {
			}, Items.AIR, 0, new ItemStack(Items.STICK, 10)),
			new Refusal("full cart", () -> {
			}, Items.RAW_IRON, 256, new ItemStack(Items.RAW_IRON, 10)));
		for (Refusal r : refusals) {
			HopperBlockEntity[] source = new HopperBlockEntity[1];
			sequence = sequence
				.thenExecute(() -> {
					station(helper, RAIL, Direction.NORTH);
					clear(helper, above);
					hopper(helper, above, Direction.DOWN);
					helper.setBlock(above.east(), Blocks.AIR);
					clear(helper, above.above());
					r.setUp().run();
					BlockPos at = helper.getBlockState(above).is(Blocks.HOPPER) ? above : above.above();
					source[0] = (HopperBlockEntity) helper.getLevel().getBlockEntity(helper.absolutePos(at));
					source[0].setItem(0, r.offered().copy());
					cart[0] = SkipGameTests.cartOn(helper, RAIL, r.cartItem(), r.cartCount());
				})
				.thenIdle(40)
				.thenExecute(() -> {
					helper.assertTrue(count(cart[0]) == r.cartCount() && source[0].getItem(0).getCount() == r.offered().getCount(),
						r.name() + ": cart " + count(cart[0]) + ", hopper " + source[0].getItem(0));
					log.add(r.name() + ": nothing moved");
					remove(cart[0]);
					clear(helper, above);
					clear(helper, above.above());
				});
		}
		sequence.thenExecute(() -> SimpleBlockTestLog.info("HOP-01 hopper over a docked cart: {}", log)).thenSucceed();
	}

	/**
	 * HOP-02: a cart docked at a Loading Rail tips into a hopper diagonally below it through the gate on
	 * that side, one item a tick after the gate has opened, each item leaving the cart as it enters the
	 * hopper; emptied, the gate shuts and the car is let go. Both rail directions, a hopper on each of
	 * the four sides: two end gates (folding down) and two side gates (swinging up) each way.
	 */
	@GameTest(structure = YARD, maxTicks = 1200)
	public void cartTipsIntoAHopperThroughThatGate(GameTestHelper helper) {
		List<String> log = new ArrayList<>();
		Minecart[] cart = new Minecart[1];
		HopperBlockEntity[] hopper = new HopperBlockEntity[1];
		Direction[] gates = new Direction[1];
		var sequence = helper.startSequence();
		for (Direction facing : List.of(Direction.NORTH, Direction.EAST)) {
			for (Direction side : Direction.Plane.HORIZONTAL) {
				List<Integer> fills = new ArrayList<>();
				sequence = sequence
					.thenExecute(() -> {
						station(helper, RAIL, facing);
						hopper[0] = hopper(helper, RAIL.relative(side).below(), Direction.DOWN);
						cart[0] = SkipGameTests.cartOn(helper, RAIL, Items.RAW_GOLD, 24);
						gates[0] = null;
					})
					.thenExecuteFor(45, () -> {
						fills.add(count(cart[0]));
						Direction open = LoadingTracks.freight(cart[0]).simpleBlock$gateSide();
						if (open != null) {
							helper.assertTrue(gates[0] == null || gates[0] == open, "one gate only: " + gates[0] + ", " + open);
							gates[0] = open;
						}
						helper.assertValueEqual(count(cart[0]) + held(hopper[0], Items.RAW_GOLD), 24, "raw gold in cart and hopper");
					})
					.thenExecute(() -> {
						String kind = side.getAxis() == facing.getAxis() ? "end" : "side";
						helper.assertValueEqual(gates[0], side, facing + " rail, hopper " + side + ": the gate that opened");
						helper.assertTrue(count(cart[0]) == 0 && held(hopper[0], Items.RAW_GOLD) == 24, "all 24 in the hopper: " + count(cart[0]));
						int firstMove = fills.indexOf(23);
						helper.assertTrue(firstMove >= FreightHoppers.DOOR_TICKS, "the gate opens before the cargo runs: " + fills);
						for (int i = 1; i < fills.size(); i++) {
							helper.assertTrue(fills.get(i - 1) - fills.get(i) <= 1, "one a tick: " + fills);
						}
						helper.assertFalse(docked(cart[0]), "emptied: let go");
						log.add(facing + " rail, hopper " + side + ": " + kind + " gate " + gates[0] + ", first item at tick " + firstMove + ", 24 in by "
							+ fills.indexOf(0));
						remove(cart[0]);
						clear(helper, RAIL.relative(side).below());
					})
					.thenIdle(5)
					.thenExecute(() -> helper.assertFalse(LoadingTracks.freight(cart[0]).simpleBlock$gateOpen(), "gate shut afterwards"));
			}
		}
		sequence.thenExecute(() -> SimpleBlockTestLog.info("HOP-02 cart -> hopper: {}", log)).thenSucceed();
	}

	/**
	 * HOP-03: what stops a cart tipping into a hopper beside it, and which hopper it uses. A full hopper,
	 * one full of something else, a block or a rail or another cart in the way, a hopper level with the
	 * rail, and a hopper feeding the cart from above all keep the cargo in and the gates shut. A hopper
	 * with room for 10 takes 10 and the gate shuts. A hopper locked by redstone still takes items in, as
	 * from another hopper. Of hoppers on the right, the left and the front, the right one is used until
	 * full, then the left one.
	 */
	@GameTest(structure = YARD, maxTicks = 1600)
	public void hopperDischargeRefusalsAndOrder(GameTestHelper helper) {
		List<String> log = new ArrayList<>();
		Minecart[] cart = new Minecart[1];
		BlockPos south = RAIL.south().below();
		ItemStack dirt = new ItemStack(Items.DIRT, 64);
		record Case(String name, Runnable setUp, int expectMoved) {
		}
		HopperBlockEntity[] receiver = new HopperBlockEntity[1];
		List<Case> cases = List.of(
			new Case("full hopper", () -> receiver[0] = hopper(helper, south, Direction.DOWN, dirt.copy(), dirt.copy(), dirt.copy(), dirt.copy(), dirt.copy()), 0),
			new Case("hopper full of another kind", () -> receiver[0] = hopper(helper, south, Direction.DOWN, new ItemStack(Items.RAW_COPPER),
				new ItemStack(Items.RAW_COPPER), new ItemStack(Items.RAW_COPPER), new ItemStack(Items.RAW_COPPER), new ItemStack(Items.RAW_COPPER)), 0),
			new Case("block over the hopper", () -> {
				receiver[0] = hopper(helper, south, Direction.DOWN);
				helper.setBlock(RAIL.south(), Blocks.STONE);
			}, 0),
			new Case("rail over the hopper", () -> {
				receiver[0] = hopper(helper, south, Direction.DOWN);
				helper.setBlock(RAIL.south(), Blocks.RAIL.defaultBlockState().setValue(RailBlock.SHAPE, RailShape.NORTH_SOUTH));
			}, 0),
			new Case("another cart over the hopper", () -> {
				receiver[0] = hopper(helper, south, Direction.DOWN);
				helper.spawn(EntityTypes.MINECART, Vec3.atBottomCenterOf(RAIL.south()).add(0.0, 0.0625, 0.0)).setNoGravity(true);
			}, 0),
			new Case("hopper level with the rail", () -> receiver[0] = hopper(helper, RAIL.south(), Direction.DOWN), 0),
			new Case("fed from above", () -> {
				receiver[0] = hopper(helper, south, Direction.DOWN);
				hopper(helper, RAIL.above(), Direction.DOWN);
			}, 0),
			new Case("room for 10", () -> receiver[0] = hopper(helper, south, Direction.DOWN, dirt.copy(), dirt.copy(), dirt.copy(), dirt.copy(),
				new ItemStack(Items.RAW_IRON, 54)), 10),
			new Case("locked hopper", () -> {
				receiver[0] = hopper(helper, south, Direction.DOWN);
				helper.setBlock(south.east(), Blocks.REDSTONE_BLOCK);
			}, 30));
		var sequence = helper.startSequence();
		for (Case c : cases) {
			sequence = sequence
				.thenExecute(() -> {
					station(helper, RAIL, Direction.NORTH);
					c.setUp().run();
					cart[0] = SkipGameTests.cartOn(helper, RAIL, Items.RAW_IRON, 30);
				})
				.thenIdle(60)
				.thenExecute(() -> {
					int moved = 30 - count(cart[0]);
					List<String> slots = new ArrayList<>();
					for (int i = 0; i < receiver[0].getContainerSize(); i++) {
						slots.add(receiver[0].getItem(i).toString());
					}
					helper.assertValueEqual(moved, c.expectMoved(), c.name() + ": items tipped (receiver " + slots + " at " + receiver[0].getBlockPos()
						+ ", docked " + docked(cart[0]) + ", over " + helper.getBlockState(RAIL.south()) + ", around " + surroundings(helper) + ")");
					helper.assertFalse(LoadingTracks.freight(cart[0]).simpleBlock$gateOpen(), c.name() + ": gate shut");
					helper.assertValueEqual(docked(cart[0]), moved < 30, c.name() + ": still docked unless emptied");
					log.add(c.name() + ": " + moved + " tipped");
					remove(cart[0]);
					for (Minecart other : helper.getLevel().getEntitiesOfClass(Minecart.class, new AABB(helper.absolutePos(RAIL)).inflate(3.0))) {
						remove(other);
					}
					for (BlockPos pos : List.of(south, south.east(), RAIL.south(), RAIL.above())) {
						clear(helper, pos);
					}
				});
		}
		// Three hoppers: right side (east, the rail facing north, room for 5), left side (west) and front end
		// (north): the right one first, then the left; the front is never needed.
		HopperBlockEntity[] three = new HopperBlockEntity[3];
		List<Direction> opened = new ArrayList<>();
		sequence
			.thenExecute(() -> {
				station(helper, RAIL, Direction.NORTH);
				three[0] = hopper(helper, RAIL.east().below(), Direction.DOWN, dirt.copy(), dirt.copy(), dirt.copy(), dirt.copy(),
					new ItemStack(Items.RAW_IRON, 59));
				three[1] = hopper(helper, RAIL.west().below(), Direction.DOWN);
				three[2] = hopper(helper, RAIL.north().below(), Direction.DOWN);
				cart[0] = SkipGameTests.cartOn(helper, RAIL, Items.RAW_IRON, 20);
			})
			.thenExecuteFor(80, () -> {
				Direction gate = LoadingTracks.freight(cart[0]).simpleBlock$gateSide();
				if (gate != null && (opened.isEmpty() || opened.getLast() != gate)) {
					opened.add(gate);
				}
			})
			.thenExecute(() -> {
				helper.assertValueEqual(opened, List.of(Direction.EAST, Direction.WEST), "gates in turn (around " + surroundings(helper) + ")");
				helper.assertTrue(held(three[0], Items.RAW_IRON) == 64 && held(three[1], Items.RAW_IRON) == 15 && held(three[2], Items.RAW_IRON) == 0
					&& count(cart[0]) == 0, "5 to the right, then 15 to the left, none to the front: " + held(three[0], Items.RAW_IRON) + ", "
					+ held(three[1], Items.RAW_IRON) + ", " + held(three[2], Items.RAW_IRON));
				log.add("three hoppers: right side (east) took 5 until full, then the left side (west) 15; the front end none");
				SimpleBlockTestLog.info("HOP-03: {}", log);
			})
			.thenSucceed();
	}

	/**
	 * HOP-04: a coupled train's docked car tips sideways into a hopper beside the rail; a hopper at its
	 * end, under the coupled neighbour, gets nothing; the neighbour keeps its load and the train stays
	 * coupled.
	 */
	@GameTest(structure = YARD, maxTicks = 300)
	public void coupledTrainUnloadsSideways(GameTestHelper helper) {
		Minecart[] train = new Minecart[2];
		HopperBlockEntity[] hoppers = new HopperBlockEntity[2];
		ServerPlayer player = TestPlayers.create(helper, GameType.SURVIVAL, new BlockPos(1, 2, 6));
		helper.startSequence()
			.thenExecute(() -> {
				station(helper, RAIL, Direction.WEST);
				helper.setBlock(RAIL.east().below(), Blocks.STONE);
				helper.setBlock(RAIL.east(), Blocks.RAIL.defaultBlockState().setValue(RailBlock.SHAPE, RailShape.EAST_WEST));
				hoppers[0] = hopper(helper, RAIL.south().below(), Direction.DOWN);
				helper.setBlock(RAIL.east().below(), Blocks.AIR);
				hoppers[1] = hopper(helper, RAIL.east().below(), Direction.DOWN);
				train[0] = SkipGameTests.cartOn(helper, RAIL, Items.COAL, 40);
				train[1] = SkipGameTests.cartOn(helper, RAIL.east(), Items.COAL, 30);
				TrainGameTests.couple(player, train[0], train[1]);
			})
			.thenIdle(70)
			.thenExecute(() -> {
				helper.assertTrue(held(hoppers[0], Items.COAL) == 40 && count(train[0]) == 0, "docked car into the side hopper: " + held(hoppers[0], Items.COAL));
				helper.assertTrue(held(hoppers[1], Items.COAL) == 0 && count(train[1]) == 30, "nothing under the neighbour");
				helper.assertTrue(TrainGameTests.coupled(train[0], train[1]), "still coupled");
				SimpleBlockTestLog.info("HOP-04: coupled car 40 coal -> south side hopper; end hopper under the neighbour 0; neighbour keeps 30; still coupled");
				remove(train[0]);
				remove(train[1]);
				TestPlayers.remove(player);
			})
			.thenSucceed();
	}

	/**
	 * HOP-05: an exchange cut short loses nothing. Saved mid-flow, the cart loads back with what it had;
	 * the hopper broken mid-flow drops what it took and the flow stops; a cart broken while a hopper
	 * feeds it drops what it had and the hopper keeps the rest.
	 */
	@GameTest(structure = YARD, maxTicks = 400)
	public void cutShortTransfersLoseNothing(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		Minecart[] cart = new Minecart[1];
		HopperBlockEntity[] hopper = new HopperBlockEntity[1];
		List<String> log = new ArrayList<>();
		helper.startSequence()
			.thenExecute(() -> {
				station(helper, RAIL, Direction.NORTH);
				hopper[0] = hopper(helper, RAIL.west().below(), Direction.DOWN);
				cart[0] = SkipGameTests.cartOn(helper, RAIL, Items.GOLD_INGOT, 60);
			})
			.thenIdle(30)
			.thenExecute(() -> {
				int inCart = count(cart[0]);
				int inHopper = held(hopper[0], Items.GOLD_INGOT);
				helper.assertTrue(inCart > 0 && inCart < 60 && inCart + inHopper == 60, "mid-flow: cart " + inCart + ", hopper " + inHopper);
				TagValueOutput output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, level.registryAccess());
				cart[0].saveWithoutId(output);
				CompoundTag tag = output.buildResult();
				Minecart copy = EntityTypes.MINECART.create(level, EntitySpawnReason.LOAD);
				copy.load(TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), tag));
				helper.assertValueEqual(count(copy), inCart, "saved mid-flow and loaded back");
				log.add("saved mid-flow at " + inCart + "/" + inHopper + ", loaded back " + count(copy));
				level.destroyBlock(helper.absolutePos(RAIL.west().below()), true);
			})
			.thenIdle(20)
			.thenExecute(() -> {
				int dropped = level.getEntitiesOfClass(ItemEntity.class, new AABB(helper.absolutePos(RAIL)).inflate(4.0)).stream()
					.filter(e -> e.getItem().is(Items.GOLD_INGOT)).mapToInt(e -> e.getItem().getCount()).sum();
				int left = count(cart[0]);
				helper.assertValueEqual(left + dropped, 60, "gold in the cart and dropped by the broken hopper");
				helper.assertFalse(LoadingTracks.freight(cart[0]).simpleBlock$gateOpen(), "gate shut once the hopper is gone");
				log.add("hopper broken: " + dropped + " dropped, " + left + " kept, gate shut");
				for (ItemEntity e : level.getEntitiesOfClass(ItemEntity.class, new AABB(helper.absolutePos(RAIL)).inflate(4.0))) {
					e.discard();
				}
				remove(cart[0]);
				cart[0] = SkipGameTests.cartOn(helper, RAIL, Items.AIR, 0);
				hopper[0] = hopper(helper, RAIL.above(), Direction.DOWN, new ItemStack(Items.DIAMOND, 30));
			})
			.thenIdle(60)
			.thenExecute(() -> {
				int inCart = count(cart[0]);
				helper.assertTrue(inCart > 0 && inCart < 30, "fed some: " + inCart);
				Vec3 at = cart[0].position();
				cart[0].hurtServer(level, level.damageSources().generic(), 100.0F);
				int dropped = level.getEntitiesOfClass(ItemEntity.class, new AABB(at, at).inflate(3.0)).stream()
					.filter(e -> e.getItem().is(Items.DIAMOND)).mapToInt(e -> e.getItem().getCount()).sum();
				helper.assertTrue(cart[0].isRemoved(), "cart broken");
				helper.assertValueEqual(dropped + held(hopper[0], Items.DIAMOND), 30, "diamonds dropped by the cart and kept by the hopper");
				log.add("cart broken while fed: " + dropped + " dropped, " + held(hopper[0], Items.DIAMOND) + " kept by the hopper");
				SimpleBlockTestLog.info("HOP-05: {}", log);
			})
			.thenSucceed();
	}
}
