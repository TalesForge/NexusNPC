package com.talesforge.masternpc.api.event;

import com.talesforge.masternpc.entity.custom.NpcEntity;
import net.minecraft.world.entity.ai.goal.GoalSelector;
import net.neoforged.bus.api.Event;

/** This is called at the end of registerGoals(), including with each AI rebuild. Here you can add your own goals. */
public class NpcGoalsEvent extends Event {
    private final NpcEntity npc;
    private final GoalSelector goalSelector, targetSelector;

    public NpcGoalsEvent(NpcEntity npc, GoalSelector goalSelector, GoalSelector targetSelector) {
        this.npc = npc; this.goalSelector = goalSelector; this.targetSelector = targetSelector;
    }
    public NpcEntity getNpc() { return npc; }
    public GoalSelector getGoalSelector() { return goalSelector; }
    public GoalSelector getTargetSelector() { return targetSelector; }
}
