package io.github.boxofdog.simpleblock.gametest.client;

import net.minecraft.client.Minecraft;
import java.util.ArrayList;
import java.util.List;

import com.mojang.blaze3d.platform.InputConstants;

import net.minecraft.client.gui.screens.InBedChatScreen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.vehicle.minecart.Minecart;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.client.gametest.v1.world.TestWorldSave;

import io.github.boxofdog.simpleblock.client.InventoryScreenAddons;
import io.github.boxofdog.simpleblock.feature.CreeperEvaporation;
import io.github.boxofdog.simpleblock.feature.HeavyLoad;
import io.github.boxofdog.simpleblock.feature.InventorySlots;
import io.github.boxofdog.simpleblock.feature.MinecartCargo;
import io.github.boxofdog.simpleblock.feature.MinecartCushions;
import io.github.boxofdog.simpleblock.gametest.SimpleBlockTestLog;
import io.github.boxofdog.simpleblock.gametest.mixin.HudAccessor;

/**
 * End-to-end smoke test in the real game client: a freshly created survival world, driven through
 * simulated keyboard/mouse input, with screenshots saved under {@code build/run/clientGameTest/screenshots}.
 * The rules themselves are covered in detail by the server game tests; this checks that the player
 * experiences them as intended (feedback messages, the bed screen, normal movement).
 */
public class SurvivalSmokeClientTest implements FabricClientGameTest {
	@Override
	public void runTest(ClientGameTestContext context) {
		TestWorldSave save;
		int ground;
		try (TestSingleplayerContext world = context.worldBuilder().create()) {
			save = world.getWorldSave();
			TestServerContext server = world.getServer();
			server.runCommand("gamerule minecraft:advance_time true");
			ground = server.computeOnServer(s -> s.overworld().getHeight(Heightmap.Types.MOTION_BLOCKING, 0, 0));
			server.runCommand("tp @a 0.5 " + ground + " 0.5 0 30");
			world.getConnection().waitForChunksRender();
			context.takeScreenshot("smoke-fresh-survival-world");

			bed(context, server, ground);
			shulkerBox(context, server, ground);
			movement(context, server, ground);
			elytra(context, server, ground);
			inventory(context, server);
			light(context, server, ground);
			creeper(context, server, ground);
			food(context, server, ground);
			minecart(context, server, ground);
		}
		// Save and quit, then open the same world again: the loaded minecart must still hold its cargo.
		try (TestSingleplayerContext world = save.open()) {
			world.getConnection().waitForChunksRender();
			minecartAfterReload(context, world.getServer(), ground);
			cushion(context, world.getServer(), ground);
			storageBypasses(context, world.getServer(), ground);
		}
	}

	/** Every minecart near the rails, on the server or (synced) on the client: {count, cargo item id}. */
	private static List<String> cartsOnServer(TestServerContext server, int ground) {
		return server.computeOnServer(s -> s.overworld().getEntitiesOfClass(Minecart.class, new AABB(-2, ground - 1, 0, 3, ground + 3, 10)).stream()
			.map(cart -> MinecartCargo.cart(cart).simpleBlock$count() + " " + BuiltInRegistries.ITEM.getKey(MinecartCargo.cart(cart).simpleBlock$cargo()))
			.toList());
	}

	private static List<String> cartsOnClient(ClientGameTestContext context, int ground) {
		return context.computeOnClient(client -> client.level.getEntitiesOfClass(Minecart.class, new AABB(-2, ground - 1, 0, 3, ground + 3, 10)).stream()
			.map(cart -> MinecartCargo.cart(cart).simpleBlock$count() + " " + BuiltInRegistries.ITEM.getKey(MinecartCargo.cart(cart).simpleBlock$cargo()))
			.toList());
	}

