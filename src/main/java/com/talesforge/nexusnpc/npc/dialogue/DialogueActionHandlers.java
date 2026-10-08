package com.talesforge.nexusnpc.npc.dialogue;

import net.minecraft.world.entity.Mob;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Open registry of server-side handlers for {@link DialogueAction.Custom}. Same pattern as
 * {@code NpcSettingFields} / {@code NpcQuestObjectiveTypes}: addons call {@link #register} from their mod
 * constructor. Handlers run on the server only, after the usual distance/validity checks.
 */
public final class DialogueActionHandlers {
    @FunctionalInterface
    public interface Handler {
        /** @return true on success (dialogue goes to the success page), false otherwise (fail page). */
        boolean handle(ServerPlayer player, Mob npc, DialogueAction.Custom action);
    }

    private static final Map<ResourceLocation, Handler> HANDLERS = new LinkedHashMap<>();

    private DialogueActionHandlers() {}

    public static synchronized void register(ResourceLocation id, Handler handler) {
        if (HANDLERS.putIfAbsent(id, handler) != null) {
            throw new IllegalStateException("Duplicate dialogue action handler id: " + id);
        }
    }

    @Nullable
    public static synchronized Handler get(ResourceLocation id) {
        return HANDLERS.get(id);
    }
}
