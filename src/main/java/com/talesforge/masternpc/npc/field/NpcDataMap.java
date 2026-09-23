package com.talesforge.masternpc.npc.field;

import com.talesforge.masternpc.entity.custom.NpcEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Open, extensible replacement for the old fixed {@code NpcSettings} record.
 * <p>
 * Holds a snapshot of {@link NpcSettingField} values keyed by field id, and knows how to
 * (de)serialize itself to a single {@link CompoundTag} — used as the wire format for the
 * editor's open/save packets. Adding a new field (core or addon) never requires touching
 * this class, the network payloads, or the packet handlers.
 * <p>
 * Unknown ids — a field registered by a mod that isn't installed on the receiving side —
 * are silently skipped on read. This keeps the protocol forward- and cross-compatible:
 * a server without some addon simply never sees that addon's field, instead of crashing.
 */
public final class NpcDataMap {
    public static final StreamCodec<FriendlyByteBuf, NpcDataMap> STREAM_CODEC = StreamCodec.of(
            (buf, data) -> buf.writeNbt(data.toTag()),
            buf -> fromTag(buf.readNbt())
    );

    private final Map<ResourceLocation, Object> values = new LinkedHashMap<>();

    private NpcDataMap() {}

    /**
     * An empty map: {@link #applyAll} on it touches nothing. Use this as the base when
     * building an outgoing map that only sets a handful of fields (e.g. from a GUI screen
     * that only has widgets for some of the registered fields) — it guarantees any field
     * you don't explicitly {@link #put} is left completely untouched on the NPC, instead of
     * being silently reset to its default. {@link #defaults()} is for the opposite case:
     * showing sensible starting values in a brand-new "create NPC" screen.
     */
    public static NpcDataMap empty() {
        return new NpcDataMap();
    }

    /** Every registered field at its {@link NpcSettingField#defaultValue()} — used by the "create NPC" screen. */
    public static NpcDataMap defaults() {
        NpcDataMap data = new NpcDataMap();
        for (NpcSettingField<?> field : NpcSettingFields.all()) {
            data.putDefault(field);
        }
        return data;
    }

    private <T> void putDefault(NpcSettingField<T> field) {
        values.put(field.id(), field.defaultValue());
    }

    /** Snapshot every registered field's current value from a live NPC — the new {@code NpcEntity#getSettings()}. */
    public static NpcDataMap capture(NpcEntity npc) {
        NpcDataMap data = new NpcDataMap();
        for (NpcSettingField<?> field : NpcSettingFields.all()) {
            data.captureOne(field, npc);
        }
        return data;
    }

    private <T> void captureOne(NpcSettingField<T> field, NpcEntity npc) {
        values.put(field.id(), field.get(npc));
    }

    @SuppressWarnings("unchecked")
    public <T> T get(NpcSettingField<T> field) {
        Object value = values.get(field.id());
        return value != null ? (T) value : field.defaultValue();
    }

    public <T> void put(NpcSettingField<T> field, T value) {
        values.put(field.id(), value);
    }

    /**
     * Apply every value present in this map back onto a live NPC — the new
     * {@code NpcEntity#applySettings(...)}. Server-side only.
     *
     * @param heal true when creating a fresh NPC (heal to the new max health),
     *             false when editing an existing one (health only ever clamped down).
     */
    public void applyAll(NpcEntity npc, boolean heal) {
        boolean refreshAi = false;
        for (NpcSettingField<?> field : NpcSettingFields.all()) {
            if (!values.containsKey(field.id())) continue; // absent from this map — leave untouched
            refreshAi |= applyOne(field, npc);
        }
        if (refreshAi) npc.refreshAi();

        if (heal) {
            npc.setHealth(npc.getMaxHealth());
        } else {
            npc.setHealth(Math.min(npc.getHealth(), npc.getMaxHealth()));
        }
    }

    @SuppressWarnings("unchecked")
    private <T> boolean applyOne(NpcSettingField<T> field, NpcEntity npc) {
        field.set(npc, (T) values.get(field.id()));
        return field.needsAiRefresh();
    }

    // ============================================================
    //  NBT (de)serialization — shared by the network StreamCodec above.
    //  Feel free to reuse toTag()/fromTag() for on-disk persistence too,
    //  though core fields already persist through NpcEntity's own NBT.
    // ============================================================

    public CompoundTag toTag() {
        CompoundTag tag = new CompoundTag();
        for (Map.Entry<ResourceLocation, Object> entry : values.entrySet()) {
            encodeOne(tag, NpcSettingFields.get(entry.getKey()), entry.getValue());
        }
        return tag;
    }

    @SuppressWarnings("unchecked")
    private static <T> void encodeOne(CompoundTag tag, NpcSettingField<T> field, Object value) {
        if (field == null) return; // defensive: shouldn't happen for maps we built ourselves
        field.codec().encodeStart(NbtOps.INSTANCE, (T) value)
                .resultOrPartial(err -> {})
                .ifPresent(encoded -> tag.put(field.id().toString(), encoded));
    }

    public static NpcDataMap fromTag(CompoundTag tag) {
        NpcDataMap data = new NpcDataMap();
        for (String key : tag.getAllKeys()) {
            ResourceLocation id = ResourceLocation.tryParse(key);
            NpcSettingField<?> field = id != null ? NpcSettingFields.get(id) : null;
            if (field == null) continue; // field from a mod not installed on this side — skip, don't crash
            decodeOne(data, field, tag.get(key));
        }
        return data;
    }

    private static <T> void decodeOne(NpcDataMap data, NpcSettingField<T> field, Tag raw) {
        field.codec().parse(NbtOps.INSTANCE, raw)
                .resultOrPartial(err -> {})
                .ifPresent(value -> data.values.put(field.id(), value));
    }
}