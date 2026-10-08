package com.talesforge.nexusnpc.npc.attitude;

import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.GoalSelector;

public interface NpcAttitudeType {
    /** Behavioral goals (attack, etc.). */
    default void createGoals(PathfinderMob npc, GoalSelector goals) {}

    /** Target selection for enemies. */
    default void createTargetGoals(PathfinderMob npc, GoalSelector targets) {}

    /** If false, the type cannot be selected (for example, it is prohibited by the config). */
    default boolean isEnabled() { return true; }
}
