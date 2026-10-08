package com.talesforge.nexusnpc.npc.runtime;

import net.minecraft.world.entity.ai.goal.GoalSelector;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

/**
 * Implemented by {@code net.minecraft.world.entity.Mob} through {@code MobMixin}. Gives NexusNPC
 * a place for TRANSIENT per-mob state (editor lock, merchant adapter, stashed goals) plus access
 * to the protected goal selectors, with no side map and no per-tick lookups.
 * <p>
 * Do not use directly — go through {@link com.talesforge.nexusnpc.npc.Npcs}. This interface lives
 * outside the mixin package on purpose: mixin packages must never be loaded by regular code.
 */
@ApiStatus.Internal
public interface NpcHost {
    /** Lazily created. */
    NpcRuntime nexusnpc$getRuntime();

    /** Never allocates — null for every mob nobody has touched. Use this on hot paths. */
    @Nullable NpcRuntime nexusnpc$peekRuntime();

    GoalSelector nexusnpc$goalSelector();

    GoalSelector nexusnpc$targetSelector();
}
