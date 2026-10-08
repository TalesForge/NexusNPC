package com.talesforge.nexusnpc.client.gui.section;

import com.talesforge.nexusnpc.npc.field.NpcDataMap;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

/**
 * One block of widgets in the NPC editor — e.g. "name", "attitude + behavior", "skin",
 * "stats". A section is a one-shot object: a fresh instance is created every time the editor lays itself out
 * (resize, coming back from a picker screen, ...) by an {@link NpcGuiSectionFactory}.
 * <p>
 * Sections do NOT keep their own copy of the values. They read the current value from the
 * {@link NpcGuiContext#session() session} when building and write every change straight back with
 * {@code session.set(field, value)}; the session saves it to the server right away.
 */
public interface NpcGuiSection {

    /** A heading drawn above this section, or null for none. */
    @Nullable
    default Component title() { return null; }

    /**
     * Add this section's widgets starting at (x, y), using the given width, through
     * {@code addWidget}. Must return the total vertical space consumed (0 = show nothing, not even the
     * title) so the screen can stack the next section directly below it. The widgets live in a scrolling
     * panel, so a section never needs to worry about running out of room.
     * <p>
     * Call {@code requestRebuild} if a change should make the whole screen lay itself out again.
     */
    int build(int x, int y, int width, Font font, Consumer<AbstractWidget> addWidget, Runnable requestRebuild);

    /**
     * Legacy hook for sections that keep private state instead of writing to the session: the editor calls it
     * when it closes (and on Create) and merges anything that differs. New sections need not implement it.
     */
    default void collect(NpcDataMap out) {}
}
