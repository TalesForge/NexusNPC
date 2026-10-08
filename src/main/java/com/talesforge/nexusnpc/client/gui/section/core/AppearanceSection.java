package com.talesforge.nexusnpc.client.gui.section.core;

import com.talesforge.nexusnpc.NexusNPC;
import com.talesforge.nexusnpc.client.gui.EditorSession;
import com.talesforge.nexusnpc.client.gui.section.NpcGuiContext;
import com.talesforge.nexusnpc.client.gui.section.NpcGuiSection;
import com.talesforge.nexusnpc.client.gui.section.NpcGuiSectionFactory;
import com.talesforge.nexusnpc.client.gui.ui.ListRow;
import com.talesforge.nexusnpc.client.gui.ui.PickerScreen;
import com.talesforge.nexusnpc.npc.NpcRegistries;
import com.talesforge.nexusnpc.npc.field.NpcDataMap;
import com.talesforge.nexusnpc.npc.field.NpcSettingFields;
import com.talesforge.nexusnpc.npc.model.NpcModelSkins;
import com.talesforge.nexusnpc.npc.model.NpcModels;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Owns NpcSettingFields.MODEL and .SKIN. Textures only make sense for the model they were painted for (see
 * {@code NpcModelSkins}), so choosing another model also resets the skin to that model's default. Both open full
 * picker screens; the editor re-lays itself out when the picker returns, so the skin button always reflects the
 * model that is set now.
 */
public final class AppearanceSection implements NpcGuiSection {
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(NexusNPC.MOD_ID, "appearance");

    private final EditorSession session;

    private AppearanceSection(EditorSession session) { this.session = session; }

    @Override
    public Component title() { return Component.translatable("gui.nexusnpc.section.appearance"); }

    @Override
    public int build(int x, int y, int width, Font font, Consumer<AbstractWidget> addWidget, Runnable requestRebuild) {
        // Comes straight from the registry, so addon-registered models show up automatically
        List<ResourceLocation> models = NpcRegistries.ids(NpcRegistries.MODELS, NpcModels.DEFAULT_ID, m -> true);
        ResourceLocation model = session.get(NpcSettingFields.MODEL);
        if (!models.contains(model)) model = NpcModels.DEFAULT_ID;
        String skin = NpcModelSkins.validate(model, session.get(NpcSettingFields.SKIN));
        final ResourceLocation shownModel = model;
        final String shownSkin = skin;

        addWidget.accept(PickerScreen.openerButton(x, y, width, Component.translatable("gui.nexusnpc.model"),
                Component.translatable(Util.makeDescriptionId("npc_model", model)),
                () -> openModelPicker(models, shownModel)));

        addWidget.accept(PickerScreen.openerButton(x, y + 24, width, Component.translatable("gui.nexusnpc.skin"),
                NpcModelSkins.label(skin),
                () -> openSkinPicker(shownModel, shownSkin)));
        return 48;
    }

    private void openModelPicker(List<ResourceLocation> models, ResourceLocation current) {
        List<PickerScreen.Entry<ResourceLocation>> entries = new ArrayList<>();
        for (ResourceLocation id : models) {
            String key = Util.makeDescriptionId("npc_model", id);
            Component description = I18n.exists(key + ".desc") ? Component.translatable(key + ".desc") : null;
            entries.add(new PickerScreen.Entry<>(id, Component.translatable(key), description, null));
        }
        Minecraft mc = Minecraft.getInstance();
        mc.setScreen(new PickerScreen<>(mc.screen, Component.translatable("gui.nexusnpc.picker.model"), entries, current, picked -> {
            if (picked.equals(current)) return;
            session.set(NpcSettingFields.MODEL, picked);
            // The old skin is almost certainly invalid for the new model: start from that model's own default
            session.set(NpcSettingFields.SKIN, NpcModelSkins.all(picked).get(0));
        }));
    }

    private void openSkinPicker(ResourceLocation model, String current) {
        List<PickerScreen.Entry<String>> entries = new ArrayList<>();
        for (String skin : NpcModelSkins.all(model)) {
            ResourceLocation texture = ResourceLocation.tryParse(skin);
            ListRow.Icon icon = texture == null ? null : (g, ix, iy, size) -> {
                // The face of a standard 64x64 player-layout skin, with its hat layer on top
                g.blit(texture, ix, iy, size, size, 8f, 8f, 8, 8, 64, 64);
                g.blit(texture, ix, iy, size, size, 40f, 8f, 8, 8, 64, 64);
            };
            entries.add(new PickerScreen.Entry<>(skin, NpcModelSkins.label(skin), null, icon));
        }
        Minecraft mc = Minecraft.getInstance();
        mc.setScreen(new PickerScreen<>(mc.screen, Component.translatable("gui.nexusnpc.picker.skin"), entries, current,
                picked -> session.set(NpcSettingFields.SKIN, picked)));
    }

    public static final class Factory implements NpcGuiSectionFactory {
        @Override public ResourceLocation id() { return ID; }
        @Override public NpcGuiSection create(NpcDataMap initial) { return new AppearanceSection(EditorSession.local(initial)); }
        @Override public NpcGuiSection create(NpcDataMap initial, NpcGuiContext context) { return new AppearanceSection(context.session()); }
    }
}
