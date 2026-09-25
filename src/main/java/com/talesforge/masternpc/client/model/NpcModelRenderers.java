package com.talesforge.masternpc.client.model;

import com.talesforge.masternpc.entity.custom.NpcEntity;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.resources.ResourceLocation;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * Client-only counterpart to {@code NpcModelType}: the actual 3D geometry for a model id.
 * MasterNPC registers its own base model(s) here; an addon registers exactly the same way
 * for as many models of its own as it wants — there's no limit and no special-casing
 * between "core" and "addon" models, same philosophy as {@code NpcSettingFields}.
 * <p>
 * This is intentionally plain, hand-written Java models (the normal, well-supported
 * Blockbench → Java workflow every Minecraft mod uses) — NOT a dynamic JSON geometry
 * loader. A true data-only "no Java code at all" model format is a reasonable future step
 * on top of this registry, but it's a separate, much larger undertaking (figuring out a
 * safe/well-tested resource-reload hook to bake LayerDefinitions from JSON at the right
 * point in the client lifecycle) — this registry doesn't block that work, it's simply
 * scoped out of it for now.
 */
public final class NpcModelRenderers {
    /** One entry: how to bake this model's layer, and how to wrap a baked ModelPart into a renderable EntityModel. */
    public interface Factory {
        ModelLayerLocation layerLocation();
        LayerDefinition createBodyLayer();
        EntityModel<NpcEntity> bake(ModelPart root);
    }

    private static final Map<ResourceLocation, Factory> FACTORIES = new LinkedHashMap<>();

    private NpcModelRenderers() {}

    /** Call from your mod's client-side constructor / client setup, before RegisterLayerDefinitions fires. */
    public static void register(ResourceLocation modelId, Factory factory) {
        if (FACTORIES.putIfAbsent(modelId, factory) != null) {
            throw new IllegalStateException("Duplicate NPC model renderer for id: " + modelId);
        }
    }

    /** Convenience overload for the common case: a single ModelLayerLocation + createBodyLayer + constructor reference. */
    public static void register(ResourceLocation modelId, ModelLayerLocation layer,
                                java.util.function.Supplier<LayerDefinition> createBodyLayer,
                                Function<ModelPart, EntityModel<NpcEntity>> bake) {
        register(modelId, new Factory() {
            public ModelLayerLocation layerLocation() { return layer; }
            public LayerDefinition createBodyLayer() { return createBodyLayer.get(); }
            public EntityModel<NpcEntity> bake(ModelPart root) { return bake.apply(root); }
        });
    }

    public static Map<ResourceLocation, Factory> all() {
        return Map.copyOf(FACTORIES);
    }
}
