package com.talesforge.nexusnpc.client.gui.screen.settings.trade;

import com.talesforge.nexusnpc.client.gui.NpcEditingScreen;
import com.talesforge.nexusnpc.menu.TradeEditMenu;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import org.jetbrains.annotations.Nullable;

/**
 * Left: the trade grid and the player's inventory. Right: a short help panel and "Done".
 * Drawn with plain rectangles, so no texture asset is needed.
 */
public class TradeEditScreen extends AbstractContainerScreen<TradeEditMenu> implements NpcEditingScreen {
    private static final int MAIN_W = 176;
    private static final int GAP = 4;
    private static final int SIDE_W = 108;
    private static final int SIDE_H = 120;

    private static final int PANEL = 0xFFC6C6C6, LIGHT = 0xFFFFFFFF, DARK = 0xFF555555;
    private static final int SLOT = 0xFF8B8B8B, GHOST_SLOT = 0xFF8FA3C4;   // grid slots are bluish

    /** Set by the settings screen right before it asks the server to open the menu. */
    @Nullable
    private static Screen pendingParent;
    public static void rememberParent(@Nullable Screen screen) { pendingParent = screen; }

    @Nullable
    private final Screen parent;

    public TradeEditScreen(TradeEditMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.parent = pendingParent;
        pendingParent = null;
        this.imageWidth = MAIN_W + GAP + SIDE_W;
        this.imageHeight = TradeEditMenu.INV_Y + 58 + 18 + 8;
    }

    @Override
    protected void init() {
        super.init();
        addRenderableWidget(Button.builder(Component.translatable("gui.done"), b -> onClose())
                .bounds(leftPos + MAIN_W + GAP + 6, topPos + SIDE_H - 26, SIDE_W - 12, 20)
                .build());
    }

    /** Done, Esc and the inventory key all end up here: close the menu (the server saves), then go back. */
    @Override
    public void onClose() {
        super.onClose();
        if (parent != null) Minecraft.getInstance().setScreen(parent);
    }

    private static void drawPanel(GuiGraphics g, int x, int y, int w, int h) {
        g.fill(x, y, x + w, y + h, DARK);
        g.fill(x, y, x + w - 1, y + h - 1, LIGHT);
        g.fill(x + 1, y + 1, x + w - 1, y + h - 1, PANEL);
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        drawPanel(g, leftPos, topPos, MAIN_W, imageHeight);
        drawPanel(g, leftPos + MAIN_W + GAP, topPos, SIDE_W, SIDE_H);

        for (Slot slot : this.menu.slots) {
            int sx = leftPos + slot.x - 1;
            int sy = topPos + slot.y - 1;
            g.fill(sx, sy, sx + 18, sy + 18, LIGHT);
            g.fill(sx, sy, sx + 17, sy + 17, DARK);
            g.fill(sx + 1, sy + 1, sx + 17, sy + 17, slot.index < TradeEditMenu.GRID_SLOTS ? GHOST_SLOT : SLOT);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        g.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, 0x404040, false);
        g.drawString(this.font, Component.translatable("gui.nexusnpc.trade.hint"),
                8, TradeEditMenu.INV_Y - 11, 0x404040, false);
        g.drawWordWrap(this.font, Component.translatable("gui.nexusnpc.trade.help"),
                MAIN_W + GAP + 6, 8, SIDE_W - 12, 0x404040);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        this.renderTooltip(g, mouseX, mouseY);
    }
}
