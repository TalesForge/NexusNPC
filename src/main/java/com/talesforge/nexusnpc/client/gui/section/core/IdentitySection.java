package com.talesforge.nexusnpc.client.gui.section.core;

import com.talesforge.nexusnpc.NexusNPC;
import com.talesforge.nexusnpc.client.gui.section.NpcGuiSection;
import com.talesforge.nexusnpc.client.gui.section.NpcGuiSectionFactory;
import com.talesforge.nexusnpc.npc.field.NpcDataMap;
import com.talesforge.nexusnpc.npc.field.NpcSettingFields;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.function.Consumer;

/** Owns NpcSettingFields.NAME. */
public final class IdentitySection implements NpcGuiSection {
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(NexusNPC.MOD_ID, "identity");

    private String name;

    private IdentitySection(NpcDataMap initial) {
        this.name = initial.get(NpcSettingFields.NAME);
    }

    @Override
    public int build(int x, int y, int width, Font font, Consumer<AbstractWidget> addWidget, Runnable requestRebuild) {
        EditBox box = new EditBox(font, x, y, width, 20, Component.translatable("gui.nexusnpc.name"));
        box.setMaxLength(32);
        box.setHint(Component.translatable("gui.nexusnpc.name"));
        box.setValue(name);
        box.setResponder(v -> this.name = v);
        addWidget.accept(box);
        return 24;
    }

    @Override
    public void collect(NpcDataMap out) {
        out.put(NpcSettingFields.NAME, name);
    }

    public static final class Factory implements NpcGuiSectionFactory {
        @Override public ResourceLocation id() { return ID; }
        @Override public NpcGuiSection create(NpcDataMap initial) { return new IdentitySection(initial); }
    }
}
