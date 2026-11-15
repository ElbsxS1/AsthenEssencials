package br.asthen.essentials.core;

import java.util.LinkedHashMap;
import java.util.Map;

/** A world position without any Bukkit type, so the stores stay testable. */
public record Loc(String world, double x, double y, double z, float yaw, float pitch) {

    Map<String, Object> toMap() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("world", world);
        m.put("x", x);
        m.put("y", y);
        m.put("z", z);
        m.put("yaw", (double) yaw);
        m.put("pitch", (double) pitch);
        return m;
    }

    static Loc fromMap(Map<String, Object> m) {
        if (!(m.get("world") instanceof String w) || w.isBlank()) return null;
        double x = JsonFile.num(m.get("x"), Double.NaN), y = JsonFile.num(m.get("y"), Double.NaN),
                z = JsonFile.num(m.get("z"), Double.NaN);
        if (Double.isNaN(x) || Double.isNaN(y) || Double.isNaN(z)) return null;
        return new Loc(w, x, y, z, (float) JsonFile.num(m.get("yaw"), 0), (float) JsonFile.num(m.get("pitch"), 0));
    }
}
