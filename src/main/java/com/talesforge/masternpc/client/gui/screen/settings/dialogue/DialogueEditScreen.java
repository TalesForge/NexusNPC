package com.talesforge.masternpc.client.gui.screen.settings.dialogue;

import com.talesforge.masternpc.MasterNPC;
import com.talesforge.masternpc.client.gui.NpcEditingScreen;
import com.talesforge.masternpc.client.gui.screen.CustomScreen;
import com.talesforge.masternpc.client.gui.screen.PagePickerScreen;
import com.talesforge.masternpc.npc.dialogue.DialogueAction;
import com.talesforge.masternpc.npc.dialogue.DialogueOption;
import com.talesforge.masternpc.npc.dialogue.DialoguePage;
import com.talesforge.masternpc.npc.dialogue.NpcDialogue;
import com.talesforge.masternpc.npc.field.NpcDataMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.*;
import java.util.function.Consumer;

/**
 * Edits ONE dialogue page: name, text and up to four answers. Where an answer leads is
 * picked from {@code linkTargets} (by name), not typed.
 * The source of truth is the draft below, not the widgets, so nothing typed is lost when the
 * screen is resized or a picker screen returns here.
 */
public class DialogueEditScreen extends CustomScreen implements NpcEditingScreen {
    private static final int MAX_OPTIONS = 4;

    private enum Kind { GOTO, OPEN_TRADE, ACCEPT_QUEST, TURN_IN_QUEST, CLOSE }

    private static final class OptionDraft {
        String text = "";
        Kind kind = Kind.GOTO;
        String quest = "";  // Quest id as typed
        String pageA = "";  // Goto target / next page after accepting / page on success ("" = the starting dialogue)
        String pageB = "";  // Page on failure (turn-in only)
    }

    private final NpcDialogue dialogue;
    private final Consumer<NpcDialogue> onDone;

    /**
     * The pool of pages a GOTO/accept/turn-in option can point at. For a page that's part of
     * an NPC's own dialogue (DialogueListScreen) this is just {@code dialogue.pages()} — see
     * the 4-arg constructor. A page edited from the standalone LIBRARY instead needs the
     * whole library as its target pool, since {@code dialogue} there is only ever a
     * throwaway single-page wrapper (see DialogueLibraryScreen#edit), not the real set of
     * valid link targets.
     */
    private final List<DialoguePage> linkTargets;

    private final String pageId;
    private String name;
    private String text;
    private final OptionDraft[] options = new OptionDraft[MAX_OPTIONS];

    private EditBox nameBox;
    private MultiLineEditBox textBox;
    private final EditBox[] optionText = new EditBox[MAX_OPTIONS];
    private final EditBox[] questBox = new EditBox[MAX_OPTIONS];

    /** @param editing the page to edit, or null to create a new one. Link targets default to the dialogue's own pages. */
    public DialogueEditScreen(Screen parent, NpcDialogue dialogue, DialoguePage editing, Consumer<NpcDialogue> onDone) {
        this(parent, dialogue, editing, onDone, List.copyOf(dialogue.pages().values()));
    }

    /** @param editing the page to edit, or null to create a new one */
    public DialogueEditScreen(Screen parent, NpcDialogue dialogue, DialoguePage editing,
                              Consumer<NpcDialogue> onDone, List<DialoguePage> linkTargets) {
        super(Component.translatable("gui.masternpc.dialogue.title"), parent, NpcDataMap.empty());
        this.dialogue = dialogue;
        this.onDone = onDone;
        this.linkTargets = linkTargets;
        this.pageId = editing != null ? editing.id() : dialogue.freshId();
        this.name = editing != null ? editing.name() : "";
        this.text = editing != null ? editing.text() : "";
        for (int i = 0; i < MAX_OPTIONS; i++) {
            options[i] = editing != null && i < editing.options().size()
                    ? draftOf(editing.options().get(i)) : new OptionDraft();
        }
    }

