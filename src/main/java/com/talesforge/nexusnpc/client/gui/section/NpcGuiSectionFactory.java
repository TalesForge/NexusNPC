package com.talesforge.nexusnpc.client.gui.section;

import com.talesforge.nexusnpc.npc.field.NpcDataMap;
import net.minecraft.resources.ResourceLocation;

/**
 * Registered once, at mod-load time, through {@link NpcGuiRegistry#register}.
 * Produces a new {@link NpcGuiSection} instance every time an editor screen is opened.
 */
public interface NpcGuiSectionFactory {
    /** Unique id, e.g. {@code nexusnpc:stats} or {@code mymod:shop_inventory}. Used for disable(...). */
    ResourceLocation id();

    /** initial holds the NPC's current settings (editing) or field defaults (creating). */
    NpcGuiSection create(NpcDataMap initial);

    /**
     * Same as {@link #create(NpcDataMap)}, but also tells the section which NPC it belongs to.
     * Override this only if the section needs it (e.g. it opens a server-side menu for the
     * NPC); by default the context is ignored.
     */
    default NpcGuiSection create(NpcDataMap initial, NpcGuiContext context) {
        return create(initial);
    }
}