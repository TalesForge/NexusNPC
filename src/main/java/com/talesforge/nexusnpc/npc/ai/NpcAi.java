package com.talesforge.nexusnpc.npc.ai;

import com.talesforge.nexusnpc.NexusNPC;
import com.talesforge.nexusnpc.api.event.NpcGoalsEvent;
import com.talesforge.nexusnpc.npc.NpcRegistries;
import com.talesforge.nexusnpc.npc.Npcs;
import com.talesforge.nexusnpc.npc.attitude.NpcAttitudeType;
import com.talesforge.nexusnpc.npc.data.NpcAiMode;
import com.talesforge.nexusnpc.npc.runtime.NpcHost;
import com.talesforge.nexusnpc.npc.runtime.NpcRuntime;
import com.talesforge.nexusnpc.npc.runtime.NpcRuntime.SavedGoal;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.GoalSelector;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WrappedGoal;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.common.NeoForge;

import java.util.ArrayList;
import java.util.List;

/**
 * Applies / removes the NPC goal set on an arbitrary mob.
 * <p>
 * We can't hook {@code registerGoals()}: most Mob subclasses override it without calling super, so an
 * injection into Mob would never run for them. Instead the goals are installed after the mob exists
 * (see NpcEvents#onJoin) and whenever the settings change.
 * <p>
 * In {@link NpcAiMode#OVERRIDE} the mob's own goals are snapshotted first and put back by
 * {@link #restore}, so switching a cow back to {@code VANILLA} (or removing its NPC data) gives the
 * cow its cow AI again. Brain-driven mobs (villager, piglin, warden, allay, frog...) are never overridden —
 * their behavior comes from the Brain, so goal-based attitudes/behaviors would only fight it.
 */
public final class NpcAi {
    private NpcAi() {}

    /** True for mobs whose behavior is driven by a Brain (they have active Brain activities). */
    public static boolean isBrainDriven(Mob mob) {
        return !mob.getBrain().getActiveActivities().isEmpty();
    }

    /** Whether OVERRIDE can do anything for this mob. The editor can use this to grey out the option. */
    public static boolean supportsOverride(Mob mob) {
        return mob instanceof PathfinderMob && !isBrainDriven(mob);
    }

    /** (Re)build the AI according to the mob's current NPC data. Server only. */
    public static void apply(Mob mob) {
        if (mob.level().isClientSide()) return;

        restore(mob);  // Undo a previous override first, so this is idempotent
        if (!Npcs.isNpc(mob) || !supportsOverride(mob) || Npcs.aiMode(mob) != NpcAiMode.OVERRIDE) return;

        PathfinderMob pathfinder = (PathfinderMob) mob;
        NpcHost host = (NpcHost) mob;
        NpcRuntime rt = Npcs.runtime(mob);

        rt.stashedGoals = snapshot(host.nexusnpc$goalSelector());
        rt.stashedTargets = snapshot(host.nexusnpc$targetSelector());
        try {
            host.nexusnpc$goalSelector().removeAllGoals(g -> true);
            host.nexusnpc$targetSelector().removeAllGoals(g -> true);
            mob.setTarget(null);
            install(pathfinder, host.nexusnpc$goalSelector(), host.nexusnpc$targetSelector());
        } catch (RuntimeException e) {
            // Some modded mob did not like our goals — put its own AI back rather than leave it brainless
            NexusNPC.LOGGER.error("Could not apply NPC AI to {}; keeping its own AI", mob.getType(), e);
            restore(mob);
        }
    }

    private static void install(PathfinderMob mob, GoalSelector goals, GoalSelector targets) {
        goals.addGoal(0, new FloatGoal(mob));  // Not to drown

        NpcAttitudeType attitude = NpcRegistries.attitude(Npcs.attitudeId(mob));
        attitude.createGoals(mob, goals);
        attitude.createTargetGoals(mob, targets);

        NpcRegistries.behavior(Npcs.behaviorId(mob)).createGoals(mob, goals);

        goals.addGoal(6, new LookAtPlayerGoal(mob, Player.class, 6.0F));
        goals.addGoal(7, new RandomLookAroundGoal(mob));

        NeoForge.EVENT_BUS.post(new NpcGoalsEvent(mob, goals, targets));
    }

    /** Put back the mob's own goals if an override is active; no-op otherwise. */
    public static void restore(Mob mob) {
        NpcRuntime rt = Npcs.peekRuntime(mob);
        if (rt == null || rt.stashedGoals == null) return;

        NpcHost host = (NpcHost) mob;
        host.nexusnpc$goalSelector().removeAllGoals(g -> true);
        host.nexusnpc$targetSelector().removeAllGoals(g -> true);
        for (SavedGoal saved : rt.stashedGoals) host.nexusnpc$goalSelector().addGoal(saved.priority(), saved.goal());
        if (rt.stashedTargets != null) {
            for (SavedGoal saved : rt.stashedTargets) host.nexusnpc$targetSelector().addGoal(saved.priority(), saved.goal());
        }
        rt.stashedGoals = null;
        rt.stashedTargets = null;
        mob.setTarget(null);
    }

    private static List<SavedGoal> snapshot(GoalSelector selector) {
        List<SavedGoal> saved = new ArrayList<>();
        for (WrappedGoal wrapped : selector.getAvailableGoals()) {
            saved.add(new SavedGoal(wrapped.getPriority(), wrapped.getGoal()));
        }
        return saved;
    }
}
