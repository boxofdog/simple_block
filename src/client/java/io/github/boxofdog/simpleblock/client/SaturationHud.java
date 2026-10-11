package io.github.boxofdog.simpleblock.client;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;

import io.github.boxofdog.simpleblock.SimpleBlock;
import io.github.boxofdog.simpleblock.network.SaturationPayload;

/**
 * FOOD-01 saturation display, on by default: the food icons that are backed by saturation are
 * drawn again on top in gold and underlined in gold, one icon per 2 saturation points (half an icon
 * for an odd point), from the right end of the bar, like the food level itself. Saturation never exceeds the food level, so
 * the gold always sits on full icons. Uses the vanilla food sprites (no new art) and the position of
 * the vanilla food bar; drawn only when vanilla draws the food bar (survival/adventure, no mount
 * health in its place, HUD not hidden). The value comes from {@code SaturationPayload}.
 */
public final class SaturationHud {
	private static final Identifier ID = Identifier.fromNamespaceAndPath(SimpleBlock.MOD_ID, "saturation");
	private static final Identifier FOOD_FULL = Identifier.withDefaultNamespace("hud/food_full");
	private static final Identifier FOOD_HALF = Identifier.withDefaultNamespace("hud/food_half");
	/** Gold tint (ARGB) multiplied onto the food sprite. */
	private static final int GOLD = 0xFFFFC21F;
	private static final int UNDERLINE = 0xFFFFD84A;

	private SaturationHud() {
	}

	public static void register() {
		ClientPlayNetworking.registerGlobalReceiver(SaturationPayload.TYPE,
			(payload, context) -> context.player().getFoodData().setSaturation(payload.saturation()));
		HudElementRegistry.attachElementAfter(VanillaHudElements.FOOD_BAR, ID, SaturationHud::extract);
	}

	private static void extract(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
		Minecraft minecraft = Minecraft.getInstance();
		if (minecraft.gui.hud.isHidden() || minecraft.gameMode == null || !minecraft.gameMode.canHurtPlayer()
			|| !(minecraft.getCameraEntity() instanceof Player player) || hasMountHealth(player)) {
			return;
		}
		// Whole saturation points, as the food bar shows whole food points.
		int points = (int) player.getFoodData().getSaturationLevel();
		int right = graphics.guiWidth() / 2 + 91;
		int y = graphics.guiHeight() - 39;
		for (int i = 0; i < 10 && i * 2 < points; i++) {
			int x = right - i * 8 - 9;
			boolean full = i * 2 + 1 < points;
			graphics.blitSprite(RenderPipelines.GUI_TEXTURED, full ? FOOD_FULL : FOOD_HALF, x, y, 9, 9, GOLD);
			// Underline in the 1-pixel gap between the food row and the experience bar.
			graphics.fill(full ? x : x + 4, y + 9, x + 8, y + 10, UNDERLINE);
		}
	}

	/** Vanilla puts the mount's hearts where the food bar would be. */
	private static boolean hasMountHealth(Player player) {
		return player.getVehicle() instanceof LivingEntity vehicle && vehicle.showVehicleHealth() && (int) (vehicle.getMaxHealth() + 0.5F) / 2 > 0;
	}
}
