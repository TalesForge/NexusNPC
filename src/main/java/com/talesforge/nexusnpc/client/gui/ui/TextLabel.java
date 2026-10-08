package com.talesforge.nexusnpc.client.gui.ui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

import java.util.List;

/** Non-interactive wrapped text (hints, empty-state messages, errors). Its height follows the wrapped line count. */
public class TextLabel extends AbstractWidget {
    private final List<FormattedCharSequence> lines;
    private final int color;

    public TextLabel(int x, int y, int width, Component text, int color) {
        super(x, y, width, Minecraft.getInstance().font.split(text, width).size() * 10, text);
        this.lines = Minecraft.getInstance().font.split(text, width);
        this.color = color;
        this.active = false;
    }

    @Override
    protected void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        Font font = Minecraft.getInstance().font;
        int y = getY();
        for (FormattedCharSequence line : lines) {
            g.drawString(font, line, getX(), y, color, false);
            y += 10;
        }
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {}
}
