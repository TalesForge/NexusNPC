package com.talesforge.nexusnpc.api.event;

import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.GoalSelector;
import net.neoforged.bus.api.Event;

/** Fired every time NexusNPC (re)builds an NPC's goals in OVERRIDE mode (never in VANILLA mode, where the mob keeps its own goals). Here you can add your own goals. */
public class NpcGoalsEvent extends Event {
    private final PathfinderMob npc;
    private final GoalSelector goalSelector, targetSelector;

    public NpcGoalsEvent(PathfinderMob npc, GoalSelector goalSelector, GoalSelector targetSelector) {
        this.npc = npc; this.goalSelector = goalSelector; this.targetSelector = targetSelector;
    }
    public PathfinderMob getNpc() { return npc; }
    public GoalSelector getGoalSelector() { return goalSelector; }
    public GoalSelector getTargetSelector() { return targetSelector; }
}
