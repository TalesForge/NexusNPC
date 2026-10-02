package com.talesforge.masternpc.client.gui.screen.settings;

import com.talesforge.masternpc.client.gui.NpcEditingScreen;
import com.talesforge.masternpc.client.gui.screen.CustomScreen;
import com.talesforge.masternpc.client.gui.screen.settings.dialogue.DialogueLibraryScreen;
import com.talesforge.masternpc.client.gui.screen.settings.dialogue.DialogueListScreen;
import com.talesforge.masternpc.client.gui.section.NpcGuiContext;
import com.talesforge.masternpc.client.gui.section.NpcGuiRegistry;
import com.talesforge.masternpc.client.gui.section.NpcGuiSection;
import com.talesforge.masternpc.client.gui.section.NpcGuiSectionFactory;
import com.talesforge.masternpc.client.gui.section.core.DialogueLibrarySection;
import com.talesforge.masternpc.client.gui.section.core.DialogueListSection;
import com.talesforge.masternpc.client.gui.section.core.TradeSection;
import com.talesforge.masternpc.npc.field.NpcDataMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

public class SettingsScreen extends CustomScreen implements NpcEditingScreen {

    private ResourceLocation typeId;

    private final List<NpcGuiSection> activeSections = new ArrayList<>();
    private int formTop;

    public SettingsScreen(NpcDataMap initial, ResourceLocation typeId) {
        super(Component.translatable("gui.masternpc.settings.title"), null, initial);
        this.typeId = typeId;
    }

    @Override
    protected void init() {
        super.init();

        int w = 200;
        int x = this.width / 2 - w / 2;
        int y = this.height / 2 - 120;
        formTop = y;


        // Was NpcDataMap.defaults() — silently discarded whatever `initial` the constructor
        // was given (and any in-progress edits CustomScreen#init() just snapshotted back
        // into it). `this.initial` (inherited from CustomScreen) is the one that's actually kept up to date.
        NpcGuiContext context = new NpcGuiContext(-1, false, typeId);
        for (NpcGuiSectionFactory factory : NpcGuiRegistry.activeSections(typeId)) {
            NpcGuiSection section = factory.create(NpcDataMap.defaults(), context);

            boolean shouldAdd = switch (section) {
                case DialogueLibrarySection s -> true;
                case TradeSection s -> true;
                default -> false;
            };

            if (!shouldAdd) continue;
            y += addSectionToScreen(section, x, y, w);
        }


        addRenderableWidget(Button.builder(Component.translatable("gui.done"), b -> onClose())
                .bounds(x, y + 30 + 6, w, 20).build());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);  // Background and widgets
        graphics.drawCenteredString(this.font, this.title, this.width / 2, formTop - 16, 0xFFFFFF);
    }

    @Override
    public boolean isPauseScreen() {
        return true;  // In single-player mode, the world pauses
    }
}
