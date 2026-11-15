package br.asthen.essentials.core;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Persistent balances in whole cents. Transfers are atomic (all-or-nothing). */
public final class Economy {
    private final Path file;
    private final long starting;
    private final Map<UUID, Long> balances = new LinkedHashMap<>();
    private final Map<UUID, String> names = new LinkedHashMap<>();

    public Economy(Path file, long startingCents) {
        this.file = file;
        this.starting = Math.max(0, Math.min(Money.MAX, startingCents));
        Map<String, Object> root = JsonFile.load(file);
        for (Map.Entry<String, Object> e : root.entrySet()) {
            try {
                UUID id = UUID.fromString(e.getKey());
                Map<String, Object> m = JsonFile.obj(e.getValue());
                long bal = JsonFile.lng(m.get("balance"), starting);
                String name = m.get("name") instanceof String s ? s : "?";
                balances.put(id, Math.max(0, Math.min(Money.MAX, bal)));
                names.put(id, name);
            } catch (Exception ignored) {
            }
        }
    }

    public synchronized void touch(UUID id, String name) {
        if (!balances.containsKey(id)) balances.put(id, starting);
        if (name != null && !name.isBlank()) names.put(id, name);
        save();
    }

    public synchronized long get(UUID id) {
        return balances.getOrDefault(id, starting);
    }

    public synchronized String nameOf(UUID id) {
        return names.getOrDefault(id, "?");
    }

    public synchronized UUID findByName(String name) {
        if (name == null) return null;
        String lower = name.toLowerCase();
        for (Map.Entry<UUID, String> e : names.entrySet()) {
            if (e.getValue().equalsIgnoreCase(lower)) return e.getKey();
        }
        return null;
    }

    public synchronized boolean set(UUID id, long cents) {
        if (cents < 0 || cents > Money.MAX) return false;
        balances.put(id, cents);
        save();
        return true;
    }

    public synchronized boolean add(UUID id, long delta) {
        long cur = get(id);
        long next = cur + delta;
        if (next < 0 || next > Money.MAX) return false;
        balances.put(id, next);
        save();
        return true;
    }

    /** Atomic transfer: both balances update or neither does. */
    public synchronized boolean transfer(UUID from, UUID to, long amount) {
        if (amount <= 0 || from.equals(to)) return false;
        long a = get(from), b = get(to);
        if (a < amount || b + amount > Money.MAX) return false;
        balances.put(from, a - amount);
        balances.put(to, b + amount);
        save();
        return true;
    }

    public synchronized List<Map.Entry<UUID, Long>> top(int n) {
        List<Map.Entry<UUID, Long>> list = new ArrayList<>(balances.entrySet());
        list.sort(Map.Entry.<UUID, Long>comparingByValue().reversed());
        return list.subList(0, Math.min(n, list.size()));
    }

    public synchronized void save() {
        Map<String, Object> root = new LinkedHashMap<>();
        for (UUID id : balances.keySet()) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("balance", balances.get(id));
            m.put("name", names.getOrDefault(id, "?"));
            root.put(id.toString(), m);
        }
        JsonFile.save(file, root);
    }
}
