package com.talesforge.masternpc.client.gui.screen.settings;

import com.talesforge.masternpc.api.MasterNpcApi;
import com.talesforge.masternpc.client.gui.EditorKeepAlive;
import com.talesforge.masternpc.client.gui.NpcEditingScreen;
import com.talesforge.masternpc.client.gui.screen.CustomScreen;
import com.talesforge.masternpc.client.gui.section.NpcGuiContext;
import com.talesforge.masternpc.client.gui.section.NpcGuiRegistry;
import com.talesforge.masternpc.client.gui.section.NpcGuiSection;
import com.talesforge.masternpc.client.gui.section.NpcGuiSectionFactory;
import com.talesforge.masternpc.client.gui.section.core.DialogueLibrarySection;
import com.talesforge.masternpc.client.gui.section.core.DialogueListSection;
import com.talesforge.masternpc.network.payload.action.DeleteNpcPayload;
import com.talesforge.masternpc.network.payload.action.EditorStatusPayload;
import com.talesforge.masternpc.npc.field.NpcDataMap;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;
import java.util.function.BiConsumer;

/**
 * The standard NPC editor screen. It no longer knows what "name" or "attitude" are — it
 * just asks {@link NpcGuiRegistry} which sections are active for the current NPC type,
 * lays them out top to bottom, and on confirm asks each of them to write its values into
 * one shared {@link NpcDataMap}. An addon that wants a totally different layout registers
 * an {@code NpcGuiScreenFactory} override instead of going through this class at all —
 * see {@code ClientPayloadHandler}, which checks for that override before constructing this.
 */
public class NpcEditorScreen extends CustomScreen implements NpcEditingScreen {

    private final BiConsumer<ResourceLocation, NpcDataMap> onConfirm;
    private ResourceLocation typeId;
    private final boolean creating;

    private final int entityId;  // -1 if this is a creation window (no locking is needed)
    private boolean isDeleted = false;

    private int formTop;

    public NpcEditorScreen(Component title, NpcDataMap initial, boolean creating, int entityId,
                           ResourceLocation typeId, BiConsumer<ResourceLocation, NpcDataMap> onConfirm) {
        super(title, null, initial);
        this.creating = creating;
        this.entityId = entityId;
        this.typeId = typeId;
        this.onConfirm = onConfirm;
    }

    @Override
    protected void init() {
        super.init();
        if (entityId >= 0) EditorKeepAlive.start(entityId);

        int w = 200;
        int x = this.width / 2 - w / 2;
        int y = this.height / 2 - 120;
        formTop = y;

        List<ResourceLocation> types = MasterNpcApi.typeIds();
        if (creating && types.size() > 1) {
            addRenderableWidget(CycleButton.<ResourceLocation>builder(id -> Component.translatable(Util.makeDescriptionId("entity", id)))
                    .withValues(types)
                    .withInitialValue(typeId)
                    .create(x, y, w, 20, Component.translatable("gui.masternpc.type"), (btn, value) -> {
                        this.typeId = value;
                        // A different NPC type may have different active sections
                        // (or its own full screen override) — rebuild from scratch.
                        this.rebuild();
                    }));
            y += 24;
        }

        NpcGuiContext context = new NpcGuiContext(entityId, creating, typeId);
        for (NpcGuiSectionFactory factory : NpcGuiRegistry.activeSections(typeId)) {
            NpcGuiSection section = factory.create(initial, context);

            boolean shouldAdd = switch (section) {
                case DialogueListSection s -> !creating;
                case DialogueLibrarySection s -> false;
                default -> true;
            };

            if (!shouldAdd) continue;
            y += addSectionToScreen(section, x, y, w);
        }

        Component confirmText = Component.translatable(creating ? "gui.masternpc.create" : "gui.masternpc.save");
        addRenderableWidget(Button.builder(confirmText, b -> {
            onConfirm.accept(typeId, collectAll());
            onClose();
        }).bounds(x, y + 6, creating ? w : w / 2 - 2, 20).build());

        if (!creating) {
            addRenderableWidget(Button.builder(Component.translatable("gui.masternpc.delete"), b -> {
                if (entityId >= 0) {
                    PacketDistributor.sendToServer(new DeleteNpcPayload(entityId));
                    isDeleted = true;
                }
                onClose();
            }).bounds(x + w / 2 + 2, y + 6, w / 2 - 2, 20).build());
        }

        addRenderableWidget(Button.builder(Component.translatable("gui.cancel"), b -> onClose())
                .bounds(x, y + 30 + 6, w, 20).build());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);  // Background and widgets
        graphics.drawCenteredString(this.font, this.title, this.width / 2, formTop - 16, 0xFFFFFF);
    }

    @Override
    public void removed() {
        super.removed();
        if (childIsOpen()) return;
        EditorKeepAlive.stop();
        if (entityId >= 0 && !isDeleted) sendStatus(true);
    }

    private void sendStatus(boolean closed) {
        // When disconnected from the server, there is no longer a connection, and sendToServer would throw an exception
        if (Minecraft.getInstance().getConnection() != null) {
            PacketDistributor.sendToServer(new EditorStatusPayload(entityId, closed));
        }
    }

    @Override
    public boolean isPauseScreen() {
        return true;  // In single-player mode, the world pauses
    }
}
