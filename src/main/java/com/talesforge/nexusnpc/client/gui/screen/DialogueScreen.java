package com.talesforge.nexusnpc.client.gui.screen;

import com.talesforge.nexusnpc.network.payload.DialogueActionPayload;
import com.talesforge.nexusnpc.npc.dialogue.DialogueAction;
import com.talesforge.nexusnpc.npc.dialogue.DialogueOption;
import com.talesforge.nexusnpc.npc.dialogue.DialoguePage;
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
            DialogueOption option = options.get(i);
            addRenderableWidget(Button.builder(Component.literal(option.text()), b -> {
                PacketDistributor.sendToServer(new DialogueActionPayload(entityId, page.id(), index));
                if (option.action() instanceof DialogueAction.Close) onClose();
            }).bounds(x, y, w, 20).build());
            y += 24;
        }
    }

    @Override public boolean isPauseScreen() { return true; }
}
