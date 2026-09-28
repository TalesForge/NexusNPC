package com.talesforge.masternpc.npc.dialogue;

import com.mojang.serialization.Codec;

import javax.annotation.Nullable;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Дерево диалога одного NPC. DialoguePage.START_ID — точка входа при обычном клике ПКМ. */
public record NpcDialogue(Map<String, DialoguePage> pages) {
    public static final NpcDialogue EMPTY = new NpcDialogue(Map.of());

    public static final Codec<NpcDialogue> CODEC = DialoguePage.CODEC.listOf()
            .xmap(NpcDialogue::fromList, NpcDialogue::toList);

    private static NpcDialogue fromList(List<DialoguePage> list) {
        Map<String, DialoguePage> map = new LinkedHashMap<>();
        for (DialoguePage page : list) map.put(page.id(), page);
        return new NpcDialogue(map);
    }

    private static List<DialoguePage> toList(NpcDialogue dialogue) {
        return List.copyOf(dialogue.pages().values());
    }

    @Nullable
    public DialoguePage page(String id) { return pages.get(id); }

    public boolean isEmpty() { return page(DialoguePage.START_ID) == null; }
}
