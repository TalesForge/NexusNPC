package com.talesforge.masternpc.client.gui;

import com.talesforge.masternpc.npc.dialogue.DialogueAction;
import com.talesforge.masternpc.npc.dialogue.DialogueOption;
import com.talesforge.masternpc.npc.dialogue.DialoguePage;
import com.talesforge.masternpc.npc.dialogue.NpcDialogue;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.MultiLineEditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.*;
import java.util.function.Consumer;

/**
 * Сознательно простой, чисто текстовый редактор дерева: одна страница на экране,
 * до MAX_OPTIONS вариантов. Хватает на "простенькие" ветвящиеся диалоги;
 * визуальный редактор дерева — отдельная задача поверх этой же модели данных, не переделка.
 */
public class DialogueEditScreen extends Screen {
    private static final int MAX_OPTIONS = 4;
    private enum ActionKind { GOTO, OPEN_TRADE, ACCEPT_QUEST, TURN_IN_QUEST, CLOSE }

    private final Screen parent;
    private final Map<String, DialoguePage> pages;
    private final Consumer<NpcDialogue> onDone;

    private String currentPageId = DialoguePage.START_ID;
    private EditBox pageIdBox;
    private MultiLineEditBox textBox;
    private final EditBox[] optionText = new EditBox[MAX_OPTIONS];
    private final EditBox[] optionParam = new EditBox[MAX_OPTIONS];
    private CycleButton<ActionKind>[] optionKind;

    public DialogueEditScreen(Screen parent, NpcDialogue initial, Consumer<NpcDialogue> onDone) {
        super(Component.translatable("gui.masternpc.dialogue.title"));
        this.parent = parent;
        this.pages = new LinkedHashMap<>(initial.pages());
        this.onDone = onDone;
    }

    @Override
    @SuppressWarnings("unchecked")
    protected void init() {
        int w = 260;
        int x = this.width / 2 - w / 2;
        int y = 24;

        DialoguePage page = pages.getOrDefault(currentPageId, new DialoguePage(currentPageId, "", List.of()));

        pageIdBox = new EditBox(font, x, y, w - 90, 20, Component.translatable("gui.masternpc.dialogue.page_id"));
        pageIdBox.setValue(currentPageId);
        addRenderableWidget(pageIdBox);
        addRenderableWidget(Button.builder(Component.translatable("gui.masternpc.dialogue.load"), b -> {
            saveCurrentPage();
            currentPageId = pageIdBox.getValue().isBlank() ? DialoguePage.START_ID : pageIdBox.getValue().trim();
            rebuild();
        }).bounds(x + w - 85, y, 85, 20).build());
        y += 24;

        textBox = new MultiLineEditBox(font, x, y, w, 60,
                Component.translatable("gui.masternpc.dialogue.text"), Component.translatable("gui.masternpc.dialogue.text"));
        textBox.setValue(page.text());
        addRenderableWidget(textBox);
        y += 66;

        optionKind = new CycleButton[MAX_OPTIONS];
        List<DialogueOption> options = page.options();
        for (int i = 0; i < MAX_OPTIONS; i++) {
            DialogueOption opt = i < options.size() ? options.get(i) : null;

            EditBox txt = new EditBox(font, x, y, 120, 20, Component.literal("option"));
            txt.setValue(opt != null ? opt.text() : "");
            optionText[i] = txt;
            addRenderableWidget(txt);

            CycleButton<ActionKind> kind = CycleButton.<ActionKind>builder(
                            k -> Component.translatable("gui.masternpc.dialogue.action." + k.name().toLowerCase(Locale.ROOT)))
                    .withValues(ActionKind.values())
                    .withInitialValue(kindOf(opt))
                    .create(x + 122, y, 90, 20, Component.empty(), (btn, value) -> {});
            optionKind[i] = kind;
            addRenderableWidget(kind);

            EditBox param = new EditBox(font, x + 214, y, w - 214, 20, Component.literal("param"));
            param.setValue(paramOf(opt));
            optionParam[i] = param;
            addRenderableWidget(param);
            y += 24;
        }

        addRenderableWidget(Button.builder(Component.translatable("gui.masternpc.dialogue.delete_page"), b -> {
            pages.remove(currentPageId);
            currentPageId = DialoguePage.START_ID;
            rebuild();
        }).bounds(x, y + 6, w / 2 - 2, 20).build());

        addRenderableWidget(Button.builder(Component.translatable("gui.masternpc.done"), b -> {
            saveCurrentPage();
            onDone.accept(new NpcDialogue(Map.copyOf(pages)));
            Minecraft.getInstance().setScreen(parent);
        }).bounds(x + w / 2 + 2, y + 6, w / 2 - 2, 20).build());
    }

    private void rebuild() { clearWidgets(); init(); }

    private void saveCurrentPage() {
        List<DialogueOption> options = new ArrayList<>();
        for (int i = 0; i < MAX_OPTIONS; i++) {
            String text = optionText[i].getValue().trim();
            if (text.isEmpty()) continue;
            options.add(new DialogueOption(text, actionFrom(optionKind[i].getValue(), optionParam[i].getValue().trim())));
        }
        pages.put(currentPageId, new DialoguePage(currentPageId, textBox.getValue(), options));
    }

    private static ActionKind kindOf(DialogueOption opt) {
        if (opt == null) return ActionKind.GOTO;
        return switch (opt.action()) {
            case DialogueAction.Goto g -> ActionKind.GOTO;
            case DialogueAction.OpenTrade t -> ActionKind.OPEN_TRADE;
            case DialogueAction.AcceptQuest a -> ActionKind.ACCEPT_QUEST;
            case DialogueAction.TurnInQuest t -> ActionKind.TURN_IN_QUEST;
            case DialogueAction.Close c -> ActionKind.CLOSE;
        };
    }

    /** Простое текстовое кодирование параметров: "pageId" для GOTO, "questId|next" для ACCEPT, "questId|success|fail" для TURN_IN. */
    private static String paramOf(DialogueOption opt) {
        if (opt == null) return "";
        return switch (opt.action()) {
            case DialogueAction.Goto g -> g.pageId();
            case DialogueAction.OpenTrade t -> "";
            case DialogueAction.AcceptQuest a -> a.questId() + "|" + a.nextPageId();
            case DialogueAction.TurnInQuest t -> t.questId() + "|" + t.successPageId() + "|" + t.failPageId();
            case DialogueAction.Close c -> "";
        };
    }

    private static DialogueAction actionFrom(ActionKind kind, String param) {
        String[] parts = param.split("\\|");
        return switch (kind) {
            case GOTO -> new DialogueAction.Goto(param.isBlank() ? DialoguePage.START_ID : param);
            case OPEN_TRADE -> new DialogueAction.OpenTrade();
            case ACCEPT_QUEST -> new DialogueAction.AcceptQuest(
                    ResourceLocation.parse(parts[0]), parts.length > 1 ? parts[1] : DialoguePage.START_ID);
            case TURN_IN_QUEST -> new DialogueAction.TurnInQuest(
                    ResourceLocation.parse(parts[0]),
                    parts.length > 1 ? parts[1] : DialoguePage.START_ID,
                    parts.length > 2 ? parts[2] : DialoguePage.START_ID);
            case CLOSE -> new DialogueAction.Close();
        };
    }

    @Override public boolean isPauseScreen() { return true; }
}
