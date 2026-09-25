package com.talesforge.masternpc.npc.model;

import net.minecraft.resources.ResourceLocation;

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
 * @param height      hitbox height.
 * @param eyeHeight   used for aiming/targeting calculations, independent of hitbox height.
 * @param renderScale uniform visual scale applied before rendering this model — geometry
 *                    is drawn at whatever size it was authored at, this just avoids every
 *                    addon model being stuck with the core humanoid's cosmetic 0.94 fudge.
 * @param defaultSkin fallback texture for this model — used when a skin id doesn't belong
 *                    to this model (wrong model switched to, addon uninstalled, corrupt data).
 */
public record NpcModelType(float width, float height, float eyeHeight, float renderScale, ResourceLocation defaultSkin) {
}