	/**
	 * FRT-01..08 in the real client: place a minecart on rails with the use key, load three stacks of
	 * raw iron, see the cargo label, fail to get in, sneak-use to unload a stack. The cart is left
	 * loaded for {@link #minecartAfterReload}.
	 */
	private static void minecart(ClientGameTestContext context, TestServerContext server, int ground) {
		server.runCommand("clear @a");
		server.runCommand("effect clear @a");
		server.runCommand("gamemode survival @a");
		server.runCommand("time set noon");
		server.runCommand("kill @e[type=!minecraft:player]");
		server.runCommand("fill 0 " + ground + " 2 0 " + ground + " 8 minecraft:rail");
		server.runCommand("tp @a 0.5 " + ground + " 0.5 0 30");
		server.runCommand("give @a minecraft:minecart");
		context.waitTicks(10);
		context.getInput().lookAt(new BlockPos(0, ground, 2));
		context.getInput().pressKey(options -> options.keyUse);
		context.waitTicks(10);
		List<String> placed = cartsOnServer(server, ground);
		assertTrue(placed.size() == 1, "expected one minecart on the rails after using the item, found " + placed);

		for (int stack = 0; stack < 3; stack++) {
			server.runCommand("item replace entity @a weapon.mainhand with minecraft:raw_iron 64");
			context.waitTicks(3);
			context.getInput().lookAt(new BlockPos(0, ground, 2));
			context.getInput().pressKey(options -> options.keyUse);
			context.waitTicks(5);
		}
		String loadedMessage = overlayMessage(context);
		List<String> serverCarts = cartsOnServer(server, ground);
		List<String> clientCarts = cartsOnClient(context, ground);
		int held = context.computeOnClient(client -> client.player.getInventory().countItem(Items.RAW_IRON));
		context.takeScreenshot("minecart-loaded-192-raw-iron");
		SimpleBlockTestLog.info("CLIENT MINECART: after three right-clicks with 64 raw iron: server {}, client {}, raw iron in hand {}, message '{}'",
			serverCarts, clientCarts, held, loadedMessage);
		assertTrue(serverCarts.equals(List.of("192 minecraft:raw_iron")), "server cargo after loading: " + serverCarts);
		assertTrue(clientCarts.equals(serverCarts), "client cargo " + clientCarts + " differs from server " + serverCarts);
		assertTrue(held == 0, "raw iron left in hand: " + held);
		assertTrue(loadedMessage.contains("192 / 256"), "expected the cargo in the message, saw: " + loadedMessage);

		// Empty hand: no seat, just the cargo.
		context.getInput().pressKey(options -> options.keyUse);
		context.waitTicks(5);
		boolean riding = context.computeOnClient(client -> client.player.getVehicle() != null);
		boolean ridingOnServer = server.computeOnServer(s -> playerOf(s.overworld()).getVehicle() != null);
		assertTrue(!riding && !ridingOnServer, "using a minecart with an empty hand should not seat the player");

		// Sneak + use: one stack comes back.
		context.getInput().holdKey(options -> options.keyShift);
		context.waitTicks(3);
		// Crouching lowers the eyes; aim at the cart again so the crosshair is on it and not the rail.
		context.getInput().lookAt(new BlockPos(0, ground, 2));
		context.waitTicks(1);
		context.getInput().pressKey(options -> options.keyUse);
		context.waitTicks(5);
		context.getInput().releaseKey(options -> options.keyShift);
		context.waitTicks(3);
		int unloaded = context.computeOnClient(client -> client.player.getInventory().countItem(Items.RAW_IRON));
		List<String> afterUnload = cartsOnServer(server, ground);
		String unloadMessage = overlayMessage(context);
		context.takeScreenshot("minecart-unloaded-one-stack");
		SimpleBlockTestLog.info("CLIENT MINECART: riding after use: {}; sneak-use unloaded {} raw iron, cart now {}, message '{}'", riding, unloaded,
			afterUnload, unloadMessage);
		assertTrue(unloaded == 64, "expected 64 raw iron unloaded, got " + unloaded);
		assertTrue(afterUnload.equals(List.of("128 minecraft:raw_iron")), "cart after unloading: " + afterUnload);
		server.runCommand("clear @a");
	}

	/** FRT-05 after a real save and reload; then FRT-07: break the cart by hand and see the cargo spill. */
	private static void minecartAfterReload(ClientGameTestContext context, TestServerContext server, int ground) {
		context.waitTicks(10);
		List<String> serverCarts = cartsOnServer(server, ground);
		List<String> clientCarts = cartsOnClient(context, ground);
		context.getInput().lookAt(new BlockPos(0, ground, 2));
		context.waitTicks(3);
		context.takeScreenshot("minecart-after-reload-label");
		SimpleBlockTestLog.info("CLIENT MINECART: after save, quit and reopen: server {}, client {}", serverCarts, clientCarts);
		assertTrue(serverCarts.equals(List.of("128 minecraft:raw_iron")), "server cargo after reopening: " + serverCarts);
		assertTrue(clientCarts.equals(serverCarts), "client cargo after reopening " + clientCarts + " differs from server " + serverCarts);

		for (int hit = 0; hit < 8 && !cartsOnServer(server, ground).isEmpty(); hit++) {
			context.getInput().pressKey(options -> options.keyAttack);
			context.waitTicks(4);
		}
		context.waitTicks(5);
		List<String> left = cartsOnServer(server, ground);
		int[] spilled = server.computeOnServer(s -> {
			List<ItemEntity> items = s.overworld().getEntitiesOfClass(ItemEntity.class, new AABB(-3, ground - 1, -2, 4, ground + 3, 6));
			int iron = items.stream().filter(e -> e.getItem().is(Items.RAW_IRON)).mapToInt(e -> e.getItem().getCount()).sum();
			int carts = items.stream().filter(e -> e.getItem().is(Items.MINECART)).mapToInt(e -> e.getItem().getCount()).sum();
			return new int[] {iron, carts};
		});
		int picked = context.computeOnClient(client -> client.player.getInventory().countItem(Items.RAW_IRON));
		int pickedCarts = context.computeOnClient(client -> client.player.getInventory().countItem(Items.MINECART));
		context.takeScreenshot("minecart-broken-cargo-spilled");
		SimpleBlockTestLog.info("CLIENT MINECART: broken by hand: carts left {}, raw iron on the ground {} + picked up {}, minecart items {} + picked up {}",
			left, spilled[0], picked, spilled[1], pickedCarts);
		assertTrue(left.isEmpty(), "the cart should be broken: " + left);
		assertTrue(spilled[0] + picked == 128, "expected 128 raw iron out of the cart, saw " + spilled[0] + " + " + picked);
		assertTrue(spilled[1] + pickedCarts == 1, "expected one minecart item, saw " + spilled[1] + " + " + pickedCarts);
		server.runCommand("kill @e[type=minecraft:item]");
		server.runCommand("clear @a");
	}

