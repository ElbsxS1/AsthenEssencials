package br.asthen.essentials.core;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.LongSupplier;

/** Bans and mutes keyed by UUID, permanent (expires = 0) or temporary. Saved after each change. */
public final class Punishments {

    public enum Kind { BAN, MUTE }

    public record Entry(UUID id, String name, String reason, String by, long createdAt, long expiresAt) {
        public boolean permanent() {
            return expiresAt == 0;
        }
    }

    private final Path file;
    private final LongSupplier clock;
    private final Map<UUID, Entry> bans = new LinkedHashMap<>();
    private final Map<UUID, Entry> mutes = new LinkedHashMap<>();

    public Punishments(Path file) {
        this(file, System::currentTimeMillis);
    }

    public Punishments(Path file, LongSupplier clock) {
        this.file = file;
        this.clock = clock;
        Map<String, Object> root = JsonFile.load(file);
        read(root.get("bans"), bans);
        read(root.get("mutes"), mutes);
    }

    private void read(Object o, Map<UUID, Entry> into) {
        for (Map.Entry<String, Object> e : JsonFile.obj(o).entrySet()) {
            try {
                Map<String, Object> m = JsonFile.obj(e.getValue());
                UUID id = UUID.fromString(e.getKey());
                into.put(id, new Entry(id, str(m.get("name")), str(m.get("reason")), str(m.get("by")),
                        JsonFile.lng(m.get("created"), 0), JsonFile.lng(m.get("expires"), 0)));
            } catch (IllegalArgumentException ignored) {
            }
        }
    }

    private static String str(Object o) {
        return o instanceof String s ? s : "";
    }

    private synchronized void save() {
        Map<String, Object> root = new LinkedHashMap<>();
        root.put("bans", write(bans));
        root.put("mutes", write(mutes));
        JsonFile.save(file, root);
    }

    private static Map<String, Object> write(Map<UUID, Entry> src) {
        Map<String, Object> out = new LinkedHashMap<>();
        for (Entry e : src.values()) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("name", e.name());
            m.put("reason", e.reason());
            m.put("by", e.by());
            m.put("created", e.createdAt());
            m.put("expires", e.expiresAt());
            out.put(e.id().toString(), m);
        }
        return out;
    }

    private Map<UUID, Entry> map(Kind k) {
        return k == Kind.BAN ? bans : mutes;
    }

    /** durationMillis <= 0 means permanent. */
    public synchronized Entry add(Kind k, UUID id, String name, String reason, String by, long durationMillis) {
        long now = clock.getAsLong();
        Entry e = new Entry(id, name, reason == null || reason.isBlank() ? "Sem motivo" : reason, by, now,
                durationMillis > 0 ? now + durationMillis : 0);
        map(k).put(id, e);
        save();
        return e;
    }

    /** Active entry or null; an expired one is removed on the spot. */
    public synchronized Entry active(Kind k, UUID id) {
        Entry e = map(k).get(id);
        if (e == null) return null;
        if (!e.permanent() && e.expiresAt() <= clock.getAsLong()) {
            map(k).remove(id);
            save();
            return null;
        }
        return e;
    }

    public synchronized boolean remove(Kind k, UUID id) {
        boolean r = map(k).remove(id) != null;
        if (r) save();
        return r;
    }

    public synchronized UUID findByName(Kind k, String name) {
        for (Entry e : map(k).values()) {
            if (e.name().equalsIgnoreCase(name)) return e.id();
        }
        return null;
    }

    /** "30m", "2h", "7d", "1w", "45s". Returns -1 when invalid; max 365 days. */
    public static long parseDuration(String s) {
        if (s == null || !s.matches("\\d{1,6}[smhdw]")) return -1;
        long n = Long.parseLong(s.substring(0, s.length() - 1));
        long unit = switch (s.charAt(s.length() - 1)) {
            case 's' -> 1_000L;
            case 'm' -> 60_000L;
            case 'h' -> 3_600_000L;
            case 'd' -> 86_400_000L;
            default -> 7 * 86_400_000L;
        };
        long total = n * unit;
        return n <= 0 || total > 365L * 86_400_000L ? -1 : total;
    }

    public static String formatDuration(long millis) {
        long s = Math.max(1, millis / 1000);
        if (s >= 86400) return s / 86400 + "d " + (s % 86400) / 3600 + "h";
        if (s >= 3600) return s / 3600 + "h " + (s % 3600) / 60 + "min";
        if (s >= 60) return s / 60 + "min";
        return s + "s";
    }
}
