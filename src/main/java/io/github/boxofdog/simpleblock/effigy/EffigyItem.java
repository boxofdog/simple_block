package io.github.boxofdog.simpleblock.effigy;

import java.util.function.Consumer;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

/**
 * M12: a Monster Effigy item. Using it on a block does not place it: the server works out where it
 * would stand (the clicked block if replaceable, else the block against the clicked face) and which way
 * it would face (the way the player looks, any horizontal angle: away from the player), and opens the
 * confirmation screen ({@link EffigyPlacementMenu}) for that spot. Only "Place" there places it and
 * uses up the item. Nothing happens if the spot has no room for both halves.
 */
public class EffigyItem extends Item {
	private final EffigyType type;

	public EffigyItem(EffigyType type, Item.Properties properties) {
		super(properties);
		this.type = type;
	}

	public EffigyType type() {
		return this.type;
	}

	@Override
	public InteractionResult useOn(UseOnContext context) {
		BlockPlaceContext place = new BlockPlaceContext(context);
		BlockPos lower = place.getClickedPos();
		Level level = context.getLevel();
		if (context.getPlayer() == null || !EffigyPlacementMenu.hasRoom(level, lower, context.getPlayer())) {
			return InteractionResult.FAIL;
		}
		if (context.getPlayer() instanceof ServerPlayer player) {
			float yaw = player.getYRot();
			player.openMenu(new SimpleMenuProvider((id, inventory, p) -> EffigyPlacementMenu.server(id, inventory, this.type, lower, yaw,
				context.getHand()), context.getItemInHand().getHoverName()));
		}
		return InteractionResult.SUCCESS;
	}

	@Override
	@SuppressWarnings("deprecation")
	public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display, Consumer<Component> builder, TooltipFlag flag) {
		builder.accept(tooltip(this.type));
	}

	/** "Scares away [monster] in a 100-block conical radius", naming the monster (in the plural: "Scares away Zombies ..."). */
	public static Component tooltip(EffigyType type) {
		return Component.translatable("item.simple_block." + type.getSerializedName() + "_effigy.tooltip").withStyle(ChatFormatting.GRAY);
	}
}
