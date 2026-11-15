package br.asthen.essentials.core;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Balances in cents. Every mutation is validated and all-or-nothing; saved after each change. */
public final class Economy {

    public enum Result { OK, INVALID_AMOUNT, NO_FUNDS, OVERFLOW, SAME_ACCOUNT }

    public record Entry(UUID id, String name, long cents) {
    }

    private final Path file;
    private final long startingCents;
    private final Map<UUID, Long> balances = new HashMap<>();
    private final Map<UUID, String> names = new HashMap<>();

    public Economy(Path file, long startingCents) {
        this.file = file;
        this.startingCents = startingCents;
        load();
    }

    private void load() {
        Map<String, Object> root = JsonFile.load(file);
        for (Map.Entry<String, Object> e : root.entrySet()) {
            try {
                UUID id = UUID.fromString(e.getKey());
                Map<String, Object> a = JsonFile.obj(e.getValue());
                balances.put(id, Math.max(0, JsonFile.lng(a.get("cents"), startingCents)));
                if (a.get("name") instanceof String n) names.put(id, n);
            } catch (IllegalArgumentException ignored) {
                // skip corrupt key
            }
        }
    }

    private synchronized void save() {
        Map<String, Object> root = new LinkedHashMap<>();
        for (Map.Entry<UUID, Long> e : balances.entrySet()) {
            Map<String, Object> a = new LinkedHashMap<>();
            a.put("name", names.getOrDefault(e.getKey(), ""));
            a.put("cents", e.getValue());
            root.put(e.getKey().toString(), a);
        }
        JsonFile.save(file, root);
    }

    public synchronized void touch(UUID id, String name) {
        boolean isNew = !balances.containsKey(id);
        if (isNew) balances.put(id, startingCents);
        if (name != null && !name.equals(names.get(id))) names.put(id, name);
        if (isNew || name != null) save();
    }

    public synchronized long balance(UUID id) {
        return balances.getOrDefault(id, startingCents);
    }

    public synchronized boolean has(UUID id) {
        return balances.containsKey(id);
    }

    public synchronized UUID findByName(String name) {
        for (Map.Entry<UUID, String> e : names.entrySet()) {
            if (e.getValue().equalsIgnoreCase(name)) return e.getKey();
        }
        return null;
    }

    public synchronized String nameOf(UUID id) {
        return names.getOrDefault(id, id.toString().substring(0, 8));
    }

    public synchronized Result set(UUID id, long cents) {
        if (cents < 0 || cents > Money.MAX) return Result.INVALID_AMOUNT;
        balances.put(id, cents);
        save();
        return Result.OK;
    }

    public synchronized Result give(UUID id, long cents) {
        if (cents <= 0) return Result.INVALID_AMOUNT;
        long next = balance(id) + cents;
        if (next > Money.MAX) return Result.OVERFLOW;
        balances.put(id, next);
        save();
        return Result.OK;
    }

    public synchronized Result take(UUID id, long cents) {
        if (cents <= 0) return Result.INVALID_AMOUNT;
        long cur = balance(id);
        if (cur < cents) return Result.NO_FUNDS;
        balances.put(id, cur - cents);
        save();
        return Result.OK;
    }

    /** Atomic transfer: either both sides change or neither does. */
    public synchronized Result transfer(UUID from, UUID to, long cents) {
        if (from.equals(to)) return Result.SAME_ACCOUNT;
        if (cents <= 0) return Result.INVALID_AMOUNT;
        long a = balance(from), b = balance(to);
        if (a < cents) return Result.NO_FUNDS;
        if (b + cents > Money.MAX) return Result.OVERFLOW;
        balances.put(from, a - cents);
        balances.put(to, b + cents);
        save();
        return Result.OK;
    }

    public synchronized List<Entry> top(int n) {
        List<Entry> all = new ArrayList<>();
        for (Map.Entry<UUID, Long> e : balances.entrySet()) {
            all.add(new Entry(e.getKey(), nameOf(e.getKey()), e.getValue()));
        }
        all.sort((x, y) -> Long.compare(y.cents(), x.cents()));
        return all.subList(0, Math.min(n, all.size()));
    }
}
