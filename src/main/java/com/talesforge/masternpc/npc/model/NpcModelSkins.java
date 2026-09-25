package com.talesforge.masternpc.npc.model;

import com.talesforge.masternpc.npc.NpcRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.ApiStatus;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Textures are registered PER MODEL, not globally — a texture painted for one model's UV
 * layout will look wrong (or crash the UV mapping entirely) on a model with a different
 * geometry/texture size, so there is no such thing as a texture that's valid "in general".
 * <p>
 * A base MasterNPC skin and a completely incompatible addon skin can coexist without any
 * risk of a player picking the wrong one for the wrong model: {@link #all(ResourceLocation)}
 * only ever returns the skins registered for that exact model id.
 */
public final class NpcModelSkins {
    private static final Map<ResourceLocation, Set<String>> REGISTERED = new ConcurrentHashMap<>();

    private NpcModelSkins() {}

    /** Call in your mod's constructor, both on the client and on the server. */
    @ApiStatus.Internal
    public static void add(ResourceLocation modelId, ResourceLocation texture) {
        REGISTERED.computeIfAbsent(modelId, id -> ConcurrentHashMap.newKeySet()).add(texture.toString());
    }

    /** Every texture registered for this exact model, defaultSkin first. */
    public static List<String> all(ResourceLocation modelId) {
        List<String> list = new ArrayList<>();
        String defaultSkin = NpcRegistries.model(modelId).defaultSkin().toString();
        list.add(defaultSkin);
        REGISTERED.getOrDefault(modelId, Set.of()).stream()
                .filter(s -> !s.equals(defaultSkin))
                .sorted()
                .forEach(list::add);
        return list;
    }

    /** An unknown skin, or one that belongs to a different model, falls back to that model's own default. */
    public static String validate(ResourceLocation modelId, String skin) {
        String defaultSkin = NpcRegistries.model(modelId).defaultSkin().toString();
        Set<String> registered = REGISTERED.getOrDefault(modelId, Set.of());
        return defaultSkin.equals(skin) || registered.contains(skin) ? skin : defaultSkin;
    }

    public static Component label(String skin) {
        String file = skin.substring(skin.lastIndexOf('/') + 1);
        return Component.literal(file.replace(".png", ""));
    }
}
