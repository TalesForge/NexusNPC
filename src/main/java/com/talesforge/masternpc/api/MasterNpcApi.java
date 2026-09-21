package com.talesforge.masternpc.api;

import com.talesforge.masternpc.entity.custom.NpcEntity;
import com.talesforge.masternpc.npc.NpcSettings;
import com.talesforge.masternpc.npc.NpcSkins;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public final class MasterNpcApi {
    // Constructors of mods work in parallel, so the collections are thread‑safe
    private static final List<NpcTypeEntry> TYPES = new CopyOnWriteArrayList<>();

    private MasterNpcApi() {}

    /** Register the NPC type. Call the mod in the constructor. The attributes and the standard renderer will be enabled automatically. */
    public static <T extends NpcEntity> DeferredHolder<EntityType<?>, EntityType<T>> registerType(
            DeferredRegister<EntityType<?>> register, String name,
            EntityType.EntityFactory<T> factory, NpcTypeProperties props) {

        DeferredHolder<EntityType<?>, EntityType<T>> holder = register.register(name, () ->
                EntityType.Builder.of(factory, props.category)
                        .sized(props.width, props.height)
                        .eyeHeight(props.eyeHeight)
                        .build(name));

        TYPES.add(new NpcTypeEntry(
                ResourceLocation.fromNamespaceAndPath(register.getNamespace(), name),
                holder, props.attributes, props.defaultRenderer));
        return holder;
    }

    /** Add the skin to the selection list. Call in the constructor of the mod, both on the client and on the server. */
    public static void registerSkin(ResourceLocation texture) {
        NpcSkins.add(texture);
    }

    public static List<NpcTypeEntry> types() {
        return Collections.unmodifiableList(TYPES);
    }

    /** A sorted list of IDs so that the order does not depend on the loading order of the mods. */
    public static List<ResourceLocation> typeIds() {
        return TYPES.stream().map(NpcTypeEntry::id)
                .sorted(Comparator.comparing(ResourceLocation::toString)).toList();
    }

    /** Spawn an NPC by type id. Returns null if the type is not registered via the API. */
    @Nullable
    public static NpcEntity spawn(ResourceLocation typeId, ServerLevel level, Vec3 pos,
                                  float yRot, NpcSettings settings) {
        NpcTypeEntry entry = TYPES.stream().filter(e -> e.id().equals(typeId)).findFirst().orElse(null);
        if (entry == null) return null;

        NpcEntity npc = entry.type().get().create(level);
        if (npc == null) return null;

        npc.moveTo(pos.x, pos.y, pos.z, yRot, 0.0F);
        npc.applySettings(settings);
        level.addFreshEntity(npc);
        return npc;
    }
}
