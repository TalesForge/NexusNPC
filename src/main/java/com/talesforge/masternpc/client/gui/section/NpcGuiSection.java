package com.talesforge.masternpc.client.gui.section;

import com.talesforge.masternpc.npc.field.NpcDataMap;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.AbstractWidget;

import java.util.function.Consumer;

/**
 * One block of widgets in the NPC editor — e.g. "name", "attitude + behavior", "skin",
 * "stats". A section is a stateful, one-shot object: a fresh instance is created every
 * time the screen opens (by an {@link NpcGuiSectionFactory}), it holds its own edited
 * values internally (exactly like the old NpcEditorScreen's private fields did), and it
 * is discarded when the screen closes.
 */
public interface NpcGuiSection {

    /**
     * Add this section's widgets starting at (x, y), using the given width, through
     * {@code addWidget} (pass {@code screen::addRenderableWidget}). Must return the total
     * vertical space consumed (including any internal spacing) so the screen can stack the
     * next section directly below it.
     */
    int build(int x, int y, int width, Font font, Consumer<AbstractWidget> addWidget);

    /** Write this section's current, edited values into the outgoing map. Called on Save/Create. */
    void collect(NpcDataMap out);
}