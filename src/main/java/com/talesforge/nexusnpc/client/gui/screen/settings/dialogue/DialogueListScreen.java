package com.talesforge.nexusnpc.client.gui.screen.settings.dialogue;

import com.talesforge.nexusnpc.client.dialogue.DialogueLibrary;
import com.talesforge.nexusnpc.client.gui.NpcEditingScreen;
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
 * The NPC's dialogue list: create, open, choose the starting dialogue, delete. Every change is handed to
 * {@code onDone} the moment it happens (the editor session saves it), so closing the screen is not a "save".
 */
public class DialogueListScreen extends PanelScreen implements NpcEditingScreen {
    private static final int ICON = 20;

    private final Consumer<NpcDialogue> onDone;
    private NpcDialogue dialogue;
    private Component status = Component.empty();

    public DialogueListScreen(Screen parent, NpcDialogue initial, Consumer<NpcDialogue> onDone) {
        super(Component.translatable("gui.nexusnpc.dialogue.list_title"), parent);
        this.dialogue = initial;
        this.onDone = onDone;
    }

    private void commit() {
        onDone.accept(dialogue);
    }

    @Override
    protected int buildBody(ScrollPanel body, int x, int width) {
        int half = (width - 4) / 2;
        body.add(Button.builder(Component.translatable("gui.nexusnpc.dialogue.new"), b -> edit(null))
                .bounds(x, 0, half, 20).build());
        body.add(Button.builder(Component.translatable("gui.nexusnpc.dialogue.add_saved"), b -> addFromLibrary())
                .bounds(x + half + 4, 0, width - half - 4, 20).build());
        int y = 26;

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
        int buttonsW = ICON * 3 + 6;
        for (DialoguePage page : pages) {
            boolean isStart = start != null && start.id().equals(page.id());
            boolean inLibrary = libraryIds.contains(page.id());

            Component label = Component.literal((isStart ? "★ " : "") + page.displayName());
            body.add(new ListRow(x, y, width - buttonsW - 2, label, null, null, false, () -> edit(page)));

            int bx = x + width - buttonsW;
            int by = y + (ListRow.HEIGHT - ICON) / 2;

            Button saveButton = Button.builder(Component.literal("↓"), b -> {
                        DialogueLibrary.save(page);
                        requestRebuild();
                    })
                    .bounds(bx, by, ICON, ICON)
                    .tooltip(Tooltip.create(Component.translatable(inLibrary
                            ? "gui.nexusnpc.already_in_library" : "gui.nexusnpc.save_to_library")))
                    .build();
            saveButton.active = !inLibrary;
            body.add(saveButton);

            Button starButton = Button.builder(Component.literal("★"), b -> {
                        dialogue = dialogue.withStart(page.id());
                        status = Component.empty();
                        commit();
                        requestRebuild();
                    })
                    .bounds(bx + ICON + 3, by, ICON, ICON)
                    .tooltip(Tooltip.create(Component.translatable("gui.nexusnpc.dialogue.make_start")))
                    .build();
            starButton.active = !isStart;
            body.add(starButton);

            body.add(Button.builder(Component.literal("✕"), b -> delete(page))
                    .bounds(bx + (ICON + 3) * 2, by, ICON, ICON)
                    .tooltip(Tooltip.create(Component.translatable("gui.nexusnpc.dialogue.delete")))
                    .build());
            y += ListRow.HEIGHT + 3;
        }
        return y;
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
        }));
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
