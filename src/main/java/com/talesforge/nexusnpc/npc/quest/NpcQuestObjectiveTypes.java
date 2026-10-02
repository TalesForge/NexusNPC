package com.talesforge.nexusnpc.npc.quest;

import com.mojang.serialization.MapCodec;
import com.talesforge.nexusnpc.NexusNPC;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.ApiStatus;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The open list of target types follows the same pattern as NpcSettingFields:
 * three built‑in types are registered via the same register(...) method,
 * which the addon will use to add its own
 * ("reach reputation level N with the faction", "talk to another NPC").
 */
public final class NpcQuestObjectiveTypes {
    private static final Map<ResourceLocation, MapCodec<? extends NpcQuestObjective>> TYPES = new LinkedHashMap<>();
    private static boolean bootstrapped = false;

    public static final ResourceLocation COLLECT_ITEM_ID = id("collect_item");
    public static final ResourceLocation TRAVEL_ID = id("travel");
    public static final ResourceLocation KILL_ID = id("kill");

    private NpcQuestObjectiveTypes() {}

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(NexusNPC.MOD_ID, path);
    }

    public static synchronized void register(ResourceLocation id, MapCodec<? extends NpcQuestObjective> codec) {
        if (TYPES.putIfAbsent(id, codec) != null) {
            throw new IllegalStateException("Duplicate quest objective type id: " + id);
        }
    }

    public static synchronized MapCodec<? extends NpcQuestObjective> get(ResourceLocation id) {
        MapCodec<? extends NpcQuestObjective> codec = TYPES.get(id);
        if (codec == null) throw new IllegalArgumentException("Unknown quest objective type: " + id);
        return codec;
    }

    @ApiStatus.Internal
    public static synchronized void bootstrap() {
        if (bootstrapped) return;
        bootstrapped = true;
        register(COLLECT_ITEM_ID, CollectItemObjective.CODEC);
        register(TRAVEL_ID, TravelObjective.CODEC);
        register(KILL_ID, KillObjective.CODEC);
    }
}
