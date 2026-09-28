package com.talesforge.masternpc.npc.quest;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.talesforge.masternpc.MasterNPC;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.ApiStatus;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Открытый список видов целей — тот же паттерн, что NpcSettingFields: три встроенных вида
 * регистрируются через тот же register(...), которым аддон добавит свой ("дойди до репутации N
 * у фракции", "поговори с другим NPC").
 */
public final class NpcQuestObjectiveTypes {
    private static final Map<ResourceLocation, MapCodec<? extends NpcQuestObjective>> TYPES = new LinkedHashMap<>();
    private static boolean bootstrapped = false;

    public static final ResourceLocation COLLECT_ITEM_ID = id("collect_item");
    public static final ResourceLocation TRAVEL_ID = id("travel");
    public static final ResourceLocation KILL_ID = id("kill");

    private NpcQuestObjectiveTypes() {}

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MasterNPC.MOD_ID, path);
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
