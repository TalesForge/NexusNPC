package com.talesforge.nexusnpc.item.custom;

import com.talesforge.nexusnpc.api.NexusNpcApi;
import com.talesforge.nexusnpc.npc.Npcs;
import com.talesforge.nexusnpc.npc.runtime.NpcEditing;
import net.minecraft.world.entity.Mob;
import com.talesforge.nexusnpc.network.payload.screen.OpenCreatorPayload;
import com.talesforge.nexusnpc.network.payload.screen.OpenEditorPayload;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.PacketDistributor;

public class ControlStaffItem extends Item {
    public ControlStaffItem(Properties properties) {
        super(properties);
    }

    // RMB on block: creation window
    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        if (!level.isClientSide() && context.getPlayer() instanceof ServerPlayer player) {
            // Spawn not inside the block, but on the side where the click occurred.
            BlockPos spawnPos = context.getClickedPos().relative(context.getClickedFace());
            PacketDistributor.sendToPlayer(player, new OpenCreatorPayload(spawnPos));
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    // RMB on mob: settings window
    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
        if (!(target instanceof Mob npc)) return InteractionResult.PASS;  // Any mob can be configured, not only NexusNPC entities

        if (!player.level().isClientSide() && player instanceof ServerPlayer serverPlayer) {
            if (!Npcs.isEligible(npc)) return InteractionResult.PASS;  // Server config (blocked types / vanilla mobs allowed)
            if (NpcEditing.tryStart(npc, serverPlayer)) {
                PacketDistributor.sendToPlayer(serverPlayer, new OpenEditorPayload(npc.getId(), NexusNpcApi.getSettings(npc)));
            } else {
                serverPlayer.displayClientMessage(Component.translatable("message.nexusnpc.npc_busy"), true);
            }
        }
        return InteractionResult.sidedSuccess(player.level().isClientSide());
    }
}
