package com.talesforge.nexusnpc.menu;

import com.talesforge.nexusnpc.npc.Npcs;
import com.talesforge.nexusnpc.npc.runtime.NpcEditing;
import net.minecraft.world.entity.Mob;
import com.talesforge.nexusnpc.npc.trade.NpcTrades;
import com.talesforge.nexusnpc.npc.trade.TradeOffer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.ItemCost;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Trade layout editor. The grid slots are "ghosts": they never hold or take real items,
 * they only remember WHICH item and how many. Clicking with a stack in hand copies it
 * into the slot and leaves the hand untouched, so nothing can be lost or duplicated.
 * The trades are applied to the NPC when the menu closes.
 */
public class TradeEditMenu extends AbstractContainerMenu {
    public static final int ROWS = 8;
    private static final int COLS = 3;  // cost1, cost2, result
    public static final int GRID_SLOTS = ROWS * COLS;
    public static final int INV_Y = 18 + ROWS * 18 + 14;

    private final Mob npc;
    private final Container tradeContainer;
    private final ServerPlayer serverPlayer;

    public TradeEditMenu(int containerId, Inventory playerInv, Mob npc) {
        super(ModMenus.TRADE_EDIT.get(), containerId);
        this.npc = npc;
        this.serverPlayer = playerInv.player instanceof ServerPlayer sp ? sp : null;
        this.tradeContainer = new SimpleContainer(GRID_SLOTS);
        if (serverPlayer != null) seedFrom(Npcs.trades(npc));  // The client gets it via slot sync

        for (int row = 0; row < ROWS; row++)
            for (int col = 0; col < COLS; col++)
                addSlot(new GhostSlot(tradeContainer, row * COLS + col, 8 + col * 18, 18 + row * 18));

        for (int r = 0; r < 3; r++)
            for (int c = 0; c < 9; c++)
                addSlot(new Slot(playerInv, 9 + r * 9 + c, 8 + c * 18, INV_Y + r * 18));
        for (int c = 0; c < 9; c++)
            addSlot(new Slot(playerInv, c, 8 + c * 18, INV_Y + 58));
    }

    private void seedFrom(NpcTrades trades) {
        List<TradeOffer> offers = trades.offers();
        for (int i = 0; i < offers.size() && i < ROWS; i++) {
            TradeOffer offer = offers.get(i);
            tradeContainer.setItem(i * COLS, new ItemStack(offer.cost1().item(), offer.cost1().count()));
            int row = i;
            offer.cost2().ifPresent(c -> tradeContainer.setItem(row * COLS + 1, new ItemStack(c.item(), c.count())));
            tradeContainer.setItem(i * COLS + 2, offer.result().copy());
        }
    }

    // ========== Clicks ==========
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

    // ========== Lifetime ==========
    /** Runs every server tick while open: keeps the NPC frozen (the old editor screen's "closed" packet releases the lock). */
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
        if (serverPlayer == null) return;

        List<TradeOffer> offers = new ArrayList<>();
        for (int row = 0; row < ROWS; row++) {
            ItemStack cost1 = tradeContainer.getItem(row * COLS);
            ItemStack cost2 = tradeContainer.getItem(row * COLS + 1);
            ItemStack result = tradeContainer.getItem(row * COLS + 2);
            if (cost1.isEmpty() || result.isEmpty()) continue;   // incomplete row

            offers.add(new TradeOffer(
                    new ItemCost(cost1.getItem(), cost1.getCount()),
                    cost2.isEmpty() ? Optional.empty() : Optional.of(new ItemCost(cost2.getItem(), cost2.getCount())),
                    result.copy()));
        }
        Npcs.attach(npc);  // Saving trades makes an ordinary mob an NPC (data only; the AI is untouched in VANILLA mode)
        Npcs.setTrades(npc, new NpcTrades(offers));
        // The editing lock is intentionally NOT released here: the player usually returns to the
        // settings screen, whose pings keep it alive. If they don't, it expires by itself in 5 s.
    }

    @Override public ItemStack quickMoveStack(Player player, int index) { return ItemStack.EMPTY; }

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
