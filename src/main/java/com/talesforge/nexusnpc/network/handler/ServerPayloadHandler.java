package com.talesforge.nexusnpc.network.handler;

import com.talesforge.nexusnpc.npc.dialogue.DialogueActionHandlers;
import com.talesforge.nexusnpc.api.NexusNpcApi;
import com.talesforge.nexusnpc.config.Config;
import com.talesforge.nexusnpc.entity.custom.NpcEntity;
import com.talesforge.nexusnpc.item.ModItems;
import com.talesforge.nexusnpc.menu.TradeEditMenu;
import com.talesforge.nexusnpc.network.payload.*;
import com.talesforge.nexusnpc.network.payload.action.CreateNpcPayload;
import com.talesforge.nexusnpc.network.payload.action.DeleteNpcPayload;
import com.talesforge.nexusnpc.network.payload.action.EditorStatusPayload;
import com.talesforge.nexusnpc.network.payload.action.SaveNpcPayload;
import com.talesforge.nexusnpc.network.payload.screen.OpenDialoguePayload;
import com.talesforge.nexusnpc.network.payload.screen.OpenTradeEditorPayload;
import com.talesforge.nexusnpc.npc.dialogue.DialogueAction;
import com.talesforge.nexusnpc.npc.dialogue.DialoguePage;
import com.talesforge.nexusnpc.npc.quest.ModAttachments;
import com.talesforge.nexusnpc.npc.quest.NpcQuest;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public class ServerPayloadHandler {
    private static boolean holdsStaff(Player player) {
        return player.getMainHandItem().is(ModItems.CONTROL_STAFF)
                || player.getOffhandItem().is(ModItems.CONTROL_STAFF);
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

    /** True if this player holds the NPC's editing lock now (taking it if it was free). Otherwise tells them why not. */
    private static boolean ensureEditor(ServerPlayer player, NpcEntity npc) {
        if (npc.isEditedBy(player) || npc.tryStartEditing(player)) return true;
        player.sendSystemMessage(Component.translatable("message.nexusnpc.npc_busy"));
        return false;
    }

    public static void saveNpc(SaveNpcPayload payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)) return;
        if (!canManageNpcs(player)) {
            player.sendSystemMessage(Component.translatable("message.nexusnpc.no_permission"));
            return;
        }

        Entity entity = player.level().getEntity(payload.entityId());
        if (!(entity instanceof NpcEntity npc)) return;
        if (player.distanceToSqr(npc) > NpcEntity.EDITOR_MAX_DIST_SQ) {
            player.sendSystemMessage(Component.translatable("message.nexusnpc.too_far"));
            return;
        }
        if (!ensureEditor(player, npc)) return;

        npc.applySettings(payload.settings(), false);
    }

    public static void createNpc(CreateNpcPayload payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)) return;
        if (!canManageNpcs(player)) {
            player.sendSystemMessage(Component.translatable("message.nexusnpc.no_permission"));
            return;
        }

        BlockPos pos = payload.pos();
        ServerLevel level = player.serverLevel();
        if (!level.isLoaded(pos)) return;
        if (player.distanceToSqr(Vec3.atCenterOf(pos)) > 100.0) return;  // ~10 blocks

        int limit = Config.MAX_NPCS_PER_LEVEL.get();
        if (limit > 0 && countNpcs(level) >= limit) {
            player.sendSystemMessage(Component.translatable("message.nexusnpc.npc_limit_reached"));
            return;
        }

        NexusNpcApi.spawn(payload.typeId(), level, Vec3.atBottomCenterOf(pos), player.getYRot() + 180.0F, payload.settings());
    }

    public static void deleteNpc(DeleteNpcPayload payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)) return;
        if (!canManageNpcs(player)) {
            player.sendSystemMessage(Component.translatable("message.nexusnpc.no_permission"));
            return;
        }

        Entity entity = player.level().getEntity(payload.entityId());
        if (!(entity instanceof NpcEntity npc)) return;
        if (player.distanceToSqr(npc) > NpcEntity.EDITOR_MAX_DIST_SQ) {
            player.sendSystemMessage(Component.translatable("message.nexusnpc.too_far"));
            return;
        }
        if (!ensureEditor(player, npc)) return;

        npc.discard();
    }

    private static int countNpcs(ServerLevel level) {
        return level.getEntities((Entity) null, AABB.INFINITE, e -> e instanceof NpcEntity).size();
    }


    public static void dialogueAction(DialogueActionPayload payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)) return;
        if (!(player.level().getEntity(payload.entityId()) instanceof NpcEntity npc)) return;
        if (player.distanceToSqr(npc) > NpcEntity.EDITOR_MAX_DIST_SQ) return;

        DialoguePage page = npc.getDialogue().page(payload.pageId());
        if (page == null || payload.optionIndex() < 0 || payload.optionIndex() >= page.options().size()) return;

        switch (page.options().get(payload.optionIndex()).action()) {
            case DialogueAction.Goto g -> sendPage(player, npc, g.pageId());
            case DialogueAction.OpenTrade t -> {
                if (npc.getTradingPlayer() != null && npc.getTradingPlayer() != player) return;  // Busy with someone else
                npc.setTradingPlayer(player);
                npc.openTradingScreen(player, npc.getDisplayName(), 1);  // Opens the menu AND sends the offers to the client
                if (!(player.containerMenu instanceof MerchantMenu)) npc.setTradingPlayer(null);  // Failed to open
            }
            case DialogueAction.AcceptQuest a -> {
                NpcQuest quest = npc.getQuests().get(a.questId());
                if (quest != null) {
                    var progress = player.getData(ModAttachments.QUEST_PROGRESS);
                    player.setData(ModAttachments.QUEST_PROGRESS, progress.accept(a.questId(), quest.objective()));
                }
                sendPage(player, npc, a.nextPageId());
            }
            case DialogueAction.TurnInQuest t -> {
                var progress = player.getData(ModAttachments.QUEST_PROGRESS);
                var active = progress.get(t.questId());
                NpcQuest quest = npc.getQuests().get(t.questId());
                boolean done = active != null && quest != null && active.objective().tryComplete(player, active.progress());
                if (done) {
                    player.setData(ModAttachments.QUEST_PROGRESS, progress.complete(t.questId()));
                    if (!quest.reward().isEmpty() && !player.getInventory().add(quest.reward().copy())) {
                        player.drop(quest.reward().copy(), false);
                    }
                    sendPage(player, npc, t.successPageId());
                } else {
                    sendPage(player, npc, t.failPageId());
                }
            }
            case DialogueAction.Custom c -> {
                DialogueActionHandlers.Handler handler = DialogueActionHandlers.get(c.handlerId());
                boolean ok = handler != null && handler.handle(player, npc, c);
                String next = ok ? c.successPageId() : c.failPageId();
                if (!next.isBlank()) sendPage(player, npc, next);  // Blank = the dialogue just ends
            }
            case DialogueAction.Close c -> {}
        }
    }

    private static void sendPage(ServerPlayer player, NpcEntity npc, String pageId) {
        DialoguePage page = pageId.isBlank() ? npc.getDialogue().startPage() : npc.getDialogue().page(pageId);
        if (page != null) PacketDistributor.sendToPlayer(player, new OpenDialoguePayload(npc.getId(), page));
    }


    public static void openTradeEditor(OpenTradeEditorPayload payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)) return;
        if (!canManageNpcs(player)) {
            player.sendSystemMessage(Component.translatable("message.nexusnpc.no_permission"));
            return;
        }

        Entity entity = player.level().getEntity(payload.entityId());
        if (!(entity instanceof NpcEntity npc)) return;
        if (!npc.isEditedBy(player)) return;  // Same lock as saveNpc
        if (player.distanceToSqr(npc) > NpcEntity.EDITOR_MAX_DIST_SQ) return;

        player.openMenu(
                new SimpleMenuProvider(
                        (containerId, inventory, p) -> new TradeEditMenu(containerId, inventory, npc),
                        Component.translatable("gui.nexusnpc.trade.title")),
                buf -> buf.writeVarInt(npc.getId()));  // Extra data: lets the CLIENT build the same menu
    }
}
