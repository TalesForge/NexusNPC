package com.talesforge.masternpc.client.gui.section.core;

import com.talesforge.masternpc.MasterNPC;
import com.talesforge.masternpc.client.gui.element.ValueSlider;
import com.talesforge.masternpc.client.gui.section.NpcGuiSection;
import com.talesforge.masternpc.client.gui.section.NpcGuiSectionFactory;
import com.talesforge.masternpc.npc.field.NpcDataMap;
import com.talesforge.masternpc.npc.field.NpcSettingFields;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.function.Consumer;

/**
 * Owns NpcSettingFields.MAX_HEALTH / DAMAGE / SPEED. A good example of a section an addon
 * would fully disable for its own NPC type — e.g. a shopkeeper NPC that never fights and
 * shouldn't expose combat stats at all: {@code NpcGuiRegistry.disable(SHOPKEEPER_TYPE_ID, StatsSection.ID)}.
 */
public final class StatsSection implements NpcGuiSection {
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(MasterNPC.MOD_ID, "stats");

    private double maxHealth, damage, speed;

    private StatsSection(NpcDataMap initial) {
        this.maxHealth = initial.get(NpcSettingFields.MAX_HEALTH);
        this.damage = initial.get(NpcSettingFields.DAMAGE);
        this.speed = initial.get(NpcSettingFields.SPEED);
    }

    @Override
    public int build(int x, int y, int width, Font font, Consumer<AbstractWidget> addWidget, Runnable requestRebuild) {
        addWidget.accept(new ValueSlider(x, y, width, 20, Component.translatable("gui.masternpc.health"),
                1, 100, maxHealth, v -> this.maxHealth = v));
        addWidget.accept(new ValueSlider(x, y + 24, width, 20, Component.translatable("gui.masternpc.damage"),
                0, 20, damage, v -> this.damage = v));
        addWidget.accept(new ValueSlider(x, y + 48, width, 20, Component.translatable("gui.masternpc.speed"),
                0.05, 0.6, speed, 0.05, v -> this.speed = v));
        return 72;
    }

    @Override
    public void collect(NpcDataMap out) {
        out.put(NpcSettingFields.MAX_HEALTH, maxHealth);
        out.put(NpcSettingFields.DAMAGE, damage);
        out.put(NpcSettingFields.SPEED, speed);
    }

    public static final class Factory implements NpcGuiSectionFactory {
        @Override public ResourceLocation id() { return ID; }
        @Override public NpcGuiSection create(NpcDataMap initial) { return new StatsSection(initial); }
    }
}
