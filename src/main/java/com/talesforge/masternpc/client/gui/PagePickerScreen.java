package com.talesforge.masternpc.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/** "Where does this answer lead?" — pick one of the NPC's dialogues by name. */
public class PagePickerScreen extends Screen {
    private static final int VISIBLE = 8;

    private final Screen parent;
    private final List<Map.Entry<String, String>> rows = new ArrayList<>();   // id -> name
    private final String current;
    private final Consumer<String> onPick;
    private int scroll = 0;

    /** @param pages id -> display name. An extra first entry with id "" means "the starting dialogue". */
    public PagePickerScreen(Screen parent, Map<String, String> pages, String current, Consumer<String> onPick) {
        super(Component.translatable("gui.masternpc.dialogue.pick_title"));
        this.parent = parent;
        this.current = current;
        this.onPick = onPick;
        rows.add(Map.entry("", Component.translatable("gui.masternpc.dialogue.start_page").getString()));
        rows.addAll(pages.entrySet());
    }

    @Override
    protected void init() {
        int w = 260;
        int x = width / 2 - w / 2;
        scroll = Mth.clamp(scroll, 0, Math.max(0, rows.size() - VISIBLE));

        int y = 26;
        for (int i = 0; i < VISIBLE; i++) {
            int index = scroll + i;
            if (index >= rows.size()) break;
            Map.Entry<String, String> row = rows.get(index);
            String label = (row.getKey().equals(current) ? "✔ " : "") + font.plainSubstrByWidth(row.getValue(), w - 30);
            addRenderableWidget(Button.builder(Component.literal(label), b -> {
                onPick.accept(row.getKey());
                Minecraft.getInstance().setScreen(parent);
            }).bounds(x, y, w, 20).build());
            y += 22;
        }

        addRenderableWidget(Button.builder(Component.literal("▲"), b -> scrollBy(-1))
                .bounds(x + w + 4, 26, 20, 20).build());
        addRenderableWidget(Button.builder(Component.literal("▼"), b -> scrollBy(1))
                .bounds(x + w + 4, 26 + (VISIBLE - 1) * 22, 20, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.cancel"), b -> onClose())
                .bounds(x, 26 + VISIBLE * 22 + 6, w, 20).build());
    }

    private void scrollBy(int delta) {
        int next = Mth.clamp(scroll + delta, 0, Math.max(0, rows.size() - VISIBLE));
        if (next != scroll) {
            scroll = next;
            clearWidgets();
            init();
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        scrollBy(scrollY > 0 ? -1 : 1);
        return true;
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().setScreen(parent);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        g.drawCenteredString(font, title, width / 2, 8, 0xFFFFFF);
    }

    @Override public boolean isPauseScreen() { return true; }
}
