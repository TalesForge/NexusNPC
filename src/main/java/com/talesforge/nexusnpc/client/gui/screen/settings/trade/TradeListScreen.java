package com.talesforge.nexusnpc.client.gui.screen.settings.trade;

import com.talesforge.nexusnpc.client.gui.NpcEditingScreen;
import com.talesforge.nexusnpc.client.gui.ui.PanelScreen;
import com.talesforge.nexusnpc.client.gui.ui.ScrollPanel;
import com.talesforge.nexusnpc.client.gui.ui.TextLabel;
import com.talesforge.nexusnpc.client.gui.ui.UiTheme;
import com.talesforge.nexusnpc.network.payload.action.EditTradePayload;
import com.talesforge.nexusnpc.network.payload.action.TradeListActionPayload;
import com.talesforge.nexusnpc.npc.trade.NpcTrades;
import com.talesforge.nexusnpc.npc.trade.PaymentRule;
import com.talesforge.nexusnpc.npc.trade.TradeOffer;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * The NPC's trades as a list, like the dialogue list: any number of trades, each row showing what the player pays and
 * gets with real item icons, and buttons to move, duplicate and delete. Click a row (or "New trade") to edit one in the
 * trade editor. The trades live on the server: every button asks it to do the change and it sends back the refreshed list.
 */
public class TradeListScreen extends PanelScreen implements NpcEditingScreen {
    private static final int BUTTON = 18;
    private static final int BUTTONS = 4;

    /** Set by the editor right before it asks the server for the list, so closing can return there. */
    @Nullable
    private static Screen pendingParent;
    public static void rememberParent(@Nullable Screen screen) { pendingParent = screen; }

    /** The screen remembered by {@link #rememberParent}, once. */
    @Nullable
    public static Screen takeParent() {
        Screen screen = pendingParent;
        pendingParent = null;
        return screen;
    }

    private final int entityId;
    private NpcTrades trades;
    private final List<TradeRow> rows = new ArrayList<>();

    public TradeListScreen(@Nullable Screen parent, int entityId, NpcTrades trades) {
        super(Component.translatable("gui.nexusnpc.trade.list_title"), parent);
        this.entityId = entityId;
        this.trades = trades;
    }

    public int entityId() { return entityId; }

    /** The server sent the trades as they are now. */
    public void refresh(NpcTrades updated) {
        this.trades = updated;
        rebuildWidgets();
    }

    @Override protected int maxPanelWidth() { return 340; }

    @Override
    protected int buildBody(ScrollPanel body, int x, int width) {
        rows.clear();
        Button add = Button.builder(Component.translatable("gui.nexusnpc.trade.new"), b -> edit(-1))
                .bounds(x, 0, width, 20).build();
        add.active = trades.size() < NpcTrades.MAX;
        body.add(add);
        int y = 26;

        if (trades.offers().isEmpty()) {
            body.add(new TextLabel(x, y + 4, width, Component.translatable("gui.nexusnpc.trade.none"), UiTheme.TEXT_DIM));
            return y + 20;
        }

        int buttonsW = BUTTONS * (BUTTON + 2);
        for (int i = 0; i < trades.size(); i++) {
            final int index = i;
            TradeRow row = new TradeRow(x, y, width - buttonsW - 2, trades.offers().get(i), () -> edit(index));
            rows.add(row);
            body.add(row);

            int bx = x + width - buttonsW;
            int by = y + (TradeRow.HEIGHT - BUTTON) / 2;
            Button up = small("▲", "gui.nexusnpc.trade.up", bx, by, () -> act(TradeListActionPayload.MOVE_UP, index));
            up.active = index > 0;
            Button down = small("▼", "gui.nexusnpc.trade.down", bx + BUTTON + 2, by, () -> act(TradeListActionPayload.MOVE_DOWN, index));
            down.active = index < trades.size() - 1;
            Button copy = small("×2", "gui.nexusnpc.trade.duplicate", bx + (BUTTON + 2) * 2, by, () -> act(TradeListActionPayload.DUPLICATE, index));
            copy.active = trades.size() < NpcTrades.MAX;
            Button delete = small("✕", "gui.nexusnpc.trade.delete", bx + (BUTTON + 2) * 3, by, () -> act(TradeListActionPayload.DELETE, index));
            body.add(up);
            body.add(down);
            body.add(copy);
            body.add(delete);
            y += TradeRow.HEIGHT + 3;
        }
        return y;
    }

    private static Button small(String symbol, String tooltipKey, int x, int y, Runnable action) {
        return Button.builder(Component.literal(symbol), b -> action.run())
                .bounds(x, y, BUTTON, BUTTON)
                .tooltip(Tooltip.create(Component.translatable(tooltipKey)))
                .build();
    }

    @Override
    protected void buildFooter(int x, int y, int width) {
        footerButton(Component.translatable("gui.done"), x, y, width, b -> onClose());
    }

