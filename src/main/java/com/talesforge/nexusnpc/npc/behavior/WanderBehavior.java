package com.talesforge.nexusnpc.npc.behavior;

import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.GoalSelector;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;

public class WanderBehavior implements NpcBehaviorType {
    private final double speed;
    public WanderBehavior(double speed) { this.speed = speed; }

    @Override
    public void createGoals(PathfinderMob npc, GoalSelector goals) {
        goals.addGoal(5, new WaterAvoidingRandomStrollGoal(npc, speed));
    }
}
