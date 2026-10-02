package com.talesforge.masternpc.client.gui.section.core;

import com.talesforge.masternpc.MasterNPC;
import com.talesforge.masternpc.client.gui.screen.CustomScreen;
import com.talesforge.masternpc.client.gui.screen.settings.dialogue.DialogueListScreen;
import com.talesforge.masternpc.client.gui.section.NpcGuiSection;
import com.talesforge.masternpc.client.gui.section.NpcGuiSectionFactory;
import com.talesforge.masternpc.npc.dialogue.NpcDialogue;
import com.talesforge.masternpc.npc.field.NpcDataMap;
import com.talesforge.masternpc.npc.field.NpcSettingFields;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.function.Consumer;

public final class DialogueListSection implements NpcGuiSection {
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(MasterNPC.MOD_ID, "dialogue");
    private NpcDialogue dialogue;

    private DialogueListSection(NpcDataMap initial) { this.dialogue = initial.get(NpcSettingFields.DIALOGUE); }

    @Override
    public int build(int x, int y, int width, Font font, Consumer<AbstractWidget> addWidget, Runnable requestRebuild) {
        addWidget.accept(Button.builder(
                        Component.translatable("gui.masternpc.dialogue.edit"),
                        b -> {
                            Screen parent = Minecraft.getInstance().screen;
                            Screen child = new DialogueListScreen(parent, dialogue, updated -> this.dialogue = updated);
                            if (parent instanceof CustomScreen custom) custom.openChild(child);
                            else Minecraft.getInstance().setScreen(child);
                        })
                .bounds(x, y, width, 20).build());
        return 24;
    }

    @Override public void collect(NpcDataMap out) { out.put(NpcSettingFields.DIALOGUE, dialogue); }

    public static final class Factory implements NpcGuiSectionFactory {
        @Override public ResourceLocation id() { return ID; }
        @Override public NpcGuiSection create(NpcDataMap initial) { return new DialogueListSection(initial); }
    }
}
