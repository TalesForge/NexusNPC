package com.talesforge.nexusnpc.entity;

import com.talesforge.nexusnpc.NexusNPC;
import com.talesforge.nexusnpc.api.NexusNpcApi;
import com.talesforge.nexusnpc.api.NpcTypeProperties;
import com.talesforge.nexusnpc.entity.custom.NpcEntity;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES = DeferredRegister.create(BuiltInRegistries.ENTITY_TYPE, NexusNPC.MOD_ID);

    public static void register(IEventBus eventBus) { ENTITY_TYPES.register(eventBus); }


    public static final DeferredHolder<EntityType<?>, EntityType<NpcEntity>> NPC =
            NexusNpcApi.registerType(ENTITY_TYPES, "npc",
                    NpcEntity::new,
                    NpcTypeProperties.create()
                            .category(MobCategory.MISC)
                            .size(0.6F, 1.8F)
                            .eyeHeight(1.62F));
}
