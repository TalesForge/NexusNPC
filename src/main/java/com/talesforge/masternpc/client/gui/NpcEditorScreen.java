package com.talesforge.masternpc.client.gui;

import com.talesforge.masternpc.api.MasterNpcApi;
import com.talesforge.masternpc.client.gui.element.ValueSlider;
import com.talesforge.masternpc.entity.ModEntities;
import com.talesforge.masternpc.network.payload.DeleteNpcPayload;
import com.talesforge.masternpc.network.payload.EditorStatusPayload;
import com.talesforge.masternpc.npc.NpcRegistries;
import com.talesforge.masternpc.npc.NpcSettings;
import com.talesforge.masternpc.npc.NpcSkins;
import com.talesforge.masternpc.npc.attitude.NpcAttitudeType;
import com.talesforge.masternpc.npc.attitude.NpcAttitudes;
import com.talesforge.masternpc.npc.behavior.NpcBehaviors;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;
import java.util.function.BiConsumer;

public class NpcEditorScreen extends Screen {
    private final BiConsumer<ResourceLocation, NpcSettings> onConfirm;
    private ResourceLocation typeId = ModEntities.NPC.getId();
    private final boolean creating;

    private final int entityId;  // -1 if this is a creation window (no locking is needed)
    private int pingTimer = 0;
    private boolean isDeleted = false;

    // Current values in the form
    private String name;
    private ResourceLocation attitude;
    private ResourceLocation behavior;
    private String skin;
    private double maxHealth, damage, speed;

    public NpcEditorScreen(Component title, NpcSettings initial, boolean creating, int entityId,
                           BiConsumer<ResourceLocation, NpcSettings> onConfirm) {
        super(title);
        this.creating = creating;
        this.entityId = entityId;
        this.onConfirm = onConfirm;
        this.name = initial.name();
        this.attitude = initial.attitude();
        this.behavior = initial.behavior();
        this.skin = NpcSkins.validate(initial.skin());
        this.maxHealth = initial.maxHealth();
        this.damage = initial.damage();
        this.speed = initial.speed();
    }

    @Override
    protected void init() {
        int w = 200;
        int x = this.width / 2 - w / 2;
        int y = this.height / 2 - 100;
        int step = 24;

        List<ResourceLocation> types = MasterNpcApi.typeIds();
        if (creating && types.size() > 1) {
            addRenderableWidget(CycleButton.<ResourceLocation>builder(id ->
                            Component.translatable(Util.makeDescriptionId("entity", id)))
                    .withValues(types)
                    .withInitialValue(typeId)
                    .create(x, y, w, 20, Component.translatable("gui.masternpc.type"),
                            (btn, value) -> this.typeId = value));
            y += step; // the remaining widgets shift down, their code doesn't need to change
        }

        // Lists come from the registries, so addon-registered values show up automatically
        List<ResourceLocation> attitudes = NpcRegistries.ids(NpcRegistries.ATTITUDES,
                NpcAttitudes.DEFAULT_ID, NpcAttitudeType::isEnabled);
        List<ResourceLocation> behaviors = NpcRegistries.ids(NpcRegistries.BEHAVIORS,
                NpcBehaviors.DEFAULT_ID, b -> true);

        // If the current value is not in the list (unknown id or disabled by config),
        // CycleButton would misbehave, so fall back to the default
        if (!attitudes.contains(attitude)) attitude = NpcAttitudes.DEFAULT_ID;
        if (!behaviors.contains(behavior)) behavior = NpcBehaviors.DEFAULT_ID;

        EditBox nameBox = new EditBox(this.font, x, y, w, 20, Component.translatable("gui.masternpc.name"));
        nameBox.setMaxLength(32);
        nameBox.setHint(Component.translatable("gui.masternpc.name"));
        nameBox.setValue(name);
        nameBox.setResponder(v -> this.name = v);
        addRenderableWidget(nameBox);

        // Attitude button
        addRenderableWidget(CycleButton.<ResourceLocation>builder(
                id -> Component.translatable(Util.makeDescriptionId("npc_attitude", id)))
                .withValues(attitudes)
                .withInitialValue(attitude)
                .create(x, y + step, w, 20, Component.translatable("gui.masternpc.attitude"),
                        (btn, value) -> this.attitude = value));

        // Behavior button
        addRenderableWidget(CycleButton.<ResourceLocation>builder(
                id -> Component.translatable(Util.makeDescriptionId("npc_behavior", id)))
                .withValues(behaviors)
                .withInitialValue(behavior)
                .create(x, y + step * 2, w, 20, Component.translatable("gui.masternpc.behavior"),
                        (btn, value) -> this.behavior = value));

        // Skin button
        addRenderableWidget(CycleButton.<String>builder(NpcSkins::label)
                .withValues(NpcSkins.all())
                .withInitialValue(skin)
                .create(x, y + step * 3, w, 20, Component.translatable("gui.masternpc.skin"),
                        (btn, value) -> this.skin = value));

        // Attributes slider
        addRenderableWidget(new ValueSlider(x, y + step * 4, w, 20, Component.translatable("gui.masternpc.health"),
                1, 100, maxHealth, v -> this.maxHealth = v));
        addRenderableWidget(new ValueSlider(x, y + step * 5, w, 20, Component.translatable("gui.masternpc.damage"),
                0, 20, damage, v -> this.damage = v));
        addRenderableWidget(new ValueSlider(x, y + step * 6, w, 20, Component.translatable("gui.masternpc.speed"),
                0.05, 0.6, speed, 0.05, v -> this.speed = v));

        // Create/Save button
        Component confirmText = Component.translatable(creating ? "gui.masternpc.create" : "gui.masternpc.save");
        addRenderableWidget(Button.builder(confirmText, b -> {
            onConfirm.accept(typeId, new NpcSettings(name, attitude, behavior, skin, maxHealth, damage, speed));
            onClose();
        }).bounds(x, y + step * 7 + 6, creating ? w : w / 2 - 2, 20).build());

        if (!creating) {
            // Delete button
            addRenderableWidget(Button.builder(Component.translatable("gui.masternpc.delete"), b -> {
                if (entityId >= 0) {
                    PacketDistributor.sendToServer(new DeleteNpcPayload(entityId));
                    isDeleted = true;
                }
                onClose();
            }).bounds(x + w / 2 + 2, y + step * 7 + 6, w / 2 - 2, 20).build());
        }

        // Cancel button
        addRenderableWidget(Button.builder(Component.translatable("gui.cancel"), b -> onClose())
                .bounds(x, y + step * 8 + 6, w, 20).build());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);  // Background and widgets
        graphics.drawCenteredString(this.font, this.title, this.width / 2, this.height / 2 - 100 - 16, 0xFFFFFF);
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