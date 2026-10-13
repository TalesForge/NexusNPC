package com.talesforge.nexusnpc.npc.model;

import com.talesforge.nexusnpc.NexusNPC;
import com.talesforge.nexusnpc.npc.NpcRegistries;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class NpcModels {
    public static final DeferredRegister<NpcModelType> REGISTER =
            DeferredRegister.create(NpcRegistries.MODELS, NexusNPC.MOD_ID);

    public static final ResourceLocation HUMANOID_ID =
            ResourceLocation.fromNamespaceAndPath(NexusNPC.MOD_ID, "humanoid");
    public static final ResourceLocation DEFAULT_ID = HUMANOID_ID;

    /** The one skin that always exists no matter what — every model's own default is separate from this. */
    public static final ResourceLocation HUMANOID_DEFAULT_SKIN =
            ResourceLocation.fromNamespaceAndPath(NexusNPC.MOD_ID, "textures/entity/npc/default.png");

    /**
     * Shortest and tallest humanoid NPC, in blocks. The geometry has to cover the whole range (see HeightProfile):
     * going shorter than ~1.3 would need more than the 6 repeatable rows of a leg can give up.
     */
    public static final float MIN_HUMANOID_HEIGHT = 1.3f;
    public static final float MAX_HUMANOID_HEIGHT = 3.0f;

    static {
        // Same hitbox NexusNPC has always used (ModEntities.NPC's EntityType.Builder), now
        // also the source of truth for the RUNTIME per-instance hitbox (see ModEventBusEvents#onSize).
        // 0.94f matches the cosmetic fudge NpcRenderer used to hardcode for every model.
        REGISTER.register("humanoid", () -> new NpcModelType(
                0.6f, 1.8f, 1.62f, 0.94f, HUMANOID_DEFAULT_SKIN,
                MIN_HUMANOID_HEIGHT, MAX_HUMANOID_HEIGHT));  // Any height in this range: see HeightProfile
    }

    private NpcModels() {}
}
