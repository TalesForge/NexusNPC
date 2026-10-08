package com.talesforge.nexusnpc.config;

import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.List;

public class Config {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    // ===== NPC =====
    public static final ModConfigSpec.DoubleValue MAX_HEALTH_LIMIT = BUILDER
            .comment("The maximum allowable health of an NPC")
            .defineInRange("max_health_limit", 100.0, 1.0, 1024.0);

    public static final ModConfigSpec.DoubleValue MAX_DAMAGE_LIMIT = BUILDER
            .comment("The maximum allowable attack damage of an NPC")
            .defineInRange("max_damage_limit", 20.0, 0.0, 1024.0);

    public static final ModConfigSpec.BooleanValue ALLOW_HOSTILE = BUILDER
            .comment("Allow hostile NPCs to be created")
            .define("allow_hostile_npc", true);

    public static final ModConfigSpec.IntValue MAX_NPCS_PER_LEVEL = BUILDER
            .comment("Maximum number of NPCs allowed in a single dimension at once. 0 = unlimited")
            .defineInRange("max_npcs_per_level", 200, 0, Integer.MAX_VALUE);

    public static final ModConfigSpec.BooleanValue REQUIRE_OP_PERMISSION = BUILDER
            .comment("Require operator permission (level 2) to create or edit NPCs with the Staff of Control")
            .define("require_op_permission", true);

    public static final ModConfigSpec.BooleanValue ALLOW_VANILLA_MOBS = BUILDER
            .comment("Allow turning ordinary (vanilla and modded) mobs into NPCs, not only NexusNPC's own NPC entity types")
            .define("allow_vanilla_mobs", true);

    public static final ModConfigSpec.ConfigValue<List<? extends String>> BLOCKED_ENTITY_TYPES = BUILDER
            .comment("Entity type ids that can never become NPCs (bosses and other mobs that break when their AI or data is touched)")
            .defineListAllowEmpty("blocked_entity_types",
                    List.of("minecraft:ender_dragon", "minecraft:wither"),
                    () -> "minecraft:ender_dragon",
                    o -> o instanceof String);

    public static final ModConfigSpec SPEC = BUILDER.build();

    private Config() {}
}