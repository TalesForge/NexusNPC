package com.talesforge.masternpc.npc.dialogue;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.List;

/** Один "экран" текста + варианты ответа. id уникален внутри одного дерева NpcDialogue. */
public record DialoguePage(String id, String text, List<DialogueOption> options) {
    public static final String START_ID = "start";

    public static final Codec<DialoguePage> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.STRING.fieldOf("id").forGetter(DialoguePage::id),
            Codec.STRING.fieldOf("text").forGetter(DialoguePage::text),
            DialogueOption.CODEC.listOf().fieldOf("options").forGetter(DialoguePage::options)
    ).apply(i, DialoguePage::new));
}
