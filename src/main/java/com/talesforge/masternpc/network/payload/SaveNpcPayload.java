package com.talesforge.masternpc.network.payload;

import com.talesforge.masternpc.MasterNPC;
import com.talesforge.masternpc.npc.NpcSettings;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record SaveNpcPayload(int entityId, NpcSettings settings) implements CustomPacketPayload {
    public static final Type<SaveNpcPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(MasterNPC.MOD_ID, "save_npc"));

    public static final StreamCodec<FriendlyByteBuf, SaveNpcPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, SaveNpcPayload::entityId,
            NpcSettings.STREAM_CODEC, SaveNpcPayload::settings,
            SaveNpcPayload::new);

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}