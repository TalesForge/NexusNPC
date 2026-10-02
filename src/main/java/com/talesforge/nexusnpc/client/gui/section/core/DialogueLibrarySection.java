package com.talesforge.nexusnpc.client.gui.section.core;

import com.talesforge.nexusnpc.NexusNPC;
import com.talesforge.nexusnpc.client.gui.screen.CustomScreen;
import com.talesforge.nexusnpc.client.gui.screen.settings.dialogue.DialogueLibraryScreen;
import com.talesforge.nexusnpc.client.gui.section.NpcGuiSection;
import com.talesforge.nexusnpc.client.gui.section.NpcGuiSectionFactory;
import com.talesforge.nexusnpc.npc.field.NpcDataMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.function.Consumer;

/**
 * Just a button that opens the GLOBAL dialogue library (browse/create/edit/delete saved
 * pages). Not tied to any one NPC, so it has nothing to read from or write back into an
 * NpcDataMap — it used to round-trip NpcSettingFields.DIALOGUE for no reason (a leftover
 * from before this was split out from the per-NPC dialogue section); removed that.
 */
public final class DialogueLibrarySection implements NpcGuiSection {
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(NexusNPC.MOD_ID, "dialogue");

    private DialogueLibrarySection() {}

    @Override
    public int build(int x, int y, int width, Font font, Consumer<AbstractWidget> addWidget, Runnable requestRebuild) {
        addWidget.accept(Button.builder(
                        Component.translatable("gui.nexusnpc.dialogue.library_title"),
                        b -> {
                            Screen parent = Minecraft.getInstance().screen;
                            Screen child = new DialogueLibraryScreen(parent, false, updated -> {});
                            if (parent instanceof CustomScreen custom) custom.openChild(child);
                            else Minecraft.getInstance().setScreen(child);
                        })
                .bounds(x, y, width, 20).build());
        return 24;
    }

    @Override
    public void collect(NpcDataMap out) {}

    public static final class Factory implements NpcGuiSectionFactory {
        @Override public ResourceLocation id() { return ID; }
        @Override public NpcGuiSection create(NpcDataMap initial) { return new DialogueLibrarySection(); }
    }
}