package com.talesforge.masternpc.network.payload;

import com.talesforge.masternpc.MasterNPC;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record DeleteNpcPayload(int entityId) implements CustomPacketPayload {
    public static final Type<DeleteNpcPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(MasterNPC.MOD_ID, "delete_npc"));

    public static final StreamCodec<FriendlyByteBuf, DeleteNpcPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, DeleteNpcPayload::entityId,
            DeleteNpcPayload::new);

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
