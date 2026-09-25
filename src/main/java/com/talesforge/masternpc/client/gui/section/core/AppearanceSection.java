package com.talesforge.masternpc.client.gui.section.core;

import com.talesforge.masternpc.MasterNPC;
import com.talesforge.masternpc.client.gui.section.NpcGuiSection;
import com.talesforge.masternpc.client.gui.section.NpcGuiSectionFactory;
import com.talesforge.masternpc.npc.NpcRegistries;
import com.talesforge.masternpc.npc.field.NpcDataMap;
import com.talesforge.masternpc.npc.field.NpcSettingFields;
import com.talesforge.masternpc.npc.model.NpcModelSkins;
import com.talesforge.masternpc.npc.model.NpcModels;
import net.minecraft.Util;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.function.Consumer;

/**
 * Owns NpcSettingFields.MODEL and .SKIN. Textures only make sense for the model they were
 * painted for (see {@code NpcModelSkins}), so the skin list has to follow whatever model is
 * currently picked — that's why changing the model calls {@code requestRebuild}: the whole
 * screen re-lays out, this section is recreated with the just-picked model already reflected
 * in {@code initial} (see NpcEditorScreen#rebuild), and the skin CycleButton gets built with
 * the correct value list for it, instead of stale options left over from the old model.
 */
public final class AppearanceSection implements NpcGuiSection {
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(MasterNPC.MOD_ID, "appearance");

    private ResourceLocation model;
    private String skin;

    private AppearanceSection(NpcDataMap initial) {
        this.model = initial.get(NpcSettingFields.MODEL);
        this.skin = initial.get(NpcSettingFields.SKIN);
    }

    @Override
    public int build(int x, int y, int width, Font font, Consumer<AbstractWidget> addWidget, Runnable requestRebuild) {
        // Comes straight from the registry, so addon-registered models show up automatically
        List<ResourceLocation> models = NpcRegistries.ids(NpcRegistries.MODELS, NpcModels.DEFAULT_ID, m -> true);
        if (!models.contains(model)) model = NpcModels.DEFAULT_ID;

        List<String> skins = NpcModelSkins.all(model);
        skin = NpcModelSkins.validate(model, skin);

        addWidget.accept(CycleButton.<ResourceLocation>builder(
                        id -> Component.translatable(Util.makeDescriptionId("npc_model", id)))
                .withValues(models)
                .withInitialValue(model)
                .create(x, y, width, 20, Component.translatable("gui.masternpc.model"), (btn, value) -> {
                    this.model = value;
                    // The old skin is almost certainly invalid for the new model — reset to
                    // its default rather than let requestRebuild silently keep a mismatch.
                    this.skin = NpcModelSkins.all(value).get(0);
                    requestRebuild.run();
                }));

        addWidget.accept(CycleButton.<String>builder(NpcModelSkins::label)
                .withValues(skins)
                .withInitialValue(skin)
                .create(x, y + 24, width, 20, Component.translatable("gui.masternpc.skin"),
                        (btn, value) -> this.skin = value));

        return 48;
    }

    @Override
    public void collect(NpcDataMap out) {
        out.put(NpcSettingFields.MODEL, model);
        out.put(NpcSettingFields.SKIN, skin);
    }

    public static final class Factory implements NpcGuiSectionFactory {
        @Override public ResourceLocation id() { return ID; }
        @Override public NpcGuiSection create(NpcDataMap initial) { return new AppearanceSection(initial); }
    }
}
