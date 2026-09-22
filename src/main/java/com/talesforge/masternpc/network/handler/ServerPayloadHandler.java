package com.talesforge.masternpc.network.handler;

import com.talesforge.masternpc.api.MasterNpcApi;
import com.talesforge.masternpc.config.Config;
import com.talesforge.masternpc.entity.ModEntities;
import com.talesforge.masternpc.entity.custom.NpcEntity;
import com.talesforge.masternpc.item.ModItems;
import com.talesforge.masternpc.network.payload.CreateNpcPayload;
import com.talesforge.masternpc.network.payload.DeleteNpcPayload;
import com.talesforge.masternpc.network.payload.EditorStatusPayload;
import com.talesforge.masternpc.network.payload.SaveNpcPayload;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public class ServerPayloadHandler {
    private static boolean holdsStaff(Player player) {
        return player.getMainHandItem().is(ModItems.STAFF_CONTROL)
                || player.getOffhandItem().is(ModItems.STAFF_CONTROL);
    }

    /** One staff is not enough: operator rights are required, unless this is disabled in the config. */
    private static boolean canManageNpcs(ServerPlayer player) {
        if (!holdsStaff(player)) return false;
        return !Config.REQUIRE_OP_PERMISSION.get() || player.hasPermissions(2);
    }

    public static void editorStatus(EditorStatusPayload payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)) return;
        if (!(player.level().getEntity(payload.entityId()) instanceof NpcEntity npc)) return;
        if (!npc.isEditedBy(player)) return;  // Ignore other people’s packages

        if (payload.closed()) npc.stopEditing();
        else npc.editorPing();
    }

    public static void saveNpc(SaveNpcPayload payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)) return;
        if (!canManageNpcs(player)) {
            player.sendSystemMessage(Component.translatable("message.masternpc.no_permission"));
            return;
        }

        Entity entity = player.level().getEntity(payload.entityId());
        if (entity == null) return;
        if (!(entity instanceof NpcEntity npc)) return;
        if (!npc.isEditedBy(player)) return;
        if (player.distanceToSqr(npc) > NpcEntity.EDITOR_MAX_DIST_SQ) return;

        npc.applySettings(payload.settings(), false);
    }

    public static void createNpc(CreateNpcPayload payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)) return;
        if (!canManageNpcs(player)) {
            player.sendSystemMessage(Component.translatable("message.masternpc.no_permission"));
            return;
        }

        BlockPos pos = payload.pos();
        ServerLevel level = player.serverLevel();
        if (!level.isLoaded(pos)) return;
        if (player.distanceToSqr(Vec3.atCenterOf(pos)) > 100.0) return;  // ~10 blocks

        int limit = Config.MAX_NPCS_PER_LEVEL.get();
        if (limit > 0 && countNpcs(level) >= limit) {
            player.sendSystemMessage(Component.translatable("message.masternpc.npc_limit_reached"));
            return;
        }

        MasterNpcApi.spawn(payload.typeId(), level, Vec3.atBottomCenterOf(pos), player.getYRot() + 180.0F, payload.settings());
    }

    public static void deleteNpc(DeleteNpcPayload payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)) return;
        if (!canManageNpcs(player)) {
            player.sendSystemMessage(Component.translatable("message.masternpc.no_permission"));
            return;
        }

        Entity entity = player.level().getEntity(payload.entityId());
        if (entity == null) return;
        if (!(entity instanceof NpcEntity npc)) return;
        if (!npc.isEditedBy(player)) return;
        if (player.distanceToSqr(npc) > NpcEntity.EDITOR_MAX_DIST_SQ) return;

        npc.discard();
    }

    private static int countNpcs(ServerLevel level) {
        return level.getEntities((Entity) null, AABB.INFINITE, e -> e instanceof NpcEntity).size();
    }
}
