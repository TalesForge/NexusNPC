package com.talesforge.nexusnpc.compat.rpg.client;

import com.talesforge.nexusnpc.client.gui.section.NpcGuiRegistry;
import com.talesforge.nexusnpc.compat.rpg.NexusRpgCompat;

/** Client side of {@link NexusRpgCompat}: the editor section is only added when NexusRPG is installed. */
public final class NexusRpgClientCompat {
    private NexusRpgClientCompat() {}

    public static void registerGui() {
        if (NexusRpgCompat.isLoaded()) Impl.registerGui();
    }

    private static final class Impl {
        static void registerGui() {
            NpcGuiRegistry.register(new RpgSection.Factory());
        }
    }
}
