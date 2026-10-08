package com.talesforge.nexusnpc.client.gui.ui;

import com.talesforge.nexusnpc.client.gui.NpcEditingScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

/**
 * A full screen for choosing ONE value out of a list — a model, a skin, an attitude, a faction, an answer action...
 * Entries show a name, an optional description and an optional icon; the current value is ticked; a search box
 * appears when the list is long. Picking a value calls {@code onPick} and returns to the parent screen.
 */
public class PickerScreen<T> extends PanelScreen implements NpcEditingScreen {
    /** One choice. {@code description} and {@code icon} may be null. */
    public record Entry<T>(T value, Component label, @Nullable Component description, @Nullable ListRow.Icon icon) {
        public Entry(T value, Component label) { this(value, label, null, null); }
    }

    private static final int SEARCH_THRESHOLD = 7;

    private final List<Entry<T>> entries;
    private final T current;
    private final Consumer<T> onPick;
    private String filter = "";
    private int bodyWidth = 200;
    private int bodyX = 0;

    public PickerScreen(Screen parent, Component title, List<Entry<T>> entries, @Nullable T current, Consumer<T> onPick) {
        super(title, parent);
        this.entries = entries;
        this.current = current;
        this.onPick = onPick;
    }

    private boolean searchable() { return entries.size() >= SEARCH_THRESHOLD; }

    @Override protected boolean fillHeight() { return searchable(); }

    @Override protected int headerExtra() { return searchable() ? 24 : 0; }

    @Override
    protected void buildHeaderExtra(int x, int y, int width) {
        if (!searchable()) return;
        EditBox search = new EditBox(font, x, y, width, 18, Component.translatable("gui.nexusnpc.picker.search"));
        search.setHint(Component.translatable("gui.nexusnpc.picker.search"));
        search.setMaxLength(48);
        search.setValue(filter);
        search.setResponder(text -> {
            filter = text.toLowerCase(Locale.ROOT).trim();
            fillRows();   // Only the list changes: rebuilding the screen would drop focus from the box mid-typing
        });
        addRenderableWidget(search);
        setInitialFocus(search);
    }

    @Override
    protected int buildBody(ScrollPanel body, int x, int width) {
        this.bodyX = x;
        this.bodyWidth = width;
        return populate(body);
    }

    private void fillRows() {
        body.clear();
        body.setContentHeight(populate(body) + 2);
    }

    private int populate(ScrollPanel panel) {
        int y = 0;
        for (Entry<T> entry : entries) {
            String haystack = entry.label().getString().toLowerCase(Locale.ROOT);
            if (!filter.isEmpty() && !haystack.contains(filter)) continue;
            boolean selected = entry.value().equals(current);
            ListRow row = new ListRow(bodyX, y, bodyWidth, entry.label(), entry.description(), entry.icon(), selected, () -> {
                onPick.accept(entry.value());
                Minecraft.getInstance().setScreen(parent);
            });
            panel.add(row);
            y += row.getHeight() + 2;
        }
        if (y == 0) {
            panel.add(new TextLabel(bodyX, 4, bodyWidth, Component.translatable("gui.nexusnpc.picker.empty"), UiTheme.TEXT_DIM));
            y = 20;
        }
        return y;
    }

    @Override
    protected void buildFooter(int x, int y, int width) {
        footerButton(Component.translatable("gui.back"), x, y, width, b -> onClose());
    }

    /** The label of a picker-opening button: "Label: Value". */
    public static Component buttonText(Component label, Component value) {
        return Component.empty().append(label).append(": ").append(value.copy().withStyle(net.minecraft.ChatFormatting.YELLOW));
    }

    /** A button that opens a picker. Returns it unattached, so the caller decides where it goes. */
    public static Button openerButton(int x, int y, int width, Component label, Component currentValue, Runnable open) {
        return Button.builder(buttonText(label, currentValue), b -> open.run()).bounds(x, y, width, 20).build();
    }
}
