package com.talesforge.nexusnpc.network.payload.screen;

import com.talesforge.nexusnpc.NexusNPC;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record OpenCreatorPayload(BlockPos pos) implements CustomPacketPayload {
    public static final Type<OpenCreatorPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(NexusNPC.MOD_ID, "open_creator"));

    public static final StreamCodec<FriendlyByteBuf, OpenCreatorPayload> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, OpenCreatorPayload::pos,
            OpenCreatorPayload::new);

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}