    private void edit(int index) {
        TradeEditScreen.rememberParent(this);
        PacketDistributor.sendToServer(new EditTradePayload(entityId, index));   // The server opens the editor menu
    }

    private void act(int action, int index) {
        PacketDistributor.sendToServer(new TradeListActionPayload(entityId, action, index));   // ...and sends the list back
    }

    // ================= item tooltips =================

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        if (!body.isMouseOver(mouseX, mouseY)) return;
        for (TradeRow row : rows) {
            List<Component> tooltip = row.tooltipAt(mouseX, mouseY);
            if (tooltip != null) {
                g.renderComponentTooltip(font, tooltip, mouseX, mouseY);
                return;
            }
        }
    }

    // ================= one row =================

    /** [pay] + [pay] -> [get] and the name of what is sold. Clicking it edits the trade. */
    private static final class TradeRow extends AbstractWidget {
        static final int HEIGHT = 24;

        private final TradeOffer offer;
        private final Runnable onEdit;

        TradeRow(int x, int y, int width, TradeOffer offer, Runnable onEdit) {
            super(x, y, width, HEIGHT, Component.empty());
            this.offer = offer;
            this.onEdit = onEdit;
        }

        @Override public void onClick(double mouseX, double mouseY) { onEdit.run(); }

        // Icon positions, relative to the row: payment 1, "+", payment 2, arrow, result
        private int pay1X() { return getX() + 4; }
        private int pay2X() { return pay1X() + 24; }
        private int arrowX() { return (offer.hasSecondPayment() ? pay2X() + 18 : pay1X() + 18) + 4; }
        private int resultX() { return arrowX() + 14; }
        private int iconY() { return getY() + 4; }

        @Override
        protected void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
            var font = Minecraft.getInstance().font;
            g.fill(getX(), getY(), getX() + width, getY() + height, isHoveredOrFocused() ? UiTheme.ROW_HOVER : UiTheme.ROW);

            icon(g, font, offer.cost1(), pay1X(), offer.rule1());
            if (offer.hasSecondPayment()) {
                g.drawString(font, "+", pay1X() + 18, iconY() + 4, UiTheme.TEXT_DIM, false);
                icon(g, font, offer.cost2(), pay2X(), offer.rule2());
            }
            drawArrow(g, arrowX(), iconY() + 5);
            icon(g, font, offer.result(), resultX(), PaymentRule.DEFAULT);

            // What is sold, when there is room for its name
            int textX = resultX() + 24;
            int room = getX() + width - 6 - textX;
            if (room > 24) {
                String name = font.plainSubstrByWidth(offer.result().getHoverName().getString(), room);
                g.drawString(font, name, textX, getY() + (height - 8) / 2, UiTheme.TEXT, false);
            }
        }

        /** An item with its count; a payment that accepts any variant is marked with a small accent-coloured star. */
        private void icon(GuiGraphics g, net.minecraft.client.gui.Font font, ItemStack stack, int x, PaymentRule rule) {
            g.renderItem(stack, x, iconY());
            g.renderItemDecorations(font, stack, x, iconY());
            if (rule.anyVariant()) g.drawString(font, "*", x + 11, iconY() - 3, UiTheme.ACCENT, false);
        }

        private static void drawArrow(GuiGraphics g, int x, int y) {
            g.fill(x, y + 3, x + 7, y + 5, UiTheme.ACCENT);
            for (int i = 0; i < 4; i++) {
                int h = 7 - i * 2;
                int top = y + (7 - h) / 2;
                g.fill(x + 6 + i, top, x + 7 + i, top + h, UiTheme.ACCENT);
            }
        }

        /** The tooltip of the icon under the mouse (the item's own, plus what the payment accepts), or null. */
        @Nullable
        List<Component> tooltipAt(int mx, int my) {
            if (!isMouseOver(mx, my)) return null;
            if (hit(mx, my, pay1X())) return describe(offer.cost1(), offer.rule1());
            if (offer.hasSecondPayment() && hit(mx, my, pay2X())) return describe(offer.cost2(), offer.rule2());
            if (hit(mx, my, resultX())) return describe(offer.result(), null);
            return null;
        }

        private boolean hit(int mx, int my, int x) {
            return mx >= x && mx < x + 16 && my >= iconY() && my < iconY() + 16;
        }

        private static List<Component> describe(ItemStack stack, @Nullable PaymentRule rule) {
            List<Component> lines = new ArrayList<>(Screen.getTooltipFromItem(Minecraft.getInstance(), stack));
            if (rule != null) {
                lines.add(Component.translatable(rule.anyVariant() ? "gui.nexusnpc.trade.rule.any.tip" : "gui.nexusnpc.trade.rule.exact.tip")
                        .withStyle(ChatFormatting.AQUA));
                if (rule.perfectOnly()) lines.add(Component.translatable("gui.nexusnpc.trade.rule.perfect.tip").withStyle(ChatFormatting.AQUA));
            }
            return lines;
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput output) {}
    }
}
