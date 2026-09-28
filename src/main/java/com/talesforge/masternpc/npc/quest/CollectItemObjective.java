package com.talesforge.masternpc.npc.quest;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/** "Принеси N предметов X" — работает с любыми предметами, в т.ч. из других модов: это просто поиск по реестру. */
public record CollectItemObjective(Item item, int count) implements NpcQuestObjective {
    public static final MapCodec<CollectItemObjective> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            BuiltInRegistries.ITEM.byNameCodec().fieldOf("item").forGetter(CollectItemObjective::item),
            Codec.INT.fieldOf("count").forGetter(CollectItemObjective::count)
    ).apply(i, CollectItemObjective::new));

    @Override public ResourceLocation typeId() { return NpcQuestObjectiveTypes.COLLECT_ITEM_ID; }

    @Override
    public Component describe() {
        return Component.translatable("quest.masternpc.collect_item", count, new ItemStack(item).getHoverName());
    }

    @Override
    public boolean tryComplete(ServerPlayer player, int progress) {
        int have = 0;
        for (ItemStack stack : player.getInventory().items) if (stack.is(item)) have += stack.getCount();
        if (have < count) return false;

        int remaining = count;
        for (ItemStack stack : player.getInventory().items) {
            if (remaining <= 0) break;
            if (!stack.is(item)) continue;
            int take = Math.min(remaining, stack.getCount());
            stack.shrink(take);
            remaining -= take;
        }
        return true;
    }
}
