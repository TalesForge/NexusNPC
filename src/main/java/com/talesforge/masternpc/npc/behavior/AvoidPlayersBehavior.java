package com.talesforge.masternpc.npc.behavior;

import com.talesforge.masternpc.entity.custom.NpcEntity;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.ai.goal.GoalSelector;
import net.minecraft.world.entity.player.Player;

public class AvoidPlayersBehavior implements NpcBehaviorType {
    @Override
    public void createGoals(NpcEntity npc, GoalSelector goals) {
        goals.addGoal(2, new AvoidEntityGoal<>(npc, Player.class, 8.0F, 1.0, 1.4));
    }
}
