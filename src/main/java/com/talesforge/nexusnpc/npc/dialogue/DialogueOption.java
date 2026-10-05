package com.talesforge.nexusnpc.npc.dialogue;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/** One clickable line below the page text. */
public record DialogueOption(String text, DialogueAction action) {
    public static final Codec<DialogueOption> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.STRING.fieldOf("text").forGetter(DialogueOption::text),
            DialogueAction.CODEC.fieldOf("action").forGetter(DialogueOption::action)
    ).apply(i, DialogueOption::new));
}
