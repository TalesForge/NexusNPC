package com.talesforge.masternpc.client.gui;

import com.talesforge.masternpc.npc.dialogue.DialoguePage;
import com.talesforge.masternpc.npc.dialogue.NpcDialogue;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/** The NPC's dialogue library: create, open, choose the starting dialogue, delete. Changes apply only on "Done". */
public class DialogueListScreen extends Screen {
    private static final int VISIBLE = 7;

    private final Screen parent;
    private final Consumer<NpcDialogue> onDone;
    private NpcDialogue dialogue;
    private int scroll = 0;
    private Component status = Component.empty();

    public DialogueListScreen(Screen parent, NpcDialogue initial, Consumer<NpcDialogue> onDone) {
        super(Component.translatable("gui.masternpc.dialogue.list_title"));
        this.parent = parent;
        this.dialogue = initial;
        this.onDone = onDone;
    }

    @Override
    protected void init() {
        int w = 300;
        int x = width / 2 - w / 2;

        addRenderableWidget(Button.builder(Component.translatable("gui.masternpc.dialogue.new"), b -> edit(null))
                .bounds(x, 20, w, 20).build());

        List<DialoguePage> pages = new ArrayList<>(dialogue.pages().values());
        scroll = Mth.clamp(scroll, 0, Math.max(0, pages.size() - VISIBLE));
        DialoguePage start = dialogue.startPage();

        int y = 44;
        for (int i = 0; i < VISIBLE; i++) {
            int index = scroll + i;
            if (index >= pages.size()) break;
            DialoguePage page = pages.get(index);
            boolean isStart = start != null && start.id().equals(page.id());

            String label = (isStart ? "★ " : "") + font.plainSubstrByWidth(page.displayName(), w - 90);
            addRenderableWidget(Button.builder(Component.literal(label), b -> edit(page))
                    .bounds(x, y, w - 46, 20).build());

            Button starButton = Button.builder(Component.literal("★"), b -> {
                        dialogue = dialogue.withStart(page.id());
                        status = Component.empty();
                        rebuild();
                    })
                    .bounds(x + w - 44, y, 20, 20)
                    .tooltip(Tooltip.create(Component.translatable("gui.masternpc.dialogue.make_start")))
                    .build();
            starButton.active = !isStart;
            addRenderableWidget(starButton);

            addRenderableWidget(Button.builder(Component.literal("✕"), b -> delete(page))
                    .bounds(x + w - 22, y, 20, 20)
                    .tooltip(Tooltip.create(Component.translatable("gui.masternpc.dialogue.delete")))
                    .build());
            y += 22;
        }

        addRenderableWidget(Button.builder(Component.literal("▲"), b -> scrollBy(-1))
                .bounds(x + w + 4, 44, 20, 20).build());
        addRenderableWidget(Button.builder(Component.literal("▼"), b -> scrollBy(1))
                .bounds(x + w + 4, 44 + (VISIBLE - 1) * 22, 20, 20).build());

        int by = 44 + VISIBLE * 22 + 18;
        addRenderableWidget(Button.builder(Component.translatable("gui.cancel"), b -> onClose())
                .bounds(x, by, w / 2 - 2, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.masternpc.done"), b -> {
            onDone.accept(dialogue);
            Minecraft.getInstance().setScreen(parent);
        }).bounds(x + w / 2 + 2, by, w / 2 - 2, 20).build());
    }

    private void edit(@Nullable DialoguePage page) {
        Minecraft.getInstance().setScreen(new DialogueEditScreen(this, dialogue, page, updated -> {
            this.dialogue = updated;
            this.status = Component.empty();
        }));
    }

    private void delete(DialoguePage page) {
        List<String> users = dialogue.linkedFrom(page.id());
        if (users.isEmpty()) {
            dialogue = dialogue.without(page.id());
            status = Component.empty();
        } else {
            status = Component.translatable("gui.masternpc.dialogue.in_use", String.join(", ", users));
        }
        rebuild();
    }

    private void scrollBy(int delta) {
        int max = Math.max(0, dialogue.pages().size() - VISIBLE);
        int next = Mth.clamp(scroll + delta, 0, max);
        if (next != scroll) {
            scroll = next;
            rebuild();
        }
    }

    private void rebuild() {
        clearWidgets();
        init();
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        scrollBy(scrollY > 0 ? -1 : 1);
        return true;
    }

    /** Esc / Cancel: back to the NPC settings, discarding everything done in this window. */
    @Override
    public void onClose() {
        Minecraft.getInstance().setScreen(parent);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        g.drawCenteredString(font, title, width / 2, 6, 0xFFFFFF);
        if (dialogue.isEmpty()) {
            g.drawCenteredString(font, Component.translatable("gui.masternpc.dialogue.none"), width / 2, 90, 0xAAAAAA);
        }
        if (!status.getString().isEmpty()) {
            g.drawCenteredString(font, status, width / 2, 44 + VISIBLE * 22 + 4, 0xFF5555);
        }
    }

    @Override public boolean isPauseScreen() { return true; }
}
