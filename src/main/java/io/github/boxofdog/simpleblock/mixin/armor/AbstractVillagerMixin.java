package io.github.boxofdog.simpleblock.mixin.armor;

import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.npc.villager.AbstractVillager;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.item.trading.TradeSet;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import io.github.boxofdog.simpleblock.feature.RetiredItems;

/** M10: villagers never offer retired items (the armorer's chainmail pieces); their other offers stay. */
@Mixin(AbstractVillager.class)
abstract class AbstractVillagerMixin {
	@Inject(method = "addOffersFromTradeSet", at = @At("TAIL"))
	private void simpleBlock$noRetiredOffers(ServerLevel level, MerchantOffers offers, ResourceKey<TradeSet> resourceKey, CallbackInfo ci) {
		offers.removeIf(offer -> RetiredItems.isRetired(offer.getResult()));
	}
}
