package br.asthen.essentials.core;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.LongSupplier;

/** One pending teleport request per target; expires on its own. */
public final class TpaManager {

    public record Request(UUID from, UUID to, long expiresAt) {
    }

    private final Map<UUID, Request> byTarget = new HashMap<>();
    private final long timeoutMillis;
    private final LongSupplier clock;

    public TpaManager(long timeoutSeconds, LongSupplier clock) {
        this.timeoutMillis = timeoutSeconds * 1000;
        this.clock = clock;
    }

    public TpaManager(long timeoutSeconds) {
        this(timeoutSeconds, System::currentTimeMillis);
    }

    /** Replaces any older request to the same target. */
    public synchronized void create(UUID from, UUID to) {
        byTarget.put(to, new Request(from, to, clock.getAsLong() + timeoutMillis));
    }

    /** Returns and removes the live request for this target, or null if none / expired. */
    public synchronized Request take(UUID target) {
        Request r = byTarget.remove(target);
        if (r == null || r.expiresAt() < clock.getAsLong()) return null;
        return r;
    }

    public synchronized void forget(UUID player) {
        byTarget.remove(player);
        byTarget.values().removeIf(r -> r.from().equals(player));
    }
}
