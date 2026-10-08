package com.talesforge.nexusnpc.npc.data;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;

/**
 * How NexusNPC treats the AI of the mob it is attached to.
 * <ul>
 *   <li>{@link #VANILLA} — the mob keeps its own AI untouched. Only dialogue, quests, trading,
 *       the editor lock etc. are layered on top. The safe default for every vanilla mob.</li>
 *   <li>{@link #OVERRIDE} — the mob's goals are replaced by the NPC attitude/behavior goals. The
 *       original goals are stashed and restored when the mode is switched back or the NPC is removed.
 *       Only works for goal-driven {@code PathfinderMob}s (not for Brain-driven ones, see NpcAi).</li>
 * </ul>
 */
public enum NpcAiMode implements StringRepresentable {
    VANILLA("vanilla"),
    OVERRIDE("override");

    public static final Codec<NpcAiMode> CODEC = StringRepresentable.fromEnum(NpcAiMode::values);

    private final String serializedName;

    NpcAiMode(String serializedName) { this.serializedName = serializedName; }

    @Override
    public String getSerializedName() { return serializedName; }
}
