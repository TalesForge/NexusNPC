package com.talesforge.masternpc.client.gui.section;

import com.talesforge.masternpc.npc.field.NpcDataMap;
import net.minecraft.resources.ResourceLocation;

/**
 * Registered once, at mod-load time, through {@link NpcGuiRegistry#register}.
 * Produces a new {@link NpcGuiSection} instance every time an editor screen is opened.
 */
public interface NpcGuiSectionFactory {
    /** Unique id, e.g. {@code masternpc:stats} or {@code mymod:shop_inventory}. Used for disable(...). */
    ResourceLocation id();

    /** initial holds the NPC's current settings (editing) or field defaults (creating). */
    NpcGuiSection create(NpcDataMap initial);
}