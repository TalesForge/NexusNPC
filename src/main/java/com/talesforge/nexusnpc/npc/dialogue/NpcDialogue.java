package com.talesforge.nexusnpc.npc.dialogue;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/** All dialogue pages of one NPC plus which one starts the conversation. Immutable: every change returns a new copy. */
public record NpcDialogue(String startId, Map<String, DialoguePage> pages) {
    public static final NpcDialogue EMPTY = new NpcDialogue("", Map.of());

    public static final Codec<NpcDialogue> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.STRING.optionalFieldOf("start", "").forGetter(NpcDialogue::startId),
            DialoguePage.CODEC.listOf().fieldOf("pages").forGetter(d -> List.copyOf(d.pages().values()))
    ).apply(i, NpcDialogue::of));

    private static NpcDialogue of(String startId, List<DialoguePage> list) {
        Map<String, DialoguePage> map = new LinkedHashMap<>();
        for (DialoguePage page : list) map.put(page.id(), page);
        return new NpcDialogue(startId, map);
    }

    @Nullable
    public DialoguePage page(String id) { return pages.get(id); }

    public boolean contains(String id) { return pages.containsKey(id); }

    public boolean contains(DialoguePage page) { return contains(page.id()); }

    /** The page a conversation opens on: the chosen start page, or the first one if none is chosen (or it was lost). */
    @Nullable
    public DialoguePage startPage() {
        DialoguePage start = pages.get(startId);
        if (start != null) return start;
        return pages.isEmpty() ? null : pages.values().iterator().next();
    }

    public boolean isEmpty() { return pages.isEmpty(); }

    /** Adds the page, or replaces the one with the same id (keeping its place in the list). The first page becomes the start page. */
    public NpcDialogue withPage(DialoguePage page) {
        Map<String, DialoguePage> copy = new LinkedHashMap<>(pages);
        copy.put(page.id(), page);
        return new NpcDialogue(startId.isBlank() ? page.id() : startId, copy);
    }

    public NpcDialogue withStart(String id) { return new NpcDialogue(id, pages); }

    public NpcDialogue without(String id) {
        Map<String, DialoguePage> copy = new LinkedHashMap<>(pages);
        copy.remove(id);
        String start = startId;
        if (start.equals(id)) start = copy.isEmpty() ? "" : copy.keySet().iterator().next();
        return new NpcDialogue(start, copy);
    }

    /** Names of the OTHER pages that lead to {@code id}. Non-empty means the page must not be deleted. */
    public List<String> linkedFrom(String id) {
        List<String> result = new ArrayList<>();
        for (DialoguePage page : pages.values()) {
            if (page.id().equals(id)) continue;
            if (page.options().stream().anyMatch(o -> o.action().targetPages().contains(id))) {
                result.add(page.displayName());
            }
        }
        return result;
    }

    /** An id not used by any page yet. */
    public String freshId() {
        String id;
        do {
            id = Integer.toHexString(ThreadLocalRandom.current().nextInt());
        } while (pages.containsKey(id));
        return id;
    }
}
