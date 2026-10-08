package com.talesforge.nexusnpc.client.gui;

import com.talesforge.nexusnpc.network.payload.action.SaveNpcPayload;
import com.talesforge.nexusnpc.npc.field.NpcDataMap;
import com.talesforge.nexusnpc.npc.field.NpcSettingField;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

/**
 * The client-side truth of ONE editing session: the NPC's current values plus the changes the server has not
 * heard about yet. Every widget writes here the moment its value changes ({@link #set}); changes are sent to the
 * server a fraction of a second later, batched into a single packet, so nothing a player did is ever lost to a
 * crash or a closed screen.
 * <p>
 * Three kinds exist:
 * <ul>
 *   <li>{@link #open} — editing a live NPC: changes are autosaved (this is the "current" session);</li>
 *   <li>{@link #forCreation} — nothing exists yet, values are only collected for the Create button;</li>
 *   <li>{@link #local} — a throw-away holder for screens that edit no NPC (the global settings).</li>
 * </ul>
 */
public final class EditorSession {
    /** Ticks without a new change before the pending ones are sent (20 ticks = 1 s). */
    private static final int DEBOUNCE_TICKS = 8;

    @Nullable private static EditorSession current;

    private final int entityId;
    private ResourceLocation typeId;
    private final NpcDataMap values;
    private final NpcDataMap pending = NpcDataMap.empty();
    private int idleTicks = 0;
    private boolean touched = false;

    private EditorSession(int entityId, ResourceLocation typeId, NpcDataMap values) {
        this.entityId = entityId;
        this.typeId = typeId;
        this.values = values;
    }

    // ================= factories =================

    /** Start autosaving edits of a live NPC. Replaces (and first flushes) any previous session. */
    public static EditorSession open(int entityId, ResourceLocation typeId, NpcDataMap initial) {
        if (current != null) current.flush();
        EditorSession session = new EditorSession(entityId, typeId, initial.copy());
        current = session;
        EditorKeepAlive.start(entityId);
        return session;
    }

    public static EditorSession forCreation(ResourceLocation typeId, NpcDataMap initial) {
        return new EditorSession(-1, typeId, initial.copy());
    }

    public static EditorSession local(NpcDataMap initial) {
        return new EditorSession(-1, ResourceLocation.withDefaultNamespace("none"), initial.copy());
    }

    // ================= state =================

    public int entityId() { return entityId; }

    public boolean creating() { return entityId < 0; }

    public ResourceLocation typeId() { return typeId; }

    public void setTypeId(ResourceLocation typeId) { this.typeId = typeId; }

    /** Live view of every value. Treat as read-only; write through {@link #set}. */
    public NpcDataMap values() { return values; }

    public <T> T get(NpcSettingField<T> field) { return values.get(field); }

    /** Record a new value. Does nothing if it is the same as the current one. */
    public <T> void set(NpcSettingField<T> field, T value) {
        if (!values.putIfChanged(field, value)) return;
        markPending(field.id(), value);
    }

    /**
     * Merge values written by sections that only implement {@code collect} (older addon sections). Only values that
     * actually differ from what the session already holds become changes.
     */
    public void merge(NpcDataMap other) {
        for (ResourceLocation id : other.keys()) {
            Object value = other.raw(id);
            if (value == null || Objects.equals(values.raw(id), value)) continue;
            values.putRaw(id, value);
            markPending(id, value);
        }
    }

    private void markPending(ResourceLocation id, Object value) {
        touched = true;
        if (creating()) return;   // Nothing to save to yet
        pending.putRaw(id, value);
        idleTicks = 0;
    }

    // ================= saving =================

    public boolean hasPending() { return !pending.isEmpty(); }

    /** Whether this session saves by itself (an existing NPC), as opposed to waiting for a Create button. */
    public boolean autosaves() { return !creating() && this == current; }

    public boolean touched() { return touched; }

    /** Send everything not yet sent, right now. */
    public void flush() {
        if (creating() || pending.isEmpty()) return;
        if (Minecraft.getInstance().getConnection() != null) {
            PacketDistributor.sendToServer(new SaveNpcPayload(entityId, pending.copy()));
        }
        pending.clear();
    }

    private void tick() {
        if (pending.isEmpty()) return;
        if (++idleTicks >= DEBOUNCE_TICKS) flush();
    }

    // ================= lifecycle (driven by EditorKeepAlive) =================

    public static void tickCurrent() {
        if (current != null) current.tick();
    }

    /** The editing screens are gone: flush what is left and forget the session. */
    public static void endCurrent() {
        if (current == null) return;
        current.flush();
        current = null;
    }
}
