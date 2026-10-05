package net.joseplay.plugin;

import net.joseplay.core.Core;
import org.bukkit.plugin.java.JavaPlugin;

public final class Plugin extends JavaPlugin {
    private Core core;

    @Override
    public void onEnable() {
        core = new Core(this);

        core.onEnable();
        // Plugin startup logic
    }

    @Override
    public void onDisable() {
        if (core != null){
            core.onDisable();
        }
    }
}
