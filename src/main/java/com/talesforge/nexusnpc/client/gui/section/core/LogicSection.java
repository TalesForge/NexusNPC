package com.talesforge.nexusnpc.client.gui.section.core;

import com.talesforge.nexusnpc.NexusNPC;
import com.talesforge.nexusnpc.client.gui.EditorSession;
import com.talesforge.nexusnpc.client.gui.section.NpcGuiContext;
import com.talesforge.nexusnpc.client.gui.section.NpcGuiSection;
import com.talesforge.nexusnpc.client.gui.section.NpcGuiSectionFactory;
import com.talesforge.nexusnpc.client.gui.ui.PickerScreen;
import com.talesforge.nexusnpc.client.gui.ui.TextLabel;
import com.talesforge.nexusnpc.client.gui.ui.UiTheme;
import com.talesforge.nexusnpc.npc.NpcRegistries;
import com.talesforge.nexusnpc.npc.attitude.NpcAttitudeType;
import com.talesforge.nexusnpc.npc.attitude.NpcAttitudes;
import com.talesforge.nexusnpc.npc.behavior.NpcBehaviors;
import com.talesforge.nexusnpc.npc.data.NpcAiMode;
import com.talesforge.nexusnpc.npc.field.NpcDataMap;
import com.talesforge.nexusnpc.npc.field.NpcSettingFields;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Owns NpcSettingFields.AI_MODE, .ATTITUDE and .BEHAVIOR. Each is a button that opens a full picker screen.
 * Attitude and behavior are greyed out while the mob keeps its own (vanilla) AI, because they would do nothing.
 */
public final class LogicSection implements NpcGuiSection {
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(NexusNPC.MOD_ID, "logic");

    private final EditorSession session;

    private LogicSection(EditorSession session) { this.session = session; }

    @Override
    public Component title() { return Component.translatable("gui.nexusnpc.section.ai"); }

    @Override
    public int build(int x, int y, int width, Font font, Consumer<AbstractWidget> addWidget, Runnable requestRebuild) {
        // Lists come straight from the registries, so addon-registered attitudes/behaviors show up automatically
        List<ResourceLocation> attitudes = NpcRegistries.ids(NpcRegistries.ATTITUDES,
                NpcAttitudes.DEFAULT_ID, NpcAttitudeType::isEnabled);
        List<ResourceLocation> behaviors = NpcRegistries.ids(NpcRegistries.BEHAVIORS,
                NpcBehaviors.DEFAULT_ID, b -> true);

        // Unknown id (removed mod) or disabled by config: show the fallback instead of a broken value
        ResourceLocation attitude = session.get(NpcSettingFields.ATTITUDE);
        ResourceLocation behavior = session.get(NpcSettingFields.BEHAVIOR);
        if (!attitudes.contains(attitude)) attitude = NpcAttitudes.DEFAULT_ID;
        if (!behaviors.contains(behavior)) behavior = NpcBehaviors.DEFAULT_ID;
        NpcAiMode mode = session.get(NpcSettingFields.AI_MODE);

        // --- AI mode ---
        Button modeButton = PickerScreen.openerButton(x, y, width, Component.translatable("gui.nexusnpc.ai_mode"),
                modeLabel(mode), () -> openModePicker(mode));
        addWidget.accept(modeButton);

        // --- attitude / behavior ---
        boolean active = mode == NpcAiMode.OVERRIDE;
        final ResourceLocation shownAttitude = attitude;
        final ResourceLocation shownBehavior = behavior;
        Button attitudeButton = PickerScreen.openerButton(x, y + 24, width, Component.translatable("gui.nexusnpc.attitude"),
                Component.translatable(Util.makeDescriptionId("npc_attitude", attitude)),
                () -> openPicker(Component.translatable("gui.nexusnpc.picker.attitude"), "npc_attitude", attitudes,
                        shownAttitude, id -> session.set(NpcSettingFields.ATTITUDE, id)));
        attitudeButton.active = active;
        addWidget.accept(attitudeButton);

        Button behaviorButton = PickerScreen.openerButton(x, y + 48, width, Component.translatable("gui.nexusnpc.behavior"),
                Component.translatable(Util.makeDescriptionId("npc_behavior", behavior)),
                () -> openPicker(Component.translatable("gui.nexusnpc.picker.behavior"), "npc_behavior", behaviors,
                        shownBehavior, id -> session.set(NpcSettingFields.BEHAVIOR, id)));
        behaviorButton.active = active;
        addWidget.accept(behaviorButton);

        int used = 70;
        if (!active) {
            TextLabel hint = new TextLabel(x, y + used, width,
                    Component.translatable("gui.nexusnpc.logic.inactive_hint"), UiTheme.TEXT_DIM);
            addWidget.accept(hint);
            used += hint.getHeight() + 2;
        }
        return used;
    }

    private static Component modeLabel(NpcAiMode mode) {
        return Component.translatable("gui.nexusnpc.ai_mode." + mode.getSerializedName());
    }

    private void openModePicker(NpcAiMode current) {
        List<PickerScreen.Entry<NpcAiMode>> entries = new ArrayList<>();
        for (NpcAiMode mode : NpcAiMode.values()) {
            entries.add(new PickerScreen.Entry<>(mode, modeLabel(mode),
                    Component.translatable("gui.nexusnpc.ai_mode." + mode.getSerializedName() + ".tooltip"), null));
        }
        Minecraft mc = Minecraft.getInstance();
        mc.setScreen(new PickerScreen<>(mc.screen, Component.translatable("gui.nexusnpc.picker.ai_mode"), entries, current,
                mode -> session.set(NpcSettingFields.AI_MODE, mode)));
    }

    /** Picker for registry ids whose names (and optional descriptions) come from lang keys {@code <prefix>.<ns>.<path>[.desc]}. */
    private static void openPicker(Component title, String prefix, List<ResourceLocation> ids,
                                   ResourceLocation current, Consumer<ResourceLocation> onPick) {
        List<PickerScreen.Entry<ResourceLocation>> entries = new ArrayList<>();
        for (ResourceLocation id : ids) {
            String key = Util.makeDescriptionId(prefix, id);
            Component description = I18n.exists(key + ".desc") ? Component.translatable(key + ".desc") : null;
            entries.add(new PickerScreen.Entry<>(id, Component.translatable(key), description, null));
        }
        Minecraft mc = Minecraft.getInstance();
        mc.setScreen(new PickerScreen<>(mc.screen, title, entries, current, onPick));
    }

    public static final class Factory implements NpcGuiSectionFactory {
        @Override public ResourceLocation id() { return ID; }
        @Override public NpcGuiSection create(NpcDataMap initial) { return new LogicSection(EditorSession.local(initial)); }
        @Override public NpcGuiSection create(NpcDataMap initial, NpcGuiContext context) { return new LogicSection(context.session()); }
    }
}
