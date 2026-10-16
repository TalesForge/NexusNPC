package com.talesforge.nexusnpc.network.payload.screen;

import com.talesforge.nexusnpc.NexusNPC;
import com.talesforge.nexusnpc.npc.trade.NpcTrades;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Server to client: the trades of an NPC, to show (or refresh) the trade list. The list lives on the server — it holds
 * real items, which the client could not rebuild from the editor's settings.
 */
public record OpenTradeListPayload(int entityId, NpcTrades trades) implements CustomPacketPayload {
    public static final Type<OpenTradeListPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(NexusNPC.MOD_ID, "open_trade_list"));

    public static final StreamCodec<RegistryFriendlyByteBuf, OpenTradeListPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, OpenTradeListPayload::entityId,
            NpcTrades.STREAM_CODEC, OpenTradeListPayload::trades,
            OpenTradeListPayload::new);

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
