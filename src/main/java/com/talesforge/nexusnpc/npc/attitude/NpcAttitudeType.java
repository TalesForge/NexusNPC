package com.talesforge.nexusnpc.npc.attitude;

import com.talesforge.nexusnpc.entity.custom.NpcEntity;
import net.minecraft.world.entity.ai.goal.GoalSelector;

public interface NpcAttitudeType {
    /** Behavioral goals (attack, etc.). */
    default void createGoals(NpcEntity npc, GoalSelector goals) {}

    /** Target selection for enemies. */
    default void createTargetGoals(NpcEntity npc, GoalSelector targets) {}

    /** If false, the type cannot be selected (for example, it is prohibited by the config). */
    default boolean isEnabled() { return true; }
}
