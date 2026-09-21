package com.talesforge.masternpc.entity.custom;

import com.talesforge.masternpc.api.event.NpcGoalsEvent;
import com.talesforge.masternpc.api.event.NpcInteractEvent;
import com.talesforge.masternpc.config.Config;
import com.talesforge.masternpc.item.ModItems;
import com.talesforge.masternpc.npc.NpcRegistries;
import com.talesforge.masternpc.npc.NpcSettings;
import com.talesforge.masternpc.npc.NpcSkins;
import com.talesforge.masternpc.npc.attitude.NpcAttitudeType;
import com.talesforge.masternpc.npc.attitude.NpcAttitudes;
import com.talesforge.masternpc.npc.behavior.NpcBehaviors;
import net.minecraft.core.Holder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.NeoForge;

import javax.annotation.Nullable;
import java.util.UUID;

public class NpcEntity extends PathfinderMob {
    private static final EntityDataAccessor<String > DATA_ATTITUDE =
            SynchedEntityData.defineId(NpcEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<String> DATA_BEHAVIOR =
            SynchedEntityData.defineId(NpcEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<String> DATA_SKIN =
            SynchedEntityData.defineId(NpcEntity.class, EntityDataSerializers.STRING);

    private static final ResourceLocation DEFAULT_SKIN =
//            ResourceLocation.fromNamespaceAndPath(MasterNPC.MOD_ID, "textures/entity/npc/default.png");
            ResourceLocation.parse(NpcSkins.DEFAULT);


    public final AnimationState idleAnimationState = new AnimationState();
    private int idleAnimationTimeout = 0;

    public NpcEntity(EntityType<? extends NpcEntity> entityType, Level level) {
        super(entityType, level);
    }





    // ========== Synchronizable data ==========
    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_ATTITUDE, NpcAttitudes.DEFAULT_ID.toString());
        builder.define(DATA_BEHAVIOR, NpcBehaviors.DEFAULT_ID.toString());
        builder.define(DATA_SKIN, DEFAULT_SKIN.toString());
    }

    public ResourceLocation getAttitudeId() {
        ResourceLocation id = ResourceLocation.tryParse(entityData.get(DATA_ATTITUDE));
        return id != null && NpcRegistries.ATTITUDES.containsKey(id) ? id : NpcAttitudes.DEFAULT_ID;
    }

    public ResourceLocation getBehaviorId() {
        ResourceLocation id = ResourceLocation.tryParse(entityData.get(DATA_BEHAVIOR));
        return id != null && NpcRegistries.BEHAVIORS.containsKey(id) ? id : NpcBehaviors.DEFAULT_ID;
    }

    public ResourceLocation getSkinTexture() {
        ResourceLocation id = ResourceLocation.tryParse(entityData.get(DATA_SKIN));
        return id != null ? id : DEFAULT_SKIN;
    }


    // ========== Setters (to be called on the server) ==========
    public void setAttitude(ResourceLocation id) {
        if (!NpcRegistries.ATTITUDES.containsKey(id) || !NpcRegistries.attitude(id).isEnabled()) {
            id = NpcAttitudes.DEFAULT_ID;   // неизвестный или запрещённый конфигом
        }
        entityData.set(DATA_ATTITUDE, id.toString());
        refreshAi();
    }

    public void setBehavior(ResourceLocation id) {
        if (!NpcRegistries.BEHAVIORS.containsKey(id)) id = NpcBehaviors.DEFAULT_ID;
        entityData.set(DATA_BEHAVIOR, id.toString());
        refreshAi();
    }

    public void setSkin(ResourceLocation texture) {
        entityData.set(DATA_SKIN, texture.toString());
    }

    public void applyStats(double maxHealth, double damage, double speed) {
        setBase(Attributes.MAX_HEALTH, Mth.clamp(maxHealth, 1.0, Config.MAX_HEALTH_LIMIT.get()));
        setBase(Attributes.ATTACK_DAMAGE, Mth.clamp(damage, 0.0, Config.MAX_DAMAGE_LIMIT.get()));
        setBase(Attributes.MOVEMENT_SPEED, Mth.clamp(speed, 0.05, 1.0));
        setHealth(getMaxHealth());
    }

    private void setBase(Holder<Attribute> attribute, double value) {
        AttributeInstance instance = getAttribute(attribute);
        if (instance != null) instance.setBaseValue(value);
    }


    // ========== AI ==========
    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));  // Not to drown

        NpcAttitudeType attitude = NpcRegistries.attitude(getAttitudeId());
        attitude.createGoals(this, goalSelector);
        attitude.createTargetGoals(this, targetSelector);

        NpcRegistries.behavior(getBehaviorId()).createGoals(this, goalSelector);

        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 6.0F));
        goalSelector.addGoal(7, new RandomLookAroundGoal(this));

        NeoForge.EVENT_BUS.post(new NpcGoalsEvent(this, goalSelector, targetSelector));
    }

    /** Rebuild the AI after changing the settings */
    public void refreshAi() {
        if (level().isClientSide()) return;
        goalSelector.removeAllGoals(goal -> true);
        targetSelector.removeAllGoals(goal -> true);
        setTarget(null);
        registerGoals();
    }


    // ========== Saving ==========
    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putString("Attitude", getAttitudeId().toString());
        tag.putString("Behavior", getBehaviorId().toString());
        tag.putString("Skin", entityData.get(DATA_SKIN));
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("Attitude")) entityData.set(DATA_ATTITUDE, tag.getString("Attitude"));
        if (tag.contains("Behavior")) entityData.set(DATA_BEHAVIOR, tag.getString("Behavior"));
        if (tag.contains("Skin")) entityData.set(DATA_SKIN, tag.getString("Skin"));
        refreshAi();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20D)
                .add(Attributes.MOVEMENT_SPEED, 0.25D)
                .add(Attributes.ATTACK_DAMAGE, 2D)   // обязательно для MeleeAttackGoal
                .add(Attributes.FOLLOW_RANGE, 24D);
    }



    // RMB on NPC
    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (player.getItemInHand(hand).is(ModItems.STAFF_CONTROL)) {
            return InteractionResult.PASS;
        }
