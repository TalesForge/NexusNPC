package com.talesforge.nexusnpc.client.gui.screen.settings.dialogue;

import com.talesforge.nexusnpc.client.dialogue.DialogueLibrary;
import com.talesforge.nexusnpc.client.gui.NpcEditingScreen;
import com.talesforge.nexusnpc.client.gui.screen.DialogueScreen;
import com.talesforge.nexusnpc.client.gui.ui.ListRow;
import com.talesforge.nexusnpc.client.gui.ui.PanelScreen;
import com.talesforge.nexusnpc.client.gui.ui.ScrollPanel;
import com.talesforge.nexusnpc.client.gui.ui.TextLabel;
import com.talesforge.nexusnpc.client.gui.ui.UiTheme;
import com.talesforge.nexusnpc.npc.dialogue.DialoguePage;
import com.talesforge.nexusnpc.npc.dialogue.NpcDialogue;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * The NPC's dialogue list: create, open, duplicate, choose the starting dialogue, delete, preview the whole
 * conversation. Each row shows the beginning of its text, how many answers it has, and a red warning if one of its
 * answers leads to a page that no longer exists. Every change is handed to {@code onDone} the moment it happens (the
 * editor session saves it), so closing the screen is not a "save".
 */
public class DialogueListScreen extends PanelScreen implements NpcEditingScreen {
    private static final int ICON = 20;
    private static final int BUTTONS = 4;

    private final Consumer<NpcDialogue> onDone;
    private final int previewEntityId;
    private NpcDialogue dialogue;
    private Component status = Component.empty();

    public DialogueListScreen(Screen parent, NpcDialogue initial, Consumer<NpcDialogue> onDone) {
        this(parent, initial, onDone, -1);
    }

    /** @param previewEntityId the NPC whose portrait and name the preview shows, or -1 */
    public DialogueListScreen(Screen parent, NpcDialogue initial, Consumer<NpcDialogue> onDone, int previewEntityId) {
        super(Component.translatable("gui.nexusnpc.dialogue.list_title"), parent);
        this.dialogue = initial;
        this.onDone = onDone;
        this.previewEntityId = previewEntityId;
    }

    private void commit() {
        onDone.accept(dialogue);
    }

    @Override
    protected int maxPanelWidth() { return 340; }

