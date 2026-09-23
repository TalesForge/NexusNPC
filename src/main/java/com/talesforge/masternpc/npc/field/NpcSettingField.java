package com.talesforge.masternpc.npc.field;

import com.mojang.serialization.Codec;
import com.talesforge.masternpc.entity.custom.NpcEntity;
import net.minecraft.resources.ResourceLocation;

/**
 * One editable / persistable "slot" of NPC data — a name, an attitude, a skin, or a
 * completely custom field an addon mod wants to attach to the NPC editor and to the
 * create/save network protocol.
 * <p>
 * There is no special-cased path for "built-in" fields: {@link NpcSettingFields} registers
 * the core ones (name, attitude, behavior, skin, stats) through exactly the same API an
 * addon would use. That is intentional — it is the proof that the API actually works.
 * <p>
 * IMPORTANT: implementations must not reference client-only classes (widgets, screens).
 * This interface and {@link NpcSettingFields} are loaded on the dedicated server too.
 * GUI representation is registered separately on the client side (see the addon example
 * for the pattern), so a server-only mod can add fields without needing any client code.
 *
 * @param <T> the value type. Must be encodable via {@link #codec()} — plain values
 *            (String, ResourceLocation, a record of primitives, etc.) are the easiest fit.
 */
public interface NpcSettingField<T> {

    /** Unique id, e.g. {@code masternpc:attitude} or {@code mymod:quest_id}. */
    ResourceLocation id();

    /** Used to (de)serialize the value for both the network protocol and NBT. */
    Codec<T> codec();

    /** Value used when nothing else is known yet (e.g. the "Create NPC" screen). */
    T defaultValue();

    /** Current value on this live NPC instance. May be called on either side. */
    T get(NpcEntity npc);

    /** Apply a new value to this NPC instance. Called only on the server. Must NOT rebuild AI itself. */
    void set(NpcEntity npc, T value);

    /** If true, {@link NpcDataMap#applyAll} calls {@code npc.refreshAi()} once after all fields are applied. */
    default boolean needsAiRefresh() {
        return false;
    }
}