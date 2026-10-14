package com.talesforge.nexusnpc.client.gui.section.core;

import com.talesforge.nexusnpc.NexusNPC;
import com.talesforge.nexusnpc.client.gui.EditorSession;
import com.talesforge.nexusnpc.client.gui.screen.settings.trade.TradeEditScreen;
import com.talesforge.nexusnpc.client.gui.ui.PickerScreen;
import com.talesforge.nexusnpc.npc.data.NpcTradeMode;
import com.talesforge.nexusnpc.npc.field.NpcSettingFields;
import com.talesforge.nexusnpc.client.gui.section.NpcGuiContext;
import com.talesforge.nexusnpc.client.gui.section.NpcGuiSection;
import com.talesforge.nexusnpc.client.gui.section.NpcGuiSectionFactory;
import com.talesforge.nexusnpc.network.payload.screen.OpenTradeEditorPayload;
import com.talesforge.nexusnpc.npc.field.NpcDataMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Trades are not part of NpcDataMap: laying them out needs real item slots, which only a
 * container menu can provide. The button asks the server to open TradeEditMenu for this NPC (which saves every
 * change as it happens). Hidden while creating: a not-yet-spawned NPC has no entity id to open a menu for.
 */
public final class TradeSection implements NpcGuiSection {
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(NexusNPC.MOD_ID, "trade");

    private final int entityId;
    private final EditorSession session;

    private TradeSection(int entityId, EditorSession session) {
        this.entityId = entityId;
        this.session = session;
    }

    @Override
    public Component title() { return Component.translatable("gui.nexusnpc.section.trade"); }

    @Override
    public int build(int x, int y, int width, Font font, Consumer<AbstractWidget> addWidget, Runnable requestRebuild) {
        if (entityId < 0) return 0;
        addWidget.accept(Button.builder(Component.translatable("gui.nexusnpc.trade.edit"),
                        b -> {
                            TradeEditScreen.rememberParent(Minecraft.getInstance().screen);
                            PacketDistributor.sendToServer(new OpenTradeEditorPayload(entityId));
                        }
                )
                .bounds(x, y, width, 20)
                .tooltip(Tooltip.create(Component.translatable("gui.nexusnpc.trade.edit.tooltip")))
                .build());

        // Where the goods come from: nowhere, or the NPC's own inventory
        NpcTradeMode mode = session.get(NpcSettingFields.TRADE_MODE);
        addWidget.accept(PickerScreen.openerButton(x, y + 24, width, Component.translatable("gui.nexusnpc.trade_mode"),
                modeLabel(mode), () -> openModePicker(mode)));
        return 46;
    }

    private static Component modeLabel(NpcTradeMode mode) {
        return Component.translatable("gui.nexusnpc.trade_mode." + mode.getSerializedName());
    }

    private void openModePicker(NpcTradeMode current) {
        List<PickerScreen.Entry<NpcTradeMode>> entries = new ArrayList<>();
        for (NpcTradeMode mode : NpcTradeMode.values()) {
            entries.add(new PickerScreen.Entry<>(mode, modeLabel(mode),
                    Component.translatable("gui.nexusnpc.trade_mode." + mode.getSerializedName() + ".desc"), null));
        }
        Minecraft mc = Minecraft.getInstance();
        mc.setScreen(new PickerScreen<>(mc.screen, Component.translatable("gui.nexusnpc.picker.trade_mode"), entries, current,
                picked -> session.set(NpcSettingFields.TRADE_MODE, picked)));
    }

    public static final class Factory implements NpcGuiSectionFactory {
        @Override public ResourceLocation id() { return ID; }

        @Override public NpcGuiSection create(NpcDataMap initial) { return new TradeSection(-1, EditorSession.local(initial)); }

        @Override
        public NpcGuiSection create(NpcDataMap initial, NpcGuiContext context) {
            return new TradeSection(context.entityId(), context.session());
        }
    }
}
