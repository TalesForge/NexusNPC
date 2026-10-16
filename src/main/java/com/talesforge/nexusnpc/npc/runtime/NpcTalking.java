package com.talesforge.nexusnpc.npc.runtime;

import com.talesforge.nexusnpc.npc.Npcs;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * The "conversation lock": while a player has the dialogue window open, the NPC stops walking and turns to face
 * them (LivingEntityMixin#isImmobile does the stopping). It is released when the window closes, when the player
 * walks away or dies, or after a minute without any dialogue activity — so a crashed client can never leave an
 * NPC frozen. Works on any mob, like every other NPC feature. Never saved.
 */
public final class NpcTalking {
    public static final double MAX_DIST_SQ = 100.0;   // 10 blocks: beyond this the player has clearly left
    private static final int TIMEOUT_TICKS = 20 * 60;

    private NpcTalking() {}

    public static boolean isTalking(Mob mob) {
        NpcRuntime rt = Npcs.peekRuntime(mob);
        return rt != null && rt.talkerId != null;
    }

    /** A player opened (or advanced) a conversation. If someone else is already talking, they keep the NPC's attention. */
    public static void touch(Mob mob, ServerPlayer player) {
        NpcRuntime rt = Npcs.runtime(mob);
        if (rt.talkerId == null || rt.talkerId.equals(player.getUUID())) {
            if (rt.talkerId == null) mob.getNavigation().stop();
            rt.talkerId = player.getUUID();
            rt.talkerLastTick = mob.tickCount;
        }
    }

    /** The player's dialogue window closed. */
    public static void stop(Mob mob, @Nullable Player who) {
        NpcRuntime rt = Npcs.peekRuntime(mob);
        if (rt == null || rt.talkerId == null) return;
        if (who != null && !rt.talkerId.equals(who.getUUID())) return;
        // The dialogue window closes the moment the trade window opens: that must not release the NPC while it trades.
        // The merchant releases it itself when the trade window closes.
        if (who != null && rt.merchant != null && rt.merchant.getTradingPlayer() == who) return;
        rt.talkerId = null;
    }

    /** True while this player has a trade window open with the NPC. */
    private static boolean isTrading(NpcRuntime rt, UUID player) {
        return rt.merchant != null && rt.merchant.getTradingPlayer() != null
                && rt.merchant.getTradingPlayer().getUUID().equals(player);
    }

    /** Server tick for every mob: release the lock when it is no longer valid, otherwise keep facing the player. */
    public static void tick(Mob mob) {
        NpcRuntime rt = Npcs.peekRuntime(mob);
        if (rt == null || rt.talkerId == null) return;

        @Nullable UUID id = rt.talkerId;
        if (isTrading(rt, id)) rt.talkerLastTick = mob.tickCount;   // A long trade is not an idle conversation
        Player player = mob.level().getPlayerByUUID(id);
        boolean invalid = player == null
                || !player.isAlive()
                || mob.distanceToSqr(player) > MAX_DIST_SQ
                || mob.tickCount - rt.talkerLastTick > TIMEOUT_TICKS;
        if (invalid) {
            rt.talkerId = null;
            return;
        }
        faceTowards(mob, player);
    }

    /** isImmobile() skips the AI step that normally turns the head, so the turn is done here by hand. */
    private static void faceTowards(Mob mob, Player player) {
        double dx = player.getX() - mob.getX();
        double dz = player.getZ() - mob.getZ();
        double dy = player.getEyeY() - mob.getEyeY();
        float yaw = (float) (Mth.atan2(dz, dx) * (180.0 / Math.PI)) - 90.0F;
        float pitch = (float) -(Mth.atan2(dy, Math.sqrt(dx * dx + dz * dz)) * (180.0 / Math.PI));

        float smoothYaw = Mth.rotLerp(0.35F, mob.getYRot(), yaw);
        mob.setYRot(smoothYaw);
        mob.setYBodyRot(smoothYaw);
        mob.setYHeadRot(smoothYaw);
        mob.setXRot(Mth.rotLerp(0.35F, mob.getXRot(), pitch));
    }
}
