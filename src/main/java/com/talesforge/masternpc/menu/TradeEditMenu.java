package com.talesforge.masternpc.menu;

import com.talesforge.masternpc.entity.custom.NpcEntity;
import com.talesforge.masternpc.npc.trade.NpcTrades;
import com.talesforge.masternpc.npc.trade.TradeOffer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.ItemCost;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class TradeEditMenu extends AbstractContainerMenu {
    public static final int ROWS = 8;
    private static final int COLS = 3;  // cost1, cost2, result
    public static final int INV_Y = 18 + ROWS * 18 + 14;

    private final NpcEntity npc;
    private final Container tradeContainer;
    private final boolean serverSide;
    private final ServerPlayer serverPlayer;

    public TradeEditMenu(int containerId, Inventory playerInv, NpcEntity npc) {
        super(ModMenus.TRADE_EDIT.get(), containerId);
        this.npc = npc;
        this.serverSide = !playerInv.player.level().isClientSide();
        this.serverPlayer = playerInv.player instanceof ServerPlayer sp ? sp : null;
        this.tradeContainer = new SimpleContainer(ROWS * COLS);
        if (serverSide) seedFrom(npc.getTrades());

        for (int row = 0; row < ROWS; row++)
            for (int col = 0; col < COLS; col++)
                addSlot(new Slot(tradeContainer, row * COLS + col, 8 + col * 18, 18 + row * 18));

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
            int index = i * COLS + 1;
            offer.cost2().ifPresent(c -> tradeContainer.setItem(index, new ItemStack(c.item(), c.count())));
            tradeContainer.setItem(i * COLS + 2, offer.result().copy());
        }
    }

    /** Called every server tick while the menu is open: keeps the NPC frozen and survives the "closed" packet from the old screen. */
    @Override
    public void broadcastChanges() {
        super.broadcastChanges();
        if (serverPlayer == null) return;
        if (npc.isEditedBy(serverPlayer)) npc.editorPing();
        else npc.tryStartEditing(serverPlayer);   // re-acquire if the old screen's "closed" packet released it
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        if (!serverSide) return;

        List<TradeOffer> offers = new ArrayList<>();
        for (int row = 0; row < ROWS; row++) {
            ItemStack cost1 = tradeContainer.getItem(row * COLS);
            ItemStack cost2 = tradeContainer.getItem(row * COLS + 1);
            ItemStack result = tradeContainer.getItem(row * COLS + 2);
            if (cost1.isEmpty() || result.isEmpty()) continue;

            offers.add(new TradeOffer(
                    new ItemCost(cost1.getItem(), cost1.getCount()),
                    cost2.isEmpty() ? Optional.empty() : Optional.of(new ItemCost(cost2.getItem(), cost2.getCount())),
                    result.copy()));
        }
        npc.setTrades(new NpcTrades(offers));

        for (int i = 0; i < tradeContainer.getContainerSize(); i++) {
            ItemStack stack = tradeContainer.getItem(i);
            if (!stack.isEmpty()) player.getInventory().placeItemBackInInventory(stack);
        }

        if (npc.isEditedBy(player)) npc.stopEditing();
    }

    @Override public ItemStack quickMoveStack(Player player, int index) { return ItemStack.EMPTY; } // без shift-клика, тащим руками

    @Override
    public boolean stillValid(Player player) {
        return npc.isAlive() && player.distanceToSqr(npc) <= NpcEntity.EDITOR_MAX_DIST_SQ;
    }
}
