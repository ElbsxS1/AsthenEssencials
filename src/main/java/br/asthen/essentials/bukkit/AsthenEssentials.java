package br.asthen.essentials.bukkit;

import br.asthen.essentials.core.Cooldowns;
import br.asthen.essentials.core.Economy;
import br.asthen.essentials.core.Money;
import br.asthen.essentials.core.Places;
import br.asthen.essentials.core.Punishments;
import br.asthen.essentials.core.TpaManager;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

import java.nio.file.Path;

public final class AsthenEssentials extends JavaPlugin {

    private static final String[] COMMANDS = {"money", "pay", "baltop", "eco", "sethome", "home", "delhome",
            "homes", "setwarp", "delwarp", "warp", "warps", "setspawn", "spawn", "tpa", "tpaccept", "tpdeny",
            "back", "rtp", "kick", "ban", "tempban", "unban", "mute", "unmute", "aessentials"};

    private Economy economy;
    private Places places;
    private Punishments punishments;
    private TpaManager tpa;
    private final Cooldowns cooldowns = new Cooldowns();
    private Teleporter teleporter;
    private String symbol = "R$ ";

    @Override
    public void onEnable() {
        saveDefaultConfig();
        Path data = getDataFolder().toPath();
        applyConfig();
        long start = Money.parse(String.valueOf(getConfig().get("economy.starting-balance", "100")));
        economy = new Economy(data.resolve("economy.json"), start < 0 ? 10000 : start);
        places = new Places(data.resolve("places.json"));
        punishments = new Punishments(data.resolve("punishments.json"));
        tpa = new TpaManager(Math.max(5, getConfig().getInt("teleport.tpa-timeout-seconds", 60)));
        teleporter = new Teleporter(this, cooldowns);

        Commands handler = new Commands(this);
        for (String name : COMMANDS) {
            PluginCommand c = getCommand(name);
            if (c != null) {
                c.setExecutor(handler);
                c.setTabCompleter(handler);
            }
        }
        getServer().getPluginManager().registerEvents(new Events(this), this);
        getLogger().info("AsthenEssentials ativo.");
    }

    void applyConfig() {
        symbol = getConfig().getString("economy.currency-symbol", "R$ ");
    }

    void reload() {
        reloadConfig();
        applyConfig();
    }

    Economy economy() {
        return economy;
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

    String money(long cents) {
        return Money.format(cents, symbol);
    }
}
