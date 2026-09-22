package com.talesforge.masternpc.event;

import com.talesforge.masternpc.MasterNPC;
import com.talesforge.masternpc.api.MasterNpcApi;
import com.talesforge.masternpc.api.NpcTypeEntry;
import com.talesforge.masternpc.entity.client.NpcModel;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;

@EventBusSubscriber(modid = MasterNPC.MOD_ID)
public class ModEventBusEvents {
    @SubscribeEvent
    public static void registerAttributes(EntityAttributeCreationEvent event) {
        for (NpcTypeEntry entry : MasterNpcApi.types()) {
            event.put(entry.type().get(), entry.attributes().get().build());
        }
    }
}
