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

    private static final Map<ItemCategory, List<DeferredItem<? extends Item>>> CATEGORIZED = new EnumMap<>(ItemCategory.class);
    static {
        for (ItemCategory cat : ItemCategory.values()) {
            CATEGORIZED.put(cat, new ArrayList<>());
        }
    }


    public static List<DeferredItem<? extends Item>> getByCategory(ItemCategory category) {
        return Collections.unmodifiableList(CATEGORIZED.get(category));
    }


    static <T extends Item> DeferredItem<T> regItem(String name, ItemCategory category,
                                                         Function<Item.Properties, T> factory,
                                                         Item.Properties properties) {
        DeferredItem<T> item = ITEMS.registerItem(name, factory, properties);
        CATEGORIZED.get(category).add(item);
        return item;
    }
    static <T extends Item> DeferredItem<T> regItem(String name, ItemCategory category, Supplier<T> supplier) {
        DeferredItem<T> item = ITEMS.register(name, supplier);
        CATEGORIZED.get(category).add(item);
        return item;
    }
    static DeferredItem<Item> regItem(String name, ItemCategory category, Item.Properties properties) {
        return regItem(name, category, () -> new Item(properties));
    }
    static DeferredItem<BlockItem> regItem(String name, ItemCategory category, DeferredBlock<Block> block, Item.Properties properties) {
        return regItem(name, category, () -> new BlockItem(block.get(), properties));
    }


    // ===== ITEMS =====
    public static final DeferredItem<Item> STAFF_CONTROL = regItem("staff_control",
            ItemCategory.TOOLS,
            StaffControlItem::new,
            new Item.Properties()
                    .stacksTo(1)
    );

    public static final DeferredItem<Item> NPC_SPAWN_EGG = regItem("npc_spawn_egg",
            ItemCategory.MISC,
            () -> new DeferredSpawnEggItem(ModEntities.NPC, 0x31afaf, 0xffac00,
                    new Item.Properties())
    );

}
