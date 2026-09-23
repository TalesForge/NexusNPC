package com.talesforge.masternpc.network.payload;

import com.talesforge.masternpc.MasterNPC;
import com.talesforge.masternpc.npc.field.NpcDataMap;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record CreateNpcPayload(BlockPos pos, ResourceLocation typeId, NpcDataMap settings)
        implements CustomPacketPayload {
    public static final Type<CreateNpcPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(MasterNPC.MOD_ID, "create_npc"));

    public static final StreamCodec<FriendlyByteBuf, CreateNpcPayload> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, CreateNpcPayload::pos,
            ResourceLocation.STREAM_CODEC, CreateNpcPayload::typeId,
            NpcDataMap.STREAM_CODEC, CreateNpcPayload::settings,
            CreateNpcPayload::new);

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}