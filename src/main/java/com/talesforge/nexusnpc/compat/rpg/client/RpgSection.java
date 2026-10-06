package com.talesforge.nexusnpc.compat.rpg.client;

import com.talesforge.nexusnpc.NexusNPC;
import com.talesforge.nexusnpc.client.gui.section.NpcGuiSection;
import com.talesforge.nexusnpc.client.gui.section.NpcGuiSectionFactory;
import com.talesforge.nexusnpc.npc.field.NpcDataMap;
import com.talesforge.nexusnpc.compat.rpg.RpgSettingFields;
import com.talesforge.nexusrpg.api.NexusRPGRegistries;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.CycleButton;
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
 * Owns RpgSettingFields.FACTION, .CLASSES and .TEAM (NexusRPG data). Compact on purpose (two rows):
 * the faction is picked from the synced registry, classes are typed as a comma-separated list of ids and the
 * team by name. Invalid ids/names are simply ignored by the server.
 */
public final class RpgSection implements NpcGuiSection {
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(NexusNPC.MOD_ID, "rpg");
    private static final ResourceLocation NONE = ResourceLocation.fromNamespaceAndPath(NexusNPC.MOD_ID, "none");

    private String faction;
    private String classes;
    private String team;

    private RpgSection(NpcDataMap initial) {
        this.faction = initial.get(RpgSettingFields.FACTION);
        this.classes = String.join(", ", initial.get(RpgSettingFields.CLASSES));
        this.team = initial.get(RpgSettingFields.TEAM);
    }

    @Override
    public int build(int x, int y, int width, Font font, Consumer<AbstractWidget> addWidget, Runnable requestRebuild) {
        // Factions come from the registry synced by the server, so datapack/addon factions show up automatically
        List<ResourceLocation> values = new ArrayList<>();
        values.add(NONE);
        ClientLevel level = Minecraft.getInstance().level;
        if (level != null) {
            level.registryAccess().registry(NexusRPGRegistries.FACTION_KEY).ifPresent(registry ->
                    registry.keySet().stream().sorted(Comparator.comparing(ResourceLocation::toString)).forEach(values::add));
        }
        ResourceLocation current = faction.isBlank() ? NONE : ResourceLocation.tryParse(faction);
        if (current == null) current = NONE;
        if (!values.contains(current)) values.add(current);  // Keep a faction we cannot list instead of silently dropping it

        addWidget.accept(CycleButton.<ResourceLocation>builder(RpgSection::factionLabel)
                .withValues(values)
                .withInitialValue(current)
                .create(x, y, width, 20, Component.translatable("gui.nexusnpc.faction"),
                        (btn, value) -> this.faction = value.equals(NONE) ? "" : value.toString()));

        int classesWidth = width * 3 / 5;
        EditBox classesBox = new EditBox(font, x, y + 24, classesWidth, 20, Component.translatable("gui.nexusnpc.classes"));
        classesBox.setMaxLength(200);
        classesBox.setHint(Component.translatable("gui.nexusnpc.classes"));
        classesBox.setValue(classes);
        classesBox.setResponder(v -> this.classes = v);
        addWidget.accept(classesBox);

        EditBox teamBox = new EditBox(font, x + classesWidth + 4, y + 24, width - classesWidth - 4, 20,
                Component.translatable("gui.nexusnpc.team"));
        teamBox.setMaxLength(32);
        teamBox.setHint(Component.translatable("gui.nexusnpc.team"));
        teamBox.setValue(team);
        teamBox.setResponder(v -> this.team = v);
        addWidget.accept(teamBox);

        return 48;
    }

    /** Translated name if the id has a lang entry (faction.<namespace>.<path>), otherwise the plain id. */
    private static Component factionLabel(ResourceLocation id) {
        if (id.equals(NONE)) return Component.translatable("gui.nexusnpc.none");
        String key = Util.makeDescriptionId("faction", id);
        return I18n.exists(key) ? Component.translatable(key) : Component.literal(id.toString());
    }

    @Override
    public void collect(NpcDataMap out) {
        out.put(RpgSettingFields.FACTION, faction.trim());
        out.put(RpgSettingFields.CLASSES, Arrays.stream(classes.split(","))
                .map(String::trim).filter(s -> !s.isEmpty()).distinct().toList());
        out.put(RpgSettingFields.TEAM, team.trim());
    }

    public static final class Factory implements NpcGuiSectionFactory {
        @Override public ResourceLocation id() { return ID; }
        @Override public NpcGuiSection create(NpcDataMap initial) { return new RpgSection(initial); }
    }
}
