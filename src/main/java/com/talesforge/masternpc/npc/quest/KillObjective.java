package com.talesforge.masternpc.npc.quest;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;

/** "Убей N X" — ванильный моб, моб из другого мода или другой NPC MasterNPC — это всё просто id EntityType. */
public record KillObjective(EntityType<?> entityType, int count) implements NpcQuestObjective {
    public static final MapCodec<KillObjective> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            BuiltInRegistries.ENTITY_TYPE.byNameCodec().fieldOf("entity").forGetter(KillObjective::entityType),
            Codec.INT.fieldOf("count").forGetter(KillObjective::count)
    ).apply(i, KillObjective::new));

    @Override public ResourceLocation typeId() { return NpcQuestObjectiveTypes.KILL_ID; }
    @Override public boolean tracksProgress() { return true; }

    @Override
    public Component describe() {
        return Component.translatable("quest.masternpc.kill", count, Component.translatable(entityType.getDescriptionId()));
    }

    @Override
    public boolean tryComplete(ServerPlayer player, int progress) { return progress >= count; }
}
