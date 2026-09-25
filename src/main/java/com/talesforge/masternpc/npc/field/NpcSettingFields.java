package com.talesforge.masternpc.npc.field;

import com.mojang.serialization.Codec;
import com.talesforge.masternpc.MasterNPC;
import com.talesforge.masternpc.entity.custom.NpcEntity;
import com.talesforge.masternpc.npc.attitude.NpcAttitudes;
import com.talesforge.masternpc.npc.behavior.NpcBehaviors;
import com.talesforge.masternpc.npc.model.NpcModels;
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
        return ResourceLocation.fromNamespaceAndPath(MasterNPC.MOD_ID, path);
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
    //  through NpcEntity's PUBLIC API. An addon mod could write
    //  fields exactly like this for its own custom NpcEntity data.
    // ============================================================

    public static final NpcSettingField<String> NAME = new NpcSettingField<>() {
        public ResourceLocation id() { return rl("name"); }
        public Codec<String> codec() { return Codec.STRING; }
        public String defaultValue() { return ""; }
        public String get(NpcEntity npc) { return npc.hasCustomName() ? npc.getCustomName().getString() : ""; }
        public void set(NpcEntity npc, String value) {
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
        public ResourceLocation get(NpcEntity npc) { return npc.getAttitudeId(); }
        public void set(NpcEntity npc, ResourceLocation value) { npc.setAttitudeData(value); }
        public boolean needsAiRefresh() { return true; }
    };

    public static final NpcSettingField<ResourceLocation> BEHAVIOR = new NpcSettingField<>() {
        public ResourceLocation id() { return rl("behavior"); }
        public Codec<ResourceLocation> codec() { return ResourceLocation.CODEC; }
        public ResourceLocation defaultValue() { return NpcBehaviors.DEFAULT_ID; }
        public ResourceLocation get(NpcEntity npc) { return npc.getBehaviorId(); }
        public void set(NpcEntity npc, ResourceLocation value) { npc.setBehaviorData(value); }
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
        public ResourceLocation get(NpcEntity npc) { return npc.getModelId(); }
        public void set(NpcEntity npc, ResourceLocation value) { npc.setModelData(value); }
    };

    public static final NpcSettingField<String> SKIN = new NpcSettingField<>() {
        public ResourceLocation id() { return rl("skin"); }
        public Codec<String> codec() { return Codec.STRING; }
        public String defaultValue() { return NpcModels.HUMANOID_DEFAULT_SKIN.toString(); }
        public String get(NpcEntity npc) { return npc.getSkinTexture().toString(); }
        public void set(NpcEntity npc, String value) {
            // No pre-validation needed: setSkin stores the raw string, unparsed, and
            // NpcEntity#getSkinTexture() re-validates against whatever model is CURRENT
            // every time it's read — an incompatible or malformed value is never exposed.
            npc.setSkin(value);
        }
    };

    public static final NpcSettingField<Double> MAX_HEALTH = new NpcSettingField<>() {
        public ResourceLocation id() { return rl("max_health"); }
        public Codec<Double> codec() { return Codec.DOUBLE; }
        public Double defaultValue() { return 20.0; }
        public Double get(NpcEntity npc) { return npc.getAttributeBaseValue(Attributes.MAX_HEALTH); }
        public void set(NpcEntity npc, Double value) { npc.setMaxHealthValue(sanitize(value, defaultValue())); }
    };

    public static final NpcSettingField<Double> DAMAGE = new NpcSettingField<>() {
        public ResourceLocation id() { return rl("damage"); }
        public Codec<Double> codec() { return Codec.DOUBLE; }
        public Double defaultValue() { return 2.0; }
        public Double get(NpcEntity npc) { return npc.getAttributeBaseValue(Attributes.ATTACK_DAMAGE); }
        public void set(NpcEntity npc, Double value) { npc.setDamageValue(sanitize(value, defaultValue())); }
    };

    public static final NpcSettingField<Double> SPEED = new NpcSettingField<>() {
        public ResourceLocation id() { return rl("speed"); }
        public Codec<Double> codec() { return Codec.DOUBLE; }
        public Double defaultValue() { return 0.25; }
        public Double get(NpcEntity npc) { return npc.getAttributeBaseValue(Attributes.MOVEMENT_SPEED); }
        public void set(NpcEntity npc, Double value) { npc.setSpeedValue(sanitize(value, defaultValue())); }
    };

    private static double sanitize(double value, double fallback) {
        return Double.isFinite(value) ? value : fallback;
    }

    /**
     * Called once from {@code MasterNPC}'s constructor. Addons may register their own
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
        register(MODEL);
        register(SKIN);
        register(MAX_HEALTH);
        register(DAMAGE);
        register(SPEED);
    }
}
