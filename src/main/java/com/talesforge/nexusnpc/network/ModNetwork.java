package com.talesforge.nexusnpc.network;

import com.talesforge.nexusnpc.network.handler.ServerPayloadHandler;
import com.talesforge.nexusnpc.network.payload.*;
import com.talesforge.nexusnpc.network.handler.ClientPayloadHandler;
import com.talesforge.nexusnpc.network.payload.action.CreateNpcPayload;
import com.talesforge.nexusnpc.network.payload.action.DeleteNpcPayload;
import com.talesforge.nexusnpc.network.payload.action.EditorStatusPayload;
import com.talesforge.nexusnpc.network.payload.action.SaveNpcPayload;
import com.talesforge.nexusnpc.network.payload.screen.*;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public class ModNetwork {
    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");

        registrar.playToClient(OpenEditorPayload.TYPE, OpenEditorPayload.STREAM_CODEC, ClientPayloadHandler::openEditor);
        registrar.playToClient(OpenCreatorPayload.TYPE, OpenCreatorPayload.STREAM_CODEC, ClientPayloadHandler::openCreator);
        registrar.playToClient(OpenDialoguePayload.TYPE, OpenDialoguePayload.STREAM_CODEC, ClientPayloadHandler::openDialogue);
        registrar.playToClient(OpenSettingsPayload.TYPE, OpenSettingsPayload.STREAM_CODEC, ClientPayloadHandler::openSettings);

        registrar.playToServer(SaveNpcPayload.TYPE, SaveNpcPayload.STREAM_CODEC, ServerPayloadHandler::saveNpc);
        registrar.playToServer(CreateNpcPayload.TYPE, CreateNpcPayload.STREAM_CODEC, ServerPayloadHandler::createNpc);
        registrar.playToServer(DeleteNpcPayload.TYPE, DeleteNpcPayload.STREAM_CODEC, ServerPayloadHandler::deleteNpc);
        registrar.playToServer(EditorStatusPayload.TYPE, EditorStatusPayload.STREAM_CODEC, ServerPayloadHandler::editorStatus);
        registrar.playToServer(DialogueActionPayload.TYPE, DialogueActionPayload.STREAM_CODEC, ServerPayloadHandler::dialogueAction);
        registrar.playToServer(OpenTradeEditorPayload.TYPE, OpenTradeEditorPayload.STREAM_CODEC, ServerPayloadHandler::openTradeEditor);
    }
}
