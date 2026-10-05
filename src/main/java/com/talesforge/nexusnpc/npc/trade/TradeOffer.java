package com.talesforge.nexusnpc.npc.trade;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;

import java.util.Optional;

/** MerchantOffer does not have its own public codec — this is what is actually stored/edited. */
public record TradeOffer(ItemCost cost1, Optional<ItemCost> cost2, ItemStack result) {
    public static final Codec<TradeOffer> CODEC = RecordCodecBuilder.create(i -> i.group(
            ItemCost.CODEC.fieldOf("cost1").forGetter(TradeOffer::cost1),
            ItemCost.CODEC.optionalFieldOf("cost2").forGetter(TradeOffer::cost2),
            ItemStack.CODEC.fieldOf("result").forGetter(TradeOffer::result)
    ).apply(i, TradeOffer::new));

    /** For now, there’s no usage limit — I’ll leave the "per N trades" restriction for later. */
    public MerchantOffer toMerchantOffer() {
        return new MerchantOffer(cost1, cost2, result, Integer.MAX_VALUE, 0, 0.0f);
    }
}
