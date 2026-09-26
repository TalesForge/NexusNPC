package com.talesforge.masternpc.item;

import com.talesforge.masternpc.MasterNPC;
import com.talesforge.masternpc.entity.ModEntities;
import com.talesforge.masternpc.item.custom.StaffControlItem;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.DeferredSpawnEggItem;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.*;
import java.util.function.Function;
import java.util.function.Supplier;

public class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MasterNPC.MOD_ID);

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }


    static <T extends Item> DeferredItem<T> regItem(String name,
                                                         Function<Item.Properties, T> factory,
                                                         Item.Properties properties) {
        DeferredItem<T> item = ITEMS.registerItem(name, factory, properties);
        return item;
    }
    static <T extends Item> DeferredItem<T> regItem(String name, Supplier<T> supplier) {
        DeferredItem<T> item = ITEMS.register(name, supplier);
        return item;
    }
    static DeferredItem<Item> regItem(String name, Item.Properties properties) {
        return regItem(name, () -> new Item(properties));
    }
    static DeferredItem<BlockItem> regItem(String name, DeferredBlock<Block> block, Item.Properties properties) {
        return regItem(name, () -> new BlockItem(block.get(), properties));
    }


    // ===== ITEMS =====
    public static final DeferredItem<Item> STAFF_CONTROL = regItem("staff_control",
            StaffControlItem::new,
            new Item.Properties()
                    .stacksTo(1)
    );

    public static final DeferredItem<Item> NPC_SPAWN_EGG = regItem("npc_spawn_egg",
            () -> new DeferredSpawnEggItem(ModEntities.NPC, 0x31afaf, 0xffac00,
                    new Item.Properties())
    );

}
