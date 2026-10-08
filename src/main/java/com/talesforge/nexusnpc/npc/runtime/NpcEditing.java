package com.talesforge.nexusnpc.npc.runtime;

import com.talesforge.nexusnpc.npc.Npcs;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * The editor lock. While a player configures a mob it stands still (LivingEntityMixin#isImmobile) and
 * cannot be hurt (NpcEvents). The state is intentionally NOT saved. Works on any mob, NPC or not yet.
 */
public final class NpcEditing {
    public static final double MAX_DIST_SQ = 64.0;  // 8 blocks
    private static final int TIMEOUT_TICKS = 100;   // 5 seconds without a pulse

    private NpcEditing() {}

    public static boolean isBeingEdited(Mob mob) {
        NpcRuntime rt = Npcs.peekRuntime(mob);
        return rt != null && rt.editorId != null;
    }

    public static boolean isEditedBy(Mob mob, Player player) {
        NpcRuntime rt = Npcs.peekRuntime(mob);
        return rt != null && rt.editorId != null && rt.editorId.equals(player.getUUID());
    }

    /** @return false if another player is already configuring this mob */
    public static boolean tryStart(Mob mob, ServerPlayer player) {
        NpcRuntime rt = Npcs.runtime(mob);
        if (rt.editorId != null && !rt.editorId.equals(player.getUUID())) return false;
        rt.editorId = player.getUUID();
        rt.editorLastPingTick = mob.tickCount;

        mob.getNavigation().stop();
        mob.setTarget(null);
        mob.setDeltaMovement(mob.getDeltaMovement().multiply(0.0, 1.0, 0.0));
        return true;
    }

    public static void ping(Mob mob) {
        NpcRuntime rt = Npcs.peekRuntime(mob);
        if (rt != null) rt.editorLastPingTick = mob.tickCount;
    }

    public static void stop(Mob mob) {
        NpcRuntime rt = Npcs.peekRuntime(mob);
        if (rt != null) rt.editorId = null;
    }

    /** Server tick: releases the lock if the editor left, died, walked away or stopped pinging. */
    public static void tick(Mob mob) {
        NpcRuntime rt = Npcs.peekRuntime(mob);
        if (rt == null || rt.editorId == null) return;

        @Nullable UUID id = rt.editorId;
        Player editor = mob.level().getPlayerByUUID(id);
        boolean invalid = editor == null
                || !editor.isAlive()
                || mob.distanceToSqr(editor) > MAX_DIST_SQ
                || mob.tickCount - rt.editorLastPingTick > TIMEOUT_TICKS;
        if (invalid) rt.editorId = null;
    }
}
