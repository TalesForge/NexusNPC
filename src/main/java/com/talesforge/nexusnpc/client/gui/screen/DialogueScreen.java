package com.talesforge.nexusnpc.client.gui.screen;

import com.talesforge.nexusnpc.client.dialogue.DialogueText;
import com.talesforge.nexusnpc.client.gui.ui.UiTheme;
import com.talesforge.nexusnpc.network.payload.DialogueActionPayload;
import com.talesforge.nexusnpc.network.payload.action.DialogueClosePayload;
import com.talesforge.nexusnpc.npc.dialogue.DialogueAction;
import com.talesforge.nexusnpc.npc.dialogue.DialogueOption;
import com.talesforge.nexusnpc.npc.dialogue.DialoguePage;
import com.talesforge.nexusnpc.npc.dialogue.NpcDialogue;
import com.talesforge.nexusnpc.npc.runtime.NpcTalking;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * The conversation window a PLAYER sees when talking to an NPC.
 * <p>
 * A panel docked to the bottom of the screen (the world stays visible and keeps running): the speaker's portrait
 * and name, the text typed out letter by letter, and numbered answers. Moving to the next page happens IN this
 * same screen — it does not close and reopen.
 * <ul>
 *   <li>Click / Space / Enter finishes the typing at once; the answers appear when the text is done.</li>
 *   <li>1–8 pick an answer; Esc closes. Long text scrolls with the mouse wheel.</li>
 *   <li>The window closes by itself if the NPC disappears or the player walks away.</li>
 * </ul>
 * A "preview" instance ({@link #preview}) runs the same window locally against a dialogue being edited, sending
 * nothing to the server, so authors can test a conversation without leaving the editor.
 */
public class DialogueScreen extends Screen {
    private static final int MARGIN = 8;
    private static final int BOTTOM_GAP = 22;
    private static final int PAD = 8;
    private static final int PORTRAIT_W = 64;
    private static final int LINE_H = 10;
    private static final int CHARS_PER_SECOND = 70;
    private static final long REPLY_TIMEOUT_MS = 2500;

    private final int entityId;
    @Nullable private final Screen previewParent;
    @Nullable private final NpcDialogue previewDialogue;

    private DialoguePage page;
    private long shownAt;
    private boolean waiting = false;
    private long waitingSince = 0;
    private int textScroll = 0;
    private Component previewNote = Component.empty();

    // ---- layout, recomputed in init() ----
    private int panelX, panelY, panelW, panelH;
    private int portraitW;
    private int textX, textY, textW, textAreaH;
    private List<String> lines = List.of();
    private final List<OptionButton> optionButtons = new ArrayList<>();

    /** A real conversation with the NPC with this entity id. */
    public DialogueScreen(int entityId, DialoguePage page) {
        this(entityId, page, null, null);
    }

    private DialogueScreen(int entityId, DialoguePage page, @Nullable Screen previewParent, @Nullable NpcDialogue previewDialogue) {
        super(Component.translatable("gui.nexusnpc.dialogue.title"));
        this.entityId = entityId;
        this.page = page;
        this.previewParent = previewParent;
        this.previewDialogue = previewDialogue;
        this.shownAt = Util.getMillis();
    }

    /** Run {@code dialogue} locally from {@code start}. {@code speakerEntityId} may be -1 (no portrait). */
    public static DialogueScreen preview(Screen parent, int speakerEntityId, NpcDialogue dialogue, DialoguePage start) {
        return new DialogueScreen(speakerEntityId, start, parent, dialogue);
    }

    public int entityId() { return entityId; }

    public boolean isPreview() { return previewParent != null; }

    // ================= pages =================

    /** Switch to another page without closing the window. {@link DialoguePage#END} closes it. */
    public void showPage(DialoguePage next) {
        if (next.isEnd()) {
            onClose();
            return;
        }
        this.page = next;
        this.shownAt = Util.getMillis();
        this.waiting = false;
        this.textScroll = 0;
        rebuildWidgets();
    }

    private String playerName() {
        return Minecraft.getInstance().player != null ? Minecraft.getInstance().player.getName().getString() : "";
    }

    private String speakerName() {
        Entity e = speaker();
        return e != null ? e.getDisplayName().getString() : Component.translatable("gui.nexusnpc.dialogue.npc").getString();
    }

    @Nullable
    private Entity speaker() {
        return Minecraft.getInstance().level != null && entityId >= 0 ? Minecraft.getInstance().level.getEntity(entityId) : null;
    }

    // ================= layout =================

    @Override
    protected void init() {
        optionButtons.clear();

        panelW = Math.min(420, width - 2 * MARGIN);
        panelX = (width - panelW) / 2;
        portraitW = panelW >= 280 ? PORTRAIT_W : 0;
        textX = panelX + PAD + (portraitW > 0 ? portraitW + PAD : 0);
        textW = panelX + panelW - PAD - textX;

        String text = DialogueText.apply(page.text(), playerName(), speakerName());
        List<FormattedText> split = font.getSplitter().splitLines(text, textW, Style.EMPTY);
        lines = new ArrayList<>();
        for (FormattedText line : split) lines.add(line.getString());

        // Answers: the page's own, or a single "Close" when the page has none (so nobody is ever stuck)
        List<Component> labels = new ArrayList<>();
        List<DialogueAction> actions = new ArrayList<>();
        for (DialogueOption option : page.options()) {
            labels.add(Component.literal(DialogueText.apply(option.text(), playerName(), speakerName())));
            actions.add(option.action());
        }
        boolean implicitClose = labels.isEmpty();
        if (implicitClose) {
            labels.add(Component.translatable("gui.nexusnpc.dialogue.close_button"));
            actions.add(new DialogueAction.Close());
        }

        int optionH = labels.size() > 5 ? 16 : 20;
        int optionsH = labels.size() * (optionH + 2);
        int headerH = 14;

        int maxTextH = Math.max(30, height - 2 * MARGIN - BOTTOM_GAP - 2 * PAD - headerH - 6 - optionsH);
        textAreaH = Mth.clamp(lines.size() * LINE_H, 24, maxTextH);
        panelH = PAD + headerH + textAreaH + 6 + optionsH + PAD - 2;
        panelH = Math.max(panelH, portraitW > 0 ? 96 : 0);
        panelY = Math.max(MARGIN, height - BOTTOM_GAP - panelH);

        textY = panelY + PAD + headerH;
        int y = textY + textAreaH + 6;
        for (int i = 0; i < labels.size(); i++) {
            final int index = i;
            OptionButton button = new OptionButton(textX, y, textW, optionH, i + 1, labels.get(i),
                    tagOf(actions.get(i)), () -> choose(index, implicitClose));
            optionButtons.add(addRenderableWidget(button));
            y += optionH + 2;
        }
        updateOptionState();
    }

    // ================= typing =================

    private int totalChars() {
        int n = 0;
        for (String line : lines) n += line.length();
        return n;
    }

    private int shownChars() {
        return (int) ((Util.getMillis() - shownAt) * CHARS_PER_SECOND / 1000L);
    }

    private boolean textDone() { return shownChars() >= totalChars(); }

    private void skipTyping() { shownAt = Util.getMillis() - 3_600_000L; }

    private void updateOptionState() {
        boolean done = textDone();
        for (OptionButton button : optionButtons) {
            button.visible = done;
            button.active = !waiting;
        }
    }

    // ================= choosing =================

    private void choose(int index, boolean implicitClose) {
        if (waiting || !textDone()) return;
        if (implicitClose) {
            onClose();
            return;
        }
        DialogueAction action = page.options().get(index).action();
        if (isPreview()) {
            previewStep(action);
            return;
        }
        waiting = true;
        waitingSince = Util.getMillis();
        updateOptionState();
        PacketDistributor.sendToServer(new DialogueActionPayload(entityId, page.id(), index));
        if (action instanceof DialogueAction.Close) onClose();   // Nothing to wait for
        // Every other action is answered by the server with the next page (shown in place) or by closing the window
    }

    /** What the server would do, played locally against the dialogue being edited. */
    private void previewStep(DialogueAction action) {
        previewNote = Component.empty();
        switch (action) {
            case DialogueAction.Goto g -> previewGo(g.pageId());
            case DialogueAction.OpenTrade t -> previewNote = Component.translatable("gui.nexusnpc.preview.trade");
            case DialogueAction.AcceptQuest a -> {
                previewNote = Component.translatable("gui.nexusnpc.preview.quest_accepted", a.questId().toString());
                previewGo(a.nextPageId());
            }
            case DialogueAction.TurnInQuest t -> {
                previewNote = Component.translatable("gui.nexusnpc.preview.quest_turned_in", t.questId().toString());
                previewGo(t.successPageId());
            }
            case DialogueAction.Custom c -> {
                previewNote = Component.translatable("gui.nexusnpc.preview.custom", c.handlerId().toString());
                if (c.successPageId().isBlank()) onClose(); else previewGo(c.successPageId());
            }
            case DialogueAction.Close c -> onClose();
        }
    }

    private void previewGo(String pageId) {
        NpcDialogue dialogue = previewDialogue;
        DialoguePage next = dialogue == null ? null : (pageId.isBlank() ? dialogue.startPage() : dialogue.page(pageId));
        Component note = previewNote;
        if (next == null) {
            if (!note.getString().isEmpty() || pageId.isBlank()) { onClose(); return; }
            previewNote = Component.translatable("gui.nexusnpc.preview.missing");
            return;
        }
        showPage(next);
        previewNote = note;
    }

    @Nullable
    private static Component tagOf(DialogueAction action) {
        return switch (action) {
            case DialogueAction.OpenTrade t -> Component.translatable("gui.nexusnpc.dialogue.tag.trade").withStyle(ChatFormatting.GOLD);
            case DialogueAction.AcceptQuest a -> Component.translatable("gui.nexusnpc.dialogue.tag.quest").withStyle(ChatFormatting.YELLOW);
            case DialogueAction.TurnInQuest t -> Component.translatable("gui.nexusnpc.dialogue.tag.turn_in").withStyle(ChatFormatting.GREEN);
            case DialogueAction.Close c -> Component.translatable("gui.nexusnpc.dialogue.tag.leave").withStyle(ChatFormatting.GRAY);
            default -> null;
        };
    }

    // ================= ticking =================

    @Override
    public void tick() {
        super.tick();
        updateOptionState();
        if (waiting && Util.getMillis() - waitingSince > REPLY_TIMEOUT_MS) {
            waiting = false;   // The server did not answer (e.g. the trade window was busy): let the player try again
            updateOptionState();
        }
        if (!isPreview()) {
            Minecraft mc = Minecraft.getInstance();
            Entity speaker = speaker();
            if (speaker == null || mc.player == null || mc.player.distanceToSqr(speaker) > NpcTalking.MAX_DIST_SQ + 9) {
                onClose();   // The NPC is gone or the player walked off
            }
        }
    }

    // ================= input =================

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!textDone() && mouseX >= panelX && mouseX < panelX + panelW && mouseY >= panelY && mouseY < panelY + panelH) {
            skipTyping();
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        int visible = textAreaH / LINE_H;
        if (lines.size() > visible && mouseX >= textX && mouseX < textX + textW && mouseY >= textY && mouseY < textY + textAreaH) {
            textScroll = Mth.clamp(textScroll - (int) Math.signum(scrollY), 0, lines.size() - visible);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // Space / Enter: finish the typing, or take the only answer
        if (keyCode == 32 || keyCode == 257 || keyCode == 335) {
            if (!textDone()) {
                skipTyping();
                return true;
            }
            if (optionButtons.size() == 1) {
                optionButtons.get(0).trigger();
                return true;
            }
        }
        // 1..8 (top row and numpad): pick that answer
        int number = keyCode >= 49 && keyCode <= 56 ? keyCode - 48 : keyCode >= 321 && keyCode <= 328 ? keyCode - 320 : -1;
        if (number > 0 && number <= optionButtons.size() && textDone()) {
            optionButtons.get(number - 1).trigger();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    // ================= closing =================

    @Override
    public void onClose() {
        Minecraft.getInstance().setScreen(previewParent);
    }

    @Override
    public void removed() {
        super.removed();
        if (!isPreview() && Minecraft.getInstance().getConnection() != null) {
            PacketDistributor.sendToServer(new DialogueClosePayload(entityId));   // The NPC may walk off again
        }
    }

    /** The world keeps running: the server must stay free to answer the clicks (and the NPC is frozen by the talking lock anyway). */
    @Override
    public boolean isPauseScreen() { return false; }

    // ================= drawing =================

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        // No blur: the world stays readable. Only a soft shade under the panel so the white text always has contrast
        g.fillGradient(0, panelY - 30, width, height, 0x00000000, 0xA0000000);

        UiTheme.drawPanel(g, panelX, panelY, panelW, panelH);

        // ---- portrait ----
        if (portraitW > 0) drawPortrait(g, mouseX, mouseY);

        // ---- header: speaker name (+ preview badge) ----
        int headerY = panelY + PAD;
        g.drawString(font, Component.literal(speakerName()).withStyle(ChatFormatting.BOLD), textX, headerY, UiTheme.ACCENT, false);
        UiTheme.drawDivider(g, textX, headerY + 11, textW);
        if (isPreview()) {
            Component badge = Component.translatable("gui.nexusnpc.dialogue.preview_badge");
            g.drawString(font, badge, textX + textW - font.width(badge), headerY, UiTheme.TEXT_WARN, false);
        }

        // ---- text, typed out ----
        drawText(g);

        // ---- hints under the panel ----
        int hintY = panelY + panelH + 6;
        if (hintY + 9 < height) {
            Component hint = !previewNote.getString().isEmpty() ? previewNote
                    : Component.translatable("gui.nexusnpc.dialogue.hint");
            int color = !previewNote.getString().isEmpty() ? UiTheme.TEXT_WARN : UiTheme.TEXT_DIM;
            g.drawCenteredString(font, hint, width / 2, hintY, color);
        }
    }

    private void drawText(GuiGraphics g) {
        int visible = Math.max(1, textAreaH / LINE_H);
        int remaining = shownChars();

        // While typing, keep the line being typed in view
        if (!textDone() && lines.size() > visible) {
            int typedLine = 0, count = 0;
            for (int i = 0; i < lines.size() && count + lines.get(i).length() < remaining; i++) {
                count += lines.get(i).length();
                typedLine = i + 1;
            }
            textScroll = Mth.clamp(typedLine - visible + 1, 0, lines.size() - visible);
        }
        textScroll = Mth.clamp(textScroll, 0, Math.max(0, lines.size() - visible));

        g.enableScissor(textX, textY, textX + textW, textY + textAreaH);
        int y = textY;
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            if (i >= textScroll && y + LINE_H <= textY + textAreaH + 2) {
                if (remaining > 0) {
                    String part = remaining >= line.length() ? line : line.substring(0, Math.max(0, remaining));
                    g.drawString(font, part, textX, y, UiTheme.TEXT, false);
                }
                y += LINE_H;
            }
            remaining -= line.length();
        }
        g.disableScissor();

        if (lines.size() > visible) {   // Thin scrollbar so long text is obviously scrollable
            int barX = textX + textW + 2;
            int thumbH = Math.max(8, textAreaH * visible / lines.size());
            int thumbY = textY + (textAreaH - thumbH) * textScroll / Math.max(1, lines.size() - visible);
            g.fill(barX, textY, barX + 2, textY + textAreaH, UiTheme.SCROLL_TRACK);
            g.fill(barX, thumbY, barX + 2, thumbY + thumbH, UiTheme.SCROLL_THUMB);
        }
    }

    private void drawPortrait(GuiGraphics g, int mouseX, int mouseY) {
        int px = panelX + PAD;
        int py = panelY + PAD;
        int ph = panelH - 2 * PAD;
        g.fill(px, py, px + portraitW, py + ph, UiTheme.ROW_DISABLED);
        g.fill(px, py + ph - 1, px + portraitW, py + ph, UiTheme.PANEL_BORDER);

        Entity e = speaker();
        if (e instanceof LivingEntity living) {
            float byHeight = ph * 0.78F / Math.max(0.5F, living.getBbHeight());
            float byWidth = portraitW * 0.80F / Math.max(0.4F, living.getBbWidth());
            int scale = Math.max(8, (int) Math.min(byHeight, byWidth));
            InventoryScreen.renderEntityInInventoryFollowsMouse(g, px, py, px + portraitW, py + ph, scale, 0.0625F,
                    mouseX, mouseY, living);
        } else {
            g.drawCenteredString(font, "?", px + portraitW / 2, py + ph / 2 - 4, UiTheme.TEXT_DIM);
        }
    }

    // ================= answer button =================

    /** A numbered answer row with an optional coloured tag on the right ([Trade], [Quest]...). */
    private static final class OptionButton extends AbstractWidget {
        private final int number;
        @Nullable private final Component tag;
        private final Runnable onChoose;

        OptionButton(int x, int y, int w, int h, int number, Component label, @Nullable Component tag, Runnable onChoose) {
            super(x, y, w, h, label);
            this.number = number;
            this.tag = tag;
            this.onChoose = onChoose;
        }

        void trigger() { if (visible && active) onChoose.run(); }

        @Override
        public void onClick(double mouseX, double mouseY) { onChoose.run(); }

        @Override
        protected void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
            Font font = Minecraft.getInstance().font;
            boolean hot = active && isHoveredOrFocused();
            g.fill(getX(), getY(), getX() + width, getY() + height,
                    !active ? UiTheme.ROW_DISABLED : hot ? UiTheme.ROW_SELECTED : UiTheme.ROW);
            if (hot) g.fill(getX(), getY(), getX() + 2, getY() + height, UiTheme.ACCENT);

            int ty = getY() + (height - 8) / 2;
            g.drawString(font, number + ".", getX() + 6, ty, hot ? UiTheme.ACCENT : UiTheme.TEXT_DIM, false);

            int right = getX() + width - 6;
            if (tag != null) {
                int tagW = font.width(tag);
                g.drawString(font, tag, right - tagW, ty, 0xFFFFFFFF, false);
                right -= tagW + 8;
            }
            int labelX = getX() + 6 + font.width(number + ".") + 4;
            String label = font.plainSubstrByWidth(getMessage().getString(), Math.max(10, right - labelX));
            g.drawString(font, label, labelX, ty, active ? UiTheme.TEXT : UiTheme.TEXT_DIM, false);
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput output) {
            defaultButtonNarrationText(output);
        }
    }
}
