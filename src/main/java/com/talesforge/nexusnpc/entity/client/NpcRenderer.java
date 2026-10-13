package com.talesforge.nexusnpc.entity.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.talesforge.nexusnpc.client.model.NpcModelRenderers;
import com.talesforge.nexusnpc.entity.custom.NpcEntity;
import com.talesforge.nexusnpc.npc.NpcRegistries;
import com.talesforge.nexusnpc.npc.model.HeightProfile;
import com.talesforge.nexusnpc.npc.model.NpcModelType;
import com.talesforge.nexusnpc.npc.model.NpcModels;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * One renderer instance handles EVERY NPC model — core and addon alike. {@code MobRenderer}
 * is built around a single fixed model type, so instead this bakes every model registered
 * through {@link NpcModelRenderers} up front, and swaps the live {@code this.model} field
 * for whichever one the specific NPC being drawn actually has, right before delegating to
 * the normal render path. This is the same "swap the model field before rendering" pattern
 * several vanilla-adjacent mods already use for shapeshifting/variant mobs — but if a future
 * Minecraft version changes {@code LivingEntityRenderer.model} to be non-reassignable, this
 * is the one place that would need to change (e.g. to a per-model sub-renderer dispatch).
 */
public class NpcRenderer extends MobRenderer<NpcEntity, EntityModel<NpcEntity>> {
    private final Map<ResourceLocation, EntityModel<NpcEntity>> models;
    private final EntityModel<NpcEntity> fallback;
    private final Map<ResourceLocation, NpcModelRenderers.Factory> factories = NpcModelRenderers.all();

    /** Models built for one specific height, kept so a frame never rebuilds geometry (an NPC's height rarely changes). */
    private record HeightKey(ResourceLocation model, HeightProfile profile) {}
    private final Map<HeightKey, EntityModel<NpcEntity>> heightModels = new LinkedHashMap<>(16, 0.75F, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<HeightKey, EntityModel<NpcEntity>> eldest) {
            return size() > 128;
        }
    };

    public NpcRenderer(EntityRendererProvider.Context context) {
        super(context, bake(context, requireDefault()), 0.5f);
        this.fallback = this.model;
        this.models = bakeAll(context, this.fallback);
    }

    private static NpcModelRenderers.Factory requireDefault() {
        NpcModelRenderers.Factory factory = NpcModelRenderers.all().get(NpcModels.DEFAULT_ID);
        if (factory == null) {
            // Only possible if NexusNPCClient's own registration didn't run before this
            // renderer is constructed — a real bug, not something a broken addon could cause.
            throw new IllegalStateException("Default NPC model (" + NpcModels.DEFAULT_ID +
                    ") has no registered renderer — NexusNPCClient failed to register it?");
        }
        return factory;
    }

    private static Map<ResourceLocation, EntityModel<NpcEntity>> bakeAll(EntityRendererProvider.Context context,
                                                                         EntityModel<NpcEntity> defaultModel) {
        Map<ResourceLocation, EntityModel<NpcEntity>> map = new HashMap<>();
        for (Map.Entry<ResourceLocation, NpcModelRenderers.Factory> entry : NpcModelRenderers.all().entrySet()) {
            if (entry.getKey().equals(NpcModels.DEFAULT_ID)) {
                map.put(entry.getKey(), defaultModel);  // We reuse the already created default model.
            } else {
                map.put(entry.getKey(), bake(context, entry.getValue()));
            }
        }
        return map;
    }

    private static EntityModel<NpcEntity> bake(EntityRendererProvider.Context context, NpcModelRenderers.Factory factory) {
        return factory.bake(context.bakeLayer(factory.layerLocation()));
    }

    @Override
    public void render(NpcEntity entity, float entityYaw, float partialTick,
                       PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        // Every render call for every NPC goes through here first, so this.model always
        // reflects the entity actually about to be drawn by the time super.render() reads it.
        this.model = modelFor(entity);
        super.render(entity, entityYaw, partialTick, poseStack, buffer, packedLight);
    }

    /**
     * The model for this NPC: the shared one for its model id, or — when the NPC has another height than the model's
     * normal one and the model can be built for it — a model whose limbs and torso are lengthened to match (no scaling).
     */
    private EntityModel<NpcEntity> modelFor(NpcEntity entity) {
        ResourceLocation id = entity.getModelId();
        EntityModel<NpcEntity> shared = models.getOrDefault(id, fallback);

        NpcModelRenderers.Factory factory = factories.get(id);
        NpcModelType type = NpcRegistries.model(id);
        if (factory == null || !factory.supportsHeight() || !type.supportsHeight()) return shared;

        HeightProfile profile = HeightProfile.of(type, entity.getNpcHeight());
        if (profile.isNone()) return shared;

        return heightModels.computeIfAbsent(new HeightKey(id, profile),
                key -> factory.bake(factory.createBodyLayer(profile).bakeRoot()));
    }

    @Override
    protected void scale(NpcEntity entity, PoseStack poseStack, float partialTick) {
        float scale = NpcRegistries.model(entity.getModelId()).renderScale();
        poseStack.scale(scale, scale, scale);
    }

    @Override
    public ResourceLocation getTextureLocation(NpcEntity entity) {
        return entity.getSkinTexture();
    }
}
