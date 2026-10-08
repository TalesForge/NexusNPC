package com.talesforge.nexusnpc.npc;

import com.talesforge.nexusnpc.config.Config;
import net.minecraft.core.Holder;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;

/**
 * Single-attribute helpers for the stat fields. Safe on any mob: a vanilla mob that lacks an attribute
 * (a cow has no ATTACK_DAMAGE) reads the fallback and ignores writes instead of throwing. Attributes
 * are added to most types by {@code ModEventBusEvents#extendAttributes} so this is rarely hit.
 */
public final class NpcStats {
    private NpcStats() {}

    public static double base(Mob mob, Holder<Attribute> attribute, double fallback) {
        AttributeInstance instance = mob.getAttribute(attribute);
        return instance != null ? instance.getBaseValue() : fallback;
    }

    public static void setMaxHealth(Mob mob, double value) {
        setBase(mob, Attributes.MAX_HEALTH, Mth.clamp(value, 1.0, Config.MAX_HEALTH_LIMIT.get()));
    }

    public static void setDamage(Mob mob, double value) {
        setBase(mob, Attributes.ATTACK_DAMAGE, Mth.clamp(value, 0.0, Config.MAX_DAMAGE_LIMIT.get()));
    }

    public static void setSpeed(Mob mob, double value) {
        setBase(mob, Attributes.MOVEMENT_SPEED, Mth.clamp(value, 0.05, 1.0));
    }

    private static void setBase(Mob mob, Holder<Attribute> attribute, double value) {
        AttributeInstance instance = mob.getAttribute(attribute);
        if (instance != null) instance.setBaseValue(value);
    }
}
