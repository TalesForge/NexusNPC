package com.talesforge.masternpc.npc.behavior;

import com.talesforge.masternpc.MasterNPC;
import com.talesforge.masternpc.npc.NpcRegistries;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class NpcBehaviors {
    public static final DeferredRegister<NpcBehaviorType> REGISTER =
            DeferredRegister.create(NpcRegistries.BEHAVIORS, MasterNPC.MOD_ID);

    public static final ResourceLocation STAY_ID = ResourceLocation.fromNamespaceAndPath(MasterNPC.MOD_ID, "stay");
    public static final ResourceLocation WANDER_ID = ResourceLocation.fromNamespaceAndPath(MasterNPC.MOD_ID, "wander");
    public static final ResourceLocation AVOID_PLAYERS_ID = ResourceLocation.fromNamespaceAndPath(MasterNPC.MOD_ID, "avoid_players");
    public static final ResourceLocation DEFAULT_ID = WANDER_ID;

    static {
        REGISTER.register("stay", () -> (npc, goals) -> {});
        REGISTER.register("wander", () -> new WanderBehavior(0.8));
        REGISTER.register("avoid_players", AvoidPlayersBehavior::new);
    }

    private NpcBehaviors() {}
}
