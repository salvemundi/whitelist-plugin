package nl.salvemundi.plugins;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Objects;
import java.util.logging.Logger;

public final class Whitelist extends JavaPlugin {
    FileConfiguration config = this.getConfig();

    @Override
    public void onEnable() {
        config.addDefault("salvemundi_url", "http://localhost");
        config.addDefault("salvemundi_endpoint", "/api/whitelist");
        config.addDefault("salvemundi_api_client_id",69);
        config.addDefault("salvemundi_api_client_secret","secret");
        config.options().copyDefaults(true);
        saveConfig();
        Objects.requireNonNull(getServer().getPluginCommand("get-whitelist-samu")).setExecutor(new WhitelistCommandExecutor(config, getLogger()));
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }
}
