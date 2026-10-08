package com.talesforge.nexusnpc.client.gui.screen.settings;

import com.talesforge.nexusnpc.client.gui.EditorSession;
import com.talesforge.nexusnpc.client.gui.NpcEditingScreen;
import com.talesforge.nexusnpc.client.gui.section.NpcGuiContext;
import com.talesforge.nexusnpc.client.gui.section.NpcGuiRegistry;
import com.talesforge.nexusnpc.client.gui.section.NpcGuiSection;
import com.talesforge.nexusnpc.client.gui.section.NpcGuiSectionFactory;
import com.talesforge.nexusnpc.client.gui.section.core.DialogueLibrarySection;
import com.talesforge.nexusnpc.client.gui.section.core.TradeSection;
import com.talesforge.nexusnpc.client.gui.ui.Heading;
import com.talesforge.nexusnpc.client.gui.ui.PanelScreen;
import com.talesforge.nexusnpc.client.gui.ui.ScrollPanel;
import com.talesforge.nexusnpc.npc.field.NpcDataMap;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/**
 * Global (not per-NPC) settings, opened with the Gear of Settings. Same scrolling panel as the NPC editor, so it
 * fits every window; it hosts the sections that make sense without an NPC (today: the dialogue library).
 */
public class SettingsScreen extends PanelScreen implements NpcEditingScreen {

    private final ResourceLocation typeId;
    private final EditorSession session;

    public SettingsScreen(NpcDataMap initial, ResourceLocation typeId) {
        super(Component.translatable("gui.nexusnpc.settings.title"), null);
        this.typeId = typeId;
        this.session = EditorSession.local(initial);
    }

    @Override
    protected int buildBody(ScrollPanel body, int x, int width) {
        int y = 0;
        NpcGuiContext context = new NpcGuiContext(-1, false, typeId, session);
        for (NpcGuiSectionFactory factory : NpcGuiRegistry.activeSections(typeId)) {
            NpcGuiSection section = factory.create(session.values(), context);

            boolean shouldAdd = switch (section) {
                case DialogueLibrarySection s -> true;
                case TradeSection s -> true;
                default -> false;
            };
            if (!shouldAdd) continue;

            Component heading = section.title();
            int headingH = heading != null ? Heading.HEIGHT + 2 : 0;
            int used = section.build(x, y + headingH, width, font, body::add, this::requestRebuild);
            if (used <= 0) continue;
            if (heading != null) body.add(new Heading(x, y, width, heading));
            y += headingH + used + 8;
        }
        return y;
    }

    @Override
    protected void buildFooter(int x, int y, int width) {
        footerButton(Component.translatable("gui.done"), x, y, width, b -> onClose());
    }
}
