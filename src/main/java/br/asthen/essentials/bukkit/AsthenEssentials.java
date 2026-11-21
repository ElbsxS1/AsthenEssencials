package br.asthen.essentials.bukkit;

import br.asthen.essentials.core.Cooldowns;
import br.asthen.essentials.core.Places;
import br.asthen.essentials.core.Punishments;
import br.asthen.essentials.core.TpaManager;
import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

import java.nio.file.Path;

public final class AsthenEssentials extends JavaPlugin {

    private static final String[] COMMANDS = {"menu", "bussola", "sethome", "home", "delhome", "homes", "setwarp", "delwarp",
            "warp", "warps", "setspawn", "spawn", "tpa", "tpaccept", "tpdeny", "kick", "ban", "tempban", "unban",
            "mute", "unmute", "aessentials"};

    private Places places;
    private Punishments punishments;
    private TpaManager tpa;
    private final Cooldowns cooldowns = new Cooldowns();
    private Teleporter teleporter;
    private MenuService menus;
    private MenuItem menuItem;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        Path data = getDataFolder().toPath();
        places = new Places(data.resolve("places.json"));
        punishments = new Punishments(data.resolve("punishments.json"));
        tpa = new TpaManager(Math.max(5, getConfig().getInt("teleport.tpa-timeout-seconds", 60)));
        teleporter = new Teleporter(this, cooldowns);
        menus = new MenuService(this);
        menuItem = new MenuItem(this);

        Commands handler = new Commands(this);
        for (String name : COMMANDS) {
            PluginCommand c = getCommand(name);
            if (c != null) {
                c.setExecutor(handler);
                c.setTabCompleter(handler);
            }
        }
        getServer().getPluginManager().registerEvents(new Events(this), this);
        getServer().getPluginManager().registerEvents(new MenuListener(this), this);
        getServer().getPluginManager().registerEvents(new MenuItemListener(this, menuItem), this);
        boolean floodgate = Bukkit.getPluginManager().getPlugin("floodgate") != null;
        getLogger().info("AsthenEssentials ativo. Menu Bedrock (Floodgate): " + (floodgate ? "sim" : "não, usando baú para todos"));
    }

    void reload() {
        reloadConfig();
    }

    Places places() {
        return places;
    }

    Punishments punishments() {
        return punishments;
    }

    TpaManager tpa() {
        return tpa;
    }

    Teleporter teleporter() {
        return teleporter;
    }

    Cooldowns cooldowns() {
        return cooldowns;
    }

    MenuItem menuItem() {
        return menuItem;
    }

    MenuService menus() {
        return menus;
    }
}