    @Override
    protected void init() {
        if (nameBox != null) syncDraft();  // Resized, or came back from a picker: keep what was typed
        Arrays.fill(questBox, null);

        int w = 340;
        int x = width / 2 - w / 2;

        nameBox = new EditBox(font, x, 6, w - 144, 20, Component.translatable("gui.masternpc.dialogue.name"));
        nameBox.setHint(Component.translatable("gui.masternpc.dialogue.name"));
        nameBox.setMaxLength(48);
        nameBox.setValue(name);
        addRenderableWidget(nameBox);

        addRenderableWidget(Button.builder(Component.translatable("gui.cancel"), b -> onClose())
                .bounds(x + w - 140, 6, 68, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.done"), b -> finish())
                .bounds(x + w - 70, 6, 70, 20).build());

        textBox = new MultiLineEditBox(font, x, 30, w, 44,
                Component.translatable("gui.masternpc.dialogue.text"), Component.translatable("gui.masternpc.dialogue.text"));
        textBox.setValue(text);
        addRenderableWidget(textBox);

        for (int i = 0; i < MAX_OPTIONS; i++) buildOption(i, x, 80 + i * 44, w);
    }

    private void buildOption(int i, int x, int y, int w) {
        OptionDraft d = options[i];

        EditBox answer = new EditBox(font, x, y, w, 20, Component.translatable("gui.masternpc.dialogue.option_hint"));
        answer.setHint(Component.translatable("gui.masternpc.dialogue.option_hint"));
        answer.setMaxLength(80);
        answer.setValue(d.text);
        optionText[i] = answer;
        addRenderableWidget(answer);

        int rowB = y + 22;
        addRenderableWidget(CycleButton.<Kind>builder(
                        k -> Component.translatable("gui.masternpc.dialogue.action." + k.name().toLowerCase(Locale.ROOT)))
                .withValues(Kind.values())
                .withInitialValue(d.kind)
                .displayOnlyValue()
                .create(x, rowB, 100, 20, Component.empty(), (button, value) -> {
                    syncDraft();
                    d.kind = value;
                    clearWidgets();
                    init();  // Different action -> different controls on the second row
                }));

        int bx = x + 102;
        int rest = w - 102;
        switch (d.kind) {
            case GOTO -> addPageButton(i, false, "→ ", bx, rowB, rest);
            case ACCEPT_QUEST -> {
                addQuestBox(i, bx, rowB, 84);
                addPageButton(i, false, "→ ", bx + 86, rowB, rest - 86);
            }
            case TURN_IN_QUEST -> {
                addQuestBox(i, bx, rowB, 76);
                int pw = (rest - 78 - 2) / 2;
                addPageButton(i, false, "✓ ", bx + 78, rowB, pw);
                addPageButton(i, true, "✗ ", bx + 78 + pw + 2, rowB, pw);
            }
            default -> {}  // OPEN_TRADE and CLOSE need no parameters
        }
    }

    private void addQuestBox(int i, int bx, int by, int bw) {
        EditBox box = new EditBox(font, bx, by, bw, 20, Component.translatable("gui.masternpc.dialogue.quest_hint"));
        box.setHint(Component.translatable("gui.masternpc.dialogue.quest_hint"));
        box.setMaxLength(64);
        box.setValue(options[i].quest);
        questBox[i] = box;
        addRenderableWidget(box);
    }

    private void addPageButton(int i, boolean second, String prefix, int bx, int by, int bw) {
        OptionDraft d = options[i];
        String full = pageName(second ? d.pageB : d.pageA);
        String shown = font.plainSubstrByWidth(full, bw - 12 - font.width(prefix));
        addRenderableWidget(Button.builder(Component.literal(prefix + shown), b -> pickPage(i, second))
                .bounds(bx, by, bw, 20)
                .tooltip(Tooltip.create(Component.literal(full)))
                .build());
    }

    private void pickPage(int i, boolean second) {
        syncDraft();
        OptionDraft d = options[i];
        String current = second ? d.pageB : d.pageA;
        Minecraft.getInstance().setScreen(new PagePickerScreen(this, names(), current, picked -> {
            if (second) d.pageB = picked; else d.pageA = picked;
        }));
    }

    /** id -> display name of every pickable link target, plus the page being edited (it may not be saved yet). */
    private Map<String, String> names() {
        Map<String, String> map = new LinkedHashMap<>();
        for (DialoguePage p : linkTargets) map.put(p.id(), p.displayName());
        map.put(pageId, name.isBlank() ? pageId : name.trim());
        return map;
    }

    private String pageName(String id) {
        if (id.isBlank()) return Component.translatable("gui.masternpc.dialogue.start_page").getString();
        return names().getOrDefault(id, "?");
    }

    /** Widgets -> draft. Page choices and kinds live only in the draft, so they are not touched here. */
    private void syncDraft() {
        name = nameBox.getValue();
        text = textBox.getValue();
        for (int i = 0; i < MAX_OPTIONS; i++) {
            options[i].text = optionText[i].getValue();
            if (questBox[i] != null) options[i].quest = questBox[i].getValue();
        }
    }

    private void finish() {
        syncDraft();
        List<DialogueOption> built = new ArrayList<>();
        for (OptionDraft d : options) {
            // Add Options
            String t = d.text.trim();
            if (!t.isEmpty()) built.add(new DialogueOption(t, actionOf(d)));
        }
        String finalName = name.trim();
        if (finalName.isEmpty()) {
            // Default Name — counted against linkTargets (the real pool: the NPC's dialogue,
            // or the whole library), not `dialogue`, which in library mode only ever wraps
            // the single page being edited and would always number everything "1".
            int number = linkTargets.size() + (dialogue.page(pageId) == null ? 1 : 0);
            finalName = Component.translatable("gui.masternpc.dialogue.default_name", number).getString();
        }
        onDone.accept(dialogue.withPage(new DialoguePage(pageId, finalName, text, built)));  // Add New / Replace

        Minecraft.getInstance().setScreen(parent);
    }

    /** Esc / Cancel: back to the list without saving this dialogue. */
    @Override
    public void onClose() {
        Minecraft.getInstance().setScreen(parent);
    }

    // ========== Draft <-> Action ==========
    private static OptionDraft draftOf(DialogueOption option) {
        OptionDraft d = new OptionDraft();
        d.text = option.text();
        switch (option.action()) {
            case DialogueAction.Goto g -> { d.kind = Kind.GOTO; d.pageA = g.pageId(); }
            case DialogueAction.OpenTrade t -> d.kind = Kind.OPEN_TRADE;
            case DialogueAction.AcceptQuest a -> {
                d.kind = Kind.ACCEPT_QUEST;
                d.quest = a.questId().toString();
                d.pageA = a.nextPageId();
            }
            case DialogueAction.TurnInQuest t -> {
                d.kind = Kind.TURN_IN_QUEST;
                d.quest = t.questId().toString();
                d.pageA = t.successPageId();
                d.pageB = t.failPageId();
            }
            case DialogueAction.Close c -> d.kind = Kind.CLOSE;
        }
        return d;
    }

    private static DialogueAction actionOf(OptionDraft d) {
        return switch (d.kind) {
            case GOTO -> new DialogueAction.Goto(d.pageA);
            case OPEN_TRADE -> new DialogueAction.OpenTrade();
            case ACCEPT_QUEST -> new DialogueAction.AcceptQuest(questId(d.quest), d.pageA);
            case TURN_IN_QUEST -> new DialogueAction.TurnInQuest(questId(d.quest), d.pageA, d.pageB);
            case CLOSE -> new DialogueAction.Close();
        };
    }

    /** Never throws: "quest1" becomes masternpc:quest1, illegal characters become '_'. */
    private static ResourceLocation questId(String raw) {
        String s = raw.trim().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_.\\-/:]", "_");
        if (s.isEmpty()) s = "quest";
        if (!s.contains(":")) s = MasterNPC.MOD_ID + ":" + s;
        ResourceLocation id = ResourceLocation.tryParse(s);
        return id != null ? id : ResourceLocation.fromNamespaceAndPath(MasterNPC.MOD_ID, "quest");
    }

    @Override public boolean isPauseScreen() { return true; }
}
