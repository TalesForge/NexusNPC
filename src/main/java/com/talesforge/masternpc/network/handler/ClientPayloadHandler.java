package com.talesforge.masternpc.network.handler;

import com.talesforge.masternpc.client.gui.NpcEditorScreen;
import com.talesforge.masternpc.npc.field.NpcDataMap;
import com.talesforge.masternpc.network.payload.CreateNpcPayload;
import com.talesforge.masternpc.network.payload.OpenCreatorPayload;
import com.talesforge.masternpc.network.payload.OpenEditorPayload;
import com.talesforge.masternpc.network.payload.SaveNpcPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public class ClientPayloadHandler {
    public static void openEditor(OpenEditorPayload payload, IPayloadContext context) {
        Minecraft.getInstance().setScreen(new NpcEditorScreen(
                Component.translatable("gui.masternpc.editor.title"),
                payload.settings(), false, payload.entityId(),
                (type, s) -> PacketDistributor.sendToServer(new SaveNpcPayload(payload.entityId(), s))
        ));
    }

    public static void openCreator(OpenCreatorPayload payload, IPayloadContext context) {
        Minecraft.getInstance().setScreen(new NpcEditorScreen(
                Component.translatable("gui.masternpc.creator.title"),
                NpcDataMap.defaults(), true, -1,
                (type, s) -> PacketDistributor.sendToServer(new CreateNpcPayload(payload.pos(), type, s))
        ));
    }
}