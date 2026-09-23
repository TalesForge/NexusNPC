package com.talesforge.masternpc.client.gui.section;

import com.talesforge.masternpc.client.gui.section.core.AppearanceSection;
import com.talesforge.masternpc.client.gui.section.core.IdentitySection;
import com.talesforge.masternpc.client.gui.section.core.LogicSection;
import com.talesforge.masternpc.client.gui.section.core.StatsSection;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.ApiStatus;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Client-side registry deciding what the NPC editor screen looks like:
 * <ul>
 *   <li>{@link #register} — add a new section, core or addon, in the order it should appear.</li>
 *   <li>{@link #disable(ResourceLocation)} — hide a section for every NPC type.</li>
 *   <li>{@link #disable(ResourceLocation, ResourceLocation)} — hide a section only for one NPC entity type.</li>
 *   <li>{@link #overrideScreen} — replace the whole editor screen for one NPC entity type.</li>
 * </ul>
 * All of this is intentionally client-only: it references client widget/screen types that
 * don't exist on a dedicated server, and it never needs to — sections only READ/WRITE
 * {@link com.talesforge.masternpc.npc.field.NpcDataMap}, which is the actual thing that
 * crosses the network.
 */
public final class NpcGuiRegistry {
    private static final List<NpcGuiSectionFactory> SECTIONS = new ArrayList<>();
    private static final Set<ResourceLocation> DISABLED_GLOBALLY = new HashSet<>();
    private static final Map<ResourceLocation, Set<ResourceLocation>> DISABLED_FOR_TYPE = new HashMap<>();
    private static final Map<ResourceLocation, NpcGuiScreenFactory> SCREEN_OVERRIDES = new HashMap<>();
    private static boolean bootstrapped = false;

    private NpcGuiRegistry() {}

    /** Call from your mod's client-side constructor / client setup. Order = default on-screen order. */
    public static synchronized void register(NpcGuiSectionFactory factory) {
        SECTIONS.add(factory);
    }

    /** Hide a section for every NPC type — e.g. replacing the whole attitude/behavior model with your own. */
    public static synchronized void disable(ResourceLocation sectionId) {
        DISABLED_GLOBALLY.add(sectionId);
    }

    /** Hide a section only for one specific NPC entity type (e.g. hide combat stats for a shopkeeper NPC). */
    public static synchronized void disable(ResourceLocation entityTypeId, ResourceLocation sectionId) {
        DISABLED_FOR_TYPE.computeIfAbsent(entityTypeId, k -> new HashSet<>()).add(sectionId);
    }

    /** Replace the entire editor screen for one specific NPC entity type. */
    public static synchronized void overrideScreen(ResourceLocation entityTypeId, NpcGuiScreenFactory factory) {
        SCREEN_OVERRIDES.put(entityTypeId, factory);
    }

    public static synchronized Optional<NpcGuiScreenFactory> screenOverride(ResourceLocation entityTypeId) {
        return Optional.ofNullable(SCREEN_OVERRIDES.get(entityTypeId));
    }

    /** Sections to show for this NPC entity type, in registration order, with disables already applied. */
    public static synchronized List<NpcGuiSectionFactory> activeSections(ResourceLocation entityTypeId) {
        Set<ResourceLocation> disabledHere = DISABLED_FOR_TYPE.getOrDefault(entityTypeId, Set.of());
        List<NpcGuiSectionFactory> result = new ArrayList<>();
        for (NpcGuiSectionFactory factory : SECTIONS) {
            if (DISABLED_GLOBALLY.contains(factory.id())) continue;
            if (disabledHere.contains(factory.id())) continue;
            result.add(factory);
        }
        return result;
    }

    /** Called once from MasterNPCClient. Addons register their own sections afterwards, from their own client init. */
    @ApiStatus.Internal
    public static synchronized void bootstrap() {
        if (bootstrapped) return;
        bootstrapped = true;
        register(new IdentitySection.Factory());
        register(new LogicSection.Factory());
        register(new AppearanceSection.Factory());
        register(new StatsSection.Factory());
    }
}