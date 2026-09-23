package com.talesforge.masternpc.network.payload;

import com.talesforge.masternpc.MasterNPC;
import com.talesforge.masternpc.npc.field.NpcDataMap;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record OpenEditorPayload(int entityId, NpcDataMap settings) implements CustomPacketPayload {
    public static final Type<OpenEditorPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(MasterNPC.MOD_ID, "open_editor"));

    public static final StreamCodec<FriendlyByteBuf, OpenEditorPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, OpenEditorPayload::entityId,
            NpcDataMap.STREAM_CODEC, OpenEditorPayload::settings,
            OpenEditorPayload::new);

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}