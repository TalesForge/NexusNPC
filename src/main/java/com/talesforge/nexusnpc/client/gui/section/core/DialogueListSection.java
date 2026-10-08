package com.talesforge.nexusnpc.client.gui.section.core;

import com.talesforge.nexusnpc.NexusNPC;
import com.talesforge.nexusnpc.client.gui.EditorSession;
import com.talesforge.nexusnpc.client.gui.screen.settings.dialogue.DialogueListScreen;
import com.talesforge.nexusnpc.client.gui.section.NpcGuiContext;
import com.talesforge.nexusnpc.client.gui.section.NpcGuiSection;
import com.talesforge.nexusnpc.client.gui.section.NpcGuiSectionFactory;
import com.talesforge.nexusnpc.npc.dialogue.NpcDialogue;
import com.talesforge.nexusnpc.npc.field.NpcDataMap;
import com.talesforge.nexusnpc.npc.field.NpcSettingFields;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.function.Consumer;

/**
 * Opens this NPC's dialogue list. Every change made there is written to the session (and so saved) as it happens.
 */
public final class DialogueListSection implements NpcGuiSection {
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(NexusNPC.MOD_ID, "dialogue");

    private final EditorSession session;

    private DialogueListSection(EditorSession session) { this.session = session; }

    @Override
    public Component title() { return Component.translatable("gui.nexusnpc.section.dialogue"); }

    @Override
    public int build(int x, int y, int width, Font font, Consumer<AbstractWidget> addWidget, Runnable requestRebuild) {
        NpcDialogue dialogue = session.get(NpcSettingFields.DIALOGUE);
        Component label = Component.translatable("gui.nexusnpc.dialogue.edit")
                .append(Component.literal(" (" + dialogue.pages().size() + ")"));
        addWidget.accept(Button.builder(label, b -> {
                    Minecraft mc = Minecraft.getInstance();
                    mc.setScreen(new DialogueListScreen(mc.screen, session.get(NpcSettingFields.DIALOGUE),
                            updated -> session.set(NpcSettingFields.DIALOGUE, updated)));
                })
                .bounds(x, y, width, 20).build());
        return 22;
    }

    public static final class Factory implements NpcGuiSectionFactory {
        @Override public ResourceLocation id() { return ID; }
        @Override public NpcGuiSection create(NpcDataMap initial) { return new DialogueListSection(EditorSession.local(initial)); }
        @Override public NpcGuiSection create(NpcDataMap initial, NpcGuiContext context) { return new DialogueListSection(context.session()); }
    }
}
