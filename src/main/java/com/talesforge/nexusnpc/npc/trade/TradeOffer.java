package com.talesforge.nexusnpc.npc.trade;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.MerchantOffer;

/**
 * One trade of an NPC: up to two payments, one result, and for each payment the rule that says what counts as that item.
 * Everything is stored as complete item stacks, so the data components of an item (names, enchantments, whatever a mod
 * keeps on it) are never lost. The second payment is an empty stack when there is none.
 */
public record TradeOffer(ItemStack cost1, ItemStack cost2, ItemStack result, PaymentRule rule1, PaymentRule rule2) {
    public static final Codec<TradeOffer> CODEC = RecordCodecBuilder.create(i -> i.group(
            ItemStack.CODEC.fieldOf("cost1").forGetter(TradeOffer::cost1),
            ItemStack.OPTIONAL_CODEC.optionalFieldOf("cost2", ItemStack.EMPTY).forGetter(TradeOffer::cost2),
            ItemStack.CODEC.fieldOf("result").forGetter(TradeOffer::result),
            PaymentRule.CODEC.optionalFieldOf("rule1", PaymentRule.DEFAULT).forGetter(TradeOffer::rule1),
            PaymentRule.CODEC.optionalFieldOf("rule2", PaymentRule.DEFAULT).forGetter(TradeOffer::rule2)
    ).apply(i, TradeOffer::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, TradeOffer> STREAM_CODEC = StreamCodec.composite(
            ItemStack.STREAM_CODEC, TradeOffer::cost1,
            ItemStack.OPTIONAL_STREAM_CODEC, TradeOffer::cost2,
            ItemStack.STREAM_CODEC, TradeOffer::result,
            PaymentRule.STREAM_CODEC, TradeOffer::rule1,
            PaymentRule.STREAM_CODEC, TradeOffer::rule2,
            TradeOffer::new);

    public boolean hasSecondPayment() { return !cost2.isEmpty(); }

    /** An unlimited offer. */
    public MerchantOffer toMerchantOffer() {
        return toMerchantOffer(Integer.MAX_VALUE);
    }

    /** An offer that can be taken {@code maxUses} times; 0 shows as sold out. */
    public MerchantOffer toMerchantOffer(int maxUses) {
        return new NpcMerchantOffer(this, maxUses);
    }
}
