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
import com.talesforge.nexusnpc.npc.field.NpcSettingFields;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Consumer;

/**
 * The saved-dialogue library. Opened from the settings screen it is a manager (create / edit / delete); opened from
 * an NPC's dialogue list ({@code selecting}) it is a picker for adding one page to that NPC.
 */
public class DialogueLibraryScreen extends PanelScreen implements NpcEditingScreen {
    private static final int ICON = 20;

    private final boolean selecting;
    private final Consumer<DialoguePage> onPick;
    private List<DialoguePage> pages;

    public DialogueLibraryScreen(Screen parent, boolean selecting, Consumer<DialoguePage> onPick) {
        super(Component.translatable("gui.nexusnpc.dialogue.library_title"), parent);
        this.selecting = selecting;
        this.onPick = onPick;
        this.pages = DialogueLibrary.list();
    }

    @Override
    protected int buildBody(ScrollPanel body, int x, int width) {
        int y = 0;
        if (!selecting) {
            body.add(Button.builder(Component.translatable("gui.nexusnpc.dialogue.new"), b -> edit(null))
                    .bounds(x, y, width, 20).build());
            y += 26;
        }

        if (pages.isEmpty()) {
            body.add(new TextLabel(x, y + 4, width, Component.translatable("gui.nexusnpc.dialogue.library_empty"), UiTheme.TEXT_DIM));
            return y + 20;
        }

        int rowW = selecting ? width : width - ICON - 3;
        for (DialoguePage page : pages) {
            ListRow row = new ListRow(x, y, rowW, Component.literal(page.displayName()), null, null, false, () -> {
                if (selecting) {
                    onPick.accept(page);
                    Minecraft.getInstance().setScreen(parent);
                } else {
                    edit(page);
                }
            });
            // Already on this NPC: shown, but greyed out and not clickable
            if (selecting && parent instanceof DialogueListScreen screen) row.active = !screen.pageInList(page);
            body.add(row);

            if (!selecting) {
                body.add(Button.builder(Component.literal("✕"), b -> {
                            DialogueLibrary.delete(page);
                            pages = DialogueLibrary.list();
                            requestRebuild();
                        })
                        .bounds(x + width - ICON, y + (ListRow.HEIGHT - ICON) / 2, ICON, ICON)
                        .tooltip(Tooltip.create(Component.translatable("gui.nexusnpc.delete")))
                        .build());
            }
            y += ListRow.HEIGHT + 3;
        }
        return y;
    }

    @Override
    protected void buildFooter(int x, int y, int width) {
        footerButton(Component.translatable(selecting ? "gui.back" : "gui.done"), x, y, width, b -> onClose());
    }

    /**
     * Unlike DialogueListScreen#edit, {@code dialogue} here is just a throwaway wrapper around the single page being
     * edited — a library page is not part of any NpcDialogue. So the "where does this answer lead" picker cannot draw
     * its options from {@code dialogue}: it needs the FULL library instead, passed as linkTargets.
     */
    private void edit(@Nullable DialoguePage page) {
        NpcDialogue dialogue = NpcSettingFields.DIALOGUE.defaultValue();
        if (page != null) dialogue = dialogue.withPage(page).withStart(page.id());

        Minecraft.getInstance().setScreen(new DialogueEditScreen(this, dialogue, page, updated -> {
            DialoguePage result = page != null ? updated.page(page.id())
                    : updated.pages().values().stream().findFirst().orElse(null);
            if (result != null) {
                DialogueLibrary.save(result);
                pages = DialogueLibrary.list();
            }
        }, DialogueLibrary.list()));
    }
}