//        if (hand == InteractionHand.MAIN_HAND && !this.level().isClientSide()) {
//            String name = player.getName().getString();
//            player.sendSystemMessage(Component.literal(String.format("Hello, %s!", name)));
//        }
//        return InteractionResult.sidedSuccess(this.level().isClientSide());

        // The event is triggered on both sides; the handler itself checks isClientSide().
        if (NeoForge.EVENT_BUS.post(new NpcInteractEvent(this, player, hand)).isCanceled()) {
            return InteractionResult.sidedSuccess(level().isClientSide());
        }
        return onInteract(player, hand);
    }

    /** Hook for heirs. By default, it does nothing. */
    protected InteractionResult onInteract(Player player, InteractionHand hand) {
        return InteractionResult.PASS;
    }

    // NPC shouldn’t disappear when the player is far away
    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return false;
    }



    public NpcSettings getSettings() {
        String name = hasCustomName() ? getCustomName().getString() : "";
        return new NpcSettings(name, getAttitudeId(), getBehaviorId(), entityData.get(DATA_SKIN),
                getAttributeBaseValue(Attributes.MAX_HEALTH),
                getAttributeBaseValue(Attributes.ATTACK_DAMAGE),
                getAttributeBaseValue(Attributes.MOVEMENT_SPEED));
    }

    /** Apply the settings from the client (Call only on the server) */
    public void applySettings(NpcSettings s) {
        String name = s.name().trim();
        if (name.length() > 32) name = name.substring(0, 32);
        setCustomName(name.isEmpty() ? null : Component.literal(name));
        setCustomNameVisible(!name.isEmpty());

        setSkin(ResourceLocation.parse(NpcSkins.validate(s.skin())));
        setAttitude(s.attitude());  // Inside, there’s already a configuration check and refreshAi().
        setBehavior(s.behavior());
        applyStats(safe(s.maxHealth(), 20.0), safe(s.damage(), 2.0), safe(s.speed(), 0.25));
    }

    // Protection against NaN and infinity in the package
    private static double safe(double value, double fallback) {
        return Double.isFinite(value) ? value : fallback;
    }



    private void setupAnimationStates() {
        if(this.idleAnimationTimeout <= 0) {
            this.idleAnimationTimeout = 20;  // Animation duration in Ticks (1 sec = 20 ticks)
            this.idleAnimationState.start(this.tickCount);
        } else {
            --this.idleAnimationTimeout;
        }
    }

    @Override
    public void tick() {
        super.tick();
        if(this.level().isClientSide()) {
            this.setupAnimationStates();
        } else {
            checkEditor();
        }
    }





    // ========== Edit ==========
    public static final double EDITOR_MAX_DIST_SQ = 64.0;  // 8 blocks, just like in saveNpc
    private static final int EDITOR_TIMEOUT_TICKS = 100;  // 5 seconds without a pulse

    @Nullable
    private UUID editorId;  // Intentionally NOT saved in NBT
    private int editorLastPingTick;

    public boolean isBeingEdited() {
        return editorId != null;
    }

    public boolean isEditedBy(Player player) {
        return editorId != null && editorId.equals(player.getUUID());
    }

    /** @return false if another player is already configuring the NPC */
    public boolean tryStartEditing(ServerPlayer player) {
        if (editorId != null && !editorId.equals(player.getUUID())) return false;
        editorId = player.getUUID();
        editorLastPingTick = tickCount;

        getNavigation().stop();  // Reset current path
        setTarget(null);  // Forget target of attack
        setDeltaMovement(getDeltaMovement().multiply(0.0, 1.0, 0.0));  // Extinguish horizontal inertia
        return true;
    }

    public void editorPing() {
        editorLastPingTick = tickCount;
    }

    public void stopEditing() {
        editorId = null;
    }

    // If true, LivingEntity.aiStep() does not start the AI: the NPC stands still and does not attack
    @Override
    protected boolean isImmobile() {
        return super.isImmobile() || editorId != null;
    }

    private void checkEditor() {
        if (editorId == null) return;

        Player editor = level().getPlayerByUUID(editorId);  // Null, if player has left or is in another dimension
        boolean invalid = editor == null
                || !editor.isAlive()
                || distanceToSqr(editor) > EDITOR_MAX_DIST_SQ
                || tickCount - editorLastPingTick > EDITOR_TIMEOUT_TICKS;

        if (invalid) stopEditing();
    }
}
