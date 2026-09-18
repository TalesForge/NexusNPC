package com.talesforge.masternpc.event;

import com.talesforge.masternpc.MasterNPC;
import com.talesforge.masternpc.entity.ModEntities;
import com.talesforge.masternpc.entity.client.NpcModel;
import com.talesforge.masternpc.entity.custom.NpcEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;

@EventBusSubscriber(modid = MasterNPC.MOD_ID)
public class ModEventBusEvents {
    @SubscribeEvent
    public static void registerLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(NpcModel.LAYER_LOCATION, NpcModel::createBodyLayer);
    }

    @SubscribeEvent
    public static void registerAttributes(EntityAttributeCreationEvent event) {
        event.put(ModEntities.NPC.get(), NpcEntity.createAttributes().build());
    }
}
