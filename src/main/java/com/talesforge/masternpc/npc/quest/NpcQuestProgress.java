package com.talesforge.masternpc.npc.quest;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;

/**
 * The player's progress on quests is stored on the Player (see ModAttachments) —
 * not on the NPC, so he is not afraid of unloading/deleting the NPC who issued the quest.
 * For each active quest, we store an objective snapshot of its goal taken at the time of acceptance.
 */
public final class NpcQuestProgress {
    public record ActiveQuest(NpcQuestObjective objective, int progress) {
        static final Codec<ActiveQuest> CODEC = RecordCodecBuilder.create(i -> i.group(
                NpcQuestObjective.CODEC.fieldOf("objective").forGetter(ActiveQuest::objective),
                Codec.INT.fieldOf("progress").forGetter(ActiveQuest::progress)
        ).apply(i, ActiveQuest::new));
    }

    public static final NpcQuestProgress EMPTY = new NpcQuestProgress(Map.of());
    public static final Codec<NpcQuestProgress> CODEC =
            Codec.unboundedMap(ResourceLocation.CODEC, ActiveQuest.CODEC)
                    .xmap(NpcQuestProgress::new, NpcQuestProgress::asMap);

    private final Map<ResourceLocation, ActiveQuest> active;

    private NpcQuestProgress(Map<ResourceLocation, ActiveQuest> active) { this.active = active; }
    private Map<ResourceLocation, ActiveQuest> asMap() { return active; }

    public ActiveQuest get(ResourceLocation questId) { return active.get(questId); }

    public NpcQuestProgress accept(ResourceLocation questId, NpcQuestObjective objective) {
        if (active.containsKey(questId)) return this;
        Map<ResourceLocation, ActiveQuest> copy = new HashMap<>(active);
        copy.put(questId, new ActiveQuest(objective, 0));
        return new NpcQuestProgress(copy);
    }

    public NpcQuestProgress complete(ResourceLocation questId) {
        if (!active.containsKey(questId)) return this;
        Map<ResourceLocation, ActiveQuest> copy = new HashMap<>(active);
        copy.remove(questId);
        return new NpcQuestProgress(copy);
    }

    public NpcQuestProgress incrementMatching(Predicate<NpcQuestObjective> matcher) {
        Map<ResourceLocation, ActiveQuest> copy = null;
        for (var e : active.entrySet()) {
            if (!matcher.test(e.getValue().objective())) continue;
            if (copy == null) copy = new HashMap<>(active);
            var cur = e.getValue();
            copy.put(e.getKey(), new ActiveQuest(cur.objective(), cur.progress() + 1));
        }
        return copy == null ? this : new NpcQuestProgress(copy);
    }

    public NpcQuestProgress setMatchingAtLeast(Predicate<NpcQuestObjective> matcher, int value) {
        Map<ResourceLocation, ActiveQuest> copy = null;
        for (var e : active.entrySet()) {
            if (!matcher.test(e.getValue().objective()) || e.getValue().progress() >= value) continue;
            if (copy == null) copy = new HashMap<>(active);
            copy.put(e.getKey(), new ActiveQuest(e.getValue().objective(), value));
        }
        return copy == null ? this : new NpcQuestProgress(copy);
    }
}
