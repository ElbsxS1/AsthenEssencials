package br.asthen.essentials.core;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.LinkedHashMap;

/** Blocks (e.g. a chest in the lobby) that hand out the menu item when right-clicked. */
public final class Pickups {
    private final Path file;
    private final Set<String> keys = new LinkedHashSet<>();

    public Pickups(Path file) {
        this.file = file;
        Object o = JsonFile.load(file).get("blocks");
        if (o instanceof List<?> list) {
            for (Object e : list) {
                if (e instanceof String s && s.matches("[^|]+\\|-?\\d+\\|-?\\d+\\|-?\\d+")) keys.add(s);
            }
        }
    }

    private static String key(String world, int x, int y, int z) {
        return world.toLowerCase(Locale.ROOT) + "|" + x + "|" + y + "|" + z;
    }

    private synchronized void save() {
        Map<String, Object> root = new LinkedHashMap<>();
        root.put("blocks", new ArrayList<Object>(keys));
        JsonFile.save(file, root);
    }

    public synchronized boolean has(String world, int x, int y, int z) {
        return keys.contains(key(world, x, y, z));
    }

    /** False when it was already registered. */
    public synchronized boolean add(String world, int x, int y, int z) {
        boolean added = keys.add(key(world, x, y, z));
        if (added) save();
        return added;
    }

    public synchronized boolean remove(String world, int x, int y, int z) {
        boolean removed = keys.remove(key(world, x, y, z));
        if (removed) save();
        return removed;
    }

    public synchronized int count() {
        return keys.size();
    }
}
