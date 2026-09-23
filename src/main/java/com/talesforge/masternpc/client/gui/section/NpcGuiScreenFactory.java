package com.talesforge.masternpc.client.gui.section;

import com.talesforge.masternpc.npc.field.NpcDataMap;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.function.BiConsumer;

/**
 * Full replacement for the standard section-based editor screen, for an addon whose NPC
 * editing needs go beyond "a few extra fields" — a completely custom layout, a wizard,
 * a totally different interaction model, etc. Registered per NPC entity type through
 * {@link NpcGuiRegistry#overrideScreen}.
 */
public interface NpcGuiScreenFactory {
    /**
     * @param initial    current settings (editing) or field defaults (creating)
     * @param creating   true for the "create NPC" flow, false for "edit existing NPC"
     * @param entityId   the NPC's entity id, or -1 when creating (nothing to lock/ping yet)
     * @param typeId     the NPC entity type being edited/created
     * @param onConfirm  call with (typeId, dataMap) to send the create/save packet — the
     *                   typeId you pass back only matters while creating (it selects which
     *                   entity type gets spawned); while editing it is ignored.
     */
    Screen create(Component title, NpcDataMap initial, boolean creating, int entityId,
                  ResourceLocation typeId, BiConsumer<ResourceLocation, NpcDataMap> onConfirm);
}