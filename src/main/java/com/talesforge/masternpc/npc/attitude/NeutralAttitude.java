package com.talesforge.masternpc.npc.attitude;

import com.talesforge.masternpc.entity.custom.NpcEntity;
import net.minecraft.world.entity.ai.goal.GoalSelector;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;

/** It only responds to the one who struck. */
public class NeutralAttitude implements NpcAttitudeType {
    @Override
    public void createGoals(NpcEntity npc, GoalSelector goals) {
        goals.addGoal(1, new MeleeAttackGoal(npc, 1.2, true));
    }

    @Override
    public void createTargetGoals(NpcEntity npc, GoalSelector targets) {
        targets.addGoal(1, new HurtByTargetGoal(npc));
    }
}
