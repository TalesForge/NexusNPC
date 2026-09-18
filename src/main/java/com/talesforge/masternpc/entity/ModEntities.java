package com.talesforge.masternpc.entity;

import com.talesforge.masternpc.MasterNPC;
import com.talesforge.masternpc.entity.custom.NpcEntity;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES = DeferredRegister.create(BuiltInRegistries.ENTITY_TYPE, MasterNPC.MOD_ID);

    public static void register(IEventBus eventBus) { ENTITY_TYPES.register(eventBus); }


    public static final Supplier<EntityType<NpcEntity>> NPC = ENTITY_TYPES.register("npc",
            () -> EntityType.Builder.of(NpcEntity::new, MobCategory.CREATURE)
                    .sized(1f, 1.8f)
                    .build("npc")
    );
}
