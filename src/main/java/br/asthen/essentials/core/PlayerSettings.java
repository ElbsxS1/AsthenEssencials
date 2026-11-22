package br.asthen.essentials.core;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/** Per-player preferences. Only values that differ from the default are stored. */
public final class PlayerSettings {
    private final Path file;
    /** Players who turned the ACEITAR/NEGAR window off (default: on). */
    private final Map<UUID, Boolean> tpaWindowOff = new LinkedHashMap<>();

    public PlayerSettings(Path file) {
        this.file = file;
        Map<String, Object> root = JsonFile.load(file);
        for (String key : JsonFile.obj(root.get("tpaWindowOff")).keySet()) {
            try {
                tpaWindowOff.put(UUID.fromString(key), Boolean.TRUE);
            } catch (IllegalArgumentException ignored) {
            }
        }
    }

    /** Whether a /tpa request opens the ACEITAR/NEGAR menu for this player. */
    public synchronized boolean tpaWindow(UUID id) {
        return !tpaWindowOff.containsKey(id);
    }

    public synchronized void setTpaWindow(UUID id, boolean on) {
        boolean changed = on ? tpaWindowOff.remove(id) != null : tpaWindowOff.put(id, Boolean.TRUE) == null;
        if (changed) save();
    }

    private void save() {
        Map<String, Object> off = new LinkedHashMap<>();
        tpaWindowOff.keySet().forEach(id -> off.put(id.toString(), Boolean.TRUE));
        Map<String, Object> root = new LinkedHashMap<>();
        root.put("tpaWindowOff", off);
        JsonFile.save(file, root);
    }
}
