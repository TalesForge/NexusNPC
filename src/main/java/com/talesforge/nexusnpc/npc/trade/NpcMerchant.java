package com.talesforge.nexusnpc.npc.trade;

import com.talesforge.nexusnpc.npc.Npcs;
import com.talesforge.nexusnpc.npc.data.NpcTradeMode;
import com.talesforge.nexusnpc.npc.runtime.NpcTalking;
import net.minecraft.server.level.ServerPlayer;
import com.talesforge.nexusnpc.npc.storage.NpcStorage;
import net.minecraft.world.SimpleContainer;
import java.util.List;
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

    /**
     * Vanilla calls this with the player when the trade window opens and with null when it closes (MerchantMenu#removed).
     * The NPC must not walk away in between — the dialogue that led here has just closed and let go of it — so an open
     * trade keeps it in conversation, and closing the trade lets it go.
     */
    @Override
    public void setTradingPlayer(@Nullable Player player) {
        Player previous = this.tradingPlayer;
        this.tradingPlayer = player;
        if (mob.level().isClientSide()) return;

        if (player instanceof ServerPlayer serverPlayer) {
            NpcTalking.touch(mob, serverPlayer);
        } else if (player == null && previous != null) {
            NpcTalking.stop(mob, previous);
        }
    }

    /** The trade window failed to open: forget the player WITHOUT ending the conversation (the dialogue is still open). */
    public void abortTrade() {
        this.tradingPlayer = null;
    }
    @Override @Nullable public Player getTradingPlayer() { return tradingPlayer; }

    @Override
    public MerchantOffers getOffers() {
        if (cachedOffers == null) {
            cachedOffers = new MerchantOffers();
            for (TradeOffer offer : Npcs.trades(mob).offers()) cachedOffers.add(offer.toMerchantOffer(maxUsesOf(offer)));
        }
        return cachedOffers;
    }

    /** Unlimited, or — for a shopkeeper with a stock — however many times the inventory can still supply the result. */
    private int maxUsesOf(TradeOffer offer) {
        if (Npcs.tradeMode(mob) != NpcTradeMode.STOCK) return Integer.MAX_VALUE;
        int perTrade = Math.max(1, offer.result().getCount());
        return NpcStorage.count(NpcStorage.inventory(mob), offer.result()) / perTrade;
    }

    @Override public void overrideOffers(MerchantOffers offers) {}  // Not loot-table-driven — nothing to override

    @Override
    public void notifyTrade(MerchantOffer offer) {
        offer.increaseUses();
        mob.playSound(SoundEvents.VILLAGER_YES, 1.0F, 1.0F);
        if (!mob.level().isClientSide() && Npcs.tradeMode(mob) == NpcTradeMode.STOCK) settle(offer);
    }

    /**
     * The player has paid and taken the goods (the menu did that): now the NPC's side of the deal. The goods leave its
     * inventory and the payment goes in. Payment that no longer fits falls at its feet rather than disappearing.
     */
    private void settle(MerchantOffer offer) {
        SimpleContainer inventory = NpcStorage.inventory(mob);
        ItemStack goods = offer.getResult();
        NpcStorage.remove(inventory, goods, goods.getCount());
        // The items the player really handed over (data components included), not the template the offer asked for
        List<ItemStack> paid = offer instanceof NpcMerchantOffer exact
                ? exact.takePayment()
                : List.of(offer.getCostA(), offer.getCostB());
        for (ItemStack payment : paid) {
            if (payment.isEmpty()) continue;
            ItemStack leftover = inventory.addItem(payment.copy());
            if (!leftover.isEmpty()) mob.spawnAtLocation(leftover);
        }
        refreshStock();
    }

    /**
     * Two trades may sell the same item, so after any sale every offer is recomputed from what is really left. The list
     * object stays the same (the open trade menu holds it); only its entries are replaced.
     */
    private void refreshStock() {
        if (cachedOffers == null) return;
        List<TradeOffer> trades = Npcs.trades(mob).offers();
        for (int i = 0; i < cachedOffers.size() && i < trades.size(); i++) {
            cachedOffers.set(i, trades.get(i).toMerchantOffer(maxUsesOf(trades.get(i))));
        }
    }

    @Override public void notifyTradeUpdated(ItemStack stack) {}
    @Override public int getVillagerXp() { return villagerXp; }
    @Override public void overrideXp(int xp) { this.villagerXp = xp; }
    @Override public boolean showProgressBar() { return false; }
    @Override public SoundEvent getNotifyTradeSound() { return SoundEvents.VILLAGER_YES; }
    @Override public boolean isClientSide() { return mob.level().isClientSide(); }
}
