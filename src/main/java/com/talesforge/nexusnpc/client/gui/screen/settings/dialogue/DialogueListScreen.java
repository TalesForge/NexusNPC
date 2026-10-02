package com.talesforge.nexusnpc.client.gui.screen.settings.dialogue;

import com.talesforge.nexusnpc.client.dialogue.DialogueLibrary;
import com.talesforge.nexusnpc.client.gui.NpcEditingScreen;
import com.talesforge.nexusnpc.client.gui.screen.CustomScreen;
import com.talesforge.nexusnpc.npc.dialogue.DialoguePage;
import com.talesforge.nexusnpc.npc.dialogue.NpcDialogue;
import com.talesforge.nexusnpc.npc.field.NpcDataMap;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/** The NPC's dialogue library: create, open, choose the starting dialogue, delete. Changes apply only on "Done". */
public class DialogueListScreen extends CustomScreen implements NpcEditingScreen {
    private static final int LIST_VISIBLE = 7;  // The number of dialogues that can be seen in the list at once.
    private static final int LIST_START_Y = 50 + 10;
    private static final int LIST_Y_STEP = 22;

    private static final int BUTTON_HEIGHT = 20;

    private final Consumer<NpcDialogue> onDone;
    private NpcDialogue dialogue;
    private int scroll = 0;
    private Component status = Component.empty();

    public DialogueListScreen(Screen parent, NpcDialogue initial, Consumer<NpcDialogue> onDone) {
        super(Component.translatable("gui.nexusnpc.dialogue.list_title"), parent, NpcDataMap.empty());
        this.dialogue = initial;
        this.onDone = onDone;
    }

    @Override
    protected void init() {
        int w = 300;
        int x = width / 2 - w / 2;
        int y = 24;

        // Up Buttons
        addRenderableWidget(Button.builder(Component.translatable("gui.nexusnpc.dialogue.new"), b -> edit(null))
                .bounds(x, y, w / 2 - 2, BUTTON_HEIGHT).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.nexusnpc.dialogue.add_saved"), b -> addFromLibrary())
                .bounds(x + w / 2 + 2, y, w / 2 - 2, BUTTON_HEIGHT).build());

        // Dialogue List
        List<DialoguePage> pages = new ArrayList<>(dialogue.pages().values());
        int maxScroll = Math.max(0, pages.size() - LIST_VISIBLE);
        scroll = Mth.clamp(scroll, 0, maxScroll);
        DialoguePage start = dialogue.startPage();
        Set<String> libraryIds = DialogueLibrary.list().stream().map(DialoguePage::id).collect(Collectors.toSet());
        y = LIST_START_Y;
        int iconW = BUTTON_HEIGHT + 2;
        for (int i = 0; i < LIST_VISIBLE; i++) {
            int index = scroll + i;
            if (index >= pages.size()) break;
            DialoguePage page = pages.get(index);
            boolean isStart = start != null && start.id().equals(page.id());
            boolean inLibrary = libraryIds.contains(page.id());

            String label = (isStart ? "★ " : "") + font.plainSubstrByWidth(page.displayName(), w - 2 - iconW * 3);
            addRenderableWidget(Button.builder(Component.literal(label), b -> edit(page))
                    .bounds(x, y, w - 2 - iconW * 3, BUTTON_HEIGHT).build());

            Button saveButton = Button.builder(Component.literal("\uD83D\uDCBE"), b -> {
                        DialogueLibrary.save(page);
                        rebuild();
                    })
                    .bounds(x + w - iconW * 3, y, BUTTON_HEIGHT, BUTTON_HEIGHT)
                    .tooltip(Tooltip.create(Component.translatable(inLibrary
                            ? "gui.nexusnpc.already_in_library" : "gui.nexusnpc.save_to_library")))
                    .build();
            saveButton.active = !inLibrary;
            addRenderableWidget(saveButton);

            Button starButton = Button.builder(Component.literal("★"), b -> {
                        dialogue = dialogue.withStart(page.id());
                        status = Component.empty();
                        rebuild();
                    })
                    .bounds(x + w - iconW * 2, y, BUTTON_HEIGHT, BUTTON_HEIGHT)
                    .tooltip(Tooltip.create(Component.translatable("gui.nexusnpc.dialogue.make_start")))
                    .build();
            starButton.active = !isStart;
            addRenderableWidget(starButton);

            addRenderableWidget(Button.builder(Component.literal("✕"), b -> delete(page))
                    .bounds(x + w - iconW, y, BUTTON_HEIGHT, BUTTON_HEIGHT)
                    .tooltip(Tooltip.create(Component.translatable("gui.nexusnpc.dialogue.delete")))
                    .build());
            y += LIST_Y_STEP;
        }

        // List Scroll Buttons
        Button upButton = Button.builder(Component.literal("▲"), b -> scrollBy(-1))
                .bounds(x + w + 4, LIST_START_Y, 20, BUTTON_HEIGHT).build();
        upButton.active = scroll > 0;
        addRenderableWidget(upButton);
        Button downButton = Button.builder(Component.literal("▼"), b -> scrollBy(1))
                .bounds(x + w + 4, LIST_START_Y + (LIST_VISIBLE - 1) * LIST_Y_STEP, 20, BUTTON_HEIGHT).build();
        downButton.active = dialogue.pages().size() > LIST_VISIBLE && scroll < maxScroll;
        addRenderableWidget(downButton);

        // Down Buttons
        int by = LIST_START_Y + LIST_VISIBLE * LIST_Y_STEP + 18;
        addRenderableWidget(Button.builder(Component.translatable("gui.done"), b -> onClose())
                .bounds(x, by, w, BUTTON_HEIGHT).build());
    }

    @Override
    protected void rebuild() {
        super.rebuild();
        onDone.accept(dialogue);
    }

    private void addFromLibrary() {
        openChild(new DialogueLibraryScreen(this, true, picked -> {
            if (!dialogue.contains(picked)) {
                dialogue = dialogue.withPage(picked);
            }
            status = Component.empty();
        }));
    }

    private void edit(@Nullable DialoguePage page) {
        openChild(new DialogueEditScreen(this, dialogue, page, updated -> {
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
            status = Component.translatable("gui.nexusnpc.dialogue.in_use", String.join(", ", users));
        }
        rebuild();
    }

    private void scrollBy(int delta) {
        int max = Math.max(0, dialogue.pages().size() - LIST_VISIBLE);
        int next = Mth.clamp(scroll + delta, 0, max);
        if (next != scroll) {
            scroll = next;
            rebuild();
        }
    }

    protected boolean pageInList(DialoguePage page) {
        return dialogue.contains(page);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        scrollBy(scrollY > 0 ? -1 : 1);
        return true;
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        g.drawCenteredString(font, title, width / 2, 6, 0xFFFFFF);
        if (dialogue.isEmpty()) {
            g.drawCenteredString(font, Component.translatable("gui.nexusnpc.dialogue.none"), width / 2, 90, 0xAAAAAA);
        }
        if (!status.getString().isEmpty()) {
            g.drawCenteredString(font, status, width / 2, LIST_START_Y + LIST_VISIBLE * 22 + 4, 0xFF5555);
        }
    }

    @Override public boolean isPauseScreen() { return true; }
}
