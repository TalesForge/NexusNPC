package com.talesforge.nexusnpc.npc;

import com.talesforge.nexusnpc.config.Config;
import com.talesforge.nexusnpc.entity.custom.NpcEntity;
import com.talesforge.nexusnpc.npc.ai.NpcAi;
import com.talesforge.nexusnpc.npc.attitude.NpcAttitudes;
import com.talesforge.nexusnpc.npc.behavior.NpcBehaviors;
import com.talesforge.nexusnpc.npc.data.NpcAiMode;
import com.talesforge.nexusnpc.npc.data.NpcData;
import com.talesforge.nexusnpc.npc.dialogue.NpcDialogue;
import com.talesforge.nexusnpc.npc.quest.ModAttachments;
import com.talesforge.nexusnpc.npc.quest.NpcQuests;
import com.talesforge.nexusnpc.npc.runtime.NpcHost;
import com.talesforge.nexusnpc.npc.runtime.NpcRuntime;
import com.talesforge.nexusnpc.npc.trade.NpcMerchant;
import com.talesforge.nexusnpc.npc.trade.NpcTrades;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import org.jetbrains.annotations.Nullable;

import java.util.function.UnaryOperator;

/**
 * The one entry point to the NPC layer that lives on every {@link Mob}.
 * <p>
 * Any mob — vanilla or modded — can be turned into an NPC with {@link #enable}. A mob "is an NPC"
 * exactly when it carries the {@code NPC_DATA} attachment; mobs without it are never touched,
 * never ticked by this mod and never change behavior.
 */
public final class Npcs {
    private Npcs() {}

    // ================= identity =================

    public static boolean isNpc(@Nullable Entity entity) {
        return entity instanceof Mob mob && mob.hasData(ModAttachments.NPC_DATA);
    }

    /** Can this entity type become an NPC at all? (Does not check that it is actually a Mob — see {@link #isEligible(Mob)}.) */
    public static boolean isEligible(EntityType<?> type) {
        ResourceLocation id = BuiltInRegistries.ENTITY_TYPE.getKey(type);
        if (Config.BLOCKED_ENTITY_TYPES.get().contains(id.toString())) return false;
        return Config.ALLOW_VANILLA_MOBS.get() || isOwnNpcType(id);
    }

    public static boolean isEligible(Mob mob) {
        return isEligible(mob.getType());
    }

    private static boolean isOwnNpcType(ResourceLocation id) {
        return com.talesforge.nexusnpc.api.NexusNpcApi.types().stream().anyMatch(e -> e.id().equals(id));
    }

    // ================= data =================

    /** The mob's NPC data, or a fresh default (NOT attached) if it is not an NPC yet. Safe to call on any mob. */
    public static NpcData data(Mob mob) {
        return mob.hasData(ModAttachments.NPC_DATA)
                ? mob.getData(ModAttachments.NPC_DATA)
                : NpcData.create(defaultMode(mob));
    }

    /** Server side: replace the mob's NPC data (this also makes it an NPC if it was not). */
    public static void setData(Mob mob, NpcData data) {
        mob.setData(ModAttachments.NPC_DATA, data);
    }

    public static void update(Mob mob, UnaryOperator<NpcData> change) {
        setData(mob, change.apply(data(mob)));
    }

    /** NexusNPC's own entity keeps the old behavior (replace the AI); every other mob keeps its AI. */
    public static NpcAiMode defaultMode(Mob mob) {
        return mob instanceof NpcEntity ? NpcAiMode.OVERRIDE : NpcAiMode.VANILLA;
    }

    /**
     * Turn the mob into an NPC (idempotent). Data only — the caller decides when to (re)build the AI
     * so that a batch of changes costs one rebuild.
     *
     * @return true if the mob was NOT an NPC before
     */
    public static boolean attach(Mob mob) {
        if (isNpc(mob)) return false;
        setData(mob, NpcData.create(defaultMode(mob)));
        mob.setPersistenceRequired();  // An NPC must not despawn. Saved in the mob's own NBT, works for every mob type.
        return true;
    }

