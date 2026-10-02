package com.talesforge.masternpc.client.gui;

import com.talesforge.masternpc.MasterNPC;
import com.talesforge.masternpc.network.payload.action.EditorStatusPayload;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Keeps the NPC's editing lock alive for the whole editor session, including while a child
 * screen (dialogues, ...) is open and NpcEditorScreen itself is not ticking.
 */
@EventBusSubscriber(modid = MasterNPC.MOD_ID, value = Dist.CLIENT)
public final class EditorKeepAlive {
    private static int entityId = -1;
    private static int timer = 0;

    private EditorKeepAlive() {}

    public static void start(int id) { entityId = id; timer = 0; }
    public static void stop() { entityId = -1; }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        if (entityId < 0) return;
        Minecraft mc = Minecraft.getInstance();

        if (mc.getConnection() == null || !(mc.screen instanceof NpcEditingScreen)) {
            // None of our screens is showing anymore: the session is really over.
            // Tell the server RIGHT NOW instead of waiting for the 5-second server-side timeout.
            int id = entityId;
            boolean connected = mc.getConnection() != null;
            stop();
            if (connected) PacketDistributor.sendToServer(new EditorStatusPayload(id, true));
            return;
        }

        if (++timer >= 20) {
            timer = 0;
            PacketDistributor.sendToServer(new EditorStatusPayload(entityId, false));
        }
    }
}
