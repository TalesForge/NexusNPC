package com.talesforge.masternpc.api;

import com.talesforge.masternpc.entity.custom.NpcEntity;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;

import java.util.function.Supplier;

public record NpcTypeEntry(ResourceLocation id,
                           Supplier<? extends EntityType<? extends NpcEntity>> type,
                           Supplier<AttributeSupplier.Builder> attributes,
                           boolean defaultRenderer) {

}
