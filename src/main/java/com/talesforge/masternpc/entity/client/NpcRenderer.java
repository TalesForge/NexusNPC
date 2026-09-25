package com.talesforge.masternpc.entity.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.talesforge.masternpc.client.model.NpcModelRenderers;
import com.talesforge.masternpc.entity.custom.NpcEntity;
import com.talesforge.masternpc.npc.NpcRegistries;
import com.talesforge.masternpc.npc.model.NpcModels;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
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

    public NpcRenderer(EntityRendererProvider.Context context) {
        super(context, bake(context, requireDefault()), 0.5f);
        this.models = bakeAll(context);
        // super() already baked the default (humanoid) model into this.model — reuse it
        // as the fallback rather than baking the same layer a second time.
        this.fallback = this.model;
    }

    private static NpcModelRenderers.Factory requireDefault() {
        NpcModelRenderers.Factory factory = NpcModelRenderers.all().get(NpcModels.DEFAULT_ID);
        if (factory == null) {
            // Only possible if MasterNPCClient's own registration didn't run before this
            // renderer is constructed — a real bug, not something a broken addon could cause.
            throw new IllegalStateException("Default NPC model (" + NpcModels.DEFAULT_ID +
                    ") has no registered renderer — MasterNPCClient failed to register it?");
        }
        return factory;
    }

    private static Map<ResourceLocation, EntityModel<NpcEntity>> bakeAll(EntityRendererProvider.Context context) {
        Map<ResourceLocation, EntityModel<NpcEntity>> map = new HashMap<>();
        for (Map.Entry<ResourceLocation, NpcModelRenderers.Factory> entry : NpcModelRenderers.all().entrySet()) {
            map.put(entry.getKey(), bake(context, entry.getValue()));
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
        this.model = models.getOrDefault(entity.getModelId(), fallback);
        super.render(entity, entityYaw, partialTick, poseStack, buffer, packedLight);
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