	/** The fitted cushion of the minecart at the start of the rails, on the server and (synced) on the client. */
	private static String[] cushions(ClientGameTestContext context, TestServerContext server, int ground) {
		AABB rails = new AABB(-2, ground - 1, 0, 3, ground + 3, 10);
		String onServer = server.computeOnServer(s -> s.overworld().getEntitiesOfClass(Minecart.class, rails).stream()
			.map(cart -> String.valueOf(MinecartCushions.fitted(cart))).toList().toString());
		String onClient = context.computeOnClient(client -> client.level.getEntitiesOfClass(Minecart.class, rails).stream()
			.map(cart -> String.valueOf(MinecartCushions.fitted(cart))).toList().toString());
		return new String[] {onServer, onClient};
	}

	/** Cushions in the real client: fit one with a click, see it in the cart, get in, get out, sneak-click it back out. */
	private static void cushion(ClientGameTestContext context, TestServerContext server, int ground) {
		server.runCommand("kill @e[type=minecraft:item]");
		server.runCommand("clear @a");
		server.runCommand("tp @a 0.5 " + ground + " 0.5 0 30");
		server.runCommand("give @a minecraft:minecart");
		context.waitTicks(5);
		context.getInput().lookAt(new BlockPos(0, ground, 2));
		context.getInput().pressKey(options -> options.keyUse);
		context.waitTicks(5);
		server.runCommand("item replace entity @a weapon.mainhand with minecraft:red_cushion");
		context.waitTicks(3);
		context.getInput().lookAt(new BlockPos(0, ground, 2));
		context.getInput().pressKey(options -> options.keyUse);
		context.waitTicks(5);
		String[] fitted = cushions(context, server, ground);
		String fittedMessage = overlayMessage(context);
		int cushionsLeft = context.computeOnClient(client -> client.player.getInventory().countItem(Items.CUSHION.pick(net.minecraft.world.item.DyeColor.RED)));
		context.waitTicks(3);
		context.takeScreenshot("minecart-cushion-fitted");
		assertTrue(fitted[0].equals("[red]") && fitted[1].equals("[red]"), "cushion after fitting: server " + fitted[0] + ", client " + fitted[1]);
		assertTrue(cushionsLeft == 0, "the cushion should have been used: " + cushionsLeft + " left");

		context.getInput().lookAt(new BlockPos(0, ground, 2));
		context.getInput().pressKey(options -> options.keyUse);
		context.waitTicks(10);
		boolean riding = context.computeOnClient(client -> client.player.getVehicle() instanceof Minecart);
		boolean ridingOnServer = server.computeOnServer(s -> playerOf(s.overworld()).getVehicle() instanceof Minecart);
		context.takeScreenshot("minecart-cushion-riding");
		assertTrue(riding && ridingOnServer, "the player should be sitting in the cushioned cart (client " + riding + ", server " + ridingOnServer + ")");

		// Sneak to get out; then, back at the start, sneak-click to take the cushion back.
		context.getInput().holdKeyFor(options -> options.keyShift, 5);
		context.waitTicks(5);
		boolean stillRiding = context.computeOnClient(client -> client.player.getVehicle() != null);
		server.runCommand("tp @a 0.5 " + ground + " 0.5 0 30");
		context.waitTicks(5);
		context.getInput().holdKey(options -> options.keyShift);
		context.waitTicks(3);
		context.getInput().lookAt(new BlockPos(0, ground, 2));
		context.waitTicks(1);
		context.getInput().pressKey(options -> options.keyUse);
		context.waitTicks(5);
		context.getInput().releaseKey(options -> options.keyShift);
		context.waitTicks(3);
		String[] removed = cushions(context, server, ground);
		int cushionsBack = context.computeOnClient(client -> client.player.getInventory().countItem(Items.CUSHION.pick(net.minecraft.world.item.DyeColor.RED)));
		String removedMessage = overlayMessage(context);
		SimpleBlockTestLog.info("CLIENT CUSHION: fitted server {} / client {}, message '{}'; riding client {} / server {}; after sneaking riding: {}; "
			+ "sneak-click: cushion server {} / client {}, red cushions back {}, message '{}'", fitted[0], fitted[1], fittedMessage, riding, ridingOnServer,
			stillRiding, removed[0], removed[1], cushionsBack, removedMessage);
		assertTrue(!stillRiding, "sneaking should get the player out");
		assertTrue(removed[0].equals("[null]") && removed[1].equals("[null]") && cushionsBack == 1,
			"cushion after sneak-click: server " + removed[0] + ", client " + removed[1] + ", back in inventory " + cushionsBack);
		server.runCommand("kill @e[type=minecraft:minecart]");
		server.runCommand("kill @e[type=minecraft:item]");
		server.runCommand("clear @a");
	}

