package com.talesforge.nexusnpc.client.gui.screen.settings.trade;

import com.talesforge.nexusnpc.client.gui.NpcEditingScreen;
import com.talesforge.nexusnpc.client.gui.ui.UiTheme;
import com.talesforge.nexusnpc.menu.TradeEditMenu;
import com.talesforge.nexusnpc.network.payload.screen.OpenTradeEditorPayload;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * The editor of one trade, laid out like the villager trade window the player will see: what the player PAYS on the left
 * (amber slots), an arrow, what the player GETS on the right (green slot). Next to each payment two buttons say what
 * counts as that item: exact data or any variant, and any wear or undamaged only. Hovering an empty slot says what it is.
 * Esc / the Done button returns to the trade list; every change is saved as it is made.
 */
public class TradeEditScreen extends AbstractContainerScreen<TradeEditMenu> implements NpcEditingScreen {
    private static final int PAY_BORDER = 0xFF8A6A3B, PAY_FILL = 0xFF3B2F20;
    private static final int GET_BORDER = 0xFF3F8A57, GET_FILL = 0xFF1F3B2A;

    /** Set by the trade list right before it asks the server to open the menu, so closing can return there. */
    @Nullable
    private static Screen pendingParent;
    public static void rememberParent(@Nullable Screen screen) { pendingParent = screen; }

    @Nullable
    private final Screen parent;
    private final Button[] toggles = new Button[4];

    public TradeEditScreen(TradeEditMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.parent = pendingParent;
        pendingParent = null;
        this.imageWidth = TradeEditMenu.WIDTH;
        this.imageHeight = TradeEditMenu.INV_Y + 58 + 18 + 30;
        this.titleLabelY = 5;
        this.inventoryLabelX = (TradeEditMenu.WIDTH - 176) / 2 + 8;
        this.inventoryLabelY = TradeEditMenu.INV_Y - 11;
    }

    @Override
    protected void init() {
        super.init();
        // payment 1 on the first row, payment 2 on the second: [exact data / any variant] [any wear / undamaged only]
        for (int payment = 0; payment < 2; payment++) {
            int y = topPos + TradeEditMenu.SLOT_Y[payment] - 1;
            toggles[payment * 2] = toggleButton(payment * 2, leftPos + 34, y);
            toggles[payment * 2 + 1] = toggleButton(payment * 2 + 1, leftPos + 96, y);
        }
        addRenderableWidget(Button.builder(Component.translatable("gui.done"), b -> onClose())
                .bounds(leftPos + 8, topPos + imageHeight - 26, imageWidth - 16, 20).build());
        refreshToggles();
    }

