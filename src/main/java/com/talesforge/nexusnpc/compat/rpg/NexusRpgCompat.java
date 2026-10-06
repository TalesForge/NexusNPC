package com.talesforge.nexusnpc.compat.rpg;

import com.talesforge.nexusnpc.npc.attitude.NpcAttitudeType;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Optional NexusRPG support. NexusNPC works without NexusRPG; when both mods are installed, the NPC gets
 * faction / classes / team settings and the "by faction" attitude.
 * <p>
 * RULE: nothing outside this package may reference {@code com.talesforge.nexusrpg.*}, and inside it only
 * code reached through {@link Impl} (which is loaded lazily, i.e. only when NexusRPG exists) may do so.
 * Calling an {@code Impl} method from a plain {@code if (isLoaded())} is what keeps a missing mod from
 * causing a NoClassDefFoundError.
 */
public final class NexusRpgCompat {
    public static final String MOD_ID = "nexusrpg";

    private NexusRpgCompat() {}

    public static boolean isLoaded() {
        ModList mods = ModList.get();
        return mods != null && mods.isLoaded(MOD_ID);
    }

    /** Registers the NexusRPG-backed NPC setting fields. Called from the NexusNPC constructor. */
    public static void bootstrap() {
        if (isLoaded()) Impl.bootstrap();
    }

    /** Adds the NexusRPG-only attitudes to NexusNPC's attitude register. */
    public static void registerAttitudes(DeferredRegister<NpcAttitudeType> register) {
        if (isLoaded()) Impl.registerAttitudes(register);
    }

    /** Only ever initialised when NexusRPG is present. */
    private static final class Impl {
        static void bootstrap() {
            RpgSettingFields.bootstrap();
        }

        static void registerAttitudes(DeferredRegister<NpcAttitudeType> register) {
            register.register("faction_based", FactionAttitude::new);
        }
    }
}
