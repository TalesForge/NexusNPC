package com.talesforge.nexusnpc.client.gui.screen.settings.dialogue;

import com.talesforge.nexusnpc.client.dialogue.DialogueLibrary;
import com.talesforge.nexusnpc.client.gui.NpcEditingScreen;
import com.talesforge.nexusnpc.client.gui.screen.CustomScreen;
import com.talesforge.nexusnpc.npc.dialogue.DialoguePage;
import com.talesforge.nexusnpc.npc.dialogue.NpcDialogue;
import com.talesforge.nexusnpc.npc.field.NpcDataMap;
import com.talesforge.nexusnpc.npc.field.NpcSettingFields;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Consumer;

/**
 * Browse saved dialogue pages and add one to the NPC's current list. A pure picker: pages
 * only get INTO the library via DialogueListScreen's "Save" action on a page that's already
 * part of some NPC's dialogue.
 */
public class DialogueLibraryScreen extends CustomScreen implements NpcEditingScreen {
    private static final int LIST_VISIBLE = 7;
    private static final int LIST_START_Y = 50 + 10;
    private static final int LIST_Y_STEP = 22;
    private static final int BUTTON_HEIGHT = 20;

    private final boolean selecting;
    private final Consumer<DialoguePage> onPick;
    private List<DialoguePage> pages;
    private int scroll = 0;

    public DialogueLibraryScreen(Screen parent, boolean selecting, Consumer<DialoguePage> onPick) {
        super(Component.translatable("gui.nexusnpc.dialogue.library_title"), parent, NpcDataMap.empty());
        this.selecting = selecting;
        this.onPick = onPick;
        this.pages = DialogueLibrary.list();
    }

    @Override
    protected void init() {
        super.init();

        int w = 300;
        int x = width / 2 - w / 2;
        int y = 24;

        // Up Buttons
        if (!selecting) {
            addRenderableWidget(Button.builder(Component.translatable("gui.nexusnpc.dialogue.new"), b -> edit(null))
                    .bounds(x, y, w, BUTTON_HEIGHT).build());
        }

        // List
        int maxScroll = Math.max(0, pages.size() - LIST_VISIBLE);
        scroll = Mth.clamp(scroll, 0, maxScroll);
        y = LIST_START_Y;
        int iconW = BUTTON_HEIGHT + 2;
        for (int i = 0; i < LIST_VISIBLE; i++) {
            int index = scroll + i;
            if (index >= pages.size()) break;
            DialoguePage page = pages.get(index);

            Button nameButton = Button.builder(
                            Component.literal(font.plainSubstrByWidth(page.displayName(), w - iconW)),
                            b -> {
                                if (selecting) onPick.accept(page);
                                else edit(page);
                                rebuild();
                            })
                    .bounds(x, y, w - iconW, BUTTON_HEIGHT).build();
            if (selecting && parent instanceof DialogueListScreen screen) {
                nameButton.active = !screen.pageInList(page);
            }
            addRenderableWidget(nameButton);

            if (!selecting) {
                addRenderableWidget(Button.builder(Component.literal("✕"), b -> {
                            DialogueLibrary.delete(page);
                            pages = DialogueLibrary.list();
                            rebuild();
                        })
                        .bounds(x + w - iconW, y, BUTTON_HEIGHT, BUTTON_HEIGHT)
                        .tooltip(Tooltip.create(Component.translatable("gui.nexusnpc.delete")))
                        .build());
            }
            y += LIST_Y_STEP;
        }

        // List Scroll Buttons
        Button upButton = Button.builder(Component.literal("▲"), b -> scrollBy(-1))
                .bounds(x + w + 4, LIST_START_Y, 20, BUTTON_HEIGHT).build();
        upButton.active = scroll > 0;
        addRenderableWidget(upButton);
        Button downButton = Button.builder(Component.literal("▼"), b -> scrollBy(1))
                .bounds(x + w + 4, LIST_START_Y + (LIST_VISIBLE - 1) * LIST_Y_STEP, 20, BUTTON_HEIGHT).build();
        downButton.active = pages.size() > LIST_VISIBLE && scroll < maxScroll;
        addRenderableWidget(downButton);

        // Close
        addRenderableWidget(Button.builder(Component.translatable(selecting ? "gui.back" : "gui.done"), b -> onClose())
                .bounds(x, 24 + LIST_VISIBLE * LIST_Y_STEP + 12, w, BUTTON_HEIGHT).build());
    }

    private void scrollBy(int delta) {
        int next = Mth.clamp(scroll + delta, 0, Math.max(0, pages.size() - LIST_VISIBLE));
        if (next != scroll) { scroll = next; rebuild(); }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        scrollBy(scrollY > 0 ? -1 : 1);
        return true;
    }

    /**
     * Unlike DialogueListScreen#edit, {@code dialogue} here is just a throwaway wrapper
     * around the single page being edited — a library page isn't part of any NpcDialogue.
     * So the "where does this answer lead" picker can't draw its options from {@code dialogue}
     * (that would only ever offer this one page, or nothing): it needs the FULL library
     * instead, passed as linkTargets.
     */
    private void edit(@Nullable DialoguePage page) {
        NpcDialogue dialogue = NpcSettingFields.DIALOGUE.defaultValue();
        if (page != null) dialogue = dialogue.withPage(page).withStart(page.id());

        openChild(new DialogueEditScreen(this, dialogue, page, updated -> {
            DialoguePage result = page != null ? updated.page(page.id())
                    : updated.pages().values().stream().findFirst().orElse(null);
            if (result != null) {
                DialogueLibrary.save(result);
                pages = DialogueLibrary.list();
            }
            rebuild();
        }, DialogueLibrary.list()));
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        g.drawCenteredString(font, title, width / 2, 6, 0xFFFFFF);
        if (pages.isEmpty()) {
            g.drawCenteredString(font, Component.translatable("gui.nexusnpc.dialogue.library_empty"), width / 2, 90, 0xAAAAAA);
        }
    }

    @Override public boolean isPauseScreen() { return true; }
}
