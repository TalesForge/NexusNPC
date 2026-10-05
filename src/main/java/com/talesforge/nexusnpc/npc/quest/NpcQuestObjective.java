package com.talesforge.nexusnpc.npc.quest;

import com.mojang.serialization.Codec;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

public interface NpcQuestObjective {
    Codec<NpcQuestObjective> CODEC = ResourceLocation.CODEC.dispatch(
            "type", NpcQuestObjective::typeId, NpcQuestObjectiveTypes::get);

    ResourceLocation typeId();

    /** Status bar for the dialogue/quest journal. */
    Component describe();

    /** true — if QuestEvents should maintain a progress counter (kill, travel); collect is checked in real time upon submission. */
    default boolean tracksProgress() { return false; }

    /**
     * This is called when the player attempts to complete the quest.
     * progress — a counter that kept track of QuestEvents (0 if tracksProgress()==false).
     * Returns true (and performs a side effect, for example, takes items) if the objective is completed right now.
     */
    boolean tryComplete(ServerPlayer player, int progress);
}
