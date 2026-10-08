package com.talesforge.nexusnpc.client.gui.screen.settings.dialogue;

import com.talesforge.nexusnpc.NexusNPC;
import com.talesforge.nexusnpc.client.gui.NpcEditingScreen;
import com.talesforge.nexusnpc.client.gui.screen.PagePickerScreen;
import com.talesforge.nexusnpc.client.gui.ui.Heading;
import com.talesforge.nexusnpc.client.gui.ui.PanelScreen;
import com.talesforge.nexusnpc.client.gui.ui.PickerScreen;
import com.talesforge.nexusnpc.client.gui.ui.ScrollPanel;
import com.talesforge.nexusnpc.npc.dialogue.DialogueAction;
import com.talesforge.nexusnpc.npc.dialogue.DialogueOption;
import com.talesforge.nexusnpc.npc.dialogue.DialoguePage;
import com.talesforge.nexusnpc.npc.dialogue.NpcDialogue;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.MultiLineEditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Consumer;

/**
 * Edits ONE dialogue page: name, text and up to four answers. Where an answer leads, and what it does, are chosen
 * on picker screens, not typed.
 * <p>
 * The page is saved AS YOU TYPE: every few ticks the draft is compared with what was last handed over and, if it
 * changed, passed to {@code onDone}. "Done" (and Esc) simply close; "Cancel" restores the dialogue as it was when this
 * screen opened. The draft lives in fields (updated by the widgets' listeners), so a resize or a picker screen
 * never loses anything.
 */
public class DialogueEditScreen extends PanelScreen implements NpcEditingScreen {
    private static final int MAX_OPTIONS = 4;
    private static final int AUTOSAVE_INTERVAL = 10;   // ticks

    private enum Kind { GOTO, OPEN_TRADE, ACCEPT_QUEST, TURN_IN_QUEST, CLOSE, CUSTOM }

    private static final class OptionDraft {
        String text = "";
        Kind kind = Kind.GOTO;
        String quest = "";  // Quest id as typed
        String pageA = "";  // Goto target / next page after accepting / page on success ("" = the starting dialogue)
        String pageB = "";  // Page on failure (turn-in only)
        DialogueAction.Custom custom;  // Addon action: only preserved, never created in this GUI
    }

    private final NpcDialogue dialogue;          // The dialogue as it was when this screen opened
    private final Consumer<NpcDialogue> onDone;

    /**
     * The pool of pages an option can point at. For a page that is part of an NPC's own dialogue this is just
     * {@code dialogue.pages()}; a page edited from the standalone LIBRARY needs the whole library instead, since
     * {@code dialogue} there is only a throwaway single-page wrapper.
     */
    private final List<DialoguePage> linkTargets;

    private final String pageId;
    private final boolean isNew;
    private final String defaultName;
    private String name;
    private String text;
    private final OptionDraft[] options = new OptionDraft[MAX_OPTIONS];

    @Nullable private DialoguePage lastHandedOver;
    private int autosaveTimer = 0;

    /** @param editing the page to edit, or null to create a new one. Link targets default to the dialogue's own pages. */
    public DialogueEditScreen(Screen parent, NpcDialogue dialogue, DialoguePage editing, Consumer<NpcDialogue> onDone) {
        this(parent, dialogue, editing, onDone, List.copyOf(dialogue.pages().values()));
    }

    /** @param editing the page to edit, or null to create a new one */
    public DialogueEditScreen(Screen parent, NpcDialogue dialogue, DialoguePage editing,
                              Consumer<NpcDialogue> onDone, List<DialoguePage> linkTargets) {
        super(Component.translatable("gui.nexusnpc.dialogue.title"), parent);
        this.dialogue = dialogue;
        this.onDone = onDone;
        this.linkTargets = linkTargets;
        this.isNew = editing == null;
        this.pageId = editing != null ? editing.id() : dialogue.freshId();
        this.name = editing != null ? editing.name() : "";
        this.text = editing != null ? editing.text() : "";
        this.lastHandedOver = editing;
        for (int i = 0; i < MAX_OPTIONS; i++) {
            options[i] = editing != null && i < editing.options().size()
                    ? draftOf(editing.options().get(i)) : new OptionDraft();
        }
        // Counted against linkTargets (the real pool), not `dialogue`, which in library mode only wraps the page being edited
        int number = linkTargets.size() + (dialogue.page(pageId) == null ? 1 : 0);
        this.defaultName = Component.translatable("gui.nexusnpc.dialogue.default_name", number).getString();
    }

