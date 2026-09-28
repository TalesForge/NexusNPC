package com.talesforge.masternpc.npc.quest;

import com.mojang.serialization.Codec;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

public record NpcQuests(List<NpcQuest> quests) {
    public static final NpcQuests EMPTY = new NpcQuests(List.of());
    public static final Codec<NpcQuests> CODEC = NpcQuest.CODEC.listOf().xmap(NpcQuests::new, NpcQuests::quests);

    public NpcQuest get(ResourceLocation id) {
        for (NpcQuest quest : quests) if (quest.id().equals(id)) return quest;
        return null;
    }
}
