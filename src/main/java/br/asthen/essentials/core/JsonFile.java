package br.asthen.essentials.core;

import br.asthen.essentials.util.Atomic;
import br.asthen.essentials.util.Json;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

/** Tiny helper: one JSON object per file, written atomically. A corrupt file is kept as .bad, never lost. */
final class JsonFile {
    private JsonFile() {
    }

    static Map<String, Object> load(Path file) {
        if (!Files.exists(file)) return new LinkedHashMap<>();
        try {
            return Json.parseObject(Files.readString(file, StandardCharsets.UTF_8));
        } catch (Exception e) {
            try {
                Files.move(file, file.resolveSibling(file.getFileName() + ".bad"),
                        java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException ignored) {
            }
            return new LinkedHashMap<>();
        }
    }

    static void save(Path file, Map<String, Object> data) {
        try {
            Files.createDirectories(file.getParent());
            Atomic.write(file, Json.pretty(data));
        } catch (IOException e) {
            throw new IllegalStateException("cannot save " + file + ": " + e.getMessage(), e);
        }
    }

    @SuppressWarnings("unchecked")
    static Map<String, Object> obj(Object o) {
        return o instanceof Map<?, ?> m ? (Map<String, Object>) m : new LinkedHashMap<>();
    }

    static double num(Object o, double def) {
        return o instanceof Number n ? n.doubleValue() : def;
    }

    static long lng(Object o, long def) {
        return o instanceof Number n ? n.longValue() : def;
    }
}
