package com.talesforge.nexusnpc.menu;

import com.talesforge.nexusnpc.npc.Npcs;
import com.talesforge.nexusnpc.npc.runtime.NpcEditing;
import com.talesforge.nexusnpc.npc.trade.NpcTrades;
import com.talesforge.nexusnpc.npc.trade.PaymentRule;
import com.talesforge.nexusnpc.npc.trade.TradeOffer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * The editor of ONE trade. Three "ghost" slots hold a template of each item (clicking with an item copies it in, the real
 * item never leaves your hand): the two payments on the left, the result on the right. Beside each payment two toggles
 * decide what counts as that item (see {@link PaymentRule}), kept in a ContainerData so the client shows them live.
 * <p>
 * Every change is saved to the NPC at once. A trade that has no payment or no result yet is simply not saved (the
 * previous version, if there is one, stays), and the screen says so.
 */
public class TradeEditMenu extends AbstractContainerMenu {
    public static final int GRID_SLOTS = 3;     // payment 1, payment 2, result
    public static final int WIDTH = 214;

    public static final int[] SLOT_X = {12, 12, 180};
    public static final int[] SLOT_Y = {28, 50, 39};
    public static final int INV_Y = 98;
    public static final int INV_X = (WIDTH - 176) / 2 + 8;

    /** Button ids (clickMenuButton) = indexes in the rules data: payment 1 "any variant", payment 1 "undamaged only", then payment 2. */
    public static final int BUTTON_ANY_1 = 0, BUTTON_PERFECT_1 = 1, BUTTON_ANY_2 = 2, BUTTON_PERFECT_2 = 3;

    private final Mob npc;
    private final SimpleContainer tradeContainer = new SimpleContainer(GRID_SLOTS);
    private final ContainerData rules = new SimpleContainerData(4);
    @Nullable private final ServerPlayer serverPlayer;
    private int index;   // Position of this trade in the NPC's list; -1 until a new trade is first saved

    /** @param tradeIndex the trade to edit, or -1 for a new one (the client passes -1: only the server uses it) */
    public TradeEditMenu(int containerId, Inventory playerInv, Mob npc, int tradeIndex) {
        super(ModMenus.TRADE_EDIT.get(), containerId);
        this.npc = npc;
        this.serverPlayer = playerInv.player instanceof ServerPlayer sp ? sp : null;

        List<TradeOffer> existing = serverPlayer != null ? Npcs.trades(npc).offers() : List.of();
        this.index = tradeIndex >= 0 && tradeIndex < existing.size() ? tradeIndex : -1;
        if (index >= 0) seed(existing.get(index));

        // Registered AFTER seeding, so loading the trade does not count as a change
        if (serverPlayer != null) tradeContainer.addListener(container -> saveTrade());

        for (int i = 0; i < GRID_SLOTS; i++) addSlot(new GhostSlot(tradeContainer, i, SLOT_X[i], SLOT_Y[i]));
        for (int r = 0; r < 3; r++)
            for (int c = 0; c < 9; c++)
                addSlot(new Slot(playerInv, 9 + r * 9 + c, INV_X + c * 18, INV_Y + r * 18));
        for (int c = 0; c < 9; c++)
            addSlot(new Slot(playerInv, c, INV_X + c * 18, INV_Y + 58));
        addDataSlots(rules);
    }

    private void seed(TradeOffer offer) {
        tradeContainer.setItem(0, offer.cost1().copy());
        tradeContainer.setItem(1, offer.cost2().copy());
        tradeContainer.setItem(2, offer.result().copy());
        rules.set(BUTTON_ANY_1, offer.rule1().anyVariant() ? 1 : 0);
        rules.set(BUTTON_PERFECT_1, offer.rule1().perfectOnly() ? 1 : 0);
        rules.set(BUTTON_ANY_2, offer.rule2().anyVariant() ? 1 : 0);
        rules.set(BUTTON_PERFECT_2, offer.rule2().perfectOnly() ? 1 : 0);
    }

    /** For the screen: the state of one toggle (a button id). */
    public boolean toggle(int id) { return rules.get(id) != 0; }

    // ================= saving =================

    private PaymentRule rule(int payment) {
        return new PaymentRule(toggle(payment * 2), toggle(payment * 2 + 1));
    }

