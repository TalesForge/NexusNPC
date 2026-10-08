package com.talesforge.nexusnpc.client.gui.section.core;

import com.talesforge.nexusnpc.NexusNPC;
import com.talesforge.nexusnpc.client.gui.EditorSession;
import com.talesforge.nexusnpc.client.gui.element.ValueSlider;
import com.talesforge.nexusnpc.client.gui.section.NpcGuiContext;
import com.talesforge.nexusnpc.client.gui.section.NpcGuiSection;
import com.talesforge.nexusnpc.client.gui.section.NpcGuiSectionFactory;
import com.talesforge.nexusnpc.npc.field.NpcDataMap;
import com.talesforge.nexusnpc.npc.field.NpcSettingFields;
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
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(NexusNPC.MOD_ID, "stats");

    private final EditorSession session;

    private StatsSection(EditorSession session) { this.session = session; }

    @Override
    public Component title() { return Component.translatable("gui.nexusnpc.section.attributes"); }

    @Override
    public int build(int x, int y, int width, Font font, Consumer<AbstractWidget> addWidget, Runnable requestRebuild) {
        addWidget.accept(new ValueSlider(x, y, width, 20, Component.translatable("gui.nexusnpc.health"),
                1, 100, session.get(NpcSettingFields.MAX_HEALTH), v -> session.set(NpcSettingFields.MAX_HEALTH, v)));
        addWidget.accept(new ValueSlider(x, y + 24, width, 20, Component.translatable("gui.nexusnpc.damage"),
                0, 20, session.get(NpcSettingFields.DAMAGE), v -> session.set(NpcSettingFields.DAMAGE, v)));
        addWidget.accept(new ValueSlider(x, y + 48, width, 20, Component.translatable("gui.nexusnpc.speed"),
                0.05, 0.6, session.get(NpcSettingFields.SPEED), 0.05, v -> session.set(NpcSettingFields.SPEED, v)));
        return 70;
    }

    public static final class Factory implements NpcGuiSectionFactory {
        @Override public ResourceLocation id() { return ID; }
        @Override public NpcGuiSection create(NpcDataMap initial) { return new StatsSection(EditorSession.local(initial)); }
        @Override public NpcGuiSection create(NpcDataMap initial, NpcGuiContext context) { return new StatsSection(context.session()); }
    }
}
