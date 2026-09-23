package com.talesforge.masternpc.client.gui.section.core;

import com.talesforge.masternpc.MasterNPC;
import com.talesforge.masternpc.client.gui.section.NpcGuiSection;
import com.talesforge.masternpc.client.gui.section.NpcGuiSectionFactory;
import com.talesforge.masternpc.npc.NpcRegistries;
import com.talesforge.masternpc.npc.attitude.NpcAttitudeType;
import com.talesforge.masternpc.npc.attitude.NpcAttitudes;
import com.talesforge.masternpc.npc.behavior.NpcBehaviors;
import com.talesforge.masternpc.npc.field.NpcDataMap;
import com.talesforge.masternpc.npc.field.NpcSettingFields;
import net.minecraft.Util;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.function.Consumer;

/** Owns NpcSettingFields.ATTITUDE and .BEHAVIOR. Two fields, one section, since they're picked together conceptually. */
public final class LogicSection implements NpcGuiSection {
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(MasterNPC.MOD_ID, "logic");

    private ResourceLocation attitude;
    private ResourceLocation behavior;

    private LogicSection(NpcDataMap initial) {
        this.attitude = initial.get(NpcSettingFields.ATTITUDE);
        this.behavior = initial.get(NpcSettingFields.BEHAVIOR);
    }

    @Override
    public int build(int x, int y, int width, Font font, Consumer<AbstractWidget> addWidget) {
        // Lists come straight from the registries, so addon-registered attitudes/behaviors show up automatically
        List<ResourceLocation> attitudes = NpcRegistries.ids(NpcRegistries.ATTITUDES,
                NpcAttitudes.DEFAULT_ID, NpcAttitudeType::isEnabled);
        List<ResourceLocation> behaviors = NpcRegistries.ids(NpcRegistries.BEHAVIORS,
                NpcBehaviors.DEFAULT_ID, b -> true);

        // Unknown id (removed mod) or disabled by config — fall back rather than feed CycleButton a bad value
        if (!attitudes.contains(attitude)) attitude = NpcAttitudes.DEFAULT_ID;
        if (!behaviors.contains(behavior)) behavior = NpcBehaviors.DEFAULT_ID;

        addWidget.accept(CycleButton.<ResourceLocation>builder(
                        id -> Component.translatable(Util.makeDescriptionId("npc_attitude", id)))
                .withValues(attitudes)
                .withInitialValue(attitude)
                .create(x, y, width, 20, Component.translatable("gui.masternpc.attitude"),
                        (btn, value) -> this.attitude = value));

        addWidget.accept(CycleButton.<ResourceLocation>builder(
                        id -> Component.translatable(Util.makeDescriptionId("npc_behavior", id)))
                .withValues(behaviors)
                .withInitialValue(behavior)
                .create(x, y + 24, width, 20, Component.translatable("gui.masternpc.behavior"),
                        (btn, value) -> this.behavior = value));

        return 48;
    }

    @Override
    public void collect(NpcDataMap out) {
        out.put(NpcSettingFields.ATTITUDE, attitude);
        out.put(NpcSettingFields.BEHAVIOR, behavior);
    }

    public static final class Factory implements NpcGuiSectionFactory {
        @Override public ResourceLocation id() { return ID; }
        @Override public NpcGuiSection create(NpcDataMap initial) { return new LogicSection(initial); }
    }
}