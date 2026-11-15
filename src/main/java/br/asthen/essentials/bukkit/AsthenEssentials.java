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

    private Economy economy;
    private Places places;
    private Punishments punishments;
    private TpaManager tpa;
    private Cooldowns cooldowns;
    private Teleporter teleporter;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        Path data = getDataFolder().toPath();

        economy = new Economy(data.resolve("economy.json"));
        places = new Places(data.resolve("places.json"));
        punishments = new Punishments(data.resolve("punishments.json"), System::currentTimeMillis);
        tpa = new TpaManager(getConfig().getInt("tpa.timeout-seconds", 60), System::currentTimeMillis);
        cooldowns = new Cooldowns(System::currentTimeMillis);
        teleporter = new Teleporter(this);

        getServer().getPluginManager().registerEvents(new Events(this), this);

        Commands cmds = new Commands(this);
        for (String name : new String[]{
                "money", "pay", "baltop", "eco",
                "sethome", "home", "delhome", "homes",
                "warp", "warps", "setwarp", "delwarp",
                "spawn", "setspawn",
                "tpa", "tpaccept", "tpdeny", "back", "rtp",
                "kick", "ban", "tempban", "unban", "mute", "unmute",
                "aessentials"
        }) {
            PluginCommand c = getCommand(name);
            if (c != null) {
                c.setExecutor(cmds);
                c.setTabCompleter(cmds);
            }
        }

        getLogger().info("AsthenEssentials enabled.");
    }

    @Override
    public void onDisable() {
        if (economy != null) economy.save();
        if (places != null) places.save();
        if (punishments != null) punishments.save();
        getLogger().info("AsthenEssentials disabled.");
    }

    public void reload() {
        reloadConfig();
        tpa = new TpaManager(getConfig().getInt("tpa.timeout-seconds", 60), System::currentTimeMillis);
        getLogger().info("Config reloaded.");
    }

    public Economy economy() { return economy; }
    public Places places() { return places; }
    public Punishments punishments() { return punishments; }
    public TpaManager tpa() { return tpa; }
    public Cooldowns cooldowns() { return cooldowns; }
    public Teleporter teleporter() { return teleporter; }
}
