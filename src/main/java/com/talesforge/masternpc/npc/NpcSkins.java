package com.talesforge.masternpc.npc;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.ApiStatus;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class NpcSkins {
    public static final String DEFAULT = "masternpc:textures/entity/npc/default.png";
    private static final Set<String> REGISTERED = ConcurrentHashMap.newKeySet();

    private NpcSkins() {}

    @ApiStatus.Internal
    public static void add(ResourceLocation texture) {
        REGISTERED.add(texture.toString());
    }

    public static List<String> all() {
        List<String> list = new ArrayList<>();
        list.add(DEFAULT);
        REGISTERED.stream().filter(s -> !s.equals(DEFAULT)).sorted().forEach(list::add);
        return list;
    }

    public static String validate(String skin) {
        return DEFAULT.equals(skin) || REGISTERED.contains(skin) ? skin : DEFAULT;
    }

    public static Component label(String skin) {
        String file = skin.substring(skin.lastIndexOf('/') + 1);
        return Component.literal(file.replace(".png", ""));
    }
}