    @Override
    protected int buildBody(ScrollPanel body, int x, int width) {
        int half = (width - 4) / 2;
        body.add(Button.builder(Component.translatable("gui.nexusnpc.dialogue.new"), b -> edit(null))
                .bounds(x, 0, half, 20).build());
        body.add(Button.builder(Component.translatable("gui.nexusnpc.dialogue.add_saved"), b -> addFromLibrary())
                .bounds(x + half + 4, 0, width - half - 4, 20).build());

        Button preview = Button.builder(Component.translatable("gui.nexusnpc.dialogue.preview_all"), b -> previewAll())
                .bounds(x, 24, width, 20)
                .tooltip(Tooltip.create(Component.translatable("gui.nexusnpc.dialogue.preview.tooltip")))
                .build();
        preview.active = dialogue.startPage() != null;
        body.add(preview);
        int y = 52;

        if (!status.getString().isEmpty()) {
            TextLabel error = body.add(new TextLabel(x, y, width, status, UiTheme.TEXT_BAD));
            y += error.getHeight() + 4;
        }

        List<DialoguePage> pages = new ArrayList<>(dialogue.pages().values());
        if (pages.isEmpty()) {
            body.add(new TextLabel(x, y + 4, width, Component.translatable("gui.nexusnpc.dialogue.none"), UiTheme.TEXT_DIM));
            return y + 20;
        }

        DialoguePage start = dialogue.startPage();
        Set<String> libraryIds = DialogueLibrary.list().stream().map(DialoguePage::id).collect(Collectors.toSet());
        int buttonsW = ICON * BUTTONS + 3 * (BUTTONS - 1);
        for (DialoguePage page : pages) {
            boolean isStart = start != null && start.id().equals(page.id());
            boolean inLibrary = libraryIds.contains(page.id());
            boolean broken = dialogue.hasBrokenLink(page);

            Component label = Component.literal((isStart ? "★ " : "") + page.displayName());
            Component subtitle = broken
                    ? Component.translatable("gui.nexusnpc.dialogue.broken_link")
                    : Component.literal(summary(page));
            ListRow row = new ListRow(x, y, width - buttonsW - 3, label, subtitle, null, false, () -> edit(page));
            if (broken) row.subtitleColor(UiTheme.TEXT_BAD);
            body.add(row);

            int bx = x + width - buttonsW;
            int by = y + (row.getHeight() - ICON) / 2;

            Button starButton = Button.builder(Component.literal("★"), b -> {
                        dialogue = dialogue.withStart(page.id());
                        status = Component.empty();
                        commit();
                        requestRebuild();
                    })
                    .bounds(bx, by, ICON, ICON)
                    .tooltip(Tooltip.create(Component.translatable("gui.nexusnpc.dialogue.make_start")))
                    .build();
            starButton.active = !isStart;
            body.add(starButton);

            body.add(Button.builder(Component.literal("×2"), b -> duplicate(page))
                    .bounds(bx + (ICON + 3), by, ICON, ICON)
                    .tooltip(Tooltip.create(Component.translatable("gui.nexusnpc.dialogue.duplicate")))
                    .build());

            Button saveButton = Button.builder(Component.literal("↓"), b -> {
                        DialogueLibrary.save(page);
                        requestRebuild();
                    })
                    .bounds(bx + (ICON + 3) * 2, by, ICON, ICON)
                    .tooltip(Tooltip.create(Component.translatable(inLibrary
                            ? "gui.nexusnpc.already_in_library" : "gui.nexusnpc.save_to_library")))
                    .build();
            saveButton.active = !inLibrary;
            body.add(saveButton);

            body.add(Button.builder(Component.literal("✕"), b -> delete(page))
                    .bounds(bx + (ICON + 3) * 3, by, ICON, ICON)
                    .tooltip(Tooltip.create(Component.translatable("gui.nexusnpc.dialogue.delete")))
                    .build());
            y += row.getHeight() + 3;
        }
        return y;
    }

    /** "Hello, traveller, I have a job for you... · 3 answers" */
    private static String summary(DialoguePage page) {
        String text = page.text().replace('\n', ' ').trim();
        if (text.isEmpty()) text = "—";
        return text + "  ·  " + Component.translatable("gui.nexusnpc.dialogue.answer_count", page.options().size()).getString();
    }

    @Override
    protected void buildFooter(int x, int y, int width) {
        footerButton(Component.translatable("gui.done"), x, y, width, b -> onClose());
    }

    @Override
    public void onClose() {
        commit();
        super.onClose();
    }

    // ================= actions =================

    private void previewAll() {
        DialoguePage start = dialogue.startPage();
        if (start == null) return;
        Minecraft.getInstance().setScreen(DialogueScreen.preview(this, previewEntityId, dialogue, start));
    }

    private void addFromLibrary() {
        Minecraft.getInstance().setScreen(new DialogueLibraryScreen(this, true, picked -> {
            if (!dialogue.contains(picked)) {
                dialogue = dialogue.withPage(picked);
            }
            status = Component.empty();
            commit();
        }));
    }

    private void edit(@Nullable DialoguePage page) {
        Minecraft.getInstance().setScreen(new DialogueEditScreen(this, dialogue, page, updated -> {
            this.dialogue = updated;
            this.status = Component.empty();
            commit();   // Saved right away, even if the player never presses Done
        }, null, previewEntityId));
    }

    private void duplicate(DialoguePage page) {
        String copyName = page.displayName() + " " + Component.translatable("gui.nexusnpc.dialogue.copy_suffix").getString();
        dialogue = dialogue.withPage(new DialoguePage(dialogue.freshId(), copyName, page.text(), page.options()));
        status = Component.empty();
        commit();
        requestRebuild();
    }

    private void delete(DialoguePage page) {
        List<String> users = dialogue.linkedFrom(page.id());
        if (users.isEmpty()) {
            dialogue = dialogue.without(page.id());
            status = Component.empty();
            commit();
        } else {
            status = Component.translatable("gui.nexusnpc.dialogue.in_use", String.join(", ", users));
        }
        requestRebuild();
    }

    public boolean pageInList(DialoguePage page) {
        return dialogue.contains(page);
    }
}
