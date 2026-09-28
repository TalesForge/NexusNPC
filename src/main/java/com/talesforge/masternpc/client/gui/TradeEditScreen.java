package com.talesforge.masternpc.client.gui;

import com.talesforge.masternpc.menu.TradeEditMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

/**
 * Screen for {@link TradeEditMenu}: an 8x3 grid (cost 1 / cost 2 / result per row) above
 * the player's inventory. Drawn with plain rectangles, so it needs no texture asset.
 * Only renderBg has to change when a real background texture appears.
 */
public class TradeEditScreen extends AbstractContainerScreen<TradeEditMenu> {
    private static final int PANEL_COLOR = 0xFFC6C6C6;
    private static final int LIGHT_EDGE = 0xFFFFFFFF;
    private static final int DARK_EDGE = 0xFF555555;
    private static final int SLOT_COLOR = 0xFF8B8B8B;

    public TradeEditScreen(TradeEditMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = TradeEditMenu.INV_Y + 58 + 24;   // hotbar row + bottom padding
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int x = this.leftPos;
        int y = this.topPos;

        // Panel with a beveled border
        graphics.fill(x, y, x + imageWidth, y + imageHeight, DARK_EDGE);
        graphics.fill(x, y, x + imageWidth - 1, y + imageHeight - 1, LIGHT_EDGE);
        graphics.fill(x + 1, y + 1, x + imageWidth - 1, y + imageHeight - 1, PANEL_COLOR);

        // One inset box behind every slot (trade grid and player inventory alike)
        for (Slot slot : this.menu.slots) {
            int sx = x + slot.x - 1;
            int sy = y + slot.y - 1;
            graphics.fill(sx, sy, sx + 18, sy + 18, LIGHT_EDGE);
            graphics.fill(sx, sy, sx + 17, sy + 17, DARK_EDGE);
            graphics.fill(sx + 1, sy + 1, sx + 17, sy + 17, SLOT_COLOR);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        // Title on top, a short hint in the gap between the grid and the inventory.
        // The default "Inventory" label is skipped: that gap is too small for both.
        graphics.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, 0x404040, false);
        graphics.drawString(this.font, Component.translatable("gui.masternpc.trade.hint"),
                8, TradeEditMenu.INV_Y - 11, 0x404040, false);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);   // background, slots, items
        this.renderTooltip(graphics, mouseX, mouseY);          // item tooltips on hover
    }
}
