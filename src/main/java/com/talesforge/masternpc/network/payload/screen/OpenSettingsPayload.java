package com.talesforge.masternpc.network.payload.screen;

import com.talesforge.masternpc.MasterNPC;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record OpenSettingsPayload() implements CustomPacketPayload {
    public static final Type<OpenSettingsPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(MasterNPC.MOD_ID, "open_settings"));

    public static final StreamCodec<FriendlyByteBuf, OpenSettingsPayload> STREAM_CODEC = StreamCodec.unit(
            new OpenSettingsPayload());

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
