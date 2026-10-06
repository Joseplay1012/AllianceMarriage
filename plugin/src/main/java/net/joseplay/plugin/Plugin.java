package net.joseplay.plugin;

import net.joseplay.core.Core;
import net.joseplay.plugin.commands.MarriageCommands;
import net.joseplay.plugin.listeners.MarriageListeners;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public final class Plugin extends JavaPlugin {
    private Core core;

    @Override
    public void onEnable() {
        getLogger().info("Starting plugin...");

        core = new Core(this);
        core.onEnable();

        getLogger().info("Registering commands...");

        if (getCommand("marriage") == null) {
            getLogger().severe("Failed to register /marriage command. Check plugin.yml.");
            return;
        }

        getCommand("marriage").setExecutor(new MarriageCommands());
        getLogger().info("Registered command: /marriage");

        getLogger().info("Registering listeners...");

        Bukkit.getPluginManager().registerEvents(new MarriageListeners(), this);

        getLogger().info("Registered listener: MarriageListeners");
        getLogger().info("Plugin enabled successfully.");
    }

    @Override
    public void onDisable() {
        getLogger().info("Disabling plugin...");

        if (core != null) {
            core.onDisable();
        }

        getLogger().info("Plugin disabled successfully.");
    }
}
