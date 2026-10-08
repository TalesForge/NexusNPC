package com.talesforge.nexusnpc.client.gui.screen;

import com.talesforge.nexusnpc.client.gui.ui.PickerScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/** "Where does this answer lead?" — pick one of the NPC's dialogues by name. */
public class PagePickerScreen extends PickerScreen<String> {

    /** @param pages id -> display name. An extra first entry with id "" means "the starting dialogue". */
    public PagePickerScreen(Screen parent, Map<String, String> pages, String current, Consumer<String> onPick) {
        super(parent, Component.translatable("gui.nexusnpc.dialogue.pick_title"), entriesOf(pages), current, onPick);
    }

    private static List<Entry<String>> entriesOf(Map<String, String> pages) {
        List<Entry<String>> list = new ArrayList<>();
        list.add(new Entry<>("", Component.translatable("gui.nexusnpc.dialogue.start_page")));
        for (Map.Entry<String, String> page : pages.entrySet()) {
            list.add(new Entry<>(page.getKey(), Component.literal(page.getValue())));
        }
        return list;
    }
}
