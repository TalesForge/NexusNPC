package com.talesforge.nexusnpc.client.dialogue;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.mojang.serialization.JsonOps;
import com.talesforge.nexusnpc.NexusNPC;
import com.talesforge.nexusnpc.npc.dialogue.DialoguePage;
import net.neoforged.fml.loading.FMLPaths;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Stream;

/**
 * Client-only, reusable dialogue pages, one file per page: {@code <gamedir>/nexusnpc/dialogues/<id>.json}.
 * Independent of any world or NPC — save a page once, add it to as many NPCs (in as many
 * worlds) as you like, and hand the .json file to someone else so they can drop it into
 * their own folder.
 * <p>
 * Adding a saved page to an NPC's dialogue copies it in with a FRESH page id (see
 * DialogueListScreen#add) — the copy is independent from that point on: editing the library
 * file afterwards does not change NPCs that already got a copy.
 */
public final class DialogueLibrary {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private DialogueLibrary() {}

    private static Path directory() {
        Path dir = FMLPaths.GAMEDIR.get().resolve("nexusnpc").resolve("dialogues");
        try {
            Files.createDirectories(dir);
        } catch (IOException e) {
            NexusNPC.LOGGER.error("Could not create dialogue library folder {}", dir, e);
        }
        return dir;
    }

    /** All saved pages, sorted by display name. A corrupt file is skipped and logged, never crashes the screen. */
    public static List<DialoguePage> list() {
        Path dir = directory();
        List<DialoguePage> result = new ArrayList<>();
        try (Stream<Path> files = Files.list(dir)) {
            for (Path file : files.filter(p -> p.toString().endsWith(".json")).toList()) {
                readEntry(file).ifPresent(result::add);
            }
        } catch (IOException e) {
            NexusNPC.LOGGER.error("Could not list dialogue library folder {}", dir, e);
        }
        result.sort(Comparator.comparing(DialoguePage::displayName, String.CASE_INSENSITIVE_ORDER));
        return result;
    }

    private static Optional<DialoguePage> readEntry(Path file) {
        try {
            JsonElement json = GSON.fromJson(Files.readString(file, StandardCharsets.UTF_8), JsonElement.class);
            return DialoguePage.CODEC.parse(JsonOps.INSTANCE, json)
                    .resultOrPartial(err -> NexusNPC.LOGGER.error("Could not read dialogue file {}: {}", file, err));
        } catch (IOException | JsonParseException e) {
            NexusNPC.LOGGER.error("Could not read dialogue file {}", file, e);
            return Optional.empty();
        }
    }

    public static void save(DialoguePage page) {
        Path file = directory().resolve(page.id() + ".json");
        DialoguePage.CODEC.encodeStart(JsonOps.INSTANCE, page)
                .resultOrPartial(err -> NexusNPC.LOGGER.error("Could not encode dialogue {}: {}", page.id(), err))
                .ifPresent(json -> {
                    try {
                        Files.writeString(file, GSON.toJson(json), StandardCharsets.UTF_8);
                    } catch (IOException e) {
                        NexusNPC.LOGGER.error("Could not save dialogue file {}", file, e);
                    }
                });
    }

    public static void delete(DialoguePage page) {
        try {
            Files.deleteIfExists(directory().resolve(page.id() + ".json"));
        } catch (IOException e) {
            NexusNPC.LOGGER.error("Could not delete dialogue file for {}", page.id(), e);
        }
    }
}