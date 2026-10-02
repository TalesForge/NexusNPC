package com.talesforge.masternpc.client.gui.section.core;

import com.talesforge.masternpc.MasterNPC;
import com.talesforge.masternpc.client.gui.screen.settings.trade.TradeEditScreen;
import com.talesforge.masternpc.client.gui.section.NpcGuiContext;
import com.talesforge.masternpc.client.gui.section.NpcGuiSection;
import com.talesforge.masternpc.client.gui.section.NpcGuiSectionFactory;
import com.talesforge.masternpc.network.payload.screen.OpenTradeEditorPayload;
import com.talesforge.masternpc.npc.field.NpcDataMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.function.Consumer;

/**
 * Trades are not part of NpcDataMap: laying them out needs real item slots, which only a
 * container menu can provide. The button asks the server to open TradeEditMenu for this NPC.
 * Hidden while creating: a not-yet-spawned NPC has no entity id to open a menu for.
 */
public final class TradeSection implements NpcGuiSection {
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(MasterNPC.MOD_ID, "trade");

    private final int entityId;

    private TradeSection(int entityId) { this.entityId = entityId; }

    @Override
    public int build(int x, int y, int width, Font font, Consumer<AbstractWidget> addWidget, Runnable requestRebuild) {
        if (entityId < 0) return 0;
        addWidget.accept(Button.builder(Component.translatable("gui.masternpc.trade.edit"),
                        b -> {
                            TradeEditScreen.rememberParent(Minecraft.getInstance().screen);
                            PacketDistributor.sendToServer(new OpenTradeEditorPayload(entityId));
                        }
                )
                .bounds(x, y, width, 20)
                .tooltip(Tooltip.create(Component.translatable("gui.masternpc.trade.edit.tooltip")))
                .build());
        return 24;
    }

    @Override public void collect(NpcDataMap out) {}  // Nothing: trades don't travel through NpcDataMap

    public static final class Factory implements NpcGuiSectionFactory {
        @Override public ResourceLocation id() { return ID; }

        @Override public NpcGuiSection create(NpcDataMap initial) { return new TradeSection(-1); }

        @Override
        public NpcGuiSection create(NpcDataMap initial, NpcGuiContext context) {
            return new TradeSection(context.entityId());
        }
    }
}
