package com.talesforge.nexusnpc.network.payload.action;

import com.talesforge.nexusnpc.NexusNPC;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Client to server: the player closed the dialogue window, the NPC may walk off again. */
public record DialogueClosePayload(int entityId) implements CustomPacketPayload {
    public static final Type<DialogueClosePayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(NexusNPC.MOD_ID, "dialogue_close"));

    public static final StreamCodec<FriendlyByteBuf, DialogueClosePayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, DialogueClosePayload::entityId,
            DialogueClosePayload::new);

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