	/** FRT-10, FRT-11 in the real client: an ender chest and a chest minecart are refused with a message. */
	private static void storageBypasses(ClientGameTestContext context, TestServerContext server, int ground) {
		server.runCommand("give @a minecraft:ender_chest");
		context.waitTicks(5);
		BlockPos floor = new BlockPos(1, ground - 1, 2);
		context.getInput().lookAt(floor);
		context.getInput().pressKey(options -> options.keyUse);
		context.waitTicks(5);
		String enderMessage = overlayMessage(context);
		boolean enderPlaced = server.computeOnServer(s -> s.overworld().getBlockState(floor.above()).is(net.minecraft.world.level.block.Blocks.ENDER_CHEST));
		context.takeScreenshot("ender-chest-refused");
		server.runCommand("clear @a");
		server.runCommand("give @a minecraft:chest_minecart");
		context.waitTicks(5);
		context.getInput().lookAt(new BlockPos(0, ground, 3));
		context.getInput().pressKey(options -> options.keyUse);
		context.waitTicks(5);
		String cartMessage = overlayMessage(context);
		boolean chestCart = server.computeOnServer(s -> !s.overworld().getEntitiesOfClass(net.minecraft.world.entity.vehicle.minecart.MinecartChest.class,
			new AABB(-2, ground - 1, 0, 3, ground + 3, 10)).isEmpty());
		int stillHeld = context.computeOnClient(client -> client.player.getInventory().countItem(Items.CHEST_MINECART));
		context.takeScreenshot("chest-minecart-refused");
		SimpleBlockTestLog.info("CLIENT STORAGE: ender chest placed: {}, message '{}'; chest minecart placed: {}, still held: {}, message '{}'",
			enderPlaced, enderMessage, chestCart, stillHeld, cartMessage);
		assertTrue(!enderPlaced && enderMessage.toLowerCase().contains("ender chest"), "ender chest: placed " + enderPlaced + ", message " + enderMessage);
		assertTrue(!chestCart && stillHeld == 1 && cartMessage.toLowerCase().contains("chest minecart"),
			"chest minecart: placed " + chestCart + ", held " + stillHeld + ", message " + cartMessage);
		server.runCommand("clear @a");
	}

	/** SF-01: lie down at midnight, get vanilla's "can't pass this night" message, time keeps running, Leave Bed works. */
	private static void bed(ClientGameTestContext context, TestServerContext server, int ground) {
		server.runCommand("time set midnight");
		server.runCommand("setblock 0 " + ground + " 2 minecraft:red_bed[facing=south,part=foot]");
		server.runCommand("setblock 0 " + ground + " 3 minecraft:red_bed[facing=south,part=head]");
		context.waitTicks(5);
		context.getInput().lookAt(new BlockPos(0, ground, 2));
		context.getInput().pressKey(options -> options.keyUse);
		context.waitForScreen(InBedChatScreen.class);
		long clockInBed = server.computeOnServer(s -> s.overworld().getOverworldClockTime());
		context.waitTicks(10);
		String message = overlayMessage(context);
		context.takeScreenshot("smoke-bed-at-midnight");
		assertTrue(Component.translatable("sleep.not_possible").getString().equals(message),
				"expected the 'no amount of rest' message, saw: " + message);

		context.waitTicks(150);
		long clockLater = server.computeOnServer(s -> s.overworld().getOverworldClockTime());
		boolean stillSleeping = context.computeOnClient(client -> client.player.isSleeping());
		BlockPos respawn = server.computeOnServer(s -> playerOf(s.overworld()).getRespawnConfig().respawnData().pos());
		SimpleBlockTestLog.info("CLIENT BED: message '{}', clock {} -> {} after 160 ticks in bed, still in bed: {}, respawn {}",
				message, clockInBed, clockLater, stillSleeping, respawn);
		assertTrue(stillSleeping, "player should still be in bed (no skip)");
		assertTrue(clockLater - clockInBed < 400, "clock should run normally, not jump to morning");
		assertTrue(respawn.equals(new BlockPos(0, ground, 3)), "respawn should be at the bed, was " + respawn);

		context.clickScreenButton("multiplayer.stopSleeping");
		context.waitTicks(10);
		assertTrue(!context.computeOnClient(client -> client.player.isSleeping()), "Leave Bed should get the player up");
		server.runCommand("fill 0 " + ground + " 2 0 " + ground + " 3 minecraft:air");
		server.runCommand("time set noon");
	}

	/** SF-04: right-clicking the ground with a shulker box does nothing except explain why. */
	private static void shulkerBox(ClientGameTestContext context, TestServerContext server, int ground) {
		server.runCommand("clear @a");
		server.runCommand("give @a minecraft:red_shulker_box");
		context.waitTicks(5);
		BlockPos target = new BlockPos(0, ground - 1, 2);
		context.getInput().lookAt(target);
		context.getInput().pressKey(options -> options.keyUse);
		context.waitTicks(5);
		String message = overlayMessage(context);
		context.takeScreenshot("smoke-shulker-box-refused");
		boolean placed = server.computeOnServer(s -> !s.overworld().getBlockState(target.above()).isAir());
		int boxesHeld = context.computeOnClient(client -> client.player.getInventory().countItem(Items.DYED_SHULKER_BOX.pick(net.minecraft.world.item.DyeColor.RED)));
		SimpleBlockTestLog.info("CLIENT SHULKER: placed: {}, still holding: {}, message '{}'", placed, boxesHeld, message);
		assertTrue(!placed, "the shulker box was placed");
		assertTrue(boxesHeld == 1, "the shulker box should still be in the inventory");
		assertTrue(message.toLowerCase().contains("shulker"), "expected a shulker box message, saw: " + message);
		server.runCommand("clear @a");
	}

