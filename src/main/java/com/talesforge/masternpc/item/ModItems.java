package com.talesforge.masternpc.item;

import com.talesforge.masternpc.MasterNPC;
import com.talesforge.masternpc.block.ModBlocks;
import com.talesforge.masternpc.entity.ModEntities;
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
import java.util.function.Supplier;

public class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MasterNPC.MOD_ID);

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }

    // Registry: category -> list of DeferredItems
    private static final Map<ItemCategory, List<DeferredItem<? extends Item>>> CATEGORIZED = new EnumMap<>(ItemCategory.class);
    static {
        for (ItemCategory cat : ItemCategory.values()) {
            CATEGORIZED.put(cat, new ArrayList<>());
        }
    }

    // ===== Public access to categories =====
    public static List<DeferredItem<? extends Item>> getByCategory(ItemCategory category) {
        return Collections.unmodifiableList(CATEGORIZED.get(category));
    }


    // ===== Helpers for register =====
    // Items
    static <T extends Item> DeferredItem<T> registerItem(String name, ItemCategory category, Supplier<T> supplier) {
        DeferredItem<T> item = ITEMS.register(name, supplier);
        CATEGORIZED.get(category).add(item);
        return item;
    }
    static DeferredItem<Item> registerItem(String name, ItemCategory category, Item.Properties properties) {
        return registerItem(name, category, () -> new Item(properties));
    }
    static DeferredItem<BlockItem> registerItem(String name, ItemCategory category, DeferredBlock<Block> block, Item.Properties properties) {
        return registerItem(name, category, () -> new BlockItem(block.get(), properties));
    }




    // ===== ITEMS =====
    public static final DeferredItem<Item> EXAMPLE_ITEM = registerItem("example_item",
            ItemCategory.MISC,
            new Item.Properties()
                    .food(new FoodProperties.Builder()
                            .alwaysEdible()
                            .nutrition(1)
                            .saturationModifier(2f)
                            .build()
                    )
    );

    public static final DeferredItem<Item> STAFF_CONTROL = registerItem("staff_control",
            ItemCategory.TOOLS,
            new Item.Properties()
                    .stacksTo(1)
    );


    public static final DeferredItem<Item> NPC_SPAWN_EGG = registerItem("npc_spawn_egg",
            ItemCategory.MISC,
            () -> new DeferredSpawnEggItem(ModEntities.NPC, 0x31afaf, 0xffac00,
                    new Item.Properties())
    );



    // ===== BLOCKS ITEMS =====
    public static final DeferredItem<BlockItem> EXAMPLE_BLOCK_ITEM = registerItem("example_block",
            ItemCategory.BLOCKS,
            ModBlocks.EXAMPLE_BLOCK,
            new Item.Properties()
                    .stacksTo(64)
    );

}
