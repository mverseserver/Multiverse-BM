package id.multiverse.blackmarket;

import id.multiverse.blackmarket.command.BlackmarketAdminCommand;
import id.multiverse.blackmarket.command.BlackmarketCommand;
import id.multiverse.blackmarket.config.ConfigManager;
import id.multiverse.blackmarket.economy.EconomyManager;
import id.multiverse.blackmarket.gui.MarketGuiManager;
import id.multiverse.blackmarket.listener.GuiListener;
import id.multiverse.blackmarket.market.MarketManager;
import id.multiverse.blackmarket.placeholder.BlackmarketPlaceholder;
import org.bukkit.plugin.java.JavaPlugin;

public class MultiverseBlackmarket extends JavaPlugin {

    private static MultiverseBlackmarket instance;

    private ConfigManager configManager;
    private EconomyManager economyManager;
    private MarketManager marketManager;
    private MarketGuiManager guiManager;

    @Override
    public void onEnable() {
        instance = this;

        // Save default configs
        saveDefaultConfig();
        saveResource("market.yml", false);
        saveResource("messages.yml", false);

        // Init managers
        configManager = new ConfigManager(this);
        configManager.loadAll();

        economyManager = new EconomyManager(this);
        economyManager.setup();

        marketManager = new MarketManager(this);
        marketManager.initialize();

        guiManager = new MarketGuiManager(this);

        // Register listeners
        getServer().getPluginManager().registerEvents(new GuiListener(this), this);

        // Register commands
        getCommand("blackmarket").setExecutor(new BlackmarketCommand(this));
        getCommand("blackmarket").setTabCompleter(new BlackmarketCommand(this));
        getCommand("blackmarketadmin").setExecutor(new BlackmarketAdminCommand(this));
        getCommand("blackmarketadmin").setTabCompleter(new BlackmarketAdminCommand(this));

        // PlaceholderAPI
        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new BlackmarketPlaceholder(this).register();
            getLogger().info("PlaceholderAPI hook registered.");
        }

        getLogger().info("╔═══════════════════════════════════╗");
        getLogger().info("║  Multiverse Blackmarket v" + getDescription().getVersion() + "     ║");
        getLogger().info("║  Economy: " + economyManager.getStatusLine() + "  ║");
        getLogger().info("╚═══════════════════════════════════╝");
    }

    @Override
    public void onDisable() {
        if (marketManager != null) marketManager.shutdown();
        getLogger().info("Multiverse Blackmarket disabled.");
    }

    public static MultiverseBlackmarket getInstance() { return instance; }
    public ConfigManager getConfigManager() { return configManager; }
    public EconomyManager getEconomyManager() { return economyManager; }
    public MarketManager getMarketManager() { return marketManager; }
    public MarketGuiManager getGuiManager() { return guiManager; }
}
