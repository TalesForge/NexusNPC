package com.talesforge.nexusnpc;

import com.talesforge.nexusnpc.compat.rpg.NexusRpgCompat;
import com.talesforge.nexusnpc.config.Config;
import com.talesforge.nexusnpc.entity.ModEntities;
import com.talesforge.nexusnpc.item.ModItems;
import com.talesforge.nexusnpc.item.ModCreativeModeTabs;
import com.talesforge.nexusnpc.menu.ModMenus;
import com.talesforge.nexusnpc.network.ModNetwork;
import com.talesforge.nexusnpc.npc.NpcRegistries;
import com.talesforge.nexusnpc.npc.attitude.NpcAttitudes;
import com.talesforge.nexusnpc.npc.behavior.NpcBehaviors;
import com.talesforge.nexusnpc.npc.field.NpcSettingFields;
import com.talesforge.nexusnpc.npc.model.NpcModels;
import com.talesforge.nexusnpc.npc.quest.ModAttachments;
import com.talesforge.nexusnpc.npc.quest.NpcQuestObjectiveTypes;
import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import net.minecraft.world.item.CreativeModeTabs;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.ModContainer;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;

@Mod(NexusNPC.MOD_ID)
public class NexusNPC {
    public static final String MOD_ID = "nexusnpc";
    public static final Logger LOGGER = LogUtils.getLogger();

    public NexusNPC(IEventBus modEventBus, ModContainer modContainer) {
        NpcSettingFields.bootstrap();  // Must run before any addon relies on the field list being populated
        NexusRpgCompat.bootstrap();  // faction / classes / team fields (only if NexusRPG is installed)
        NpcQuestObjectiveTypes.bootstrap();

        modEventBus.addListener(ModNetwork::register);

        NpcBehaviors.REGISTER.register(modEventBus);
        NpcAttitudes.REGISTER.register(modEventBus);
        NpcModels.REGISTER.register(modEventBus);
        modEventBus.addListener(NpcRegistries::onNewRegistry);

        ModItems.register(modEventBus);
        ModEntities.register(modEventBus);
        ModMenus.register(modEventBus);
        ModAttachments.register(modEventBus);
        ModCreativeModeTabs.register(modEventBus);

        NeoForge.EVENT_BUS.register(this);

        modEventBus.addListener(this::addCreative);

        // Game settings live in the world, so this is a SERVER‑config.
        modContainer.registerConfig(ModConfig.Type.SERVER, Config.SPEC);
    }

    // Add item to vanilla creative tab
    private void addCreative(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) {
            event.accept(ModItems.CONTROL_STAFF);
        }
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        LOGGER.info("NexusNPC ready!");
    }
}
