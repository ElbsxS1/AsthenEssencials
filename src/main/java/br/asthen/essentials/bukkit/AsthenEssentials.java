package br.asthen.essentials.bukkit;

import br.asthen.essentials.core.Cooldowns;
import br.asthen.essentials.core.Pickups;
import br.asthen.essentials.core.Places;
import br.asthen.essentials.core.Punishments;
import br.asthen.essentials.core.TpaManager;
import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.nio.file.Path;

public final class AsthenEssentials extends JavaPlugin {

    private static final String[] COMMANDS = {"menu", "bussola", "adminitem", "painel", "sethome", "home", "delhome", "homes",
            "tpa", "tpaccept", "tpdeny", "kick", "ban", "tempban", "unban",
            "mute", "unmute", "aessentials"};

    private Places places;
    private Pickups pickups;
    private Punishments punishments;
    private TpaManager tpa;
    private final Cooldowns cooldowns = new Cooldowns();
    private Teleporter teleporter;
    private MenuService menus;
    private MenuItem menuItem;
    private AdminItem adminItem;
    private AdminMenu adminMenu;
    private Prompts prompts;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        Path data = getDataFolder().toPath();
        places = new Places(data.resolve("places.json"));
        pickups = new Pickups(data.resolve("pickups.json"));
        punishments = new Punishments(data.resolve("punishments.json"));
        tpa = new TpaManager(Math.max(5, getConfig().getInt("teleport.tpa-timeout-seconds", 60)));
        teleporter = new Teleporter(this, cooldowns);
        menus = new MenuService(this);
        menuItem = new MenuItem(this);
        adminItem = new AdminItem(this);
        adminMenu = new AdminMenu(this);
        prompts = new Prompts(this);

        Commands handler = new Commands(this);
        for (String name : COMMANDS) {
            PluginCommand c = getCommand(name);
            if (c != null) {
                c.setExecutor(handler);
                c.setTabCompleter(handler);
            }
        }
        // Prompts first: it must see a chat line before the mute check does
        getServer().getPluginManager().registerEvents(prompts, this);
        getServer().getPluginManager().registerEvents(new Events(this), this);
        getServer().getPluginManager().registerEvents(new MenuListener(this), this);
        getServer().getPluginManager().registerEvents(new MenuItemListener(this, menuItem), this);
        getServer().getPluginManager().registerEvents(new AdminItemListener(this, adminItem), this);
        boolean floodgate = Bukkit.getPluginManager().getPlugin("floodgate") != null;
        getLogger().info("AsthenEssentials ativo. Menu Bedrock (Floodgate): " + (floodgate ? "sim" : "não, usando baú para todos"));
    }

    void reload() {
        reloadConfig();
    }

    Pickups pickups() {
        return pickups;
    }

    /** Menu, homes and /tpa only work in these worlds (config worlds.allowed, default: survival). */
    boolean inAllowedWorld(Player p) {
        for (String w : getConfig().getStringList("worlds.allowed")) {
            if (w.equalsIgnoreCase(p.getWorld().getName())) return true;
        }
        return false;
    }

    /** Like inAllowedWorld, but tells the player why not. */
    boolean requireAllowedWorld(Player p) {
        if (inAllowedWorld(p)) return true;
        Msg.err(p, "Isso só funciona na sobrevivência.");
        return false;
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

    AdminItem adminItem() {
        return adminItem;
    }

    AdminMenu adminMenu() {
        return adminMenu;
    }

    Prompts prompts() {
        return prompts;
    }
}
