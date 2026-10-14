package com.talesforge.nexusnpc.network.payload.screen;

import com.talesforge.nexusnpc.NexusNPC;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Client to server: "open the inventory-and-equipment window of this NPC". Whether the player may, and what is in
 * there, is decided by the server.
 */
public record OpenNpcStoragePayload(int entityId) implements CustomPacketPayload {
    public static final Type<OpenNpcStoragePayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(NexusNPC.MOD_ID, "open_npc_storage"));

    public static final StreamCodec<FriendlyByteBuf, OpenNpcStoragePayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, OpenNpcStoragePayload::entityId,
            OpenNpcStoragePayload::new);

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
