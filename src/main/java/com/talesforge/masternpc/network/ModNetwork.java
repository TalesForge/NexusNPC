package com.talesforge.masternpc.network;

import com.talesforge.masternpc.network.handler.ServerPayloadHandler;
import com.talesforge.masternpc.network.payload.*;
import com.talesforge.masternpc.network.handler.ClientPayloadHandler;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public class ModNetwork {
    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1"); // версия протокола

        registrar.playToClient(OpenEditorPayload.TYPE, OpenEditorPayload.STREAM_CODEC, ClientPayloadHandler::openEditor);
        registrar.playToClient(OpenCreatorPayload.TYPE, OpenCreatorPayload.STREAM_CODEC, ClientPayloadHandler::openCreator);

        registrar.playToServer(SaveNpcPayload.TYPE, SaveNpcPayload.STREAM_CODEC, ServerPayloadHandler::saveNpc);
        registrar.playToServer(CreateNpcPayload.TYPE, CreateNpcPayload.STREAM_CODEC, ServerPayloadHandler::createNpc);
        registrar.playToServer(EditorStatusPayload.TYPE, EditorStatusPayload.STREAM_CODEC, ServerPayloadHandler::editorStatus);
    }
}
