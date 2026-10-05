package com.talesforge.nexusnpc.npc.trade;

import com.mojang.serialization.Codec;

import java.util.List;

public record NpcTrades(List<TradeOffer> offers) {
    public static final NpcTrades EMPTY = new NpcTrades(List.of());
    public static final Codec<NpcTrades> CODEC = TradeOffer.CODEC.listOf().xmap(NpcTrades::new, NpcTrades::offers);
}
