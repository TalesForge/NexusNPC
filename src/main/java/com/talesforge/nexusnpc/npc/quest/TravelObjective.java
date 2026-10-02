package com.talesforge.nexusnpc.npc.quest;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

/** "Reach the location" — progress (0 or 1) is tracked by QuestEvents on a tick basis, not upon completion: so that the player can leave and complete it later. */
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
        return Component.translatable("quest.nexusnpc.travel", target.getX(), target.getY(), target.getZ());
    }

    @Override
    public boolean tryComplete(ServerPlayer player, int progress) { return progress >= 1; }
}