	/** ELY-03: walking, jumping and falling (with fall damage) are normal. */
	private static void movement(ClientGameTestContext context, TestServerContext server, int ground) {
		server.runCommand("tp @a 0.5 " + ground + " 0.5 0 0");
		context.waitTicks(5);
		double startZ = context.computeOnClient(client -> client.player.getZ());
		context.getInput().holdKeyFor(options -> options.keyUp, 20);
		double walked = context.computeOnClient(client -> client.player.getZ()) - startZ;

		double groundY = context.computeOnClient(client -> client.player.getY());
		context.getInput().holdKeyFor(options -> options.keyJump, 3);
		double peak = groundY;
		for (int i = 0; i < 8; i++) {
			peak = Math.max(peak, context.computeOnClient(client -> client.player.getY()));
			context.waitTick();
		}

		server.runCommand("tp @a 0.5 " + (ground + 10) + " 0.5 0 0");
		// Wait for the teleport to arrive, then for the player to land back on the ground.
		context.waitFor(client -> client.player.getY() > ground + 5, 40);
		context.waitFor(client -> client.player.getY() < ground + 0.01 && client.player.onGround(), 200);
		context.waitTicks(5);
		float health = context.computeOnClient(client -> client.player.getHealth());
		SimpleBlockTestLog.info("CLIENT MOVEMENT: walked {} blocks in 20 ticks, jump height {}, health after 10-block fall {}",
				String.format("%.2f", walked), String.format("%.2f", peak - groundY), health);
		assertTrue(walked > 2.0, "player should walk forward");
		assertTrue(peak - groundY > 0.5, "player should jump");
		assertTrue(health < 20.0F, "a 10-block fall should still hurt");
		server.runCommand("effect give @a minecraft:instant_health 1 5");
	}

	/** SF-03: an elytra given by command does not let the player glide when jumping mid-air. */
	private static void elytra(ClientGameTestContext context, TestServerContext server, int ground) {
		server.runCommand("item replace entity @a armor.chest with minecraft:elytra");
		server.runCommand("tp @a 0.5 " + (ground + 30) + " 0.5 0 0");
		context.waitTicks(4);
		boolean gliding = false;
		for (int attempt = 0; attempt < 3; attempt++) {
			context.getInput().pressKey(options -> options.keyJump);
			context.waitTicks(3);
			gliding |= context.computeOnClient(client -> client.player.isFallFlying());
			gliding |= server.computeOnServer(s -> playerOf(s.overworld()).isFallFlying());
		}

		context.takeScreenshot("smoke-elytra-jump-mid-air");
		SimpleBlockTestLog.info("CLIENT ELYTRA: wearing elytra, pressed jump 3 times mid-air, gliding at any point: {}", gliding);
		assertTrue(!gliding, "player glided with an elytra");
		server.runCommand("effect give @a minecraft:resistance 10 4");
		context.waitFor(client -> client.player.getY() < ground + 0.01 && client.player.onGround(), 300);
	}