    @Override protected int maxPanelWidth() { return 340; }

    @Override protected int maxPanelHeight() { return 440; }

    // ================= layout =================

    @Override
    protected int buildBody(ScrollPanel body, int x, int w) {
        int y = 0;

        body.add(new Heading(x, y, w, Component.translatable("gui.nexusnpc.dialogue.name")));
        y += Heading.HEIGHT + 2;
        EditBox nameBox = new EditBox(font, x, y, w, 20, Component.translatable("gui.nexusnpc.dialogue.name"));
        nameBox.setHint(Component.translatable("gui.nexusnpc.dialogue.name"));
        nameBox.setMaxLength(48);
        nameBox.setValue(name);
        nameBox.setResponder(v -> name = v);
        body.add(nameBox);
        y += 28;

        body.add(new Heading(x, y, w, Component.translatable("gui.nexusnpc.dialogue.text")));
        y += Heading.HEIGHT + 2;
        MultiLineEditBox textBox = new MultiLineEditBox(font, x, y, w, 64,
                Component.translatable("gui.nexusnpc.dialogue.text"), Component.translatable("gui.nexusnpc.dialogue.text"));
        textBox.setValue(text);
        textBox.setValueListener(v -> text = v);   // After setValue, so building does not count as an edit
        body.add(textBox);
        y += 72;

        for (int i = 0; i < MAX_OPTIONS; i++) {
            body.add(new Heading(x, y, w, Component.translatable("gui.nexusnpc.dialogue.answer", i + 1)));
            y += Heading.HEIGHT + 2;
            y += buildOption(body, i, x, y, w) + 8;
        }
        return y;
    }

    /** @return the height used */
    private int buildOption(ScrollPanel body, int i, int x, int y, int w) {
        OptionDraft d = options[i];
        int start = y;

        EditBox answer = new EditBox(font, x, y, w, 20, Component.translatable("gui.nexusnpc.dialogue.option_hint"));
        answer.setHint(Component.translatable("gui.nexusnpc.dialogue.option_hint"));
        answer.setMaxLength(80);
        answer.setValue(d.text);
        answer.setResponder(v -> d.text = v);
        body.add(answer);
        y += 24;

        body.add(PickerScreen.openerButton(x, y, w, Component.translatable("gui.nexusnpc.dialogue.action"),
                kindName(d.kind), () -> pickKind(d)));
        y += 24;

        switch (d.kind) {
            case GOTO -> {
                body.add(pageButton(x, y, w, "gui.nexusnpc.dialogue.leads_to", d, false));
                y += 24;
            }
            case ACCEPT_QUEST -> {
                body.add(questBox(x, y, w, d));
                y += 24;
                body.add(pageButton(x, y, w, "gui.nexusnpc.dialogue.after_accept", d, false));
                y += 24;
            }
            case TURN_IN_QUEST -> {
                body.add(questBox(x, y, w, d));
                y += 24;
                body.add(pageButton(x, y, w, "gui.nexusnpc.dialogue.on_success", d, false));
                y += 24;
                body.add(pageButton(x, y, w, "gui.nexusnpc.dialogue.on_failure", d, true));
                y += 24;
            }
            default -> {}  // OPEN_TRADE, CLOSE and CUSTOM need no parameters
        }
        return y - start - 4;
    }

    private EditBox questBox(int x, int y, int w, OptionDraft d) {
        EditBox box = new EditBox(font, x, y, w, 20, Component.translatable("gui.nexusnpc.dialogue.quest_hint"));
        box.setHint(Component.translatable("gui.nexusnpc.dialogue.quest_hint"));
        box.setMaxLength(64);
        box.setValue(d.quest);
        box.setResponder(v -> d.quest = v);
        return box;
    }

    private Button pageButton(int x, int y, int w, String labelKey, OptionDraft d, boolean second) {
        String full = pageName(second ? d.pageB : d.pageA);
        Button button = PickerScreen.openerButton(x, y, w, Component.translatable(labelKey), Component.literal(full),
                () -> pickPage(d, second));
        button.setTooltip(Tooltip.create(Component.literal(full)));
        return button;
    }

    @Override
    protected void buildFooter(int x, int y, int width) {
        int half = (width - 4) / 2;
        footerButton(Component.translatable("gui.cancel"), x, y, half, b -> cancel());
        footerButton(Component.translatable("gui.done"), x + half + 4, y, width - half - 4, b -> onClose());
    }

