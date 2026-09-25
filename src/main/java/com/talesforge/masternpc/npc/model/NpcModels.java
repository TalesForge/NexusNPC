package com.talesforge.masternpc.npc.model;

import com.talesforge.masternpc.MasterNPC;
import com.talesforge.masternpc.npc.NpcRegistries;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class NpcModels {
    public static final DeferredRegister<NpcModelType> REGISTER =
            DeferredRegister.create(NpcRegistries.MODELS, MasterNPC.MOD_ID);

    public static final ResourceLocation HUMANOID_ID =
            ResourceLocation.fromNamespaceAndPath(MasterNPC.MOD_ID, "humanoid");
    public static final ResourceLocation DEFAULT_ID = HUMANOID_ID;

    /** The one skin that always exists no matter what — every model's own default is separate from this. */
    public static final ResourceLocation HUMANOID_DEFAULT_SKIN =
            ResourceLocation.fromNamespaceAndPath(MasterNPC.MOD_ID, "textures/entity/npc/default.png");

    static {
        // Same hitbox MasterNPC has always used (ModEntities.NPC's EntityType.Builder), now
        // also the source of truth for the RUNTIME per-instance hitbox (see ModEventBusEvents#onSize).
        // 0.94f matches the cosmetic fudge NpcRenderer used to hardcode for every model.
        REGISTER.register("humanoid", () -> new NpcModelType(0.6f, 1.8f, 1.62f, 0.94f, HUMANOID_DEFAULT_SKIN));
    }

    private NpcModels() {}
}
