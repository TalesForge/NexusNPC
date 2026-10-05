package com.talesforge.nexusnpc.npc.behavior;

import com.talesforge.nexusnpc.entity.custom.NpcEntity;
import net.minecraft.world.entity.ai.goal.GoalSelector;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;

public class WanderBehavior implements NpcBehaviorType {
    private final double speed;
    public WanderBehavior(double speed) { this.speed = speed; }

    @Override
    public void createGoals(NpcEntity npc, GoalSelector goals) {
        goals.addGoal(5, new WaterAvoidingRandomStrollGoal(npc, speed));
    }
}
