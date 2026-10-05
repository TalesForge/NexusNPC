package com.talesforge.nexusnpc.client.gui.screen;

import com.talesforge.nexusnpc.client.gui.section.NpcGuiSection;
import com.talesforge.nexusnpc.npc.field.NpcDataMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

public class CustomScreen extends Screen {

    protected final Screen parent;

    protected NpcDataMap initial;
    private final List<NpcGuiSection> activeSections = new ArrayList<>();

    private boolean childOpen = false;


    protected CustomScreen(Component title, Screen parent, NpcDataMap initial) {
        super(title);
        this.parent = parent;
        this.initial = initial;
    }

    @Override
    protected void init() {
        setChildOpen(false);

        // init() runs again when a child screen hands control back, or the window is resized.
        // The sections holding the edits are still alive here: snapshot them BEFORE clearing.
        if (!getActiveSections().isEmpty()) {
            this.initial = this.initial.withOverrides(collectAll());
        }
        clearActiveSections();
    }

    // This is triggered by ANY screen closure: buttons, Esc, replacement with another screen, or shutdown
    @Override
    public void removed() {
        super.removed();
    }

    /**
     * Re-lays out the whole screen — a different NPC type or a different model can mean a
     * different set of active sections / valid values. Snapshots every currently active
     * section's in-progress edits into {@code initial} first, so nothing the player already
     * typed elsewhere gets discarded just because one unrelated widget changed.
     */
    protected void rebuild() {
        this.clearWidgets();
        this.init();
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().setScreen(parent);
    }

    /**
     * Only ever contains what the currently active sections chose to write. A section that
     * isn't active for this NPC type (or belongs to an addon not present at all) never gets
     * asked, so its field is simply absent here — and NpcDataMap#applyAll on the server
     * leaves anything absent completely untouched. Nothing gets silently reset.
     */
    protected NpcDataMap collectAll() {
        NpcDataMap data = NpcDataMap.empty();
        for (NpcGuiSection section : getActiveSections()) {
            section.collect(data);
        }
        return data;
    }

    /** Opens a child screen without giving up the NPC's editing lock. */
    public void openChild(Screen child) {
        setChildOpen(true);
        Minecraft.getInstance().setScreen(child);
    }





    protected int addSectionToScreen(NpcGuiSection section, int x, int y, int w) {
        if (section == null) return 0;
        int height = section.build(x, y, w, this.font, this::addRenderableWidget, this::rebuild);
        this.addActiveSection(section);
        return height;
    }


    protected void addActiveSection(NpcGuiSection section) {
        activeSections.add(section);
    }

    protected List<NpcGuiSection> getActiveSections() {
        return activeSections;
    }

    protected void clearActiveSections() {
        activeSections.clear();
    }


    protected void setChildOpen(boolean childOpen) {
        this.childOpen = childOpen;
    }

    protected boolean childIsOpen() {
        return childOpen;
    }
}
