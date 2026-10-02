package com.talesforge.nexusnpc.npc.dialogue;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.List;

/** One dialogue "screen": text plus answer options. {@code id} is internal and stable, {@code name} is the author's label. */
public record DialoguePage(String id, String name, String text, List<DialogueOption> options) {
    public static final Codec<DialoguePage> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.STRING.fieldOf("id").forGetter(DialoguePage::id),
            Codec.STRING.optionalFieldOf("name", "").forGetter(DialoguePage::name),
            Codec.STRING.fieldOf("text").forGetter(DialoguePage::text),
            DialogueOption.CODEC.listOf().fieldOf("options").forGetter(DialoguePage::options)
    ).apply(i, DialoguePage::new));

    /** What lists show: the name, or the id if the page was never named. */
    public String displayName() { return name.isBlank() ? id : name; }
}
