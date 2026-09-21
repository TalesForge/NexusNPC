package com.talesforge.masternpc.item.custom;

import com.talesforge.masternpc.entity.custom.NpcEntity;
import com.talesforge.masternpc.network.payload.OpenCreatorPayload;
import com.talesforge.masternpc.network.payload.OpenEditorPayload;
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

public class StaffControlItem extends Item {
    public StaffControlItem(Properties properties) {
        super(properties);
    }

    // RMB on block: creation window
    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        if (!level.isClientSide() && context.getPlayer() instanceof ServerPlayer player) {
            // Спавним не внутри блока, а на той стороне, по которой кликнули
            BlockPos spawnPos = context.getClickedPos().relative(context.getClickedFace());
            PacketDistributor.sendToPlayer(player, new OpenCreatorPayload(spawnPos));
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    // RMB on mob: settings window
    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
        if (!(target instanceof NpcEntity npc)) return InteractionResult.PASS;

        if (!player.level().isClientSide() && player instanceof ServerPlayer serverPlayer) {
            if (npc.tryStartEditing(serverPlayer)) {
                PacketDistributor.sendToPlayer(serverPlayer, new OpenEditorPayload(npc.getId(), npc.getSettings()));
            } else {
                serverPlayer.displayClientMessage(Component.translatable("message.masternpc.npc_busy"), true);
            }
        }
        return InteractionResult.sidedSuccess(player.level().isClientSide());
    }
}
