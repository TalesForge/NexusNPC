package com.talesforge.nexusnpc.client.gui.section;

import net.minecraft.resources.ResourceLocation;

/**
 * What a section may need to know about the screen it lives in. A record, so more
 * fields can be added later without another signature change.
 *
 * @param entityId the NPC being edited, or -1 while creating (nothing exists yet)
 * @param creating true for the "create NPC" flow
 * @param typeId   the NPC entity type being edited/created
 */
public record NpcGuiContext(int entityId, boolean creating, ResourceLocation typeId) {}
