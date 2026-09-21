package com.talesforge.masternpc;

import com.talesforge.masternpc.api.MasterNpcApi;
import com.talesforge.masternpc.api.NpcTypeEntry;
import com.talesforge.masternpc.entity.ModEntities;
import com.talesforge.masternpc.entity.client.NpcModel;
import com.talesforge.masternpc.entity.client.NpcRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.EntityRenderers;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

@Mod(value = MasterNPC.MOD_ID, dist = Dist.CLIENT)
@EventBusSubscriber(modid = MasterNPC.MOD_ID, value = Dist.CLIENT)
public class MasterNPCClient {
    public MasterNPCClient(ModContainer container) {
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
    }

    @SubscribeEvent
    static void registerLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(NpcModel.LAYER_LOCATION, NpcModel::createBodyLayer);
    }

    @SubscribeEvent
    static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        for (NpcTypeEntry entry : MasterNpcApi.types()) {
            if (entry.defaultRenderer()) {
                event.registerEntityRenderer(entry.type().get(), NpcRenderer::new);
            }
        }
    }
}
