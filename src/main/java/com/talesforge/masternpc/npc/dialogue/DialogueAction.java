package com.talesforge.masternpc.npc.dialogue;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

/**
 * What happens when you click on an answer option. It knows nothing about NpcEntity and the network —
 * this is handled by ServerPayloadHandler#dialogueAction, which interprets the action against a living NPC.
 */
public sealed interface DialogueAction {
    Codec<DialogueAction> CODEC = Codec.STRING.dispatch("type", DialogueAction::codecId, DialogueAction::codecFor);

    default List<String> targetPages() { return List.of(); }

    String codecId();

    static MapCodec<? extends DialogueAction> codecFor(String id) {
        return switch (id) {
            case Goto.ID -> Goto.CODEC;
            case OpenTrade.ID -> OpenTrade.CODEC;
            case AcceptQuest.ID -> AcceptQuest.CODEC;
            case TurnInQuest.ID -> TurnInQuest.CODEC;
            case Close.ID -> Close.CODEC;
            default -> throw new IllegalArgumentException("Unknown dialogue action type: " + id);
        };
    }

    /** Go to another page of the same dialogue tree. */
    record Goto(String pageId) implements DialogueAction {
        static final String ID = "goto";
        static final MapCodec<Goto> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
                Codec.STRING.fieldOf("page").forGetter(Goto::pageId)
        ).apply(i, Goto::new));
        public String codecId() { return ID; }
    }

    /** Close the dialogue and open the NPC trading window (vanilla UI). */
    record OpenTrade() implements DialogueAction {
        static final String ID = "open_trade";
        static final MapCodec<OpenTrade> CODEC = MapCodec.unit(OpenTrade::new);
        public String codecId() { return ID; }
    }

    /** Mark the quest as active for the player, then go to nextPageId. */
    record AcceptQuest(ResourceLocation questId, String nextPageId) implements DialogueAction {
        static final String ID = "accept_quest";
        static final MapCodec<AcceptQuest> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
                ResourceLocation.CODEC.fieldOf("quest").forGetter(AcceptQuest::questId),
                Codec.STRING.fieldOf("next").forGetter(AcceptQuest::nextPageId)
        ).apply(i, AcceptQuest::new));
        public String codecId() { return ID; }
    }

    /**
     * Checks whether the quest objective is completed right now;
     * if yes, it will collect the required item (for CollectItemObjective),
     * issue the reward, and navigate to successPageId;
     * otherwise, it will navigate to failPageId.
     */
    record TurnInQuest(ResourceLocation questId, String successPageId, String failPageId) implements DialogueAction {
        static final String ID = "turn_in_quest";
        static final MapCodec<TurnInQuest> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
                ResourceLocation.CODEC.fieldOf("quest").forGetter(TurnInQuest::questId),
                Codec.STRING.fieldOf("success").forGetter(TurnInQuest::successPageId),
                Codec.STRING.fieldOf("fail").forGetter(TurnInQuest::failPageId)
        ).apply(i, TurnInQuest::new));
        public String codecId() { return ID; }
    }

    /** It simply closes the player’s dialogue window. */
    record Close() implements DialogueAction {
        static final String ID = "close";
        static final MapCodec<Close> CODEC = MapCodec.unit(Close::new);
        public String codecId() { return ID; }
    }
}
