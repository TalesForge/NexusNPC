package com.talesforge.nexusnpc.client.gui.section.core;

import com.talesforge.nexusnpc.NexusNPC;
import com.talesforge.nexusnpc.client.gui.EditorSession;
import com.talesforge.nexusnpc.client.gui.screen.settings.storage.NpcStorageScreen;
import com.talesforge.nexusnpc.client.gui.section.NpcGuiContext;
import com.talesforge.nexusnpc.client.gui.section.NpcGuiSection;
import com.talesforge.nexusnpc.client.gui.section.NpcGuiSectionFactory;
import com.talesforge.nexusnpc.network.payload.screen.OpenNpcStoragePayload;
import com.talesforge.nexusnpc.npc.field.NpcDataMap;
import com.talesforge.nexusnpc.npc.field.NpcSettingFields;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.function.Consumer;

/**
 * What the NPC carries: its equipment (hands, armor) and its inventory (a shop's stock and takings), opened together as
 * one window by the server, plus the switch for whether it all falls when the NPC dies. Hidden while creating: a
 * not-yet-spawned NPC has no entity to open a menu for.
 */
public final class StorageSection implements NpcGuiSection {
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(NexusNPC.MOD_ID, "storage");

    private final EditorSession session;
    private final int entityId;

    private StorageSection(EditorSession session, int entityId) {
        this.session = session;
        this.entityId = entityId;
    }

    @Override
    public Component title() { return Component.translatable("gui.nexusnpc.section.storage"); }

    @Override
    public int build(int x, int y, int width, Font font, Consumer<AbstractWidget> addWidget, Runnable requestRebuild) {
        if (entityId < 0) return 0;

        addWidget.accept(Button.builder(Component.translatable("gui.nexusnpc.storage.open"), b -> open())
                .bounds(x, y, width, 20).build());

        boolean drop = session.get(NpcSettingFields.DROP_ITEMS);
        addWidget.accept(Button.builder(
                        Component.translatable("gui.nexusnpc.storage.drop").append(": ")
                                .append(drop ? CommonComponents.OPTION_ON : CommonComponents.OPTION_OFF),
                        b -> {
                            session.set(NpcSettingFields.DROP_ITEMS, !drop);
                            requestRebuild.run();
                        })
                .bounds(x, y + 24, width, 20)
                .tooltip(Tooltip.create(Component.translatable("gui.nexusnpc.storage.drop.tooltip")))
                .build());
        return 46;
    }

    private void open() {
        NpcStorageScreen.rememberParent(Minecraft.getInstance().screen);
        PacketDistributor.sendToServer(new OpenNpcStoragePayload(entityId));
    }

    public static final class Factory implements NpcGuiSectionFactory {
        @Override public ResourceLocation id() { return ID; }
        @Override public NpcGuiSection create(NpcDataMap initial) { return new StorageSection(EditorSession.local(initial), -1); }
        @Override public NpcGuiSection create(NpcDataMap initial, NpcGuiContext context) {
            return new StorageSection(context.session(), context.entityId());
        }
    }
}
