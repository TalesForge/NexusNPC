package com.talesforge.masternpc.npc.quest;

import com.mojang.serialization.Codec;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

public interface NpcQuestObjective {
    Codec<NpcQuestObjective> CODEC = ResourceLocation.CODEC.dispatch(
            "type", NpcQuestObjective::typeId, NpcQuestObjectiveTypes::get);

    ResourceLocation typeId();

    /** Строка статуса для диалога/журнала квестов. */
    Component describe();

    /** true — если QuestEvents должен вести счётчик прогресса (kill, travel); collect проверяется вживую при сдаче. */
    default boolean tracksProgress() { return false; }

    /**
     * Вызывается при попытке игрока сдать квест. progress — счётчик, который вёл QuestEvents
     * (0, если tracksProgress()==false). Возвращает true (и производит побочный эффект,
     * например забирает предметы), если цель выполнена прямо сейчас.
     */
    boolean tryComplete(ServerPlayer player, int progress);
}
