package com.talesforge.masternpc.item.custom;

import com.talesforge.masternpc.network.payload.screen.OpenSettingsPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.PacketDistributor;

public class GearSettingsItem extends Item {
    public GearSettingsItem(Properties properties) {
        super(properties);
    }

    // RMB on block: creation window
    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        if (!level.isClientSide() && context.getPlayer() instanceof ServerPlayer player) {
            PacketDistributor.sendToPlayer(player, new OpenSettingsPayload());
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }
}
