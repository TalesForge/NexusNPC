package com.talesforge.nexusnpc.api;

import com.talesforge.nexusnpc.entity.custom.NpcEntity;
import com.talesforge.nexusnpc.npc.Npcs;
import com.talesforge.nexusnpc.npc.field.NpcDataMap;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.neoforged.neoforge.event.EventHooks;
import com.talesforge.nexusnpc.npc.model.NpcModelSkins;
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

public final class NexusNpcApi {
    // Constructors of mods work in parallel, so the collections are thread‑safe
    private static final List<NpcTypeEntry> TYPES = new CopyOnWriteArrayList<>();

    private NexusNpcApi() {}

    /** Register the NPC type. Call the mod in the constructor. The attributes and the standard renderer will be enabled automatically. */
    public static <T extends NpcEntity> DeferredHolder<EntityType<?>, EntityType<T>> registerType(
            DeferredRegister<EntityType<?>> register, String name,
            EntityType.EntityFactory<T> factory, NpcTypeProperties props) {

        return registerType(register, name, factory, true, props);
    }
    public static <T extends NpcEntity> DeferredHolder<EntityType<?>, EntityType<T>> registerType(
            DeferredRegister<EntityType<?>> register, String name,
            EntityType.EntityFactory<T> factory, boolean isPossibleCreateUsingStaff,
            NpcTypeProperties props) {

        DeferredHolder<EntityType<?>, EntityType<T>> holder = register.register(name, () ->
                EntityType.Builder.of(factory, props.category)
                        .sized(props.width, props.height)
                        .eyeHeight(props.eyeHeight)
                        .build(name));

        TYPES.add(new NpcTypeEntry(
                ResourceLocation.fromNamespaceAndPath(register.getNamespace(), name),
                holder, props.attributes, props.defaultRenderer, isPossibleCreateUsingStaff));
        return holder;
    }

    /**
     * Add a texture to the selection list for one specific model — a skin only makes sense
     * for the model it was painted for (different UV layout/texture size). Call in the
     * mod's constructor, both on the client and on the server.
     */
    public static void registerSkin(ResourceLocation modelId, ResourceLocation texture) {
        NpcModelSkins.add(modelId, texture);
    }

    public static List<NpcTypeEntry> types() {
        return Collections.unmodifiableList(TYPES);
    }

    /** A sorted list of IDs so that the order does not depend on the loading order of the mods. */
    public static List<ResourceLocation> typeIds() {
        return TYPES.stream()
                .filter(NpcTypeEntry::isPossibleCreateUsingStaff)
                .map(NpcTypeEntry::id)
                .sorted(Comparator.comparing(ResourceLocation::toString))
                .toList();
    }

//    /** A sorted list of IDs that can be created using the staff. */
//    public static List<ResourceLocation> staffCreatableTypeIds() {
//        return TYPES.stream()
//                .filter(NpcTypeEntry::isPossibleCreateUsingStaff)
//                .map(NpcTypeEntry::id)
//                .sorted(Comparator.comparing(ResourceLocation::toString))
//                .toList();
//    }

    /**
     * Spawn an NPC by entity type id. The type can be one registered through {@link #registerType} OR any
     * ordinary mob type (e.g. {@code minecraft:cow}) that the config allows. Returns null if the id is
     * unknown, is not a mob, or is not allowed to become an NPC.
     */
    @Nullable
    public static Mob spawn(ResourceLocation typeId, ServerLevel level, Vec3 pos,
                            float yRot, NpcDataMap settings) {
        EntityType<?> type = TYPES.stream().filter(e -> e.id().equals(typeId))
                .<EntityType<?>>map(e -> e.type().get()).findFirst()
                .orElseGet(() -> BuiltInRegistries.ENTITY_TYPE.getOptional(typeId).orElse(null));
        if (type == null || !Npcs.isEligible(type)) return null;

        Entity created = type.create(level);
        if (!(created instanceof Mob mob)) {
            if (created != null) created.discard();
            return null;
        }

        mob.moveTo(pos.x, pos.y, pos.z, yRot, 0.0F);
        if (!(mob instanceof NpcEntity)) {
            // Ordinary mobs get their normal spawn setup (equipment, variants, baby chance...)
            EventHooks.finalizeMobSpawn(mob, level, level.getCurrentDifficultyAt(mob.blockPosition()), MobSpawnType.COMMAND, null);
        }
        applySettings(mob, settings, true);
        level.addFreshEntity(mob);
        return mob;
    }

    // ===== Working with ANY mob =====

    /** True if the entity is a mob that currently carries NPC data. */
    public static boolean isNpc(@Nullable Entity entity) { return Npcs.isNpc(entity); }

    /** Turn an ordinary mob into an NPC (idempotent). Server side. */
    public static void makeNpc(Mob mob) { Npcs.enable(mob); }

    /** Strip every NPC feature from the mob, leaving an ordinary one. Server side. */
    public static void removeNpc(Mob mob) { Npcs.disable(mob); }

    /** Snapshot of every registered setting field that applies to this mob — core AND addon fields alike. */
    public static NpcDataMap getSettings(Mob mob) { return NpcDataMap.capture(mob); }

    /**
     * Apply settings (server only). Also turns the mob into an NPC if it is not one yet. Iterates every registered
     * field present in {@code data}, so an addon field arrives here exactly like a core one.
     *
     * @param heal whether to heal the mob to its (new) maximum afterwards (true when created).
     */
    public static void applySettings(Mob mob, NpcDataMap data, boolean heal) { data.applyAll(mob, heal); }
}