    /**
     * Writes this trade into the NPC's list. A payment may sit in either slot: if only the second one is filled it is
     * treated as the first. Without a payment or without a result nothing is saved.
     */
    private void saveTrade() {
        if (serverPlayer == null) return;

        ItemStack pay1 = tradeContainer.getItem(0);
        ItemStack pay2 = tradeContainer.getItem(1);
        ItemStack result = tradeContainer.getItem(2);
        PaymentRule rule1 = rule(0);
        PaymentRule rule2 = rule(1);
        if (pay1.isEmpty() && !pay2.isEmpty()) {
            pay1 = pay2;
            rule1 = rule2;
            pay2 = ItemStack.EMPTY;
            rule2 = PaymentRule.DEFAULT;
        }
        if (pay1.isEmpty() || result.isEmpty()) return;

        TradeOffer offer = new TradeOffer(pay1.copy(), pay2.copy(), result.copy(), rule1, rule2);
        NpcTrades trades = Npcs.trades(npc);
        NpcTrades updated;
        if (index >= 0 && index < trades.size()) {
            updated = trades.with(index, offer);
        } else if (trades.size() < NpcTrades.MAX) {
            updated = trades.plus(offer);
            index = updated.size() - 1;
        } else {
            return;
        }
        Npcs.attach(npc);   // Saving a trade makes an ordinary mob an NPC (data only; the AI is untouched in VANILLA mode)
        Npcs.setTrades(npc, updated);
    }

    /** The toggles beside the payments, pressed on the client and executed here. */
    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id < 0 || id >= 4) return false;
        rules.set(id, rules.get(id) == 0 ? 1 : 0);
        saveTrade();
        return true;
    }

    // ================= ghost slots =================

    /** Click with an item in hand: copy it into the slot (the real item stays in your hand). */
    @Override
    public void clicked(int slotId, int button, ClickType clickType, Player player) {
        // Drags (QUICK_CRAFT) go to vanilla: GhostSlot#mayPlace is false, so it does nothing there.
        if (slotId >= 0 && slotId < GRID_SLOTS && clickType != ClickType.QUICK_CRAFT) {
            ghostClick(slots.get(slotId), button, clickType, player);
            return;
        }
        super.clicked(slotId, button, clickType, player);
    }

    private void ghostClick(Slot slot, int button, ClickType clickType, Player player) {
        ItemStack held = getCarried();
        ItemStack current = slot.getItem();

        switch (clickType) {
            case PICKUP -> {
                if (!held.isEmpty()) {
                    if (button == 0) {
                        slot.set(held.copy());                                   // same amount as in hand
                    } else if (ItemStack.isSameItemSameComponents(current, held)) {
                        if (current.getCount() < current.getMaxStackSize()) {
                            slot.set(current.copyWithCount(current.getCount() + 1));
                        }
                    } else {
                        slot.set(held.copyWithCount(1));
                    }
                } else if (!current.isEmpty()) {
                    if (button == 0 || current.getCount() <= 1) slot.set(ItemStack.EMPTY);
                    else slot.set(current.copyWithCount(current.getCount() - 1));
                }
            }
            case SWAP -> {
                if (button >= 0 && button < 9) {                                  // hotbar number keys
                    ItemStack fromHotbar = player.getInventory().getItem(button);
                    slot.set(fromHotbar.isEmpty() ? ItemStack.EMPTY : fromHotbar.copy());
                }
            }
            default -> {}   // shift-click, throw, clone, double-click: no real item moves out of the grid
        }
    }

    @Override public ItemStack quickMoveStack(Player player, int index) { return ItemStack.EMPTY; }

    // ================= lifecycle =================

    /** Runs every server tick while open: keeps the NPC frozen (same editing lock as before). */
    @Override
    public void broadcastChanges() {
        super.broadcastChanges();
        if (serverPlayer == null) return;
        if (NpcEditing.isEditedBy(npc, serverPlayer)) NpcEditing.ping(npc);
        else if (!NpcEditing.tryStart(npc, serverPlayer)) serverPlayer.closeContainer();  // Someone else took it
    }

    @Override
    public void removed(Player player) {
        super.removed(player);  // Vanilla returns whatever the player holds in hand
        saveTrade();            // Normally a no-op by now: every change was already saved as it happened
        // The editing lock is intentionally NOT released here: the player usually returns to the editor, whose pings
        // keep it alive. If they don't, it expires by itself in 5 s.
    }

    @Override
    public boolean stillValid(Player player) {
        return npc.isAlive() && player.distanceToSqr(npc) <= NpcEditing.MAX_DIST_SQ;
    }

    private static final class GhostSlot extends Slot {
        GhostSlot(Container container, int index, int x, int y) { super(container, index, x, y); }
        @Override public boolean mayPlace(ItemStack stack) { return false; }
        @Override public boolean mayPickup(Player player) { return false; }
    }
}
