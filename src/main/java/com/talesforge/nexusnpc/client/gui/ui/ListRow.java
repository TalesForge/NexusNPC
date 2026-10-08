package com.talesforge.nexusnpc.client.gui.ui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

/** A clickable list entry: optional icon, a label, an optional second line, and a tick when selected. */
public class ListRow extends AbstractWidget {
    /** Draws a square icon of the given size at (x, y). */
    @FunctionalInterface
    public interface Icon {
        void draw(GuiGraphics g, int x, int y, int size);
    }

    public static final int HEIGHT = 22;
    public static final int HEIGHT_WITH_SUBTITLE = 30;

    private final Runnable onClick;
    @Nullable private final Component subtitle;
    @Nullable private final Icon icon;
    private final boolean selected;

    public ListRow(int x, int y, int width, Component label, @Nullable Component subtitle,
                   @Nullable Icon icon, boolean selected, Runnable onClick) {
        super(x, y, width, subtitle != null ? HEIGHT_WITH_SUBTITLE : HEIGHT, label);
        this.subtitle = subtitle;
        this.icon = icon;
        this.selected = selected;
        this.onClick = onClick;
    }

    @Override
    public void onClick(double mouseX, double mouseY) {
        onClick.run();
    }

    @Override
    protected void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        Font font = Minecraft.getInstance().font;
        int bg = !active ? UiTheme.ROW_DISABLED
                : selected ? UiTheme.ROW_SELECTED
                : isHoveredOrFocused() ? UiTheme.ROW_HOVER : UiTheme.ROW;
        g.fill(getX(), getY(), getX() + getWidth(), getY() + getHeight(), bg);
        if (selected) g.fill(getX(), getY(), getX() + 2, getY() + getHeight(), UiTheme.ACCENT);

        int textX = getX() + 6;
        if (icon != null) {
            int size = getHeight() - 6;
            icon.draw(g, getX() + 4, getY() + 3, size);
            textX = getX() + 4 + size + 6;
        }
        int right = getX() + getWidth() - (selected ? 16 : 6);
        int color = active ? UiTheme.TEXT : UiTheme.TEXT_DIM;

        String label = font.plainSubstrByWidth(getMessage().getString(), right - textX);
        if (subtitle != null) {
            g.drawString(font, label, textX, getY() + 5, color, false);
            String sub = font.plainSubstrByWidth(subtitle.getString(), right - textX);
            g.drawString(font, sub, textX, getY() + 17, UiTheme.TEXT_DIM, false);
        } else {
            g.drawString(font, label, textX, getY() + (getHeight() - 8) / 2, color, false);
        }
        if (selected) {
            g.drawString(font, "✔", getX() + getWidth() - 14, getY() + (getHeight() - 8) / 2, UiTheme.TEXT_OK, false);
        }
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {}
}