    /** {@link #attach} plus an immediate AI build. */
    public static void enable(Mob mob) {
        if (attach(mob)) NpcAi.apply(mob);
    }

    /** Remove everything NexusNPC added: data, AI override, merchant. The mob goes back to being an ordinary mob. */
    public static void disable(Mob mob) {
        if (!isNpc(mob)) return;
        NpcAi.restore(mob);
        mob.removeData(ModAttachments.NPC_DATA);
        NpcRuntime rt = peekRuntime(mob);
        if (rt != null) rt.merchant = null;
    }

    /** Copy NPC data to another mob (e.g. after a vanilla conversion such as villager -> zombie villager). */
    public static void copyTo(Mob from, Mob to) {
        if (!isNpc(from)) return;
        setData(to, data(from));
        to.setPersistenceRequired();
    }

    // ================= typed accessors =================

    public static NpcDialogue dialogue(Mob mob) { return data(mob).dialogue(); }
    public static NpcQuests quests(Mob mob) { return data(mob).quests(); }
    public static NpcTrades trades(Mob mob) { return data(mob).trades(); }

    public static void setDialogue(Mob mob, NpcDialogue v) { update(mob, d -> d.withDialogue(v)); }
    public static void setQuests(Mob mob, NpcQuests v) { update(mob, d -> d.withQuests(v)); }

    public static void setTrades(Mob mob, NpcTrades v) {
        update(mob, d -> d.withTrades(v));
        NpcRuntime rt = peekRuntime(mob);
        if (rt != null && rt.merchant != null) rt.merchant.invalidate();
    }

    /** Validated against the registries; falls back to the default when the id is unknown (removed addon). */
    public static ResourceLocation attitudeId(Mob mob) {
        ResourceLocation id = data(mob).attitude();
        return NpcRegistries.ATTITUDES.containsKey(id) ? id : NpcAttitudes.DEFAULT_ID;
    }

    public static ResourceLocation behaviorId(Mob mob) {
        ResourceLocation id = data(mob).behavior();
        return NpcRegistries.BEHAVIORS.containsKey(id) ? id : NpcBehaviors.DEFAULT_ID;
    }

    public static NpcAiMode aiMode(Mob mob) { return data(mob).aiMode(); }

    /** Data only (no AI rebuild) — see {@link NpcAi#apply}. */
    public static void setAttitudeData(Mob mob, ResourceLocation id) {
        if (!NpcRegistries.ATTITUDES.containsKey(id) || !NpcRegistries.attitude(id).isEnabled()) id = NpcAttitudes.DEFAULT_ID;
        ResourceLocation checked = id;
        update(mob, d -> d.withAttitude(checked));
    }

    public static void setBehaviorData(Mob mob, ResourceLocation id) {
        if (!NpcRegistries.BEHAVIORS.containsKey(id)) id = NpcBehaviors.DEFAULT_ID;
        ResourceLocation checked = id;
        update(mob, d -> d.withBehavior(checked));
    }

    public static void setAiModeData(Mob mob, NpcAiMode mode) { update(mob, d -> d.withAiMode(mode)); }

    // ================= runtime =================

    public static NpcRuntime runtime(Mob mob) {
        return ((NpcHost) mob).nexusnpc$getRuntime();
    }

    /** Null if nothing has ever needed transient state for this mob. */
    @Nullable
    public static NpcRuntime peekRuntime(Mob mob) {
        return ((NpcHost) mob).nexusnpc$peekRuntime();
    }

    /** The mob's Merchant adapter — the mob itself is never made a {@code Merchant}. */
    public static NpcMerchant merchant(Mob mob) {
        NpcRuntime rt = runtime(mob);
        if (rt.merchant == null) rt.merchant = new NpcMerchant(mob);
        return rt.merchant;
    }
}
