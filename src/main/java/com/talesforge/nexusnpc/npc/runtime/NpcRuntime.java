package com.talesforge.nexusnpc.npc.runtime;

import com.talesforge.nexusnpc.npc.trade.NpcMerchant;
import net.minecraft.world.entity.ai.goal.Goal;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;

/** Transient (never saved) per-mob NPC state. Lives on the mob itself via {@link NpcHost}. */
@ApiStatus.Internal
public final class NpcRuntime {
    /** A goal as it sat in a GoalSelector, so it can be put back exactly. */
    public record SavedGoal(int priority, Goal goal) {}

    // ----- editor lock -----
    @Nullable public UUID editorId;
    public int editorLastPingTick;

    // ----- trading -----
    @Nullable public NpcMerchant merchant;

    // ----- AI override: the mob's own goals while OVERRIDE is active -----
    @Nullable public List<SavedGoal> stashedGoals;
    @Nullable public List<SavedGoal> stashedTargets;
}
