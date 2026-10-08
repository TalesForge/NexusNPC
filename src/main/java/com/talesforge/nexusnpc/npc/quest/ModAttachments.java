package com.talesforge.nexusnpc.npc.quest;

import com.talesforge.nexusnpc.NexusNPC;
import com.talesforge.nexusnpc.npc.data.NpcAiMode;
import com.talesforge.nexusnpc.npc.data.NpcData;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

public final class ModAttachments {
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, NexusNPC.MOD_ID);

    public static void register(IEventBus eventBus) {
        ATTACHMENT_TYPES.register(eventBus);
    }

    /** copyOnDeath — losing quest progress due to death would be unreasonably harsh. */
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<NpcQuestProgress>> QUEST_PROGRESS =
            ATTACHMENT_TYPES.register("quest_progress", () -> AttachmentType.builder(() -> NpcQuestProgress.EMPTY)
                    .serialize(NpcQuestProgress.CODEC)
                    .copyOnDeath()
                    .build());

    /**
     * Marks a Mob as an NPC and carries all of its NexusNPC data. Having this attachment IS the definition
     * of "is an NPC" — never call getData on a mob without checking hasData first, since getData would
     * create (and thereby attach) the default value. Use {@code Npcs} instead of touching this directly.
     */
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<NpcData>> NPC_DATA =
            ATTACHMENT_TYPES.register("npc_data", () -> AttachmentType.builder(() -> NpcData.create(NpcAiMode.VANILLA))
                    .serialize(NpcData.CODEC)
                    .build());

    private ModAttachments() {}
}
