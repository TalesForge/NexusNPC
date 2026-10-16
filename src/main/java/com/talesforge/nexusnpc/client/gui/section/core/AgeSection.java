package com.talesforge.nexusnpc.client.gui.section.core;

import com.talesforge.nexusnpc.NexusNPC;
import com.talesforge.nexusnpc.client.gui.EditorSession;
import com.talesforge.nexusnpc.client.gui.section.NpcGuiContext;
import com.talesforge.nexusnpc.client.gui.section.NpcGuiSection;
import com.talesforge.nexusnpc.client.gui.section.NpcGuiSectionFactory;
import com.talesforge.nexusnpc.npc.field.NpcDataMap;
import com.talesforge.nexusnpc.npc.field.NpcSettingFields;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.function.Consumer;

/**
 * Baby or adult, and whether a baby grows up. Owns NpcSettingFields.BABY and .AGE_LOCK. Shown for ordinary mobs only
 * (NexusNPC's own humanoid has no baby form: a child there is a shorter NPC). A mob without a baby form simply ignores
 * the switch.
 */
public final class AgeSection implements NpcGuiSection {
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(NexusNPC.MOD_ID, "age");

    private final EditorSession session;

    private AgeSection(EditorSession session) { this.session = session; }

    @Override
    public Component title() { return Component.translatable("gui.nexusnpc.section.age"); }

    @Override
    public int build(int x, int y, int width, Font font, Consumer<AbstractWidget> addWidget, Runnable requestRebuild) {
        boolean baby = session.get(NpcSettingFields.BABY);
        boolean locked = session.get(NpcSettingFields.AGE_LOCK);
        int half = (width - 4) / 2;

        addWidget.accept(Button.builder(
                        Component.translatable("gui.nexusnpc.age.baby").append(": ").append(baby ? CommonComponents.OPTION_ON : CommonComponents.OPTION_OFF),
                        b -> {
                            session.set(NpcSettingFields.BABY, !baby);
                            requestRebuild.run();
                        })
                .bounds(x, y, half, 20)
                .tooltip(Tooltip.create(Component.translatable("gui.nexusnpc.age.baby.tip")))
                .build());

        // Shown the right way round: "Grows up: ON" means the lock is off
        addWidget.accept(Button.builder(
                        Component.translatable("gui.nexusnpc.age.grows").append(": ").append(locked ? CommonComponents.OPTION_OFF : CommonComponents.OPTION_ON),
                        b -> {
                            session.set(NpcSettingFields.AGE_LOCK, !locked);
                            requestRebuild.run();
                        })
                .bounds(x + half + 4, y, width - half - 4, 20)
                .tooltip(Tooltip.create(Component.translatable("gui.nexusnpc.age.grows.tip")))
                .build());
        return 22;
    }

    public static final class Factory implements NpcGuiSectionFactory {
        @Override public ResourceLocation id() { return ID; }
        @Override public NpcGuiSection create(NpcDataMap initial) { return new AgeSection(EditorSession.local(initial)); }
        @Override public NpcGuiSection create(NpcDataMap initial, NpcGuiContext context) { return new AgeSection(context.session()); }
    }
}
