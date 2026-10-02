package com.talesforge.nexusnpc.npc.behavior;

import com.talesforge.nexusnpc.entity.custom.NpcEntity;
import net.minecraft.world.entity.ai.goal.GoalSelector;

public interface NpcBehaviorType {
    /** Adds AI goals for this behavior. */
    void createGoals(NpcEntity npc, GoalSelector goals);
}
