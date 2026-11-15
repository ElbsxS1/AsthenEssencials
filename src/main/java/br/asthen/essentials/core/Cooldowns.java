package br.asthen.essentials.core;

import java.util.HashMap;
import java.util.Map;
import java.util.function.LongSupplier;

/** Per-key cooldowns with an injectable clock (tests never sleep). */
public final class Cooldowns {
    private final Map<String, Long> until = new HashMap<>();
    private final LongSupplier clock;

    public Cooldowns() {
        this(System::currentTimeMillis);
    }

    public Cooldowns(LongSupplier clock) {
        this.clock = clock;
    }

    /** Seconds left (rounded up), 0 when free. */
    public synchronized long remaining(String key) {
        Long u = until.get(key);
        if (u == null) return 0;
        long left = u - clock.getAsLong();
        if (left <= 0) {
            until.remove(key);
            return 0;
        }
        return (left + 999) / 1000;
    }

    public synchronized void start(String key, long seconds) {
        if (seconds > 0) until.put(key, clock.getAsLong() + seconds * 1000);
    }
}
