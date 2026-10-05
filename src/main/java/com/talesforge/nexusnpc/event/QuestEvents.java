package com.talesforge.nexusnpc.event;

import com.talesforge.nexusnpc.NexusNPC;
import com.talesforge.nexusnpc.npc.quest.KillObjective;
import com.talesforge.nexusnpc.npc.quest.ModAttachments;
import com.talesforge.nexusnpc.npc.quest.TravelObjective;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * Tracks counters for objectives that require an external trigger (kill, travel) — see
 * NpcQuestObjective#tracksProgress(). CollectItemObjective does not require a listener:
 * it is checked live against the inventory at the time the quest is completed.
 */
@EventBusSubscriber(modid = NexusNPC.MOD_ID)
public final class QuestEvents {
    private QuestEvents() {}

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        if (!(event.getSource().getEntity() instanceof ServerPlayer player)) return;
        var type = event.getEntity().getType();

        var progress = player.getData(ModAttachments.QUEST_PROGRESS);
        var updated = progress.incrementMatching(o -> o instanceof KillObjective k && k.entityType() == type);
        if (updated != progress) player.setData(ModAttachments.QUEST_PROGRESS, updated);
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (player.tickCount % 20 != 0) return;

        var progress = player.getData(ModAttachments.QUEST_PROGRESS);
        var updated = progress.setMatchingAtLeast(o -> o instanceof TravelObjective t
                && player.level().dimension() == t.dimension()
                && player.blockPosition().closerThan(t.target(), t.radius()), 1);
        if (updated != progress) player.setData(ModAttachments.QUEST_PROGRESS, updated);
    }
}
