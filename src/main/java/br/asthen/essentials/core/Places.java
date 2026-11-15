package br.asthen.essentials.core;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

/** Homes (per player), warps (global) and the spawn point. Names are case-insensitive. */
public final class Places {

    public enum Result { OK, INVALID_NAME, LIMIT, NOT_FOUND }

    private final Path file;
    private final Map<UUID, Map<String, Loc>> homes = new LinkedHashMap<>();
    private final Map<String, Loc> warps = new TreeMap<>();
    private Loc spawn;

    public Places(Path file) {
        this.file = file;
        load();
    }

    private void load() {
        Map<String, Object> root = JsonFile.load(file);
        for (Map.Entry<String, Object> e : JsonFile.obj(root.get("homes")).entrySet()) {
            try {
                UUID id = UUID.fromString(e.getKey());
                Map<String, Loc> m = new TreeMap<>();
                for (Map.Entry<String, Object> h : JsonFile.obj(e.getValue()).entrySet()) {
                    Loc l = Loc.fromMap(JsonFile.obj(h.getValue()));
                    if (l != null) m.put(h.getKey().toLowerCase(), l);
                }
                homes.put(id, m);
            } catch (IllegalArgumentException ignored) {
            }
        }
        for (Map.Entry<String, Object> e : JsonFile.obj(root.get("warps")).entrySet()) {
            Loc l = Loc.fromMap(JsonFile.obj(e.getValue()));
            if (l != null) warps.put(e.getKey().toLowerCase(), l);
        }
        spawn = Loc.fromMap(JsonFile.obj(root.get("spawn")));
    }

    private synchronized void save() {
        Map<String, Object> root = new LinkedHashMap<>();
        Map<String, Object> h = new LinkedHashMap<>();
        homes.forEach((id, m) -> {
            Map<String, Object> mm = new LinkedHashMap<>();
            m.forEach((n, l) -> mm.put(n, l.toMap()));
            h.put(id.toString(), mm);
        });
        root.put("homes", h);
        Map<String, Object> w = new LinkedHashMap<>();
        warps.forEach((n, l) -> w.put(n, l.toMap()));
        root.put("warps", w);
        if (spawn != null) root.put("spawn", spawn.toMap());
        JsonFile.save(file, root);
    }

    // ---- homes
    public synchronized Result setHome(UUID id, String name, Loc loc, int max) {
        if (!Names.validPlace(name)) return Result.INVALID_NAME;
        Map<String, Loc> m = homes.computeIfAbsent(id, k -> new TreeMap<>());
        String key = name.toLowerCase();
        if (!m.containsKey(key) && m.size() >= max) return Result.LIMIT;
        m.put(key, loc);
        save();
        return Result.OK;
    }

    public synchronized Loc home(UUID id, String name) {
        Map<String, Loc> m = homes.get(id);
        return m == null ? null : m.get(name.toLowerCase());
    }

    public synchronized Result delHome(UUID id, String name) {
        Map<String, Loc> m = homes.get(id);
        if (m == null || m.remove(name.toLowerCase()) == null) return Result.NOT_FOUND;
        save();
        return Result.OK;
    }

    public synchronized List<String> homeNames(UUID id) {
        Map<String, Loc> m = homes.get(id);
        return m == null ? List.of() : new ArrayList<>(m.keySet());
    }

    // ---- warps
    public synchronized Result setWarp(String name, Loc loc) {
        if (!Names.validPlace(name)) return Result.INVALID_NAME;
        warps.put(name.toLowerCase(), loc);
        save();
        return Result.OK;
    }

    public synchronized Loc warp(String name) {
        return warps.get(name.toLowerCase());
    }

    public synchronized Result delWarp(String name) {
        if (warps.remove(name.toLowerCase()) == null) return Result.NOT_FOUND;
        save();
        return Result.OK;
    }

    public synchronized List<String> warpNames() {
        return new ArrayList<>(warps.keySet());
    }

    // ---- spawn
    public synchronized void setSpawn(Loc loc) {
        spawn = loc;
        save();
    }

    public synchronized Loc spawn() {
        return spawn;
    }
}
