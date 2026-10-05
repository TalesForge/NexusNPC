package com.talesforge.nexusnpc.npc.dialogue;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/** One saved dialogue tree in the client-side library: a stable file id plus an author-chosen display name. */
public record DialogueLibraryEntry(String id, String name, NpcDialogue dialogue) {
    public static final Codec<DialogueLibraryEntry> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.STRING.fieldOf("id").forGetter(DialogueLibraryEntry::id),
            Codec.STRING.optionalFieldOf("name", "").forGetter(DialogueLibraryEntry::name),
            NpcDialogue.CODEC.fieldOf("dialogue").forGetter(DialogueLibraryEntry::dialogue)
    ).apply(i, DialogueLibraryEntry::new));

    public String displayName() { return name.isBlank() ? id : name; }
    public DialogueLibraryEntry withName(String name) { return new DialogueLibraryEntry(id, name, dialogue); }
    public DialogueLibraryEntry withDialogue(NpcDialogue dialogue) { return new DialogueLibraryEntry(id, name, dialogue); }
}
