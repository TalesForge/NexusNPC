package com.talesforge.nexusnpc.client.gui.ui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

/**
 * Base of every NexusNPC editing screen. It owns the whole layout, so no subclass ever positions anything
 * relative to the window and nothing can run off-screen:
 * <ul>
 *   <li>the panel is as wide as the window allows (up to {@link #maxPanelWidth()}) and as tall as its content
 *       (up to the window height) — all re-computed on every resize / GUI-scale change;</li>
 *   <li>a title bar on top, a button row at the bottom, and between them a {@link ScrollPanel} body that
 *       scrolls whenever the content is taller than the space left;</li>
 *   <li>the scroll position survives a rebuild (resize, returning from a picker screen).</li>
 * </ul>
 * Subclasses fill the body with {@link #buildBody} and the button row with {@link #buildFooter}; their state
 * lives in fields, never in widgets, because the widgets are recreated on every layout pass.
 */
public abstract class PanelScreen extends Screen {
    protected static final int MARGIN = 8;
    protected static final int HEADER_H = 24;
    protected static final int FOOTER_H = 32;
    protected static final int PAD = 8;
    private static final int SCROLLBAR_SPACE = 8;

    @Nullable protected final Screen parent;

    protected ScrollPanel body;
    protected int panelX, panelY, panelW, panelH;

    private double scrollMemory = 0;
    private boolean rebuildRequested = false;

    protected PanelScreen(Component title, @Nullable Screen parent) {
        super(title);
        this.parent = parent;
    }

    // ================= what subclasses decide =================

    protected int maxPanelWidth() { return 320; }

    protected int maxPanelHeight() { return 400; }

    /** True: the body takes all the height it may (lists). False: the panel shrinks to its content. */
    protected boolean fillHeight() { return false; }

    /** Extra header space under the title bar (a search box, for example). */
    protected int headerExtra() { return 0; }

    protected void buildHeaderExtra(int x, int y, int width) {}

    protected boolean hasFooter() { return true; }

    /**
     * Add the scrolling content. Y is relative to the top of the content (start at 0), X is absolute.
     *
     * @return the total content height
     */
    protected abstract int buildBody(ScrollPanel body, int x, int width);

    /** Add the bottom button row; (x, y) is its top-left corner, 20 px high. */
    protected void buildFooter(int x, int y, int width) {}

    /** Small text on the right of the title bar ("Saving..."), or null. */
    @Nullable protected Component status() { return null; }

    protected int statusColor() { return UiTheme.TEXT_DIM; }

    // ================= layout =================

    /** Re-run the whole layout on the next tick (safe to call from inside a widget callback). */
    protected void requestRebuild() { rebuildRequested = true; }

    @Override
    protected final void init() {
        if (body != null) scrollMemory = body.getScroll();

        panelW = Math.max(120, Math.min(maxPanelWidth(), width - 2 * MARGIN));
        panelX = (width - panelW) / 2;
        int bodyX = panelX + PAD;
        int bodyW = panelW - 2 * PAD;

        int headerH = HEADER_H + headerExtra();
        int footerH = hasFooter() ? FOOTER_H : PAD;

        body = new ScrollPanel(bodyX, 0, bodyW, 10);
        int contentH = buildBody(body, bodyX, bodyW - SCROLLBAR_SPACE);
        body.setContentHeight(contentH + 2);

        int availableBody = Math.max(40, height - 2 * MARGIN - headerH - footerH);
        int maxBody = Math.max(40, maxPanelHeight() - headerH - footerH);
        int limit = Math.min(availableBody, maxBody);
        int bodyH = fillHeight() ? limit : Math.min(limit, Math.max(contentH + 2, 40));

        panelH = headerH + bodyH + footerH;
        panelY = (height - panelH) / 2;

        body.setY(panelY + headerH);
        body.setHeight(bodyH);
        body.setScroll(scrollMemory);
        addRenderableWidget(body);

        buildHeaderExtra(bodyX, panelY + HEADER_H + 4, bodyW);
        if (hasFooter()) buildFooter(bodyX, panelY + panelH - footerH + 7, bodyW);
    }

    @Override
    public void tick() {
        super.tick();
        if (rebuildRequested) {
            rebuildRequested = false;
            rebuildWidgets();
        }
    }

    // ================= drawing =================

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.renderBackground(g, mouseX, mouseY, partialTick);   // Darkens / blurs the world
        UiTheme.drawPanel(g, panelX, panelY, panelW, panelH);
        UiTheme.drawHeader(g, panelX, panelY, panelW, HEADER_H);

        Component status = status();
        int statusW = status != null ? font.width(status) : 0;
        int titleSpace = panelW - 2 * PAD - statusW - (statusW > 0 ? 8 : 0);
        g.drawString(font, font.plainSubstrByWidth(title.getString(), titleSpace),
                panelX + PAD, panelY + (HEADER_H - 8) / 2, UiTheme.TEXT, false);
        if (status != null) {
            g.drawString(font, status, panelX + panelW - PAD - statusW, panelY + (HEADER_H - 8) / 2, statusColor(), false);
        }
        if (hasFooter()) UiTheme.drawDivider(g, panelX + 1, panelY + panelH - FOOTER_H, panelW - 2);
    }

    // ================= misc =================

    @Override
    public void onClose() {
        Minecraft.getInstance().setScreen(parent);
    }

    /**
     * Not a pause screen on purpose: in single player a paused server does not process the packets this editor
     * sends, which would make "saved immediately" a lie until the screen closes.
     */
    @Override
    public boolean isPauseScreen() { return false; }

    /** A footer button that spans [fraction] of the row, for building two-button footers. */
    protected Button footerButton(Component text, int x, int y, int width, Button.OnPress onPress) {
        return addRenderableWidget(Button.builder(text, onPress).bounds(x, y, width, 20).build());
    }
}
