package com.talesforge.nexusnpc.event;

import com.talesforge.nexusnpc.NexusNPC;
import com.talesforge.nexusnpc.api.event.NpcInteractEvent;
import com.talesforge.nexusnpc.entity.custom.NpcEntity;
import com.talesforge.nexusnpc.item.ModItems;
import com.talesforge.nexusnpc.network.payload.screen.OpenDialoguePayload;
import com.talesforge.nexusnpc.npc.Npcs;
import com.talesforge.nexusnpc.npc.ai.NpcAi;
import com.talesforge.nexusnpc.npc.data.NpcData;
import com.talesforge.nexusnpc.npc.data.NpcDataSelfTest;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import com.talesforge.nexusnpc.npc.dialogue.DialoguePage;
import com.talesforge.nexusnpc.npc.runtime.NpcEditing;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingConversionEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Game-bus glue that makes the NPC layer work on ordinary mobs without subclassing them.
 * Every handler starts with a cheap "is this an NPC?" check, so mobs without NPC data cost nothing.
 */
@EventBusSubscriber(modid = NexusNPC.MOD_ID)
public final class NpcEvents {
    private NpcEvents() {}

    @SubscribeEvent
    public static void onServerStarting(ServerStartingEvent event) {
        NpcDataSelfTest.run();
    }

    /** A mob (re)appears in the world — new spawn, chunk load, dimension change: install its NPC AI. */
    @SubscribeEvent
    public static void onJoin(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide() || !(event.getEntity() instanceof Mob mob)) return;

        // Our own entity type is always an NPC (also migrates saves from before NPC data lived in an attachment)
        if (mob instanceof NpcEntity npc && !Npcs.isNpc(mob)) {  // npc: see takeLegacyData
            NpcData legacy = npc.takeLegacyData();
            Npcs.setData(mob, legacy != null ? legacy : NpcData.create(Npcs.defaultMode(mob)));
        }
        if (!Npcs.isNpc(mob)) return;

        mob.setPersistenceRequired();
        NpcAi.apply(mob);
    }

    /** Server-side editor timeout. */
    @SubscribeEvent
    public static void onTick(EntityTickEvent.Post event) {
        if (event.getEntity() instanceof Mob mob && !mob.level().isClientSide()) NpcEditing.tick(mob);
    }

    /** Edited NPCs are invulnerable. */
    @SubscribeEvent
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (event.getEntity() instanceof Mob mob && NpcEditing.isBeingEdited(mob)) event.setCanceled(true);
    }

    /** Villager -> zombie villager, piglin -> zombified piglin etc. must not silently lose their NPC data. */
    @SubscribeEvent
    public static void onConversion(LivingConversionEvent.Post event) {
        if (event.getEntity() instanceof Mob from && event.getOutcome() instanceof Mob to && Npcs.isNpc(from)) {
            Npcs.copyTo(from, to);
        }
    }

    /**
     * RMB on an NPC opens its dialogue. This runs BEFORE the mob's own interaction, so it only takes over
     * when the NPC actually has a dialogue to show; sneaking always falls through to the mob's normal
     * interaction (feeding, vanilla villager trading, ...).
     */
    @SubscribeEvent
    public static void onInteract(PlayerInteractEvent.EntityInteract event) {
        if (!(event.getTarget() instanceof Mob mob) || !Npcs.isNpc(mob)) return;
        if (event.getHand() != InteractionHand.MAIN_HAND) return;  // Fired once per hand
        Player player = event.getEntity();
        if (player.isSecondaryUseActive()) return;

        ItemStack held = event.getItemStack();
        if (held.is(ModItems.CONTROL_STAFF) || held.is(ModItems.GEAR_SETTINGS)) return;  // Those items handle the click themselves

        boolean client = player.level().isClientSide();
        InteractionResult handled = InteractionResult.sidedSuccess(client);

        // Fired on both sides; addons that cancel it must decide on both sides too
        if (NeoForge.EVENT_BUS.post(new NpcInteractEvent(mob, player, event.getHand())).isCanceled()) {
            event.setCancellationResult(handled);
            event.setCanceled(true);
            return;
        }

        DialoguePage start = Npcs.dialogue(mob).startPage();
        if (start == null) return;  // Nothing to say — let the mob behave like it always did
        if (player instanceof ServerPlayer serverPlayer) {
            PacketDistributor.sendToPlayer(serverPlayer, new OpenDialoguePayload(mob.getId(), start));
        }
        event.setCancellationResult(handled);
        event.setCanceled(true);
    }
}