    private Button toggleButton(int id, int x, int y) {
        return addRenderableWidget(Button.builder(Component.empty(), b -> {
                    if (minecraft != null && minecraft.gameMode != null) {
                        minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id);
                    }
                })
                .bounds(x, y, 60, 20).build());
    }

    /** Labels and tooltips follow the toggles the server keeps; a payment slot that is empty has nothing to configure. */
    private void refreshToggles() {
        for (int payment = 0; payment < 2; payment++) {
            boolean filled = menu.slots.get(payment).hasItem();
            boolean any = menu.toggle(payment * 2);
            boolean perfect = menu.toggle(payment * 2 + 1);

            Button dataButton = toggles[payment * 2];
            dataButton.setMessage(Component.translatable(any ? "gui.nexusnpc.trade.rule.any" : "gui.nexusnpc.trade.rule.exact"));
            dataButton.setTooltip(Tooltip.create(Component.translatable(any ? "gui.nexusnpc.trade.rule.any.tip" : "gui.nexusnpc.trade.rule.exact.tip")));
            dataButton.active = filled;

            Button wearButton = toggles[payment * 2 + 1];
            wearButton.setMessage(Component.translatable(perfect ? "gui.nexusnpc.trade.rule.perfect" : "gui.nexusnpc.trade.rule.wear_any"));
            wearButton.setTooltip(Tooltip.create(Component.translatable(perfect ? "gui.nexusnpc.trade.rule.perfect.tip" : "gui.nexusnpc.trade.rule.wear_any.tip")));
            wearButton.active = filled;
        }
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        refreshToggles();
    }

    /** Done, Esc and the inventory key all end up here: close the menu, then return to the list and ask it to refresh. */
    @Override
    public void onClose() {
        super.onClose();
        if (parent == null) return;
        Minecraft.getInstance().setScreen(parent);
        if (parent instanceof TradeListScreen list) {
            PacketDistributor.sendToServer(new OpenTradeEditorPayload(list.entityId()));   // The server answers with the trades as saved
        }
    }

    // ================= drawing =================

    private boolean complete() {
        return (menu.slots.get(0).hasItem() || menu.slots.get(1).hasItem()) && menu.slots.get(2).hasItem();
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        UiTheme.drawPanel(g, leftPos, topPos, imageWidth, imageHeight);

        // The arrow between what the player pays and what the player gets
        drawArrow(g, leftPos + 160, topPos + 44);

        for (Slot slot : menu.slots) {
            boolean grid = slot.index < TradeEditMenu.GRID_SLOTS;
            boolean result = slot.index == 2;
            int border = !grid ? UiTheme.PANEL_BORDER : result ? GET_BORDER : PAY_BORDER;
            int fill = !grid ? UiTheme.ROW_DISABLED : result ? GET_FILL : PAY_FILL;
            int sx = leftPos + slot.x - 1;
            int sy = topPos + slot.y - 1;
            g.fill(sx, sy, sx + 18, sy + 18, border);
            g.fill(sx + 1, sy + 1, sx + 17, sy + 17, fill);
        }
    }

    /** A 10x7 arrow pointing right, drawn from rectangles (no texture needed). */
    private static void drawArrow(GuiGraphics g, int x, int y) {
        g.fill(x, y + 3, x + 7, y + 5, UiTheme.ACCENT);
        for (int i = 0; i < 4; i++) {
            int h = 7 - i * 2;
            int top = y + (7 - h) / 2;
            g.fill(x + 6 + i, top, x + 7 + i, top + h, UiTheme.ACCENT);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        g.drawString(font, title, titleLabelX, titleLabelY, UiTheme.TEXT, false);

        // Is this trade being saved? An incomplete one is not, and the player should know why
        Component status = Component.translatable(complete() ? "gui.nexusnpc.trade.status.saved" : "gui.nexusnpc.trade.status.incomplete");
        g.drawString(font, status, imageWidth - 8 - font.width(status), titleLabelY,
                complete() ? UiTheme.TEXT_OK : UiTheme.TEXT_WARN, false);

        // Column headers in the colours of the slots below them
        g.drawCenteredString(font, Component.translatable("gui.nexusnpc.trade.head.pays"), 85, 16, UiTheme.TEXT_WARN);
        g.drawCenteredString(font, Component.translatable("gui.nexusnpc.trade.head.gets"), 189, 16, UiTheme.TEXT_OK);
        g.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, UiTheme.TEXT_DIM, false);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        renderTooltip(g, mouseX, mouseY);

        // An empty slot says what it is for (both payment slots say the same: the player may use either)
        if (hoveredSlot != null && hoveredSlot.index < TradeEditMenu.GRID_SLOTS && !hoveredSlot.hasItem()) {
            boolean result = hoveredSlot.index == 2;
            g.renderComponentTooltip(font, List.of(
                    Component.translatable(result ? "gui.nexusnpc.trade.slot.get" : "gui.nexusnpc.trade.slot.pay")
                            .withStyle(result ? ChatFormatting.GREEN : ChatFormatting.GOLD),
                    Component.translatable("gui.nexusnpc.trade.slot.hint").withStyle(ChatFormatting.GRAY)), mouseX, mouseY);
        }
    }
}