	/**
	 * INV-01, INV-02, INV-04 on screen: the survival inventory shows two storage rows and the hotbar
	 * (the third row is covered), the load bar's materials agree with the server, the Sort button
	 * sorts through the real client-to-server packet, and the bar's frame turns red at 64.
	 */
	private static void inventory(ClientGameTestContext context, TestServerContext server) {
		server.runCommand("clear @a");
		server.runCommand("item replace entity @a container.0 with minecraft:torch 16");
		server.runCommand("item replace entity @a container.18 with minecraft:cobblestone 40");
		server.runCommand("item replace entity @a container.20 with minecraft:dirt 5");
		server.runCommand("item replace entity @a container.22 with minecraft:iron_ingot 30");
		server.runCommand("item replace entity @a container.23 with minecraft:gold_ingot 6");
		server.runCommand("item replace entity @a container.24 with minecraft:copper_ingot 4");
		server.runCommand("item replace entity @a container.26 with minecraft:netherite_ingot 2");
		server.runCommand("item replace entity @a container.25 with minecraft:diamond_pickaxe");
		server.runCommand("item replace entity @a container.31 with minecraft:cobblestone 40");
		server.runCommand("item replace entity @a container.35 with minecraft:apple 3");
		context.waitTicks(5);
		context.getInput().pressKey(options -> options.keyInventory);
		context.waitForScreen(InventoryScreen.class);
		context.waitTicks(2);
		int usable = context.computeOnClient(client -> {
			int count = 0;
			for (Slot slot : client.player.inventoryMenu.slots) {
				if (slot.container == client.player.getInventory() && slot.getContainerSlot() < 36 && slot.isActive()) {
					count++;
				}
			}
			return count;
		});
		int clientLoad = context.computeOnClient(client -> HeavyLoad.carried(client.player));
		int serverLoad = server.computeOnServer(s -> HeavyLoad.carried(playerOf(s.overworld())));
		long[] clientMaterials = context.computeOnClient(client -> HeavyLoad.carriedByMaterial(client.player));
		long[] serverMaterials = server.computeOnServer(s -> HeavyLoad.carriedByMaterial(playerOf(s.overworld())));
		context.takeScreenshot("inventory-load-bar-mixed");
		List<String> tooltip = loadBarTooltip(context, server);

		context.clickScreenButton("simple_block.inventory.sort");
		context.waitTicks(5);
		List<String> sorted = server.computeOnServer(s -> {
			List<String> layout = new ArrayList<>();
			for (int slot : InventorySlots.SORT_ORDER) {
				ItemStack stack = playerOf(s.overworld()).getInventory().getItem(slot);
				layout.add(stack.isEmpty() ? "-" : BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath() + "x" + stack.getCount());
			}
			return layout;
		});
		boolean torchKept = server.computeOnServer(s -> playerOf(s.overworld()).getInventory().getItem(0).is(Items.TORCH));
		context.takeScreenshot("inventory-after-sort");

		server.runCommand("give @a minecraft:iron_ingot 30");
		context.waitTicks(10);
		int fullClient = context.computeOnClient(client -> HeavyLoad.carried(client.player));
		int fullServer = server.computeOnServer(s -> HeavyLoad.carried(playerOf(s.overworld())));
		context.takeScreenshot("inventory-heavy-full");
		context.getInput().pressKey(options -> options.keyInventory);
		context.waitTicks(5);

		SimpleBlockTestLog.info("CLIENT INVENTORY: {} usable inventory slots on screen; heavy load client {} / server {} (units of 1/{} ingot), by "
				+ "material (iron/gold/copper/diamond/netherite) client {} / server {}; load bar tooltip with 4 gold nuggets added, then after they "
				+ "were taken away again (still hovering): {}; after Sort (top row first): {}; hotbar torch kept: {}; after /give 30 more iron: "
				+ "client {} / server {}", usable, clientLoad, serverLoad, HeavyLoad.UNITS_PER_INGOT, java.util.Arrays.toString(clientMaterials),
				java.util.Arrays.toString(serverMaterials), tooltip, sorted, torchKept, HeavyLoad.describe(fullClient), HeavyLoad.describe(fullServer));
		assertTrue(usable == 27, "expected 27 usable inventory slots on screen, saw " + usable);
		// 30 iron + 6 gold + 4 copper + a diamond pickaxe (3) + 2 netherite ingots = 45 ingots.
		assertTrue(clientLoad == serverLoad && serverLoad == 45 * HeavyLoad.UNITS_PER_INGOT, "client load should be 45 like the server");
		assertTrue(java.util.Arrays.equals(clientMaterials, serverMaterials), "the bar's materials should match the server's");
		assertTrue(tooltip.equals(List.of("Heavy load: 45.45 / 64", "\u25A0 Iron: 30", "\u25A0 Copper: 4", "\u25A0 Gold: 6.44", "\u25A0 Diamond: 3",
				"\u25A0 Netherite: 2", "|", "Heavy load: 45 / 64", "\u25A0 Iron: 30", "\u25A0 Copper: 4", "\u25A0 Gold: 6", "\u25A0 Diamond: 3",
				"\u25A0 Netherite: 2")), "load bar tooltip: " + tooltip);
		assertTrue(sorted.indexOf("-") == 9, "9 stacks after merging, then empty slots: " + sorted);
		assertTrue(torchKept, "Sort must not touch the hotbar");
		assertTrue(fullServer == HeavyLoad.CAPACITY && fullClient == fullServer, "load should stop at 64 and the indicator agree");
		server.runCommand("kill @e[type=minecraft:item]");
		server.runCommand("clear @a");
	}

	/** LIGHT-01 in the client's own light data: a placed torch gives 12, a lantern 15. */
	private static void light(ClientGameTestContext context, TestServerContext server, int ground) {
		BlockPos torch = new BlockPos(-3, ground, 0);
		BlockPos lantern = new BlockPos(3, ground, 0);
		server.runCommand("setblock " + torch.getX() + " " + ground + " 0 minecraft:torch");
		server.runCommand("setblock " + lantern.getX() + " " + ground + " 0 minecraft:lantern");
		context.waitFor(client -> client.level.getBrightness(LightLayer.BLOCK, torch) > 0 && client.level.getBrightness(LightLayer.BLOCK, lantern) > 0, 100);
		context.waitTicks(5);
		int torchLight = context.computeOnClient(client -> client.level.getBrightness(LightLayer.BLOCK, torch));
		int lanternLight = context.computeOnClient(client -> client.level.getBrightness(LightLayer.BLOCK, lantern));
		SimpleBlockTestLog.info("CLIENT LIGHT: client block light at a torch {}, at a lantern {}", torchLight, lanternLight);
		assertTrue(torchLight == 12, "client torch light should be 12, was " + torchLight);
		assertTrue(lanternLight == 15, "client lantern light should be 15, was " + lanternLight);
		server.runCommand("fill -3 " + ground + " 0 3 " + ground + " 0 minecraft:air");
	}

