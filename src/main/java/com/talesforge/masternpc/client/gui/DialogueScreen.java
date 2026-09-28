package com.talesforge.masternpc.client.gui;

import com.talesforge.masternpc.network.payload.DialogueActionPayload;
import com.talesforge.masternpc.npc.dialogue.DialogueOption;
import com.talesforge.masternpc.npc.dialogue.DialoguePage;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.MultiLineTextWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;

public class DialogueScreen extends Screen {
    private final int entityId;
    private final DialoguePage page;

    public DialogueScreen(int entityId, DialoguePage page) {
        super(Component.literal(page.id()));
        this.entityId = entityId;
        this.page = page;
    }

    @Override
    protected void init() {
        int w = 240;
        int x = width / 2 - w / 2;
        int y = height / 2 - 80;

        addRenderableWidget(new MultiLineTextWidget(x, y, Component.literal(page.text()), font).setMaxWidth(w));
        y += 60;

        List<DialogueOption> options = page.options();
        for (int i = 0; i < options.size(); i++) {
            int index = i;
            addRenderableWidget(Button.builder(Component.literal(options.get(i).text()), b ->
                    PacketDistributor.sendToServer(new DialogueActionPayload(entityId, page.id(), index))
            ).bounds(x, y, w, 20).build());
            y += 24;
        }

        addRenderableWidget(Button.builder(Component.translatable("gui.cancel"), b -> onClose())
                .bounds(x, y + 6, w, 20).build());
    }

    @Override public boolean isPauseScreen() { return true; }
}
