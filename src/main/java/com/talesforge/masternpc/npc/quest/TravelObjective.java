package com.talesforge.masternpc.npc.quest;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

/** "Дойди до места" — прогресс (0 или 1) ведёт QuestEvents по тику, не при сдаче: чтобы игрок мог уйти и сдать позже. */
public record TravelObjective(ResourceKey<Level> dimension, BlockPos target, double radius) implements NpcQuestObjective {
    public static final MapCodec<TravelObjective> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Level.RESOURCE_KEY_CODEC.fieldOf("dimension").forGetter(TravelObjective::dimension),
            BlockPos.CODEC.fieldOf("pos").forGetter(TravelObjective::target),
            Codec.DOUBLE.fieldOf("radius").forGetter(TravelObjective::radius)
    ).apply(i, TravelObjective::new));

    @Override public ResourceLocation typeId() { return NpcQuestObjectiveTypes.TRAVEL_ID; }
    @Override public boolean tracksProgress() { return true; }

    @Override
    public Component describe() {
        return Component.translatable("quest.masternpc.travel", target.getX(), target.getY(), target.getZ());
    }

    @Override
    public boolean tryComplete(ServerPlayer player, int progress) { return progress >= 1; }
}
