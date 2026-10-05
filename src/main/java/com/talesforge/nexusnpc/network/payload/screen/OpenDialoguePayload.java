package com.talesforge.nexusnpc.network.payload.screen;

import com.talesforge.nexusnpc.NexusNPC;
import com.talesforge.nexusnpc.npc.dialogue.DialoguePage;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record OpenDialoguePayload(int entityId, DialoguePage page) implements CustomPacketPayload {
    public static final Type<OpenDialoguePayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(NexusNPC.MOD_ID, "open_dialogue"));

    private static final StreamCodec<FriendlyByteBuf, DialoguePage> PAGE_STREAM_CODEC = StreamCodec.of(
            (buf, page) -> buf.writeNbt(DialoguePage.CODEC.encodeStart(NbtOps.INSTANCE, page).getOrThrow()),
            buf -> DialoguePage.CODEC.parse(NbtOps.INSTANCE, buf.readNbt()).getOrThrow());

    public static final StreamCodec<FriendlyByteBuf, OpenDialoguePayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, OpenDialoguePayload::entityId,
            PAGE_STREAM_CODEC, OpenDialoguePayload::page,
            OpenDialoguePayload::new);

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
