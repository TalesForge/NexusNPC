package com.talesforge.nexusnpc.npc.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.talesforge.nexusnpc.npc.attitude.NpcAttitudes;
import com.talesforge.nexusnpc.npc.behavior.NpcBehaviors;
import com.talesforge.nexusnpc.npc.dialogue.NpcDialogue;
import com.talesforge.nexusnpc.npc.quest.NpcQuests;
import com.talesforge.nexusnpc.npc.trade.NpcTrades;
import net.minecraft.resources.ResourceLocation;

/**
 * Everything NexusNPC persists on a {@code Mob} that has been turned into an NPC. Stored as a
 * data attachment ({@code ModAttachments.NPC_DATA}), so it is saved with the entity's NBT for
 * ANY mob type — vanilla or modded — without that mob's class knowing anything about it.
 * <p>
 * A mob "is an NPC" exactly when it has this attachment. Attitude / behavior ids are stored raw
 * and validated against the registries on read (see {@code Npcs#attitudeId}), so a removed addon
 * can never break loading.
 */
public record NpcData(NpcDialogue dialogue,
                      NpcQuests quests,
                      NpcTrades trades,
                      ResourceLocation attitude,
                      ResourceLocation behavior,
                      NpcAiMode aiMode,
                      NpcTradeMode tradeMode,
                      boolean dropItems,
                      boolean ageLock) {

    public static final Codec<NpcData> CODEC = RecordCodecBuilder.create(i -> i.group(
            NpcDialogue.CODEC.optionalFieldOf("dialogue", NpcDialogue.EMPTY).forGetter(NpcData::dialogue),
            NpcQuests.CODEC.optionalFieldOf("quests", NpcQuests.EMPTY).forGetter(NpcData::quests),
            NpcTrades.CODEC.optionalFieldOf("trades", NpcTrades.EMPTY).forGetter(NpcData::trades),
            ResourceLocation.CODEC.optionalFieldOf("attitude", NpcAttitudes.DEFAULT_ID).forGetter(NpcData::attitude),
            ResourceLocation.CODEC.optionalFieldOf("behavior", NpcBehaviors.DEFAULT_ID).forGetter(NpcData::behavior),
            NpcAiMode.CODEC.optionalFieldOf("ai_mode", NpcAiMode.VANILLA).forGetter(NpcData::aiMode),
            NpcTradeMode.CODEC.optionalFieldOf("trade_mode", NpcTradeMode.INFINITE).forGetter(NpcData::tradeMode),
            Codec.BOOL.optionalFieldOf("drop_items", false).forGetter(NpcData::dropItems),
            Codec.BOOL.optionalFieldOf("age_lock", false).forGetter(NpcData::ageLock)
    ).apply(i, NpcData::new));

    public static NpcData create(NpcAiMode mode) {
        return new NpcData(NpcDialogue.EMPTY, NpcQuests.EMPTY, NpcTrades.EMPTY,
                NpcAttitudes.DEFAULT_ID, NpcBehaviors.DEFAULT_ID, mode, NpcTradeMode.INFINITE, false, false);
    }

    public NpcData withDialogue(NpcDialogue v) { return new NpcData(v, quests, trades, attitude, behavior, aiMode, tradeMode, dropItems, ageLock); }
    public NpcData withQuests(NpcQuests v) { return new NpcData(dialogue, v, trades, attitude, behavior, aiMode, tradeMode, dropItems, ageLock); }
    public NpcData withTrades(NpcTrades v) { return new NpcData(dialogue, quests, v, attitude, behavior, aiMode, tradeMode, dropItems, ageLock); }
    public NpcData withAttitude(ResourceLocation v) { return new NpcData(dialogue, quests, trades, v, behavior, aiMode, tradeMode, dropItems, ageLock); }
    public NpcData withBehavior(ResourceLocation v) { return new NpcData(dialogue, quests, trades, attitude, v, aiMode, tradeMode, dropItems, ageLock); }
    public NpcData withAiMode(NpcAiMode v) { return new NpcData(dialogue, quests, trades, attitude, behavior, v, tradeMode, dropItems, ageLock); }
    public NpcData withTradeMode(NpcTradeMode v) { return new NpcData(dialogue, quests, trades, attitude, behavior, aiMode, v, dropItems, ageLock); }
    public NpcData withAgeLock(boolean v) { return new NpcData(dialogue, quests, trades, attitude, behavior, aiMode, tradeMode, dropItems, v); }
    public NpcData withDropItems(boolean v) { return new NpcData(dialogue, quests, trades, attitude, behavior, aiMode, tradeMode, v, ageLock); }
}
