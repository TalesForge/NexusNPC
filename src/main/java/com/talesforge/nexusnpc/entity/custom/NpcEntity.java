package com.talesforge.nexusnpc.entity.custom;

import com.talesforge.nexusnpc.npc.NpcRegistries;
import com.talesforge.nexusnpc.npc.data.NpcAiMode;
import com.talesforge.nexusnpc.npc.data.NpcData;
import com.talesforge.nexusnpc.npc.dialogue.NpcDialogue;
import com.talesforge.nexusnpc.npc.model.NpcModelSkins;
import com.talesforge.nexusnpc.npc.model.NpcModels;
import com.talesforge.nexusnpc.npc.quest.NpcQuests;
import com.talesforge.nexusnpc.npc.trade.NpcTrades;
import com.talesforge.nexusnpc.NexusNPC;
import com.mojang.serialization.Codec;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.AnimationState;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;

/**
 * NexusNPC's own humanoid-style mob. It is ONLY the "appearance" half of the framework: a model, a
 * skin and a hitbox that depend on the selected {@code NpcModelType}. Everything that makes it an
 * NPC (dialogue, quests, trading, attitude, behavior, the editor lock) is not here — it lives on
 * {@link Mob} for every mob and is reached through {@link com.talesforge.nexusnpc.npc.Npcs}.
 * <p>
 * Addons that want custom models or resource-pack skins extend this class (or register their own type
 * through {@code NexusNpcApi.registerType}); everything else should just turn an ordinary mob into an NPC.
 */
public class NpcEntity extends PathfinderMob {

    private static final EntityDataAccessor<String> DATA_MODEL =
            SynchedEntityData.defineId(NpcEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<String> DATA_SKIN =
            SynchedEntityData.defineId(NpcEntity.class, EntityDataSerializers.STRING);

    public final AnimationState idleAnimationState = new AnimationState();
    private int idleAnimationTimeout = 0;

    /** Data from saves made before NPC data moved into an attachment; consumed once on join (see NpcEvents). */
    @Nullable private NpcData legacyData;

    public NpcEntity(EntityType<? extends NpcEntity> entityType, Level level) {
        super(entityType, level);
    }

    // ========== Synchronized appearance ==========
    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_MODEL, NpcModels.DEFAULT_ID.toString());
        builder.define(DATA_SKIN, NpcRegistries.model(NpcModels.DEFAULT_ID).defaultSkin().toString());
    }

    public ResourceLocation getModelId() {
        ResourceLocation id = ResourceLocation.tryParse(entityData.get(DATA_MODEL));
        return id != null && NpcRegistries.MODELS.containsKey(id) ? id : NpcModels.DEFAULT_ID;
    }

    /** Validated against whatever model is currently set — a skin from a different model always falls back to that model's own default. */
    public ResourceLocation getSkinTexture() {
        String validated = NpcModelSkins.validate(getModelId(), entityData.get(DATA_SKIN));
        return ResourceLocation.parse(validated);
    }

    /**
     * Changes the model and immediately recomputes the hitbox/eye height via {@link #refreshDimensions()},
     * which fires NeoForge's {@code EntityEvent.Size} (see {@code ModEventBusEvents#onSize}) — that is what
     * actually reads the new model's {@code NpcModelType}. Does NOT touch the skin; an incompatible skin
     * already falls back to the new model's default on read in {@link #getSkinTexture()}.
     */
    public void setModelData(ResourceLocation id) {
        if (!NpcRegistries.MODELS.containsKey(id)) id = NpcModels.DEFAULT_ID;
        entityData.set(DATA_MODEL, id.toString());
        refreshDimensions();
    }

    /** Stores whatever is passed in, unparsed — {@link #getSkinTexture()} validates it on read. */
    public void setSkin(String texture) {
        entityData.set(DATA_SKIN, texture);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20D)
                .add(Attributes.MOVEMENT_SPEED, 0.25D)
                .add(Attributes.ATTACK_DAMAGE, 2D)  // mandatory for MeleeAttackGoal
                .add(Attributes.FOLLOW_RANGE, 24D);
    }

    // ========== Saving ==========
    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putString("Model", getModelId().toString());
        tag.putString("Skin", entityData.get(DATA_SKIN));
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("Model")) entityData.set(DATA_MODEL, tag.getString("Model"));
        if (tag.contains("Skin")) entityData.set(DATA_SKIN, tag.getString("Skin"));

        legacyData = readLegacy(tag);
        refreshDimensions();
    }

    /** Pre-0.7 saves kept the NPC data as plain tags on the entity. Returns null for newer saves. */
    @Nullable
    private static NpcData readLegacy(CompoundTag tag) {
        if (!(tag.contains("Dialogue") || tag.contains("Quests") || tag.contains("Trades")
                || tag.contains("Attitude") || tag.contains("Behavior"))) return null;

        NpcData data = NpcData.create(NpcAiMode.OVERRIDE);
        data = data.withDialogue(load(tag, "Dialogue", NpcDialogue.CODEC, NpcDialogue.EMPTY));
        data = data.withQuests(load(tag, "Quests", NpcQuests.CODEC, NpcQuests.EMPTY));
        data = data.withTrades(load(tag, "Trades", NpcTrades.CODEC, NpcTrades.EMPTY));
        ResourceLocation attitude = ResourceLocation.tryParse(tag.getString("Attitude"));
        ResourceLocation behavior = ResourceLocation.tryParse(tag.getString("Behavior"));
        if (attitude != null) data = data.withAttitude(attitude);
        if (behavior != null) data = data.withBehavior(behavior);
        return data;
    }

    private static <T> T load(CompoundTag tag, String key, Codec<T> codec, T fallback) {
        if (!tag.contains(key)) return fallback;
        return codec.parse(NbtOps.INSTANCE, tag.get(key))
                .resultOrPartial(err -> NexusNPC.LOGGER.error("Could not migrate NPC '{}': {}", key, err))
                .orElse(fallback);
    }

    /** One-shot: returns the migrated pre-0.7 data (if any) and forgets it. */
    @Nullable
    public NpcData takeLegacyData() {
        NpcData data = legacyData;
        legacyData = null;
        return data;
    }

    // ========== Client animation ==========
    private void setupAnimationStates() {
        if (this.idleAnimationTimeout <= 0) {
            this.idleAnimationTimeout = 40;  // Animation duration in Ticks (1 sec = 20 ticks)
            this.idleAnimationState.start(this.tickCount);
        } else {
            --this.idleAnimationTimeout;
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide()) this.setupAnimationStates();
    }
}
