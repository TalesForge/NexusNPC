package com.talesforge.masternpc;

import com.talesforge.masternpc.config.Config;
import com.talesforge.masternpc.entity.ModEntities;
import com.talesforge.masternpc.item.ModItems;
import com.talesforge.masternpc.item.ModCreativeModeTabs;
import com.talesforge.masternpc.network.ModNetwork;
import com.talesforge.masternpc.npc.NpcRegistries;
import com.talesforge.masternpc.npc.attitude.NpcAttitudes;
import com.talesforge.masternpc.npc.behavior.NpcBehaviors;
import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;

@Mod(MasterNPC.MOD_ID)
public class MasterNPC {
    public static final String MOD_ID = "masternpc";
    public static final Logger LOGGER = LogUtils.getLogger();

    public MasterNPC(IEventBus modEventBus, ModContainer modContainer) {
        modEventBus.addListener(ModNetwork::register);

        NpcBehaviors.REGISTER.register(modEventBus);
        NpcAttitudes.REGISTER.register(modEventBus);
        modEventBus.addListener(NpcRegistries::onNewRegistry);

        ModItems.register(modEventBus);
        ModEntities.register(modEventBus);
        ModCreativeModeTabs.register(modEventBus);

        NeoForge.EVENT_BUS.register(this);

        modEventBus.addListener(this::addCreative);

        // Game settings live in the world, so this is a SERVER‑config.
        modContainer.registerConfig(ModConfig.Type.SERVER, Config.SPEC);
    }

    // Add item to vanilla creative tab
    private void addCreative(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) {
            event.accept(ModItems.STAFF_CONTROL);
        }
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        LOGGER.info("MasterNPC ready!");
    }
}
