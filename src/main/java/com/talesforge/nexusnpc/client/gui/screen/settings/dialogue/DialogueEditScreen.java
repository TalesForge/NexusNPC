package com.talesforge.nexusnpc.client.gui.screen.settings.dialogue;

import com.talesforge.nexusnpc.NexusNPC;
import com.talesforge.nexusnpc.client.gui.NpcEditingScreen;
import com.talesforge.nexusnpc.client.gui.screen.DialogueScreen;
import com.talesforge.nexusnpc.client.gui.screen.PagePickerScreen;
import com.talesforge.nexusnpc.client.gui.ui.Heading;
import com.talesforge.nexusnpc.client.gui.ui.PanelScreen;
import com.talesforge.nexusnpc.client.gui.ui.PickerScreen;
import com.talesforge.nexusnpc.client.gui.ui.ScrollPanel;
import com.talesforge.nexusnpc.client.gui.ui.TextLabel;
import com.talesforge.nexusnpc.client.gui.ui.UiTheme;
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
 * Edits ONE dialogue page: name, text and a list of answers (add, remove, reorder). What an answer does and where it
 * leads are chosen on picker screens, not typed.
 * <p>
 * Built for writing a whole conversation without leaving this screen:
 * <ul>
 *   <li>the "where does it lead" picker has a <b>New page</b> entry that creates the page on the spot, and a pencil
 *       button next to the target opens that page for editing — so a tree is written depth-first, one page after
 *       another, and Back climbs up again;</li>
 *   <li><b>Preview</b> plays the conversation in the real dialogue window, starting from this page, locally;</li>
 *   <li>everything is saved as you go (see {@link #commitIfChanged}); "Done" and Esc keep it, "Cancel" restores the
 *       dialogue as it was when this screen opened.</li>
 * </ul>
 * The draft lives in fields (updated by the widgets' listeners), so a resize or a picker screen never loses anything.
 */
public class DialogueEditScreen extends PanelScreen implements NpcEditingScreen {
    private static final int MAX_OPTIONS = 8;
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

    private final NpcDialogue original;          // The dialogue as it was when this screen opened (Cancel goes back to it)
    private NpcDialogue working;                 // original + this page's draft + any pages created from here
    private final Consumer<NpcDialogue> onDone;

    /**
     * Pool of pages an answer can point at when the page is NOT part of an NPC's dialogue (the standalone library).
     * Null for a normal NPC page: then the pool is simply {@code working.pages()} and new pages can be created.
     */
    @Nullable private final List<DialoguePage> libraryTargets;
    private final int previewEntityId;

    private final String pageId;
    private final boolean isNew;
    private final String defaultName;
    private String name;
    private String text;
    private final List<OptionDraft> options = new ArrayList<>();

    @Nullable private DialoguePage lastHandedOver;
    private int autosaveTimer = 0;

    /** @param editing the page to edit, or null to create a new one. */
    public DialogueEditScreen(Screen parent, NpcDialogue dialogue, @Nullable DialoguePage editing, Consumer<NpcDialogue> onDone) {
        this(parent, dialogue, editing, onDone, null, -1);
    }

    /** Library flavour: link targets come from the whole library, and new pages cannot be created from here. */
    public DialogueEditScreen(Screen parent, NpcDialogue dialogue, @Nullable DialoguePage editing,
                              Consumer<NpcDialogue> onDone, List<DialoguePage> libraryTargets) {
        this(parent, dialogue, editing, onDone, libraryTargets, -1);
    }

    /** @param previewEntityId the NPC whose portrait/name the preview shows, or -1 */
    public DialogueEditScreen(Screen parent, NpcDialogue dialogue, @Nullable DialoguePage editing, Consumer<NpcDialogue> onDone,
                              @Nullable List<DialoguePage> libraryTargets, int previewEntityId) {
        super(Component.translatable("gui.nexusnpc.dialogue.title"), parent);
        this.original = dialogue;
        this.working = dialogue;
        this.onDone = onDone;
        this.libraryTargets = libraryTargets;
        this.previewEntityId = previewEntityId;
        this.isNew = editing == null;
        this.pageId = editing != null ? editing.id() : freshIdFor(dialogue);
        this.name = editing != null ? editing.name() : "";
        this.text = editing != null ? editing.text() : "";
        this.lastHandedOver = editing;
        if (editing != null) {
            for (DialogueOption option : editing.options()) options.add(draftOf(option));
        } else {
            options.add(new OptionDraft());   // A new page starts with one empty answer, so it is obvious where to type
        }
        int poolSize = libraryTargets != null ? libraryTargets.size() : dialogue.pages().size();
        this.defaultName = Component.translatable("gui.nexusnpc.dialogue.default_name", poolSize + (editing == null ? 1 : 0)).getString();
    }

    private static String freshIdFor(NpcDialogue dialogue) { return dialogue.freshId(); }

    private boolean canCreatePages() { return libraryTargets == null; }

    @Override protected int maxPanelWidth() { return 340; }

    @Override protected int maxPanelHeight() { return 460; }

    // ================= layout =================

    @Override
    protected int buildBody(ScrollPanel body, int x, int w) {
        int y = 0;

        body.add(Button.builder(Component.translatable("gui.nexusnpc.dialogue.preview"), b -> preview())
                .bounds(x, y, w, 20)
                .tooltip(Tooltip.create(Component.translatable("gui.nexusnpc.dialogue.preview.tooltip")))
                .build());
        y += 28;

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
        MultiLineEditBox textBox = new MultiLineEditBox(font, x, y, w, 72,
                Component.translatable("gui.nexusnpc.dialogue.text"), Component.translatable("gui.nexusnpc.dialogue.text"));
        textBox.setValue(text);
        textBox.setValueListener(v -> text = v);   // After setValue, so building does not count as an edit
        body.add(textBox);
        y += 76;
        TextLabel placeholders = body.add(new TextLabel(x, y, w,
                Component.translatable("gui.nexusnpc.dialogue.placeholders"), UiTheme.TEXT_DIM));
        y += placeholders.getHeight() + 8;

        body.add(new Heading(x, y, w, Component.translatable("gui.nexusnpc.dialogue.answers", options.size(), MAX_OPTIONS)));
        y += Heading.HEIGHT + 4;

        for (int i = 0; i < options.size(); i++) {
            y += buildOption(body, i, x, y, w) + 10;
        }

        Button add = Button.builder(Component.translatable("gui.nexusnpc.dialogue.add_answer"), b -> {
                    options.add(new OptionDraft());
                    requestRebuild();
                })
                .bounds(x, y, w, 20).build();
        add.active = options.size() < MAX_OPTIONS;
        body.add(add);
        y += 24;
        return y;
    }

    /** One answer card. @return the height used */
    private int buildOption(ScrollPanel body, int i, int x, int y, int w) {
        OptionDraft d = options.get(i);
        int start = y;

        // --- title row with move / remove buttons ---
        int controls = 3 * 18;
        body.add(new Heading(x, y, w - controls - 2, Component.translatable("gui.nexusnpc.dialogue.answer", i + 1)));
        Button up = Button.builder(Component.literal("▲"), b -> move(i, -1)).bounds(x + w - controls, y, 16, 14).build();
        up.active = i > 0;
        body.add(up);
        Button down = Button.builder(Component.literal("▼"), b -> move(i, +1)).bounds(x + w - controls + 18, y, 16, 14).build();
        down.active = i < options.size() - 1;
        body.add(down);
        body.add(Button.builder(Component.literal("✕"), b -> {
                    options.remove(d);
                    requestRebuild();
                })
                .bounds(x + w - controls + 36, y, 16, 14)
                .tooltip(Tooltip.create(Component.translatable("gui.nexusnpc.dialogue.remove_answer")))
                .build());
        y += Heading.HEIGHT + 2;

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
            case GOTO -> y += pageRow(body, x, y, w, "gui.nexusnpc.dialogue.leads_to", d, false);
            case ACCEPT_QUEST -> {
                y += questRow(body, x, y, w, d);
                y += pageRow(body, x, y, w, "gui.nexusnpc.dialogue.after_accept", d, false);
            }
            case TURN_IN_QUEST -> {
                y += questRow(body, x, y, w, d);
                y += pageRow(body, x, y, w, "gui.nexusnpc.dialogue.on_success", d, false);
                y += pageRow(body, x, y, w, "gui.nexusnpc.dialogue.on_failure", d, true);
            }
            default -> {}  // OPEN_TRADE, CLOSE and CUSTOM need no parameters
        }
        return y - start - 4;
    }

    private int questRow(ScrollPanel body, int x, int y, int w, OptionDraft d) {
        EditBox box = new EditBox(font, x, y, w, 20, Component.translatable("gui.nexusnpc.dialogue.quest_hint"));
        box.setHint(Component.translatable("gui.nexusnpc.dialogue.quest_hint"));
        box.setMaxLength(64);
        box.setValue(d.quest);
        box.setResponder(v -> d.quest = v);
        body.add(box);
        return 24;
    }

    /** "Leads to: <page>" picker plus a pencil that jumps into that page. */
    private int pageRow(ScrollPanel body, int x, int y, int w, String labelKey, OptionDraft d, boolean second) {
        String target = second ? d.pageB : d.pageA;
        String full = pageName(target);
        int editW = 22;
        Button pick = PickerScreen.openerButton(x, y, w - editW - 2, Component.translatable(labelKey), Component.literal(full),
                () -> pickPage(d, second));
        pick.setTooltip(Tooltip.create(Component.literal(full)));
        body.add(pick);

        Button edit = Button.builder(Component.literal("✎"), b -> openLinked(target))
                .bounds(x + w - editW, y, editW, 20)
                .tooltip(Tooltip.create(Component.translatable("gui.nexusnpc.dialogue.edit_linked")))
                .build();
        edit.active = canCreatePages() && resolve(target) != null;
        body.add(edit);
        return 24;
    }

    @Override
    protected void buildFooter(int x, int y, int width) {
        int half = (width - 4) / 2;
        footerButton(Component.translatable("gui.cancel"), x, y, half, b -> cancel());
        footerButton(Component.translatable("gui.done"), x + half + 4, y, width - half - 4, b -> onClose());
    }

    // ================= answers =================

    private void move(int index, int delta) {
        int target = index + delta;
        if (target < 0 || target >= options.size()) return;
        Collections.swap(options, index, target);
        requestRebuild();
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
        Minecraft.getInstance().setScreen(new PagePickerScreen(this, names(), current,
                picked -> { if (second) d.pageB = picked; else d.pageA = picked; },
                canCreatePages() ? this::createStubPage : null));
    }

    /** Creates an empty page right now (saved immediately) so an answer can lead to it; returns its id. */
    private String createStubPage() {
        String id;
        do { id = working.freshId(); } while (id.equals(pageId));
        String stubName = Component.translatable("gui.nexusnpc.dialogue.default_name", working.pages().size() + 2).getString();
        working = working.withPage(new DialoguePage(id, stubName, "", List.of()));
        onDone.accept(working);
        return id;
    }

    /** The page with this id (blank = the starting dialogue), looking at this screen's own draft too. */
    @Nullable
    private DialoguePage resolve(String id) {
        NpcDialogue pool = working.withPage(buildPage());
        if (id.isBlank()) return pool.startPage();
        return pool.page(id);
    }

    /** id -> display name of every pickable link target, plus the page being edited (it may not be saved yet). */
    private Map<String, String> names() {
        Map<String, String> map = new LinkedHashMap<>();
        Collection<DialoguePage> pool = libraryTargets != null ? libraryTargets : working.pages().values();
        for (DialoguePage p : pool) map.put(p.id(), p.displayName());
        map.put(pageId, name.isBlank() ? defaultName : name.trim());
        return map;
    }

    private String pageName(String id) {
        if (id.isBlank()) return Component.translatable("gui.nexusnpc.dialogue.start_page").getString();
        return names().getOrDefault(id, "?");
    }

    /** Opens a linked page in a new editor on top of this one; its changes flow back through here. */
    private void openLinked(String id) {
        DialoguePage target = resolve(id);
        if (target == null) return;
        NpcDialogue base = working.withPage(buildPage());
        Minecraft.getInstance().setScreen(new DialogueEditScreen(this, base, target, updated -> {
            this.working = updated;
            this.lastHandedOver = updated.page(pageId);   // Our own page was part of `base`, so it is unchanged
            onDone.accept(updated);
        }, null, previewEntityId));
    }

    private void preview() {
        NpcDialogue dialogue = libraryTargets != null
                ? withLibrary(working.withPage(buildPage()))
                : working.withPage(buildPage());
        DialoguePage here = dialogue.page(pageId);
        if (here == null) return;
        Minecraft.getInstance().setScreen(DialogueScreen.preview(this, previewEntityId, dialogue, here));
    }

    /** Library pages can lead to other library pages: make them all reachable in the preview. */
    private NpcDialogue withLibrary(NpcDialogue dialogue) {
        NpcDialogue result = dialogue;
        for (DialoguePage p : libraryTargets) if (!result.contains(p)) result = result.withPage(p);
        return result.withStart(pageId);
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
        working = working.withPage(page);
        onDone.accept(working);
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

    /** Cancel: put the dialogue back the way it was when this screen opened (including pages created from here). */
    private void cancel() {
        if (!working.equals(original)) onDone.accept(original);   // Only if something was actually saved meanwhile
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