    // ================= pickers =================

    private static Component kindName(Kind kind) {
        return Component.translatable("gui.nexusnpc.dialogue.action." + kind.name().toLowerCase(Locale.ROOT));
    }

    private void pickKind(OptionDraft d) {
        List<PickerScreen.Entry<Kind>> entries = new ArrayList<>();
        for (Kind kind : kindsFor(d)) {
            entries.add(new PickerScreen.Entry<>(kind, kindName(kind),
                    Component.translatable("gui.nexusnpc.dialogue.action." + kind.name().toLowerCase(Locale.ROOT) + ".desc"), null));
        }
        Minecraft.getInstance().setScreen(new PickerScreen<>(this, Component.translatable("gui.nexusnpc.dialogue.action"),
                entries, d.kind, picked -> d.kind = picked));   // This screen re-lays itself out when the picker returns
    }

    private void pickPage(OptionDraft d, boolean second) {
        String current = second ? d.pageB : d.pageA;
        Minecraft.getInstance().setScreen(new PagePickerScreen(this, names(), current, picked -> {
            if (second) d.pageB = picked; else d.pageA = picked;
        }));
    }

    /** id -> display name of every pickable link target, plus the page being edited (it may not be saved yet). */
    private Map<String, String> names() {
        Map<String, String> map = new LinkedHashMap<>();
        for (DialoguePage p : linkTargets) map.put(p.id(), p.displayName());
        map.put(pageId, name.isBlank() ? defaultName : name.trim());
        return map;
    }

    private String pageName(String id) {
        if (id.isBlank()) return Component.translatable("gui.nexusnpc.dialogue.start_page").getString();
        return names().getOrDefault(id, "?");
    }

    // ================= saving =================

    private DialoguePage buildPage() {
        List<DialogueOption> built = new ArrayList<>();
        for (OptionDraft d : options) {
            String t = d.text.trim();
            if (!t.isEmpty()) built.add(new DialogueOption(t, actionOf(d)));
        }
        String finalName = name.trim().isEmpty() ? defaultName : name.trim();
        return new DialoguePage(pageId, finalName, text, built);
    }

    /** Hand the draft over if it changed since the last time. A brand-new page that is still blank is not created yet. */
    private void commitIfChanged() {
        DialoguePage page = buildPage();
        boolean blank = name.isBlank() && text.isBlank() && page.options().isEmpty();
        if (isNew && lastHandedOver == null && blank) return;
        if (page.equals(lastHandedOver)) return;
        lastHandedOver = page;
        onDone.accept(dialogue.withPage(page));
    }

    @Override
    public void tick() {
        super.tick();
        if (++autosaveTimer >= AUTOSAVE_INTERVAL) {
            autosaveTimer = 0;
            commitIfChanged();
        }
    }

    /** Done / Esc: keep everything. */
    @Override
    public void onClose() {
        commitIfChanged();
        super.onClose();
    }

    /** Cancel: put the dialogue back the way it was when this screen opened. */
    private void cancel() {
        if (lastHandedOver != null || !isNew) onDone.accept(dialogue);
        Minecraft.getInstance().setScreen(parent);
    }

    // ========== Draft <-> Action ==========
    /** CUSTOM is offered only for options that already are CUSTOM (addon actions cannot be authored here). */
    private static List<Kind> kindsFor(OptionDraft d) {
        return Arrays.stream(Kind.values()).filter(k -> k != Kind.CUSTOM || d.kind == Kind.CUSTOM).toList();
    }

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
            case DialogueAction.Custom c -> { d.kind = Kind.CUSTOM; d.custom = c; }
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
            case CUSTOM -> d.custom != null ? d.custom : new DialogueAction.Close();
        };
    }

    /** Never throws: "quest1" becomes nexusnpc:quest1, illegal characters become '_'. */
    private static ResourceLocation questId(String raw) {
        String s = raw.trim().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_.\\-/:]", "_");
        if (s.isEmpty()) s = "quest";
        if (!s.contains(":")) s = NexusNPC.MOD_ID + ":" + s;
        ResourceLocation id = ResourceLocation.tryParse(s);
        return id != null ? id : ResourceLocation.fromNamespaceAndPath(NexusNPC.MOD_ID, "quest");
    }

}
