package br.asthen.essentials.bukkit;

import org.bukkit.Material;

import java.util.List;

/** Platform-neutral menu: the same model is drawn as a chest (Java) or a form (Bedrock). */
record Menu(String title, String content, List<Entry> entries) {

    record Entry(String label, Material icon, String description, Runnable action) {
    }
}
