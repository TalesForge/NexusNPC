package com.talesforge.masternpc.npc.quest;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/** One quest from the NPC: goal + what to give upon successful completion. */
public record NpcQuest(ResourceLocation id, Component title, NpcQuestObjective objective, ItemStack reward) {
    public static final Codec<NpcQuest> CODEC = RecordCodecBuilder.create(i -> i.group(
            ResourceLocation.CODEC.fieldOf("id").forGetter(NpcQuest::id),
            ComponentSerialization.CODEC.fieldOf("title").forGetter(NpcQuest::title),
            NpcQuestObjective.CODEC.fieldOf("objective").forGetter(NpcQuest::objective),
            ItemStack.OPTIONAL_CODEC.fieldOf("reward").forGetter(NpcQuest::reward)
    ).apply(i, NpcQuest::new));
}
