package com.talesforge.masternpc;

import com.talesforge.masternpc.api.MasterNpcApi;
import com.talesforge.masternpc.api.NpcTypeEntry;
import com.talesforge.masternpc.client.gui.section.NpcGuiRegistry;
import com.talesforge.masternpc.client.model.NpcModelRenderers;
import com.talesforge.masternpc.entity.client.NpcModel;
import com.talesforge.masternpc.entity.client.NpcRenderer;
import com.talesforge.masternpc.npc.model.NpcModels;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

@Mod(value = MasterNPC.MOD_ID, dist = Dist.CLIENT)
@EventBusSubscriber(modid = MasterNPC.MOD_ID, value = Dist.CLIENT)
public class MasterNPCClient {
    public MasterNPCClient(ModContainer container) {
        NpcGuiRegistry.bootstrap();  // Must run before any addon relies on the core sections being registered

        // The core humanoid model registers itself through the SAME registry an addon
        // would use — no special-casing between "core" and "addon" models (see NpcModelRenderers).
        NpcModelRenderers.register(NpcModels.HUMANOID_ID, NpcModel.LAYER_LOCATION,
                NpcModel::createBodyLayer, root -> new NpcModel<>(root));

        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
    }

    @SubscribeEvent
    static void registerLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        // Every registered model gets its layer defined here — core and addon alike, and in
        // whatever order mod constructors ran in (all constructors run before this event fires).
        for (NpcModelRenderers.Factory factory : NpcModelRenderers.all().values()) {
            event.registerLayerDefinition(factory.layerLocation(), factory::createBodyLayer);
        }
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
