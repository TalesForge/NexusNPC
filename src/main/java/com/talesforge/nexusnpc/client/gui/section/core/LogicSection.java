package com.talesforge.nexusnpc.client.gui.section.core;

import com.talesforge.nexusnpc.NexusNPC;
import com.talesforge.nexusnpc.client.gui.section.NpcGuiSection;
import com.talesforge.nexusnpc.client.gui.section.NpcGuiSectionFactory;
import com.talesforge.nexusnpc.npc.NpcRegistries;
import com.talesforge.nexusnpc.npc.attitude.NpcAttitudeType;
import com.talesforge.nexusnpc.npc.attitude.NpcAttitudes;
import com.talesforge.nexusnpc.npc.behavior.NpcBehaviors;
import com.talesforge.nexusnpc.npc.data.NpcAiMode;
import com.talesforge.nexusnpc.npc.field.NpcDataMap;
import com.talesforge.nexusnpc.npc.field.NpcSettingFields;
import net.minecraft.Util;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.function.Consumer;

/** Owns NpcSettingFields.ATTITUDE, .BEHAVIOR and .AI_MODE — picked together conceptually. */
public final class LogicSection implements NpcGuiSection {
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(NexusNPC.MOD_ID, "logic");

    private ResourceLocation attitude;
    private ResourceLocation behavior;
    private NpcAiMode aiMode;

    private LogicSection(NpcDataMap initial) {
        this.attitude = initial.get(NpcSettingFields.ATTITUDE);
        this.behavior = initial.get(NpcSettingFields.BEHAVIOR);
        this.aiMode = initial.get(NpcSettingFields.AI_MODE);
    }

    @Override
    public int build(int x, int y, int width, Font font, Consumer<AbstractWidget> addWidget, Runnable requestRebuild) {
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
                .create(x, y, width, 20, Component.translatable("gui.nexusnpc.attitude"),
                        (btn, value) -> this.attitude = value));

        addWidget.accept(CycleButton.<ResourceLocation>builder(
                        id -> Component.translatable(Util.makeDescriptionId("npc_behavior", id)))
                .withValues(behaviors)
                .withInitialValue(behavior)
                .create(x, y + 24, width, 20, Component.translatable("gui.nexusnpc.behavior"),
                        (btn, value) -> this.behavior = value));

        // VANILLA: the mob keeps its own AI (attitude/behavior above do nothing). OVERRIDE: they replace it.
        addWidget.accept(CycleButton.<NpcAiMode>builder(
                        mode -> Component.translatable("gui.nexusnpc.ai_mode." + mode.getSerializedName()))
                .withValues(NpcAiMode.values())
                .withInitialValue(aiMode)
                .withTooltip(mode -> Tooltip.create(Component.translatable("gui.nexusnpc.ai_mode." + mode.getSerializedName() + ".tooltip")))
                .create(x, y + 48, width, 20, Component.translatable("gui.nexusnpc.ai_mode"),
                        (btn, value) -> this.aiMode = value));

        return 72;
    }

    @Override
    public void collect(NpcDataMap out) {
        out.put(NpcSettingFields.ATTITUDE, attitude);
        out.put(NpcSettingFields.BEHAVIOR, behavior);
        out.put(NpcSettingFields.AI_MODE, aiMode);
    }

    public static final class Factory implements NpcGuiSectionFactory {
        @Override public ResourceLocation id() { return ID; }
        @Override public NpcGuiSection create(NpcDataMap initial) { return new LogicSection(initial); }
    }
}
