package id.multiverse.blackmarket.config;

import id.multiverse.blackmarket.MultiverseBlackmarket;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;

public class ConfigManager {

    private final MultiverseBlackmarket plugin;

    private FileConfiguration mainConfig;
    private FileConfiguration marketConfig;
    private FileConfiguration messagesConfig;

    public ConfigManager(MultiverseBlackmarket plugin) {
        this.plugin = plugin;
    }

    public void loadAll() {
        plugin.reloadConfig();
        mainConfig = plugin.getConfig();
        marketConfig = loadFile("market.yml");
        messagesConfig = loadFile("messages.yml");
    }

    private FileConfiguration loadFile(String name) {
        File f = new File(plugin.getDataFolder(), name);
        if (!f.exists()) plugin.saveResource(name, false);
        return YamlConfiguration.loadConfiguration(f);
    }

    public void reloadAll() {
        loadAll();
    }

    public FileConfiguration getMain() { return mainConfig; }
    public FileConfiguration getMarket() { return marketConfig; }
    public FileConfiguration getMessages() { return messagesConfig; }

    // ── Shortcut getters ──────────────────────────────
    public int getRestockIntervalMinutes() {
        return mainConfig.getInt("settings.restock-interval-minutes", 60);
    }

    public int getOffersPerRestock() {
        return mainConfig.getInt("settings.offers-per-restock", 5);
    }

    public boolean isAnnounceRestocks() {
        return mainConfig.getBoolean("settings.announce-restocks", true);
    }

    public boolean isCommandOpensGui() {
        return mainConfig.getBoolean("settings.command-opens-gui", true);
    }

    public boolean isDynamicPricingEnabled() {
        return mainConfig.getBoolean("settings.dynamic-pricing.enabled", true);
    }

    public int getDynamicPricingVariancePercent() {
        return mainConfig.getInt("settings.dynamic-pricing.variance-percent", 30);
    }

    public long getPurchaseCooldownMillis() {
        return mainConfig.getLong("settings.purchase-cooldown-millis", 500);
    }

    public String getDefaultCurrency() {
        return mainConfig.getString("economy.default-currency", "VAULT");
    }

    public String getTimeFormat(String key) {
        return mainConfig.getString("settings.time-format." + key, key.substring(0, 1));
    }

    public String getMessage(String key) {
        String prefix = messagesConfig.getString("prefix", "⚫ MultiverseBM »");
        String msg = messagesConfig.getString("messages." + key, "&cMessage not found: " + key);
        return msg.replace("{prefix}", prefix);
    }

    public String getGuiTitle(String key) {
        return messagesConfig.getString("gui-titles." + key, "⚫ Black Market");
    }
}
