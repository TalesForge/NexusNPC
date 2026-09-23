package com.talesforge.masternpc.client.gui;

import com.talesforge.masternpc.api.MasterNpcApi;
import com.talesforge.masternpc.client.gui.section.NpcGuiRegistry;
import com.talesforge.masternpc.client.gui.section.NpcGuiSection;
import com.talesforge.masternpc.client.gui.section.NpcGuiSectionFactory;
import com.talesforge.masternpc.network.payload.DeleteNpcPayload;
import com.talesforge.masternpc.network.payload.EditorStatusPayload;
import com.talesforge.masternpc.npc.field.NpcDataMap;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
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
public class NpcEditorScreen extends Screen {
    private final BiConsumer<ResourceLocation, NpcDataMap> onConfirm;
    private final NpcDataMap initial;
    private ResourceLocation typeId;
    private final boolean creating;

    private final int entityId;  // -1 if this is a creation window (no locking is needed)
    private int pingTimer = 0;
    private boolean isDeleted = false;

    private final List<NpcGuiSection> activeSections = new ArrayList<>();
    private int formTop;

    public NpcEditorScreen(Component title, NpcDataMap initial, boolean creating, int entityId,
                           ResourceLocation typeId, BiConsumer<ResourceLocation, NpcDataMap> onConfirm) {
        super(title);
        this.creating = creating;
        this.entityId = entityId;
        this.initial = initial;
        this.typeId = typeId;
        this.onConfirm = onConfirm;
    }

    @Override
    protected void init() {
        activeSections.clear();

        int w = 200;
        int x = this.width / 2 - w / 2;
        int y = this.height / 2 - 120;
        formTop = y;

        List<ResourceLocation> types = MasterNpcApi.typeIds();
        if (creating && types.size() > 1) {
            addRenderableWidget(CycleButton.<ResourceLocation>builder(id ->
                            Component.translatable(Util.makeDescriptionId("entity", id)))
                    .withValues(types)
                    .withInitialValue(typeId)
                    .create(x, y, w, 20, Component.translatable("gui.masternpc.type"), (btn, value) -> {
                        this.typeId = value;
                        // A different NPC type may have different active sections
                        // (or its own full screen override) — rebuild from scratch.
                        this.clearWidgets();
                        this.init();
                    }));
            y += 24;
        }

        for (NpcGuiSectionFactory factory : NpcGuiRegistry.activeSections(typeId)) {
            NpcGuiSection section = factory.create(initial);
            y += section.build(x, y, w, this.font, this::addRenderableWidget);
            activeSections.add(section);
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

    /**
     * Only ever contains what the currently active sections chose to write. A section that
     * isn't active for this NPC type (or belongs to an addon not present at all) never gets
     * asked, so its field is simply absent here — and NpcDataMap#applyAll on the server
     * leaves anything absent completely untouched. Nothing gets silently reset.
     */
    private NpcDataMap collectAll() {
        NpcDataMap data = NpcDataMap.empty();
        for (NpcGuiSection section : activeSections) {
            section.collect(data);
        }
        return data;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);  // Background and widgets
        graphics.drawCenteredString(this.font, this.title, this.width / 2, formTop - 16, 0xFFFFFF);
    }

    @Override
    public void tick() {
        super.tick();
        if (entityId >= 0 && ++pingTimer >= 20) {
            pingTimer = 0;
            sendStatus(false);
        }
    }

    // This is triggered by ANY screen closure: buttons, Esc, replacement with another screen, or shutdown
    @Override
    public void removed() {
        super.removed();
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