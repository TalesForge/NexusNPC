package com.talesforge.nexusnpc.network.handler;

import com.talesforge.nexusnpc.client.gui.EditorSession;
import com.talesforge.nexusnpc.client.gui.screen.DialogueScreen;
import com.talesforge.nexusnpc.client.gui.screen.settings.NpcEditorScreen;
import com.talesforge.nexusnpc.client.gui.screen.settings.SettingsScreen;
import com.talesforge.nexusnpc.client.gui.section.NpcGuiRegistry;
import com.talesforge.nexusnpc.entity.ModEntities;
import com.talesforge.nexusnpc.network.payload.action.CreateNpcPayload;
import com.talesforge.nexusnpc.network.payload.action.SaveNpcPayload;
import com.talesforge.nexusnpc.network.payload.screen.OpenCreatorPayload;
import com.talesforge.nexusnpc.network.payload.screen.OpenSettingsPayload;
import com.talesforge.nexusnpc.network.payload.screen.OpenDialoguePayload;
import com.talesforge.nexusnpc.network.payload.screen.OpenEditorPayload;
import com.talesforge.nexusnpc.npc.data.NpcAiMode;
import com.talesforge.nexusnpc.npc.field.NpcDataMap;
import com.talesforge.nexusnpc.npc.field.NpcSettingFields;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public class ClientPayloadHandler {
    public static void openEditor(OpenEditorPayload payload, IPayloadContext context) {
        ResourceLocation typeId = resolveEntityType(payload.entityId());
        Screen screen = NpcGuiRegistry.screenOverride(typeId)
                .<Screen>map(factory -> factory.create(
                        Component.translatable("gui.nexusnpc.editor.title"),
                        payload.settings(), false, payload.entityId(), typeId,
                        (type, s) -> PacketDistributor.sendToServer(new SaveNpcPayload(payload.entityId(), s))))
                .orElseGet(() -> new NpcEditorScreen(
                        Component.translatable("gui.nexusnpc.editor.title"),
                        // Autosaving session: every change is sent to the server as the player makes it
                        EditorSession.open(payload.entityId(), typeId, payload.settings()),
                        (type, s) -> {}));
        Minecraft.getInstance().setScreen(screen);
    }

    public static void openCreator(OpenCreatorPayload payload, IPayloadContext context) {
        // Nothing is spawned yet, so there's no live entity to ask — start from the default
        // type. If the player picks a different one in the type dropdown, NpcEditorScreen
        // itself rebuilds its section list (see its type CycleButton), it just can't switch
        // to a *screen override* mid-flow — an addon with its own full creation screen for a
        // type other than the default should send the player straight to it another way
        // (e.g. its own spawn-egg-like item) rather than through the generic "create" flow.
        ResourceLocation typeId = ModEntities.NPC.getId();
        Screen screen = NpcGuiRegistry.screenOverride(typeId)
                .<Screen>map(factory -> factory.create(
                        Component.translatable("gui.nexusnpc.creator.title"),
                        creatorDefaults(), true, -1, typeId,
                        (type, s) -> PacketDistributor.sendToServer(new CreateNpcPayload(payload.pos(), type, s))))
                .orElseGet(() -> new NpcEditorScreen(
                        Component.translatable("gui.nexusnpc.creator.title"),
                        EditorSession.forCreation(typeId, creatorDefaults()),
                        (type, s) -> PacketDistributor.sendToServer(new CreateNpcPayload(payload.pos(), type, s))));
        Minecraft.getInstance().setScreen(screen);
    }

    /** New NPCs of NexusNPC's own types keep the classic behavior: their attitude/behavior goals drive them. */
    private static NpcDataMap creatorDefaults() {
        NpcDataMap defaults = NpcDataMap.defaults();
        defaults.put(NpcSettingFields.AI_MODE, NpcAiMode.OVERRIDE);
        return defaults;
    }

    public static void openSettings(OpenSettingsPayload payload, IPayloadContext context) {
        ResourceLocation typeId = ModEntities.NPC.getId();
        Minecraft.getInstance().setScreen(new SettingsScreen(NpcDataMap.defaults(), typeId));
    }


    /** The NPC is already visible client-side (the player just interacted with it), so we can read its real type straight off it. */
    private static ResourceLocation resolveEntityType(int entityId) {
        Level level = Minecraft.getInstance().level;
        Entity entity = level != null ? level.getEntity(entityId) : null;
        if (entity != null) {
            ResourceLocation id = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
            if (id != null) return id;
        }
        return ModEntities.NPC.getId();  // Defensive fallback, shouldn't normally happen
    }


    public static void openDialogue(OpenDialoguePayload payload, IPayloadContext context) {
        Minecraft.getInstance().setScreen(new DialogueScreen(payload.entityId(), payload.page()));
    }
}