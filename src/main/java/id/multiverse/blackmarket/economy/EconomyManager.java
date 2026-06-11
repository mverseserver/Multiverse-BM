package id.multiverse.blackmarket.economy;

import id.multiverse.blackmarket.MultiverseBlackmarket;
import net.milkbowl.vault.economy.Economy;
import org.black_ixx.playerpoints.PlayerPoints;
import org.black_ixx.playerpoints.PlayerPointsAPI;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;

public class EconomyManager {

    public enum CurrencyType { VAULT, PLAYERPOINTS }

    private final MultiverseBlackmarket plugin;

    private Economy vaultEconomy;
    private PlayerPointsAPI playerPointsAPI;

    private boolean vaultAvailable = false;
    private boolean playerPointsAvailable = false;

    public EconomyManager(MultiverseBlackmarket plugin) {
        this.plugin = plugin;
    }

    public void setup() {
        // Vault setup
        if (plugin.getServer().getPluginManager().getPlugin("Vault") != null) {
            RegisteredServiceProvider<Economy> rsp =
                    plugin.getServer().getServicesManager().getRegistration(Economy.class);
            if (rsp != null) {
                vaultEconomy = rsp.getProvider();
                vaultAvailable = true;
                plugin.getLogger().info("Vault economy hooked: " + vaultEconomy.getName());
            }
        }
        if (!vaultAvailable) {
            plugin.getLogger().warning("Vault not found - VAULT currency will not work.");
        }

        // PlayerPoints setup
        if (plugin.getServer().getPluginManager().getPlugin("PlayerPoints") != null) {
            PlayerPoints pp = (PlayerPoints) plugin.getServer().getPluginManager().getPlugin("PlayerPoints");
            if (pp != null) {
                playerPointsAPI = pp.getAPI();
                playerPointsAvailable = true;
                plugin.getLogger().info("PlayerPoints hooked successfully.");
            }
        }
        if (!playerPointsAvailable) {
            plugin.getLogger().info("PlayerPoints not found - PLAYERPOINTS currency will not work.");
        }
    }

    /** Check if player has enough balance for given currency */
    public boolean hasBalance(Player player, CurrencyType type, double amount) {
        if (type == CurrencyType.VAULT) {
            if (!vaultAvailable) return false;
            return vaultEconomy.has(player, amount);
        } else {
            if (!playerPointsAvailable) return false;
            return playerPointsAPI.look(player.getUniqueId()) >= (int) amount;
        }
    }

    /** Get player balance */
    public double getBalance(Player player, CurrencyType type) {
        if (type == CurrencyType.VAULT) {
            if (!vaultAvailable) return 0;
            return vaultEconomy.getBalance(player);
        } else {
            if (!playerPointsAvailable) return 0;
            return playerPointsAPI.look(player.getUniqueId());
        }
    }

    /** Withdraw from player */
    public boolean withdraw(Player player, CurrencyType type, double amount) {
        if (type == CurrencyType.VAULT) {
            if (!vaultAvailable) return false;
            return vaultEconomy.withdrawPlayer(player, amount).transactionSuccess();
        } else {
            if (!playerPointsAvailable) return false;
            return playerPointsAPI.take(player.getUniqueId(), (int) amount);
        }
    }

    /** Format currency for display */
    public String formatAmount(CurrencyType type, double amount) {
        if (type == CurrencyType.VAULT && vaultAvailable) {
            return vaultEconomy.format(amount);
        } else if (type == CurrencyType.PLAYERPOINTS) {
            return (int) amount + " Poin";
        }
        return String.valueOf((long) amount);
    }

    public boolean isVaultAvailable() { return vaultAvailable; }
    public boolean isPlayerPointsAvailable() { return playerPointsAvailable; }

    public CurrencyType parseCurrency(String s) {
        if (s == null) return CurrencyType.VAULT;
        return s.equalsIgnoreCase("PLAYERPOINTS") ? CurrencyType.PLAYERPOINTS : CurrencyType.VAULT;
    }

    public String getStatusLine() {
        String v = vaultAvailable ? "Vault✓" : "Vault✗";
        String p = playerPointsAvailable ? " PP✓" : " PP✗";
        return v + p;
    }
}
