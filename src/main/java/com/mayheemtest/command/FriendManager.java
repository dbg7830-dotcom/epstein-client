package com.mayheemtest.command;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import net.fabricmc.loader.api.FabricLoader;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * FriendManager
 *
 * In-memory whitelist of player names that persists across sessions.
 * Saved to: <minecraft>/config/mayheemtest_friends.json
 *
 * Loaded once on startup via MayheemTestMod, saved every time the list changes.
 * Nothing here ever touches the network.
 */
public class FriendManager {

    private static final FriendManager INSTANCE = new FriendManager();
    private static final Gson GSON = new Gson();
    private static final String FILE_NAME = "mayheemtest_friends.json";

    private final Set<String> friends = new HashSet<>();

    private FriendManager() {}

    public static FriendManager get() {
        return INSTANCE;
    }

    // ── Public API ────────────────────────────────────────────────────────

    public boolean isFriend(String name) {
        return friends.contains(name.toLowerCase());
    }

    /** Returns false if already on the list. Saves on success. */
    public boolean add(String name) {
        boolean added = friends.add(name.toLowerCase());
        if (added) save();
        return added;
    }

    /** Returns false if not on the list. Saves on success. */
    public boolean remove(String name) {
        boolean removed = friends.remove(name.toLowerCase());
        if (removed) save();
        return removed;
    }

    public void clear() {
        friends.clear();
        save();
    }

    public Set<String> getAll() {
        return Collections.unmodifiableSet(friends);
    }

    // ── Persistence ───────────────────────────────────────────────────────

    /** Call once from MayheemTestMod.onInitializeClient() */
    public void load() {
        Path file = savePath();
        if (!Files.exists(file)) return;

        try (Reader reader = Files.newBufferedReader(file)) {
            Type setType = new TypeToken<Set<String>>() {}.getType();
            Set<String> loaded = GSON.fromJson(reader, setType);
            if (loaded != null) {
                friends.clear();
                friends.addAll(loaded);
            }
        } catch (IOException e) {
            System.err.println("[MayheemTest] Failed to load friends list: " + e.getMessage());
        }
    }

    private void save() {
        Path file = savePath();
        try {
            Files.createDirectories(file.getParent());
            try (Writer writer = Files.newBufferedWriter(file)) {
                GSON.toJson(friends, writer);
            }
        } catch (IOException e) {
            System.err.println("[MayheemTest] Failed to save friends list: " + e.getMessage());
        }
    }

    private Path savePath() {
        return FabricLoader.getInstance().getConfigDir().resolve(FILE_NAME);
    }
}