	/**
	 * MOB-03 on screen: a creeper in the noon sun smokes for half a second and is gone, without an
	 * explosion or drops. It has no AI so that it stands still in front of the camera rather than
	 * chasing the survival player (the evaporation does not depend on the AI).
	 */
	private static void creeper(ClientGameTestContext context, TestServerContext server, int ground) {
		server.runCommand("time set noon");
		server.runCommand("weather clear");
		server.runCommand("tp @a 0.5 " + ground + " 0.5 0 10");
		context.waitTicks(5);
		float health = context.computeOnClient(client -> client.player.getHealth());
		server.runCommand("summon minecraft:creeper 0.5 " + ground + " 4.5 {NoAI:1b,Rotation:[180f,0f]}");
		boolean smoking = false;
		for (int tick = 0; tick < 400 && !smoking; tick++) {
			context.waitTick();
			smoking = server.computeOnServer(s -> s.overworld().getEntitiesOfClass(Creeper.class, new AABB(new BlockPos(0, ground, 4)).inflate(4))
					.stream().anyMatch(CreeperEvaporation::isEvaporating));
		}
		context.waitTicks(3);
		context.takeScreenshot("creeper-smoking-in-sunlight");
		context.waitTicks(20);
		context.takeScreenshot("creeper-gone");
		int creepers = server.computeOnServer(s -> s.overworld().getEntitiesOfClass(Creeper.class, new AABB(new BlockPos(0, ground, 4)).inflate(8)).size());
		int clientCreepers = context.computeOnClient(client -> client.level.getEntitiesOfClass(Creeper.class, new AABB(new BlockPos(0, ground, 4)).inflate(8)).size());
		int drops = server.computeOnServer(s -> s.overworld().getEntitiesOfClass(ItemEntity.class, new AABB(new BlockPos(0, ground, 4)).inflate(8)).size());
		float healthAfter = context.computeOnClient(client -> client.player.getHealth());
		SimpleBlockTestLog.info("CLIENT CREEPER: creeper in the noon sun started smoking: {}; afterwards creepers on server {} / client {}, item drops {}, "
				+ "player health {} -> {}", smoking, creepers, clientCreepers, drops, health, healthAfter);
		assertTrue(smoking, "the creeper should start smoking in the sun");
		assertTrue(creepers == 0 && clientCreepers == 0, "the creeper should be gone on server and client");
		assertTrue(drops == 0, "no drops");
		assertTrue(healthAfter >= health, "no explosion damage");
	}

	/**
	 * FOOD-01 on screen and over the network: the saturation shown by the client follows the server
	 * after eating, after exertion (a change vanilla does not send by itself) and during
	 * regeneration; a real sprint costs half of vanilla's 0.1 exhaustion per block; screenshots of
	 * the gold saturation icons at GUI scales 1-3 and of creative mode (no food bar, no overlay).
	 */
	private static void food(ClientGameTestContext context, TestServerContext server, int ground) {
		server.runCommand("clear @a");
		server.runCommand("effect clear @a");
		server.runCommand("gamemode survival @a");
		server.runCommand("tp @a 0.5 " + ground + " 0.5 0 0");
		server.computeOnServer(s -> {
			ServerPlayer player = playerOf(s.overworld());
			player.setHealth(20.0F);
			player.getFoodData().setFoodLevel(10);
			player.getFoodData().setSaturation(0.0F);
			return null;
		});
		server.runCommand("item replace entity @a weapon.mainhand with minecraft:cooked_beef 4");
		context.waitTicks(5);
		context.getInput().holdKeyFor(options -> options.keyUse, 40);
		context.waitTicks(5);
		float[] afterEating = saturations(context, server);
		context.takeScreenshot("hud-saturation-after-eating");

		// Exertion: the next food tick turns 4 exhaustion into 1 saturation; food and health stay as they are.
		server.computeOnServer(s -> {
			playerOf(s.overworld()).causeFoodExhaustion(4.5F);
			return null;
		});
		context.waitTicks(5);
		float[] afterExertion = saturations(context, server);

		// Regeneration from saturation: full food and missing health.
		server.computeOnServer(s -> {
			ServerPlayer player = playerOf(s.overworld());
			player.getFoodData().setFoodLevel(20);
			player.setHealth(14.0F);
			return null;
		});
		context.waitTicks(25);
		float[] afterHealing = saturations(context, server);
		float healthAfterHealing = server.computeOnServer(s -> playerOf(s.overworld()).getHealth());

		// A real sprint: the client sends its movement and the server charges it.
		server.computeOnServer(s -> {
			ServerPlayer player = playerOf(s.overworld());
			player.setHealth(20.0F);
			player.getFoodData().setFoodLevel(18);
			player.getFoodData().setSaturation(5.0F);
			setExhaustion(player, 0.0F);
			return null;
		});
		context.waitTicks(2);
		double startX = server.computeOnServer(s -> playerOf(s.overworld()).getX());
		double startZ = server.computeOnServer(s -> playerOf(s.overworld()).getZ());
		context.getInput().holdKey(options -> options.keySprint);
		context.getInput().holdKeyFor(options -> options.keyUp, 40);
		context.getInput().releaseKey(options -> options.keySprint);
		context.waitTicks(5);
		double sprinted = server.computeOnServer(s -> Math.hypot(playerOf(s.overworld()).getX() - startX, playerOf(s.overworld()).getZ() - startZ));
		float sprintExhaustion = server.computeOnServer(s -> exhaustion(playerOf(s.overworld())));

		// GUI scales at 1280x720 (auto would be 3), then creative.
		server.computeOnServer(s -> {
			ServerPlayer player = playerOf(s.overworld());
			player.getFoodData().setFoodLevel(20);
			player.getFoodData().setSaturation(7.0F);
			return null;
		});
		context.getInput().resizeWindow(1280, 720);
		for (int scale = 1; scale <= 3; scale++) {
			int guiScale = scale;
			context.runOnClient(client -> {
				client.options.guiScale().set(guiScale);
				client.resizeGui();
			});
			context.waitTicks(3);
			context.takeScreenshot("hud-saturation-gui-scale-" + scale);
		}
		// At the largest scale: the 27-slot inventory, then the HUD hidden with F1.
		context.getInput().pressKey(options -> options.keyInventory);
		context.waitForScreen(InventoryScreen.class);
		context.waitTicks(2);
		context.takeScreenshot("inventory-gui-scale-3");
		context.getInput().pressKey(options -> options.keyInventory);
		context.waitTicks(3);
		context.getInput().pressKey(InputConstants.KEY_F1);
		context.waitTicks(3);
		boolean hidden = context.computeOnClient(client -> client.gui.hud.isHidden());
		context.takeScreenshot("hud-hidden-f1");
		context.getInput().pressKey(InputConstants.KEY_F1);
		context.waitTicks(3);
		assertTrue(hidden, "F1 should hide the HUD");
		server.runCommand("gamemode creative @a");
		context.waitTicks(5);
		context.takeScreenshot("hud-creative-no-food-bar");
		server.runCommand("gamemode survival @a");
		context.runOnClient(client -> {
			client.options.guiScale().set(0);
			client.resizeGui();
		});
		context.getInput().resizeWindow(854, 480);
		context.waitTicks(3);

		SimpleBlockTestLog.info("CLIENT FOOD: saturation server/client after eating a steak {} / {}, after 4.5 exhaustion {} / {}, after regeneration "
				+ "{} / {} (health 14 -> {}); sprinted {} blocks for {} exhaustion = {} per block (vanilla 0.1)", afterEating[0], afterEating[1],
				afterExertion[0], afterExertion[1], afterHealing[0], afterHealing[1], healthAfterHealing, String.format("%.2f", sprinted),
				String.format("%.4f", sprintExhaustion), String.format("%.4f", sprintExhaustion / sprinted));
		assertTrue(afterEating[0] > 0.0F && afterEating[0] == afterEating[1], "client saturation should match the server after eating");
		assertTrue(afterExertion[0] == afterEating[0] - 1.0F && afterExertion[1] == afterExertion[0], "client saturation should follow exertion");
		assertTrue(afterHealing[0] < afterExertion[0] && afterHealing[1] == afterHealing[0], "client saturation should follow regeneration");
		assertTrue(sprinted > 5.0, "the player should have sprinted");
		double perBlock = sprintExhaustion / sprinted;
		assertTrue(perBlock > 0.035 && perBlock < 0.0525, "sprinting should cost about 0.05 exhaustion per block, was " + perBlock);
		server.runCommand("clear @a");
	}

