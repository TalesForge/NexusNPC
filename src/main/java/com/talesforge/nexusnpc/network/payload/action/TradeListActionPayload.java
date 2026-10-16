package com.talesforge.nexusnpc.network.payload.action;

import com.talesforge.nexusnpc.NexusNPC;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Client to server: a button of the trade list (move, duplicate, delete). The server answers with the refreshed list. */
public record TradeListActionPayload(int entityId, int action, int index) implements CustomPacketPayload {
    public static final int MOVE_UP = 0, MOVE_DOWN = 1, DUPLICATE = 2, DELETE = 3;

    public static final Type<TradeListActionPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(NexusNPC.MOD_ID, "trade_list_action"));

    public static final StreamCodec<FriendlyByteBuf, TradeListActionPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, TradeListActionPayload::entityId,
            ByteBufCodecs.VAR_INT, TradeListActionPayload::action,
            ByteBufCodecs.VAR_INT, TradeListActionPayload::index,
            TradeListActionPayload::new);

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
