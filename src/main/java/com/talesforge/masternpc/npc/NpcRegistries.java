package com.talesforge.masternpc.npc;

import com.talesforge.masternpc.MasterNPC;
import com.talesforge.masternpc.npc.attitude.NpcAttitudeType;
import com.talesforge.masternpc.npc.attitude.NpcAttitudes;
import com.talesforge.masternpc.npc.behavior.NpcBehaviorType;
import com.talesforge.masternpc.npc.behavior.NpcBehaviors;
import com.talesforge.masternpc.npc.model.NpcModelType;
import com.talesforge.masternpc.npc.model.NpcModels;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.registries.NewRegistryEvent;
import net.neoforged.neoforge.registries.RegistryBuilder;
import org.jetbrains.annotations.ApiStatus;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;

public final class NpcRegistries {
    public static final Registry<NpcBehaviorType> BEHAVIORS = new RegistryBuilder<>(
            ResourceKey.<NpcBehaviorType>createRegistryKey(id("behavior"))).sync(true).create();

    public static final Registry<NpcAttitudeType> ATTITUDES = new RegistryBuilder<>(
            ResourceKey.<NpcAttitudeType>createRegistryKey(id("attitude"))).sync(true).create();

    /**
     * Physical NPC models (hitbox + eye height + default skin) — common, server-safe data
     * only. The actual 3D geometry/animation for a model id is a SEPARATE, client-only
     * registration (see {@code client.model.NpcModelRenderers}); a dedicated server needs
     * this registry (e.g. for collision) but never touches that one.
     */
    public static final Registry<NpcModelType> MODELS = new RegistryBuilder<>(
            ResourceKey.<NpcModelType>createRegistryKey(id("model"))).sync(true).create();

    private NpcRegistries() {}

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MasterNPC.MOD_ID, path);
    }

    @ApiStatus.Internal
    public static void onNewRegistry(NewRegistryEvent event) {
        event.register(BEHAVIORS);
        event.register(ATTITUDES);
        event.register(MODELS);
    }

    // ===== Safe retrieval: an unknown id returns a default value. =====
    public static NpcBehaviorType behavior(ResourceLocation id) {
        NpcBehaviorType type = BEHAVIORS.get(id);
        return type != null ? type : BEHAVIORS.get(NpcBehaviors.DEFAULT_ID);
    }

    public static NpcAttitudeType attitude(ResourceLocation id) {
        NpcAttitudeType type = ATTITUDES.get(id);
        return type != null ? type : ATTITUDES.get(NpcAttitudes.DEFAULT_ID);
    }

    public static NpcModelType model(ResourceLocation id) {
        NpcModelType type = MODELS.get(id);
        return type != null ? type : MODELS.get(NpcModels.DEFAULT_ID);
    }

    /** id for the screen: the default value is first, the rest are in alphabetical order. */
    public static <T> List<ResourceLocation> ids(Registry<T> registry, ResourceLocation first, Predicate<T> filter) {
        List<ResourceLocation> list = new ArrayList<>();
        registry.keySet().stream()
                .filter(id -> filter.test(registry.get(id)))
                .sorted(Comparator.comparing((ResourceLocation id) -> !id.equals(first))
                        .thenComparing(ResourceLocation::toString))
                .forEach(list::add);
        return list;
    }
}
