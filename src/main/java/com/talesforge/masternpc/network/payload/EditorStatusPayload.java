package com.talesforge.masternpc.network.payload;

import com.talesforge.masternpc.MasterNPC;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record EditorStatusPayload(int entityId, boolean closed) implements CustomPacketPayload {
    public static final Type<EditorStatusPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(MasterNPC.MOD_ID, "editor_status"));

    public static final StreamCodec<FriendlyByteBuf, EditorStatusPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, EditorStatusPayload::entityId,
            ByteBufCodecs.BOOL, EditorStatusPayload::closed,
            EditorStatusPayload::new);

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
