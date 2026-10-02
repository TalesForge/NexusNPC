package com.talesforge.masternpc.network.payload.screen;

import com.talesforge.masternpc.MasterNPC;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Client -> server: "open the trade-layout menu for this NPC". Carries nothing but the
 * entity id on purpose: what the menu contains (the current trades) and whether this
 * player may open it at all are decided by the server.
 */
public record OpenTradeEditorPayload(int entityId) implements CustomPacketPayload {
    public static final Type<OpenTradeEditorPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(MasterNPC.MOD_ID, "open_trade_editor"));

    public static final StreamCodec<FriendlyByteBuf, OpenTradeEditorPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, OpenTradeEditorPayload::entityId,
            OpenTradeEditorPayload::new);

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
