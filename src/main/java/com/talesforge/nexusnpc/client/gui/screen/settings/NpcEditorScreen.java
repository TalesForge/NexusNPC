package com.talesforge.nexusnpc.client.gui.screen.settings;

import com.talesforge.nexusnpc.api.NexusNpcApi;
import com.talesforge.nexusnpc.client.gui.EditorSession;
import com.talesforge.nexusnpc.client.gui.NpcEditingScreen;
import com.talesforge.nexusnpc.client.gui.section.NpcGuiContext;
import com.talesforge.nexusnpc.client.gui.section.NpcGuiRegistry;
import com.talesforge.nexusnpc.client.gui.section.NpcGuiSection;
import com.talesforge.nexusnpc.client.gui.section.NpcGuiSectionFactory;
import com.talesforge.nexusnpc.client.gui.section.core.DialogueLibrarySection;
import com.talesforge.nexusnpc.client.gui.section.core.DialogueListSection;
import com.talesforge.nexusnpc.client.gui.ui.Heading;
import com.talesforge.nexusnpc.client.gui.ui.PanelScreen;
import com.talesforge.nexusnpc.client.gui.ui.PickerScreen;
import com.talesforge.nexusnpc.client.gui.ui.ScrollPanel;
import com.talesforge.nexusnpc.client.gui.ui.UiTheme;
import com.talesforge.nexusnpc.network.payload.action.DeleteNpcPayload;
import com.talesforge.nexusnpc.npc.field.NpcDataMap;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

/**
 * The standard NPC editor. It does not know what "name" or "attitude" are: it asks {@link NpcGuiRegistry} which
 * sections are active for the NPC type and stacks them in a scrolling panel, so it fits any window size.
 * <p>
 * Editing an existing NPC saves EVERYTHING AS YOU GO: sections write each change to the {@link EditorSession},
 * which sends it to the server a moment later. There is no Save button — "Done" just closes the screen.
 * Creating an NPC is different (nothing exists yet): values are collected and "Create" spawns it.
 * <p>
 * An addon that wants a totally different layout registers an {@code NpcGuiScreenFactory} override instead —
 * see {@code ClientPayloadHandler}.
 */
public class NpcEditorScreen extends PanelScreen implements NpcEditingScreen {

    private final EditorSession session;
    private final BiConsumer<ResourceLocation, NpcDataMap> onConfirm;   // Used by Create only
    private final List<NpcGuiSection> activeSections = new ArrayList<>();
    private boolean confirmDelete = false;

    public NpcEditorScreen(Component title, EditorSession session, BiConsumer<ResourceLocation, NpcDataMap> onConfirm) {
        super(title, null);
        this.session = session;
        this.onConfirm = onConfirm;
    }

    /** Kept for addons that build the standard screen themselves. */
    public NpcEditorScreen(Component title, NpcDataMap initial, boolean creating, int entityId,
                           ResourceLocation typeId, BiConsumer<ResourceLocation, NpcDataMap> onConfirm) {
        this(title, creating ? EditorSession.forCreation(typeId, initial) : EditorSession.open(entityId, typeId, initial), onConfirm);
    }

    // ================= layout =================

    @Override
    protected int buildBody(ScrollPanel body, int x, int width) {
        activeSections.clear();
        int y = 0;

        List<ResourceLocation> types = NexusNpcApi.typeIds();
        if (session.creating() && types.size() > 1) {
            body.add(new Heading(x, y, width, Component.translatable("gui.nexusnpc.type")));
            y += Heading.HEIGHT + 2;
            body.add(PickerScreen.openerButton(x, y, width, Component.translatable("gui.nexusnpc.type"),
                    typeName(session.typeId()), this::openTypePicker));
            y += 26;
        }

        NpcGuiContext context = new NpcGuiContext(session.entityId(), session.creating(), session.typeId(), session);
        for (NpcGuiSectionFactory factory : NpcGuiRegistry.activeSections(session.typeId())) {
            NpcGuiSection section = factory.create(session.values(), context);

            boolean shouldAdd = switch (section) {
                case DialogueListSection s -> !session.creating();   // A dialogue needs an NPC to belong to
                case DialogueLibrarySection s -> false;               // The library lives in the Settings screen
                default -> true;
            };
            if (!shouldAdd) continue;

            Component heading = section.title();
            int headingH = heading != null ? Heading.HEIGHT + 2 : 0;
            int used = section.build(x, y + headingH, width, font, body::add, this::requestRebuild);
            if (used <= 0) continue;   // The section has nothing to show (e.g. trades while creating)
            if (heading != null) body.add(new Heading(x, y, width, heading));
            activeSections.add(section);
            y += headingH + used + 8;
        }
        return y;
    }

    @Override
    protected void buildFooter(int x, int y, int width) {
        if (session.creating()) {
            int half = (width - 4) / 2;
            footerButton(Component.translatable("gui.nexusnpc.create"), x, y, half, b -> create());
            footerButton(Component.translatable("gui.cancel"), x + half + 4, y, width - half - 4, b -> onClose());
        } else {
            int deleteW = Math.max(60, width * 2 / 5);
            Button delete = footerButton(Component.translatable("gui.nexusnpc.delete"), x, y, deleteW, b -> {
                if (!confirmDelete) {
                    confirmDelete = true;
                    b.setMessage(Component.translatable("gui.nexusnpc.delete.confirm").withStyle(ChatFormatting.RED));
                    return;
                }
                session.flush();
                PacketDistributor.sendToServer(new DeleteNpcPayload(session.entityId()));
                onClose();
            });
            if (confirmDelete) delete.setMessage(Component.translatable("gui.nexusnpc.delete.confirm").withStyle(ChatFormatting.RED));
            footerButton(Component.translatable("gui.done"), x + deleteW + 4, y, width - deleteW - 4, b -> onClose());
        }
    }

    // ================= status line =================

    @Override
    @Nullable
    protected Component status() {
        if (session.creating()) return null;
        if (session.hasPending()) return Component.translatable("gui.nexusnpc.status.saving");
        return Component.translatable(session.touched() ? "gui.nexusnpc.status.saved" : "gui.nexusnpc.status.autosave");
    }

    @Override
    protected int statusColor() {
        if (session.hasPending()) return UiTheme.TEXT_WARN;
        return session.touched() ? UiTheme.TEXT_OK : UiTheme.TEXT_DIM;
    }

    // ================= actions =================

    private static Component typeName(ResourceLocation id) {
        return Component.translatable(Util.makeDescriptionId("entity", id));
    }

    private void openTypePicker() {
        List<PickerScreen.Entry<ResourceLocation>> entries = new ArrayList<>();
        for (ResourceLocation id : NexusNpcApi.typeIds()) entries.add(new PickerScreen.Entry<>(id, typeName(id)));
        Minecraft.getInstance().setScreen(new PickerScreen<>(this, Component.translatable("gui.nexusnpc.type"),
                entries, session.typeId(), picked -> session.setTypeId(picked)));   // A different type may have different sections
    }

    /** Sections written the old way (private state + collect) are folded in before anything is sent. */
    private void collectLegacy() {
        NpcDataMap collected = NpcDataMap.empty();
        for (NpcGuiSection section : activeSections) section.collect(collected);
        session.merge(collected);
    }

    private void create() {
        collectLegacy();
        onConfirm.accept(session.typeId(), session.values().copy());
        onClose();
    }

    @Override
    public void removed() {
        super.removed();
        // Leaving for a picker/dialogue screen also lands here; merging only sends what actually differs
        if (!session.creating()) collectLegacy();
    }
}
