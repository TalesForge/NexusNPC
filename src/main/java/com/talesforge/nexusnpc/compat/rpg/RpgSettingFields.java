package com.talesforge.nexusnpc.compat.rpg;

import com.mojang.serialization.Codec;
import com.talesforge.nexusnpc.NexusNPC;
import com.talesforge.nexusnpc.entity.custom.NpcEntity;
import com.talesforge.nexusnpc.npc.field.NpcSettingField;
import com.talesforge.nexusnpc.npc.field.NpcSettingFields;
import com.talesforge.nexusrpg.api.NexusRPGApi;
import com.talesforge.nexusrpg.api.profile.ProfileService;
import com.talesforge.nexusrpg.api.team.LeaveReason;
import com.talesforge.nexusrpg.api.team.RpgTeam;
import com.talesforge.nexusrpg.api.team.TeamService;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import org.jetbrains.annotations.ApiStatus;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * NexusRPG-backed setting fields: faction, classes and team of an NPC. The data itself lives in NexusRPG
 * (the NPC's RPG profile), so every mod sees the same values; these fields only make it editable and
 * synchronizable through the standard NPC editor protocol. No client-only classes here.
 * <p>
 * Each setter does nothing when the value did not change: saving the editor must not "customize" an
 * NPC whose faction/classes simply come from the defaults of its entity type.
 */
public final class RpgSettingFields {
    private static boolean bootstrapped = false;

    private RpgSettingFields() {}

    private static ResourceLocation rl(String path) {
        return ResourceLocation.fromNamespaceAndPath(NexusNPC.MOD_ID, path);
    }

    /** Faction id, or an empty string for "no faction". */
    public static final NpcSettingField<String> FACTION = new NpcSettingField<>() {
        public ResourceLocation id() { return rl("faction"); }
        public Codec<String> codec() { return Codec.STRING; }
        public String defaultValue() { return ""; }
        public String get(NpcEntity npc) {
            return NexusRPGApi.profiles().faction(npc).map(ResourceLocation::toString).orElse("");
        }
        public void set(NpcEntity npc, String value) {
            String raw = value.trim();
            ResourceLocation target = raw.isEmpty() ? null : ResourceLocation.tryParse(raw);
            if (!raw.isEmpty() && target == null) return;  // Malformed id: ignore
            if (Objects.equals(target, NexusRPGApi.profiles().faction(npc).orElse(null))) return;
            NexusRPGApi.profiles().setFaction(npc, target);  // Unknown faction / canceled event -> stays unchanged
        }
    };

    /** Class ids. Unknown ids and anything over the configured limit are rejected by NexusRPG. */
    public static final NpcSettingField<List<String>> CLASSES = new NpcSettingField<>() {
        public ResourceLocation id() { return rl("classes"); }
        public Codec<List<String>> codec() { return Codec.STRING.listOf(); }
        public List<String> defaultValue() { return List.of(); }
        public List<String> get(NpcEntity npc) {
            return NexusRPGApi.profiles().classes(npc).stream().map(ResourceLocation::toString).toList();
        }
        public void set(NpcEntity npc, List<String> value) {
            List<ResourceLocation> wanted = new ArrayList<>();
            for (String raw : value) {
                if (raw.isBlank()) continue;
                ResourceLocation id = ResourceLocation.tryParse(raw.trim());
                if (id != null && !wanted.contains(id)) wanted.add(id);
            }
            ProfileService profiles = NexusRPGApi.profiles();
            for (ResourceLocation current : List.copyOf(profiles.classes(npc))) {
                if (!wanted.contains(current)) profiles.removeClass(npc, current);  // Free the slots first...
            }
            for (ResourceLocation id : wanted) {
                if (!profiles.hasClass(npc, id)) profiles.addClass(npc, id);  // ...then add
            }
        }
    };

    /**
     * Name of the NexusRPG team the NPC belongs to (empty = none). Setting a name joins an EXISTING team
     * (create teams with {@code /nexusrpg team create}); an unknown name is ignored.
     */
    public static final NpcSettingField<String> TEAM = new NpcSettingField<>() {
        public ResourceLocation id() { return rl("team"); }
        public Codec<String> codec() { return Codec.STRING; }
        public String defaultValue() { return ""; }
        public String get(NpcEntity npc) {
            return NexusRPGApi.teams().teamOf(npc).map(RpgTeam::name).orElse("");
        }
        public void set(NpcEntity npc, String value) {
            MinecraftServer server = npc.getServer();
            if (server == null) return;
            TeamService teams = NexusRPGApi.teams();
            String name = value.trim();
            RpgTeam current = teams.teamOf(npc).orElse(null);
            if (name.isEmpty()) {
                if (current != null) teams.leave(npc, LeaveReason.REMOVED);
                return;
            }
            if (current != null && current.name().equalsIgnoreCase(name)) return;
            teams.findByName(server, name).ifPresent(team -> teams.join(team.id(), npc));
        }
    };

    /** Called once from {@code NexusNPC}'s constructor, right after {@link NpcSettingFields#bootstrap()}. */
    @ApiStatus.Internal
    public static synchronized void bootstrap() {
        if (bootstrapped) return;
        bootstrapped = true;
        NpcSettingFields.register(FACTION);
        NpcSettingFields.register(CLASSES);
        NpcSettingFields.register(TEAM);
    }
}
