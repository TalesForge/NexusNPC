package com.talesforge.nexusnpc.npc.field;

import com.mojang.serialization.Codec;
import com.talesforge.nexusnpc.NexusNPC;
import com.talesforge.nexusnpc.entity.custom.NpcEntity;
import com.talesforge.nexusnpc.npc.NpcStats;
import com.talesforge.nexusnpc.npc.Npcs;
import com.talesforge.nexusnpc.npc.data.NpcAiMode;
import net.minecraft.world.entity.Mob;
import com.talesforge.nexusnpc.npc.attitude.NpcAttitudes;
import com.talesforge.nexusnpc.npc.behavior.NpcBehaviors;
import com.talesforge.nexusnpc.npc.dialogue.NpcDialogue;
import com.talesforge.nexusnpc.npc.model.NpcModels;
import com.talesforge.nexusnpc.npc.quest.NpcQuests;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.Attributes;
import org.jetbrains.annotations.ApiStatus;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Central place where every mod — including this one — registers the "settings fields"
 * an NPC can have. Registration order is preserved and used as the default GUI order, so
 * register core fields first (done in {@link #bootstrap()}); addons register theirs
 * afterwards, from their own mod constructor, via {@link #register(NpcSettingField)}.
 */
public final class NpcSettingFields {
    private static final Map<ResourceLocation, NpcSettingField<?>> FIELDS = new LinkedHashMap<>();
    private static boolean bootstrapped = false;

    private NpcSettingFields() {}

    private static ResourceLocation rl(String path) {
        return ResourceLocation.fromNamespaceAndPath(NexusNPC.MOD_ID, path);
    }

    /**
     * Call from your mod's constructor (mod constructors run before any NPC editor can open,
     * so there is no ordering hazard as long as you don't need it available during your own
     * constructor's execution).
     */
    public static synchronized void register(NpcSettingField<?> field) {
        if (FIELDS.putIfAbsent(field.id(), field) != null) {
            throw new IllegalStateException("Duplicate NPC setting field id: " + field.id());
        }
    }

    @SuppressWarnings("unchecked")
    public static <T> NpcSettingField<T> get(ResourceLocation id) {
        return (NpcSettingField<T>) FIELDS.get(id);
    }

    public static synchronized List<NpcSettingField<?>> all() {
        return List.copyOf(FIELDS.values());
    }

    // ============================================================
    //  Core fields — every one of these is implemented purely
    //  through the public Npcs / NpcStats API on a plain Mob. An addon
    //  mod can write fields exactly like this for its own data.
    // ============================================================

    public static final NpcSettingField<String> NAME = new NpcSettingField<>() {
        public ResourceLocation id() { return rl("name"); }
        public Codec<String> codec() { return Codec.STRING; }
        public String defaultValue() { return ""; }
        public String get(Mob npc) { return npc.hasCustomName() ? npc.getCustomName().getString() : ""; }
        public void set(Mob npc, String value) {
            String trimmed = value.trim();
            if (trimmed.length() > 32) trimmed = trimmed.substring(0, 32);
            npc.setCustomName(trimmed.isEmpty() ? null : Component.literal(trimmed));
            npc.setCustomNameVisible(!trimmed.isEmpty());
        }
    };

    public static final NpcSettingField<ResourceLocation> ATTITUDE = new NpcSettingField<>() {
        public ResourceLocation id() { return rl("attitude"); }
        public Codec<ResourceLocation> codec() { return ResourceLocation.CODEC; }
        public ResourceLocation defaultValue() { return NpcAttitudes.DEFAULT_ID; }
        public ResourceLocation get(Mob npc) { return Npcs.attitudeId(npc); }
        public void set(Mob npc, ResourceLocation value) { Npcs.setAttitudeData(npc, value); }
        public boolean needsAiRefresh() { return true; }
    };

    public static final NpcSettingField<ResourceLocation> BEHAVIOR = new NpcSettingField<>() {
        public ResourceLocation id() { return rl("behavior"); }
        public Codec<ResourceLocation> codec() { return ResourceLocation.CODEC; }
        public ResourceLocation defaultValue() { return NpcBehaviors.DEFAULT_ID; }
        public ResourceLocation get(Mob npc) { return Npcs.behaviorId(npc); }
        public void set(Mob npc, ResourceLocation value) { Npcs.setBehaviorData(npc, value); }
        public boolean needsAiRefresh() { return true; }
    };

    /**
     * VANILLA = the mob keeps its own AI (default for every ordinary mob); OVERRIDE = NexusNPC's attitude and
     * behavior goals replace it. See {@link NpcAiMode}.
     */
    public static final NpcSettingField<NpcAiMode> AI_MODE = new NpcSettingField<>() {
        public ResourceLocation id() { return rl("ai_mode"); }
        public Codec<NpcAiMode> codec() { return NpcAiMode.CODEC; }
        public NpcAiMode defaultValue() { return NpcAiMode.VANILLA; }
        public NpcAiMode get(Mob npc) { return Npcs.aiMode(npc); }
        public void set(Mob npc, NpcAiMode value) { Npcs.setAiModeData(npc, value); }
        public boolean needsAiRefresh() { return true; }
    };

    /**
     * MUST be registered (and therefore applied by NpcDataMap#applyAll) BEFORE SKIN: an
     * NpcEditorScreen save that changes both together needs the model already switched by
     * the time the skin value gets validated, or a perfectly valid new (model, skin) pair
     * would have its skin silently rejected against the OLD model instead.
     */
    public static final NpcSettingField<ResourceLocation> MODEL = new NpcSettingField<>() {
        public ResourceLocation id() { return rl("model"); }
        public Codec<ResourceLocation> codec() { return ResourceLocation.CODEC; }
        public ResourceLocation defaultValue() { return NpcModels.DEFAULT_ID; }
        public boolean appliesTo(Mob npc) { return npc instanceof NpcEntity; }
        public ResourceLocation get(Mob npc) { return npc instanceof NpcEntity e ? e.getModelId() : NpcModels.DEFAULT_ID; }
        public void set(Mob npc, ResourceLocation value) { if (npc instanceof NpcEntity e) e.setModelData(value); }
    };

    public static final NpcSettingField<String> SKIN = new NpcSettingField<>() {
        public ResourceLocation id() { return rl("skin"); }
        public Codec<String> codec() { return Codec.STRING; }
        public String defaultValue() { return NpcModels.HUMANOID_DEFAULT_SKIN.toString(); }
        public boolean appliesTo(Mob npc) { return npc instanceof NpcEntity; }
        public String get(Mob npc) { return npc instanceof NpcEntity e ? e.getSkinTexture().toString() : NpcModels.HUMANOID_DEFAULT_SKIN.toString(); }
        public void set(Mob npc, String value) {
            // No pre-validation needed: setSkin stores the raw string, unparsed, and
            // NpcEntity#getSkinTexture() re-validates against whatever model is CURRENT
            // every time it's read — an incompatible or malformed value is never exposed.
            if (npc instanceof NpcEntity e) e.setSkin(value);
        }
    };

    public static final NpcSettingField<Double> MAX_HEALTH = new NpcSettingField<>() {
        public ResourceLocation id() { return rl("max_health"); }
        public Codec<Double> codec() { return Codec.DOUBLE; }
        public Double defaultValue() { return 20.0; }
        public Double get(Mob npc) { return NpcStats.base(npc, Attributes.MAX_HEALTH, defaultValue()); }
        public void set(Mob npc, Double value) { NpcStats.setMaxHealth(npc, sanitize(value, defaultValue())); }
    };

    public static final NpcSettingField<Double> DAMAGE = new NpcSettingField<>() {
        public ResourceLocation id() { return rl("damage"); }
        public Codec<Double> codec() { return Codec.DOUBLE; }
        public Double defaultValue() { return 2.0; }
        public Double get(Mob npc) { return NpcStats.base(npc, Attributes.ATTACK_DAMAGE, defaultValue()); }
        public void set(Mob npc, Double value) { NpcStats.setDamage(npc, sanitize(value, defaultValue())); }
    };

    public static final NpcSettingField<Double> SPEED = new NpcSettingField<>() {
        public ResourceLocation id() { return rl("speed"); }
        public Codec<Double> codec() { return Codec.DOUBLE; }
        public Double defaultValue() { return 0.25; }
        public Double get(Mob npc) { return NpcStats.base(npc, Attributes.MOVEMENT_SPEED, defaultValue()); }
        public void set(Mob npc, Double value) { NpcStats.setSpeed(npc, sanitize(value, defaultValue())); }
    };

    private static double sanitize(double value, double fallback) {
        return Double.isFinite(value) ? value : fallback;
    }

    public static final NpcSettingField<NpcDialogue> DIALOGUE = new NpcSettingField<>() {
        public ResourceLocation id() { return rl("dialogue"); }
        public Codec<NpcDialogue> codec() { return NpcDialogue.CODEC; }
        public NpcDialogue defaultValue() { return NpcDialogue.EMPTY; }
        public NpcDialogue get(Mob npc) { return Npcs.dialogue(npc); }
        public void set(Mob npc, NpcDialogue value) { Npcs.setDialogue(npc, value); }
    };

    public static final NpcSettingField<NpcQuests> QUESTS = new NpcSettingField<>() {
        public ResourceLocation id() { return rl("quests"); }
        public Codec<NpcQuests> codec() { return NpcQuests.CODEC; }
        public NpcQuests defaultValue() { return NpcQuests.EMPTY; }
        public NpcQuests get(Mob npc) { return Npcs.quests(npc); }
        public void set(Mob npc, NpcQuests value) { Npcs.setQuests(npc, value); }
    };

    /**
     * Called once from {@code NexusNPC}'s constructor. Addons may register their own
     * fields afterwards, from their own mod constructor — registration order only affects
     * the default GUI order, nothing else.
     */
    @ApiStatus.Internal
    public static synchronized void bootstrap() {
        if (bootstrapped) return;
        bootstrapped = true;
        register(NAME);
        register(ATTITUDE);
        register(BEHAVIOR);
        register(AI_MODE);
        register(MODEL);
        register(SKIN);
        register(MAX_HEALTH);
        register(DAMAGE);
        register(SPEED);
        register(DIALOGUE);
        register(QUESTS);
    }
}
