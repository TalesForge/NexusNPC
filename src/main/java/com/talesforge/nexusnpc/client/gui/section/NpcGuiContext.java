package com.talesforge.nexusnpc.client.gui.section;

import com.talesforge.nexusnpc.client.gui.EditorSession;
import com.talesforge.nexusnpc.npc.field.NpcDataMap;
import net.minecraft.resources.ResourceLocation;

/**
 * What a section may need to know about the screen it lives in. A record, so more
 * fields can be added later without another signature change.
 *
 * @param entityId the NPC being edited, or -1 while creating (nothing exists yet)
 * @param creating true for the "create NPC" flow
 * @param typeId   the NPC entity type being edited/created
 * @param session  where a section writes its values: {@code session.set(FIELD, value)} the moment a widget changes.
 *                 For an existing NPC that is also what saves them to the server, immediately.
 */
public record NpcGuiContext(int entityId, boolean creating, ResourceLocation typeId, EditorSession session) {

    /** For callers that have no session of their own: values are kept locally and never sent. */
    public NpcGuiContext(int entityId, boolean creating, ResourceLocation typeId) {
        this(entityId, creating, typeId, EditorSession.local(NpcDataMap.defaults()));
    }
}