	/**
	 * Hovers the load bar with the real mouse and returns the tooltip lines it shows: first with 4
	 * gold nuggets added (fractions), then, still hovering, after they are taken away again (the
	 * tooltip follows the inventory). The two lists are separated by "|". Takes a screenshot.
	 */
	private static List<String> loadBarTooltip(ClientGameTestContext context, TestServerContext server) {
		server.runCommand("item replace entity @a container.27 with minecraft:gold_nugget 4");
		context.waitTicks(5);
		int[] bar = context.computeOnClient(client -> InventoryScreenAddons.barBounds((AbstractContainerScreen<?>) client.gui.screen()));
		double scale = context.computeOnClient(client -> (double) client.getWindow().getGuiScale());
		context.getInput().setCursorPos((bar[0] + bar[2]) / 2.0 * scale, (bar[1] + bar[3]) / 2.0 * scale);
		context.waitTicks(3);
		List<String> lines = new ArrayList<>(shownTooltip(context));
		context.takeScreenshot("inventory-load-bar-tooltip");
		server.runCommand("item replace entity @a container.27 with minecraft:air");
		context.waitTicks(5);
		lines.add("|");
		lines.addAll(shownTooltip(context));
		context.getInput().setCursorPos(0, 0);
		context.waitTicks(2);
		return lines;
	}

	private static List<String> shownTooltip(ClientGameTestContext context) {
		return context.computeOnClient(client -> {
			List<Component> shown = InventoryScreenAddons.shownTooltip();
			return shown == null ? List.of("(no tooltip)") : shown.stream().map(Component::getString).toList();
		});
	}

	/** Server and client saturation of the player: [0] server, [1] client. */
	private static float[] saturations(ClientGameTestContext context, TestServerContext server) {
		float serverValue = server.computeOnServer(s -> playerOf(s.overworld()).getFoodData().getSaturationLevel());
		float clientValue = context.computeOnClient(client -> client.player.getFoodData().getSaturationLevel());
		return new float[] {serverValue, clientValue};
	}

	private static final java.lang.reflect.Field EXHAUSTION;

	static {
		try {
			EXHAUSTION = FoodData.class.getDeclaredField("exhaustionLevel");
			EXHAUSTION.setAccessible(true);
		} catch (ReflectiveOperationException e) {
			throw new IllegalStateException(e);
		}
	}

	private static float exhaustion(ServerPlayer player) {
		try {
			return EXHAUSTION.getFloat(player.getFoodData());
		} catch (IllegalAccessException e) {
			throw new IllegalStateException(e);
		}
	}

	private static void setExhaustion(ServerPlayer player, float value) {
		try {
			EXHAUSTION.setFloat(player.getFoodData(), value);
		} catch (IllegalAccessException e) {
			throw new IllegalStateException(e);
		}
	}

	private static ServerPlayer playerOf(ServerLevel level) {
		return level.players().getFirst();
	}

	private static String overlayMessage(ClientGameTestContext context) {
		return context.computeOnClient(client -> {
			Component message = ((HudAccessor) client.gui.hud).simple_block$getOverlayMessage();
			return message == null ? "" : message.getString();
		});
	}

	private static void assertTrue(boolean condition, String message) {
		if (!condition) {
			throw new AssertionError(message);
		}
	}
}
