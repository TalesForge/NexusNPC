package com.talesforge.masternpc.config;

import net.neoforged.neoforge.common.ModConfigSpec;

public class Config {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    // ===== NPC =====
    public static final ModConfigSpec.DoubleValue MAX_HEALTH_LIMIT = BUILDER
            .comment("The maximum allowable health of an NPC")
            .defineInRange("maxHealthLimit", 100.0, 1.0, 1024.0);

    public static final ModConfigSpec.DoubleValue MAX_DAMAGE_LIMIT = BUILDER
            .comment("The maximum allowable attack damage of an NPC")
            .defineInRange("maxDamageLimit", 20.0, 0.0, 1024.0);

    public static final ModConfigSpec.BooleanValue ALLOW_HOSTILE = BUILDER
            .comment("Allow hostile NPCs to be created")
            .define("allowHostileNpc", true);

    public static final ModConfigSpec.IntValue MAX_NPCS_PER_LEVEL = BUILDER
            .comment("Maximum number of NPCs allowed in a single dimension at once. 0 = unlimited")
            .defineInRange("maxNpcsPerLevel", 200, 0, Integer.MAX_VALUE);

    public static final ModConfigSpec.BooleanValue REQUIRE_OP_PERMISSION = BUILDER
            .comment("Require operator permission (level 2) to create or edit NPCs with the Staff of Control")
            .define("requireOpPermission", true);

    public static final ModConfigSpec SPEC = BUILDER.build();

    private Config() {}
}