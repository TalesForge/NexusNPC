package com.talesforge.nexusnpc.compat.rpg.client;

import com.talesforge.nexusnpc.NexusNPC;
import com.talesforge.nexusnpc.client.gui.EditorSession;
import com.talesforge.nexusnpc.client.gui.section.NpcGuiContext;
import com.talesforge.nexusnpc.client.gui.section.NpcGuiSection;
import com.talesforge.nexusnpc.client.gui.section.NpcGuiSectionFactory;
import com.talesforge.nexusnpc.client.gui.ui.PickerScreen;
import com.talesforge.nexusnpc.compat.rpg.RpgSettingFields;
import com.talesforge.nexusnpc.npc.field.NpcDataMap;
import com.talesforge.nexusrpg.api.NexusRPGRegistries;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;

/**
 * Owns RpgSettingFields.FACTION, .CLASSES and .TEAM (NexusRPG data). The faction is chosen on a picker screen
 * listing the synced registry; classes are typed as a comma-separated list of ids and the team by name. Invalid
 * ids/names are simply ignored by the server. Every change is written to the session as it is made.
 */
public final class RpgSection implements NpcGuiSection {
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(NexusNPC.MOD_ID, "rpg");
    private static final ResourceLocation NONE = ResourceLocation.fromNamespaceAndPath(NexusNPC.MOD_ID, "none");

    private final EditorSession session;

    private RpgSection(EditorSession session) { this.session = session; }

    @Override
    public Component title() { return Component.translatable("gui.nexusnpc.section.rpg"); }

    @Override
    public int build(int x, int y, int width, Font font, Consumer<AbstractWidget> addWidget, Runnable requestRebuild) {
        String factionValue = session.get(RpgSettingFields.FACTION);
        ResourceLocation current = factionValue.isBlank() ? NONE : ResourceLocation.tryParse(factionValue);
        if (current == null) current = NONE;
        final ResourceLocation shown = current;

        addWidget.accept(PickerScreen.openerButton(x, y, width, Component.translatable("gui.nexusnpc.faction"),
                factionLabel(current), () -> openFactionPicker(shown)));

        EditBox classesBox = new EditBox(font, x, y + 24, width, 20, Component.translatable("gui.nexusnpc.classes"));
        classesBox.setMaxLength(200);
        classesBox.setHint(Component.translatable("gui.nexusnpc.classes"));
        classesBox.setValue(String.join(", ", session.get(RpgSettingFields.CLASSES)));
        classesBox.setResponder(v -> session.set(RpgSettingFields.CLASSES, Arrays.stream(v.split(","))
                .map(String::trim).filter(s -> !s.isEmpty()).distinct().toList()));
        addWidget.accept(classesBox);

        EditBox teamBox = new EditBox(font, x, y + 48, width, 20, Component.translatable("gui.nexusnpc.team"));
        teamBox.setMaxLength(32);
        teamBox.setHint(Component.translatable("gui.nexusnpc.team"));
        teamBox.setValue(session.get(RpgSettingFields.TEAM));
        teamBox.setResponder(v -> session.set(RpgSettingFields.TEAM, v.trim()));
        addWidget.accept(teamBox);

        return 70;
    }

    private void openFactionPicker(ResourceLocation current) {
        // Factions come from the registry synced by the server, so datapack/addon factions show up automatically
        List<ResourceLocation> values = new ArrayList<>();
        values.add(NONE);
        ClientLevel level = Minecraft.getInstance().level;
        if (level != null) {
            level.registryAccess().registry(NexusRPGRegistries.FACTION_KEY).ifPresent(registry ->
                    registry.keySet().stream().sorted(Comparator.comparing(ResourceLocation::toString)).forEach(values::add));
        }
        if (!values.contains(current)) values.add(current);  // Keep a faction we cannot list instead of silently dropping it

        List<PickerScreen.Entry<ResourceLocation>> entries = new ArrayList<>();
        for (ResourceLocation id : values) {
            entries.add(new PickerScreen.Entry<>(id, factionLabel(id),
                    id.equals(NONE) ? null : Component.literal(id.toString()), null));
        }
        Minecraft mc = Minecraft.getInstance();
        mc.setScreen(new PickerScreen<>(mc.screen, Component.translatable("gui.nexusnpc.picker.faction"), entries, current,
                picked -> session.set(RpgSettingFields.FACTION, picked.equals(NONE) ? "" : picked.toString())));
    }

    /** Translated name if the id has a lang entry (faction.<namespace>.<path>), otherwise the plain id. */
    private static Component factionLabel(ResourceLocation id) {
        if (id.equals(NONE)) return Component.translatable("gui.nexusnpc.none");
        String key = Util.makeDescriptionId("faction", id);
        return I18n.exists(key) ? Component.translatable(key) : Component.literal(id.toString());
    }

    public static final class Factory implements NpcGuiSectionFactory {
        @Override public ResourceLocation id() { return ID; }
        @Override public NpcGuiSection create(NpcDataMap initial) { return new RpgSection(EditorSession.local(initial)); }
        @Override public NpcGuiSection create(NpcDataMap initial, NpcGuiContext context) { return new RpgSection(context.session()); }
    }
}
