package com.talesforge.nexusnpc.client.gui.section.core;

import com.talesforge.nexusnpc.NexusNPC;
import com.talesforge.nexusnpc.client.gui.screen.settings.dialogue.DialogueLibraryScreen;
import com.talesforge.nexusnpc.client.gui.section.NpcGuiSection;
import com.talesforge.nexusnpc.client.gui.section.NpcGuiSectionFactory;
import com.talesforge.nexusnpc.npc.field.NpcDataMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.function.Consumer;

/**
 * Just a button that opens the GLOBAL dialogue library (browse/create/edit/delete saved pages). Not tied to any
 * one NPC, so it has nothing to read or write; the library is saved to disk by the screen itself.
 */
public final class DialogueLibrarySection implements NpcGuiSection {
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(NexusNPC.MOD_ID, "dialogue_library");

    private DialogueLibrarySection() {}

    @Override
    public Component title() { return Component.translatable("gui.nexusnpc.section.library"); }

    @Override
    public int build(int x, int y, int width, Font font, Consumer<AbstractWidget> addWidget, Runnable requestRebuild) {
        addWidget.accept(Button.builder(Component.translatable("gui.nexusnpc.dialogue.library_title"),
                        b -> {
                            Minecraft mc = Minecraft.getInstance();
                            mc.setScreen(new DialogueLibraryScreen(mc.screen, false, picked -> {}));
                        })
                .bounds(x, y, width, 20).build());
        return 22;
    }

    public static final class Factory implements NpcGuiSectionFactory {
        @Override public ResourceLocation id() { return ID; }
        @Override public NpcGuiSection create(NpcDataMap initial) { return new DialogueLibrarySection(); }
    }
}
