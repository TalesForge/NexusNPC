package com.talesforge.nexusnpc.npc.behavior;

import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.ai.goal.GoalSelector;
import net.minecraft.world.entity.player.Player;

public class AvoidPlayersBehavior implements NpcBehaviorType {
    @Override
    public void createGoals(PathfinderMob npc, GoalSelector goals) {
        goals.addGoal(2, new AvoidEntityGoal<>(npc, Player.class, 8.0F, 1.0, 1.4));
    }
}
