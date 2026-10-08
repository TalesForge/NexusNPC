package com.talesforge.nexusnpc.client.gui.ui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;

/** Non-interactive section title: accent-coloured text with a thin line after it. */
public class Heading extends AbstractWidget {
    public static final int HEIGHT = 14;

    public Heading(int x, int y, int width, Component text) {
        super(x, y, width, HEIGHT, text);
        this.active = false;
    }

    @Override
    protected void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        Font font = Minecraft.getInstance().font;
        int textW = font.width(getMessage());
        g.drawString(font, getMessage(), getX(), getY() + 2, UiTheme.ACCENT, false);
        int lineX = getX() + textW + 6;
        if (lineX < getX() + getWidth()) {
            g.fill(lineX, getY() + 6, getX() + getWidth(), getY() + 7, UiTheme.DIVIDER);
        }
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {}
}
