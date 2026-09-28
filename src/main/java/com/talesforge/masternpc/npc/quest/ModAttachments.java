package com.talesforge.masternpc.npc.quest;

import com.talesforge.masternpc.MasterNPC;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

public final class ModAttachments {
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, MasterNPC.MOD_ID);

    public static void register(IEventBus eventBus) {
        ATTACHMENT_TYPES.register(eventBus);
    }

    /** copyOnDeath — терять прогресс квестов из-за смерти было бы неоправданно жёстко. */
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<NpcQuestProgress>> QUEST_PROGRESS =
            ATTACHMENT_TYPES.register("quest_progress", () -> AttachmentType.builder(() -> NpcQuestProgress.EMPTY)
                    .serialize(NpcQuestProgress.CODEC)
                    .copyOnDeath()
                    .build());

    private ModAttachments() {}
}
