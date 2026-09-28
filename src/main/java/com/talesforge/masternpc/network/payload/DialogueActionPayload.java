package com.talesforge.masternpc.network.payload;

import com.talesforge.masternpc.MasterNPC;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record DialogueActionPayload(int entityId, String pageId, int optionIndex) implements CustomPacketPayload {
    public static final Type<DialogueActionPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(MasterNPC.MOD_ID, "dialogue_action"));

    public static final StreamCodec<FriendlyByteBuf, DialogueActionPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, DialogueActionPayload::entityId,
            ByteBufCodecs.STRING_UTF8, DialogueActionPayload::pageId,
            ByteBufCodecs.VAR_INT, DialogueActionPayload::optionIndex,
            DialogueActionPayload::new);

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
