package com.talesforge.nexusnpc.npc.behavior;

import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.GoalSelector;

public interface NpcBehaviorType {
    /** Adds AI goals for this behavior. */
    void createGoals(PathfinderMob npc, GoalSelector goals);
}
