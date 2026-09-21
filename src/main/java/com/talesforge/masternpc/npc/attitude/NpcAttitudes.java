package com.talesforge.masternpc.npc.attitude;

import com.talesforge.masternpc.MasterNPC;
import com.talesforge.masternpc.npc.NpcRegistries;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class NpcAttitudes {
    public static final DeferredRegister<NpcAttitudeType> REGISTER =
            DeferredRegister.create(NpcRegistries.ATTITUDES, MasterNPC.MOD_ID);

    public static final ResourceLocation FRIENDLY_ID = ResourceLocation.fromNamespaceAndPath(MasterNPC.MOD_ID, "friendly");
    public static final ResourceLocation NEUTRAL_ID = ResourceLocation.fromNamespaceAndPath(MasterNPC.MOD_ID, "neutral");
    public static final ResourceLocation HOSTILE_ID = ResourceLocation.fromNamespaceAndPath(MasterNPC.MOD_ID, "hostile");
    public static final ResourceLocation DEFAULT_ID = FRIENDLY_ID;

    static {
        REGISTER.register("friendly", () -> new NpcAttitudeType() {});
        REGISTER.register("neutral", NeutralAttitude::new);
        REGISTER.register("hostile", HostileAttitude::new);
    }

    private NpcAttitudes() {}
}
