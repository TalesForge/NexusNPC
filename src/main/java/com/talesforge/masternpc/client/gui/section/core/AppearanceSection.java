package com.talesforge.masternpc.client.gui.section.core;

import com.talesforge.masternpc.MasterNPC;
import com.talesforge.masternpc.client.gui.section.NpcGuiSection;
import com.talesforge.masternpc.client.gui.section.NpcGuiSectionFactory;
import com.talesforge.masternpc.npc.NpcSkins;
import com.talesforge.masternpc.npc.field.NpcDataMap;
import com.talesforge.masternpc.npc.field.NpcSettingFields;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.function.Consumer;

/** Owns NpcSettingFields.SKIN. This is the section a JSON-model/skin system (see next step) would replace. */
public final class AppearanceSection implements NpcGuiSection {
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(MasterNPC.MOD_ID, "appearance");

    private String skin;

    private AppearanceSection(NpcDataMap initial) {
        this.skin = NpcSkins.validate(initial.get(NpcSettingFields.SKIN));
    }

    @Override
    public int build(int x, int y, int width, Font font, Consumer<AbstractWidget> addWidget) {
        addWidget.accept(CycleButton.<String>builder(NpcSkins::label)
                .withValues(NpcSkins.all())
                .withInitialValue(skin)
                .create(x, y, width, 20, Component.translatable("gui.masternpc.skin"),
                        (btn, value) -> this.skin = value));
        return 24;
    }

    @Override
    public void collect(NpcDataMap out) {
        out.put(NpcSettingFields.SKIN, skin);
    }

    public static final class Factory implements NpcGuiSectionFactory {
        @Override public ResourceLocation id() { return ID; }
        @Override public NpcGuiSection create(NpcDataMap initial) { return new AppearanceSection(initial); }
    }
}