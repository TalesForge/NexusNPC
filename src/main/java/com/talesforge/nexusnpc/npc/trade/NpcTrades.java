package com.talesforge.nexusnpc.npc.trade;

import com.mojang.serialization.Codec;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import java.util.ArrayList;
import java.util.List;

/** The trades of one NPC, in the order they are shown. Immutable: every change returns a new list. */
public record NpcTrades(List<TradeOffer> offers) {
    public static final NpcTrades EMPTY = new NpcTrades(List.of());
    /** Plenty for any shop; also bounds what one packet may carry. */
    public static final int MAX = 128;

    public static final Codec<NpcTrades> CODEC = TradeOffer.CODEC.listOf().xmap(NpcTrades::new, NpcTrades::offers);
    public static final StreamCodec<RegistryFriendlyByteBuf, NpcTrades> STREAM_CODEC =
            TradeOffer.STREAM_CODEC.apply(ByteBufCodecs.list(MAX)).map(NpcTrades::new, NpcTrades::offers);

    public int size() { return offers.size(); }

    public NpcTrades with(int index, TradeOffer offer) {
        List<TradeOffer> list = new ArrayList<>(offers);
        list.set(index, offer);
        return new NpcTrades(List.copyOf(list));
    }

    public NpcTrades plus(TradeOffer offer) {
        List<TradeOffer> list = new ArrayList<>(offers);
        list.add(offer);
        return new NpcTrades(List.copyOf(list));
    }

    /** A copy of the trade at {@code index} inserted right after it. */
    public NpcTrades duplicated(int index) {
        List<TradeOffer> list = new ArrayList<>(offers);
        list.add(index + 1, offers.get(index));
        return new NpcTrades(List.copyOf(list));
    }

    public NpcTrades without(int index) {
        List<TradeOffer> list = new ArrayList<>(offers);
        list.remove(index);
        return new NpcTrades(List.copyOf(list));
    }

    /** Swaps two trades (used to move one up or down). */
    public NpcTrades swapped(int a, int b) {
        List<TradeOffer> list = new ArrayList<>(offers);
        TradeOffer first = list.get(a);
        list.set(a, list.get(b));
        list.set(b, first);
        return new NpcTrades(List.copyOf(list));
    }
}
