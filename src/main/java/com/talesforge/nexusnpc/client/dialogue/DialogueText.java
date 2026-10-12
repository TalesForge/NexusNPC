package com.talesforge.nexusnpc.client.dialogue;

/**
 * Placeholders usable in dialogue text, filled in on the client when the page is shown:
 * <ul>
 *   <li>{@code {player}} — the name of the player who is talking;</li>
 *   <li>{@code {npc}} — the name of the NPC speaking.</li>
 * </ul>
 * The same page therefore reads naturally for everyone, and can be reused on any NPC through the library.
 */
public final class DialogueText {
    private DialogueText() {}

    public static String apply(String raw, String playerName, String npcName) {
        return raw.replace("{player}", playerName).replace("{npc}", npcName);
    }
}
