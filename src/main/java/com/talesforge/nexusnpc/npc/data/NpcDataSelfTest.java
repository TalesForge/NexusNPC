package com.talesforge.nexusnpc.npc.data;

import com.talesforge.nexusnpc.NexusNPC;
import com.talesforge.nexusnpc.npc.dialogue.DialogueAction;
import com.talesforge.nexusnpc.npc.dialogue.DialogueOption;
import com.talesforge.nexusnpc.npc.dialogue.DialoguePage;
import com.talesforge.nexusnpc.npc.dialogue.NpcDialogue;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;

import java.util.List;

/**
 * Startup check that {@link NpcData#CODEC} survives a write/read round trip with a non-empty dialogue.
 * NeoForge drops an attachment silently-ish when its codec fails, which looks exactly like
 * "my dialogue was not saved" — so fail loudly in the log instead.
 */
public final class NpcDataSelfTest {
    private NpcDataSelfTest() {}

    public static void run() {
        NpcDialogue dialogue = NpcDialogue.EMPTY.withPage(new DialoguePage("start", "Start", "Hello",
                List.of(new DialogueOption("Bye", new DialogueAction.Close()))));
        NpcData original = NpcData.create(NpcAiMode.OVERRIDE).withDialogue(dialogue);

        Tag encoded = NpcData.CODEC.encodeStart(NbtOps.INSTANCE, original)
                .resultOrPartial(err -> NexusNPC.LOGGER.error("[NexusNPC self-test] NpcData ENCODE failed: {}", err))
                .orElse(null);
        if (encoded == null) return;

        NpcData decoded = NpcData.CODEC.parse(NbtOps.INSTANCE, encoded)
                .resultOrPartial(err -> NexusNPC.LOGGER.error("[NexusNPC self-test] NpcData DECODE failed: {}", err))
                .orElse(null);
        if (decoded == null) return;

        if (decoded.dialogue().pages().size() != 1 || decoded.aiMode() != NpcAiMode.OVERRIDE) {
            NexusNPC.LOGGER.error("[NexusNPC self-test] NpcData round trip LOST data: {} -> {}", original, decoded);
        } else {
            NexusNPC.LOGGER.info("[NexusNPC self-test] NpcData round trip OK");
        }
    }
}
