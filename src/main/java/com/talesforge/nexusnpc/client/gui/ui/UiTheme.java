package com.talesforge.nexusnpc.client.gui.ui;

import net.minecraft.client.gui.GuiGraphics;

/** One place for every colour and shared drawing helper of the NexusNPC screens, so they look like one family. */
public final class UiTheme {
    private UiTheme() {}

    public static final int PANEL_BG = 0xF0191B22;
    public static final int PANEL_BORDER = 0xFF3E4352;
    public static final int HEADER_BG = 0xFF22252E;
    public static final int DIVIDER = 0xFF343846;
    public static final int ACCENT = 0xFF5B9BFF;

    public static final int TEXT = 0xFFE9ECF3;
    public static final int TEXT_DIM = 0xFF9AA1B2;
    public static final int TEXT_OK = 0xFF72D68F;
    public static final int TEXT_WARN = 0xFFE8C45A;
    public static final int TEXT_BAD = 0xFFEE6B6B;

    public static final int ROW = 0xFF272B35;
    public static final int ROW_HOVER = 0xFF323847;
    public static final int ROW_SELECTED = 0xFF2A4670;
    public static final int ROW_DISABLED = 0xFF1F2229;

    public static final int SCROLL_TRACK = 0x33FFFFFF;
    public static final int SCROLL_THUMB = 0xAAB8BFD0;

    /** Rounded-looking dark panel: 1px border, flat body. */
    public static void drawPanel(GuiGraphics g, int x, int y, int w, int h) {
        g.fill(x, y, x + w, y + h, PANEL_BORDER);
        g.fill(x + 1, y + 1, x + w - 1, y + h - 1, PANEL_BG);
    }

    /** Slightly lighter band used behind the title. */
    public static void drawHeader(GuiGraphics g, int x, int y, int w, int h) {
        g.fill(x + 1, y + 1, x + w - 1, y + h, HEADER_BG);
        g.fill(x + 1, y + h, x + w - 1, y + h + 1, DIVIDER);
    }

    public static void drawDivider(GuiGraphics g, int x, int y, int w) {
        g.fill(x, y, x + w, y + 1, DIVIDER);
    }
}
