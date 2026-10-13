package com.talesforge.nexusnpc.npc.model;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * The physical properties of one NPC model — everything about it that the SERVER needs to
 * know, without touching a single client-only class (ModelPart, LayerDefinition, etc).
 * <p>
 * Registered exactly like {@code NpcAttitudeType}/{@code NpcBehaviorType}, through
 * {@code NpcRegistries.MODELS}. The actual 3D geometry for a model id is a completely
 * separate, client-only registration (see {@code client.model.NpcModelRenderers}) — a
 * dedicated server never needs to load it, but still needs THIS data to compute hitboxes,
 * eye height (for aiming/targeting), and to validate which skins belong to which model.
 *
 * @param width       hitbox width/depth (same convention as {@code EntityType.Builder#sized}).
 * @param height      the model's NORMAL height: hitbox height of an NPC that was not given another one.
 * @param eyeHeight   used for aiming/targeting calculations at the normal height; scaled with the NPC's height.
 * @param renderScale uniform visual scale applied before rendering this model — geometry
 *                    is drawn at whatever size it was authored at, this just avoids every
 *                    addon model being stuck with the core humanoid's cosmetic 0.94 fudge.
 * @param defaultSkin fallback texture for this model — used when a skin id doesn't belong
 *                    to this model (wrong model switched to, addon uninstalled, corrupt data).
 * @param minHeight   the shortest NPC of this model (equal to {@code height} when the model has a fixed height)
 * @param maxHeight   the tallest NPC of this model
 */
public record NpcModelType(float width, float height, float eyeHeight, float renderScale, ResourceLocation defaultSkin,
                           float minHeight, float maxHeight) {

    /** A model with ONE fixed height. Existing addon models keep working unchanged. */
    public NpcModelType(float width, float height, float eyeHeight, float renderScale, ResourceLocation defaultSkin) {
        this(width, height, eyeHeight, renderScale, defaultSkin, height, height);
    }

    /** True if NPCs of this model may have different heights (the model's client geometry must support it too). */
    public boolean supportsHeight() { return maxHeight > minHeight; }

    public float clampHeight(float requested) { return Mth.clamp(requested, minHeight, maxHeight); }

    /** Eye height of an NPC of the given height: the same fraction of it as at the normal height. */
    public float eyeHeightFor(float npcHeight) { return eyeHeight * npcHeight / height; }
}
