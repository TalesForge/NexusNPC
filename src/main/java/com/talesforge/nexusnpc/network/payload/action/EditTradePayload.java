package com.talesforge.nexusnpc.network.payload.action;

import com.talesforge.nexusnpc.NexusNPC;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Client to server: open the editor of the trade at {@code index} (-1 = a new trade). */
public record EditTradePayload(int entityId, int index) implements CustomPacketPayload {
    public static final Type<EditTradePayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(NexusNPC.MOD_ID, "edit_trade"));

    public static final StreamCodec<FriendlyByteBuf, EditTradePayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, EditTradePayload::entityId,
            ByteBufCodecs.VAR_INT, EditTradePayload::index,
            EditTradePayload::new);

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
