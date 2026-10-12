package com.talesforge.nexusnpc.client.gui.screen;

import com.talesforge.nexusnpc.client.gui.ui.PickerScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * "Where does this answer lead?" — pick one of the NPC's dialogues by name, the starting dialogue, or (when the
 * caller allows it) create a brand-new empty page on the spot.
 */
public class PagePickerScreen extends PickerScreen<String> {
    /** Never a real page id (ids are hex numbers), so it can stand for the "new page" entry. */
    private static final String NEW_PAGE = "\u0000new";

    /**
     * @param pages     id to display name. An extra first entry with id "" means "the starting dialogue".
     * @param createNew creates a page and returns its id; null hides the "New page" entry
     */
    public PagePickerScreen(Screen parent, Map<String, String> pages, String current, Consumer<String> onPick,
                            @Nullable Supplier<String> createNew) {
        super(parent, Component.translatable("gui.nexusnpc.dialogue.pick_title"), entriesOf(pages, createNew != null), current,
                picked -> onPick.accept(NEW_PAGE.equals(picked) && createNew != null ? createNew.get() : picked));
    }

    public PagePickerScreen(Screen parent, Map<String, String> pages, String current, Consumer<String> onPick) {
        this(parent, pages, current, onPick, null);
    }

    private static List<Entry<String>> entriesOf(Map<String, String> pages, boolean withNew) {
        List<Entry<String>> list = new ArrayList<>();
        if (withNew) {
            list.add(new Entry<>(NEW_PAGE, Component.translatable("gui.nexusnpc.dialogue.new_page"),
                    Component.translatable("gui.nexusnpc.dialogue.new_page.desc"), null));
        }
        list.add(new Entry<>("", Component.translatable("gui.nexusnpc.dialogue.start_page")));
        for (Map.Entry<String, String> page : pages.entrySet()) {
            list.add(new Entry<>(page.getKey(), Component.literal(page.getValue())));
        }
        return list;
    }
}
