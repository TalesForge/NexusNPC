package com.talesforge.nexusnpc.item;

import com.talesforge.nexusnpc.NexusNPC;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModCreativeModeTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, NexusNPC.MOD_ID);

    public static void register(IEventBus eventBus) {
        CREATIVE_MODE_TABS.register(eventBus);
    }


    // Creates a creative tab for the mod items
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> NPC_TAB = CREATIVE_MODE_TABS.register("npc_tab", () -> CreativeModeTab.builder()
            .title(Component.translatable("creativetab.nexusnpc.all"))
            .icon(() -> ModItems.CONTROL_STAFF.get().getDefaultInstance())
            .withTabsBefore(CreativeModeTabs.COMBAT)
            .displayItems((parameters, output) -> {
                // TOOLS
                output.accept(ModItems.CONTROL_STAFF.get());
                output.accept(ModItems.GEAR_SETTINGS.get());

                // EGGS
                output.accept(ModItems.NPC_SPAWN_EGG.get());
            })
            .build()
    );

}
