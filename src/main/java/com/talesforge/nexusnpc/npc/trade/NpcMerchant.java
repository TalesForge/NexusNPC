package com.talesforge.nexusnpc.npc.trade;

import com.talesforge.nexusnpc.npc.Npcs;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.Merchant;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import org.jetbrains.annotations.Nullable;

/**
 * Trading for an arbitrary mob. The mob does not implement {@link Merchant} itself — that would make
 * every mob look like a merchant to other mods and clash with villagers' own Merchant methods.
 * Instead each NPC mob lazily gets one of these adapters (see {@code Npcs#merchant}).
 */
public final class NpcMerchant implements Merchant {
    private final Mob mob;
    @Nullable private MerchantOffers cachedOffers;
    @Nullable private Player tradingPlayer;
    private int villagerXp = 0;

    public NpcMerchant(Mob mob) { this.mob = mob; }

    public Mob mob() { return mob; }

    /** Call after the trade list changed. */
    public void invalidate() { cachedOffers = null; }

    @Override public void setTradingPlayer(@Nullable Player player) { this.tradingPlayer = player; }
    @Override @Nullable public Player getTradingPlayer() { return tradingPlayer; }

    @Override
    public MerchantOffers getOffers() {
        if (cachedOffers == null) {
            cachedOffers = new MerchantOffers();
            for (TradeOffer offer : Npcs.trades(mob).offers()) cachedOffers.add(offer.toMerchantOffer());
        }
        return cachedOffers;
    }

    @Override public void overrideOffers(MerchantOffers offers) {}  // Not loot-table-driven — nothing to override
    @Override public void notifyTrade(MerchantOffer offer) { offer.increaseUses(); mob.playSound(SoundEvents.VILLAGER_YES, 1.0F, 1.0F); }
    @Override public void notifyTradeUpdated(ItemStack stack) {}
    @Override public int getVillagerXp() { return villagerXp; }
    @Override public void overrideXp(int xp) { this.villagerXp = xp; }
    @Override public boolean showProgressBar() { return false; }
    @Override public SoundEvent getNotifyTradeSound() { return SoundEvents.VILLAGER_YES; }
    @Override public boolean isClientSide() { return mob.level().isClientSide(); }
}
