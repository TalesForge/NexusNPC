package com.talesforge.masternpc.npc.dialogue;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

/**
 * Что происходит при клике по варианту ответа. Ничего не знает про NpcEntity и сеть —
 * это делает ServerPayloadHandler#dialogueAction, интерпретируя действие против живого NPC.
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

    /** Перейти на другую страницу того же дерева диалога. */
    record Goto(String pageId) implements DialogueAction {
        static final String ID = "goto";
        static final MapCodec<Goto> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
                Codec.STRING.fieldOf("page").forGetter(Goto::pageId)
        ).apply(i, Goto::new));
        public String codecId() { return ID; }
    }

    /** Закрыть диалог и открыть окно торговли NPC (ванильный Merchant UI). */
    record OpenTrade() implements DialogueAction {
        static final String ID = "open_trade";
        static final MapCodec<OpenTrade> CODEC = MapCodec.unit(OpenTrade::new);
        public String codecId() { return ID; }
    }

    /** Пометить квест активным у игрока, затем перейти на nextPageId. */
    record AcceptQuest(ResourceLocation questId, String nextPageId) implements DialogueAction {
        static final String ID = "accept_quest";
        static final MapCodec<AcceptQuest> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
                ResourceLocation.CODEC.fieldOf("quest").forGetter(AcceptQuest::questId),
                Codec.STRING.fieldOf("next").forGetter(AcceptQuest::nextPageId)
        ).apply(i, AcceptQuest::new));
        public String codecId() { return ID; }
    }

    /**
     * Проверяет, выполнена ли цель квеста прямо сейчас; если да — заберёт нужное
     * (для CollectItemObjective), выдаст награду и уйдёт на successPageId, иначе — на failPageId.
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

    /** Просто закрывает окно диалога у игрока. */
    record Close() implements DialogueAction {
        static final String ID = "close";
        static final MapCodec<Close> CODEC = MapCodec.unit(Close::new);
        public String codecId() { return ID; }
    }
}
