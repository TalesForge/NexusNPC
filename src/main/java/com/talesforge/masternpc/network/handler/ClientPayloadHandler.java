package com.talesforge.masternpc.network.handler;

import com.talesforge.masternpc.client.gui.NpcEditorScreen;
import com.talesforge.masternpc.client.gui.section.NpcGuiRegistry;
import com.talesforge.masternpc.entity.ModEntities;
import com.talesforge.masternpc.npc.field.NpcDataMap;
import com.talesforge.masternpc.network.payload.CreateNpcPayload;
import com.talesforge.masternpc.network.payload.OpenCreatorPayload;
import com.talesforge.masternpc.network.payload.OpenEditorPayload;
import com.talesforge.masternpc.network.payload.SaveNpcPayload;
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
                        Component.translatable("gui.masternpc.editor.title"),
                        payload.settings(), false, payload.entityId(), typeId,
                        (type, s) -> PacketDistributor.sendToServer(new SaveNpcPayload(payload.entityId(), s))))
                .orElseGet(() -> new NpcEditorScreen(
                        Component.translatable("gui.masternpc.editor.title"),
                        payload.settings(), false, payload.entityId(), typeId,
                        (type, s) -> PacketDistributor.sendToServer(new SaveNpcPayload(payload.entityId(), s))));
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
                        Component.translatable("gui.masternpc.creator.title"),
                        NpcDataMap.defaults(), true, -1, typeId,
                        (type, s) -> PacketDistributor.sendToServer(new CreateNpcPayload(payload.pos(), type, s))))
                .orElseGet(() -> new NpcEditorScreen(
                        Component.translatable("gui.masternpc.creator.title"),
                        NpcDataMap.defaults(), true, -1, typeId,
                        (type, s) -> PacketDistributor.sendToServer(new CreateNpcPayload(payload.pos(), type, s))));
        Minecraft.getInstance().setScreen(screen);
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
}