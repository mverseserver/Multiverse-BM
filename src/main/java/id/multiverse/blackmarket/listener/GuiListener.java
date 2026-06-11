package id.multiverse.blackmarket.listener;

import id.multiverse.blackmarket.MultiverseBlackmarket;
import id.multiverse.blackmarket.gui.MarketGuiManager;
import id.multiverse.blackmarket.market.MarketManager.PurchaseResult;
import id.multiverse.blackmarket.util.TextUtil;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

public class GuiListener implements Listener {

    private final MultiverseBlackmarket plugin;

    public GuiListener(MultiverseBlackmarket plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        // Detect if our GUI
        String title = TextUtil.parse(plugin.getConfigManager().getGuiTitle("main-market"));
        if (!event.getView().getTitle().equals(title)) return;

        event.setCancelled(true);

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null) return;

        ItemMeta meta = clicked.getItemMeta();
        if (meta == null || meta.getLore() == null) return;

        List<String> lore = meta.getLore();

        // Check for hidden marker in last lore line
        if (lore.isEmpty()) return;
        String marker = lore.get(lore.size() - 1);

        // Close button
        if (marker.equals(MarketGuiManager.GUI_CLOSE_KEY)) {
            player.closeInventory();
            return;
        }

        // Info item - do nothing
        if (marker.equals(MarketGuiManager.GUI_INFO_KEY)) return;

        // Offer item
        if (marker.startsWith(MarketGuiManager.GUI_OFFER_PREFIX)) {
            String indexStr = marker.substring(MarketGuiManager.GUI_OFFER_PREFIX.length());
            try {
                int index = Integer.parseInt(indexStr);
                handlePurchase(player, index);
            } catch (NumberFormatException ignored) {}
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        String title = TextUtil.parse(plugin.getConfigManager().getGuiTitle("main-market"));
        if (event.getView().getTitle().equals(title)) {
            event.setCancelled(true);
        }
    }

    private void handlePurchase(Player player, int index) {
        PurchaseResult result = plugin.getMarketManager().purchase(player, index);
        String msg;
        switch (result) {
            case SUCCESS -> {
                msg = buildMsg("market-bought", player, index);
                plugin.getGuiManager().refreshInventory(player);
                player.playSound(player.getLocation(),
                        org.bukkit.Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1.2f);
            }
            case SOLD_OUT -> {
                msg = plugin.getConfigManager().getMessage("market-sold-out");
                player.playSound(player.getLocation(),
                        org.bukkit.Sound.ENTITY_VILLAGER_NO, 1f, 1f);
            }
            case NO_MONEY -> {
                msg = buildNoMoneyMsg(player, index);
                player.playSound(player.getLocation(),
                        org.bukkit.Sound.ENTITY_VILLAGER_NO, 1f, 0.8f);
            }
            case INVENTORY_FULL -> {
                msg = plugin.getConfigManager().getMessage("market-inventory-full");
                player.playSound(player.getLocation(),
                        org.bukkit.Sound.BLOCK_CHEST_LOCKED, 1f, 1f);
            }
            case ECONOMY_ERROR -> msg = plugin.getConfigManager().getMessage("market-purchase-failed");
            default -> {
                msg = plugin.getConfigManager().getMessage("market-cooldown");
            }
        }
        player.sendMessage(TextUtil.parse(msg));
    }

    private String buildMsg(String key, Player player, int index) {
        var offers = plugin.getMarketManager().getActiveOffers();
        if (index < 0 || index >= offers.size()) return plugin.getConfigManager().getMessage(key);
        var offer = offers.get(index);
        return plugin.getConfigManager().getMessage(key)
                .replace("{reward}", offer.getDefinition().getName())
                .replace("{stock}", String.valueOf(offer.getRemainingStock()));
    }

    private String buildNoMoneyMsg(Player player, int index) {
        var offers = plugin.getMarketManager().getActiveOffers();
        if (index < 0 || index >= offers.size())
            return plugin.getConfigManager().getMessage("market-not-enough-money");

        var offer = offers.get(index);
        var economy = plugin.getEconomyManager();
        String price = economy.formatAmount(offer.getCurrency(), offer.getCurrentPrice());
        String balance = economy.formatAmount(offer.getCurrency(), economy.getBalance(player, offer.getCurrency()));

        String key = offer.getCurrency() == id.multiverse.blackmarket.economy.EconomyManager.CurrencyType.PLAYERPOINTS
                ? "market-not-enough-points"
                : "market-not-enough-money";

        return plugin.getConfigManager().getMessage(key)
                .replace("{price}", price)
                .replace("{balance}", balance);
    }
}
