package id.multiverse.blackmarket.gui;

import id.multiverse.blackmarket.MultiverseBlackmarket;
import id.multiverse.blackmarket.config.ConfigManager;
import id.multiverse.blackmarket.economy.EconomyManager;
import id.multiverse.blackmarket.market.ActiveOffer;
import id.multiverse.blackmarket.market.MarketManager;
import id.multiverse.blackmarket.market.RarityDefinition;
import id.multiverse.blackmarket.market.RewardDefinition;
import id.multiverse.blackmarket.util.TextUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class MarketGuiManager {

    private final MultiverseBlackmarket plugin;
    private final ConfigManager cfg;
    private final MarketManager market;
    private final EconomyManager economy;

    // Metadata keys stored in item display names to identify slots
    public static final String GUI_OFFER_PREFIX = "\u00A70\u00A70mbm_offer_";
    public static final String GUI_CLOSE_KEY = "\u00A70\u00A70mbm_close";
    public static final String GUI_INFO_KEY = "\u00A70\u00A70mbm_info";

    public MarketGuiManager(MultiverseBlackmarket plugin) {
        this.plugin = plugin;
        this.cfg = plugin.getConfigManager();
        this.market = plugin.getMarketManager();
        this.economy = plugin.getEconomyManager();
    }

    public void openMarket(Player player) {
        Inventory inv = buildInventory(player);
        player.openInventory(inv);
    }

    private Inventory buildInventory(Player player) {
        FileConfiguration marketYml = cfg.getMarket();

        int size = marketYml.getInt("gui.size", 54);
        int infoSlot = marketYml.getInt("gui.info-slot", 4);
        int closeSlot = marketYml.getInt("gui.close-slot", 49);
        List<Integer> offerSlots = getSlotList(marketYml, "gui.offer-slots",
                List.of(10,11,12,13,14,15,16,19,20,21,22,23,24,25,28,29,30,31,32,33,34));
        List<Integer> raritySlots = getSlotList(marketYml, "gui.rarity-filter-slots",
                List.of(46,47,48,50,51,52));

        String title = TextUtil.parse(cfg.getGuiTitle("main-market"));
        Inventory inv = Bukkit.createInventory(null, size, title);

        // Fill with black glass
        String fillerMat = marketYml.getString("gui.filler.material", "BLACK_STAINED_GLASS_PANE");
        ItemStack filler = buildFiller(fillerMat);
        for (int i = 0; i < size; i++) inv.setItem(i, filler);

        // Info/timer item
        inv.setItem(infoSlot, buildInfoItem());

        // Close button
        inv.setItem(closeSlot, buildCloseItem());

        // Rarity filter items
        List<String> rarityKeys = new ArrayList<>(market.getRarities().keySet());
        for (int i = 0; i < raritySlots.size() && i < rarityKeys.size(); i++) {
            inv.setItem(raritySlots.get(i), buildRarityFilterItem(rarityKeys.get(i)));
        }

        // Offers
        List<ActiveOffer> offers = market.getActiveOffers();
        for (int i = 0; i < offerSlots.size(); i++) {
            if (i >= offers.size()) break;
            ActiveOffer offer = offers.get(i);
            inv.setItem(offerSlots.get(i), buildOfferItem(offer, i, player));
        }

        return inv;
    }

    private ItemStack buildOfferItem(ActiveOffer offer, int index, Player player) {
        RewardDefinition def = offer.getDefinition();
        RarityDefinition rarity = market.getRarity(def.getRarity());
        if (rarity == null) rarity = new RarityDefinition("common",
                cfg.getMarket().createSection("_temp"));

        String priceFormatted = economy.formatAmount(offer.getCurrency(), offer.getCurrentPrice());

        ItemStack item = def.buildIconItem(
                offer.getCurrentPrice(),
                priceFormatted,
                offer.getRemainingStock(),
                rarity
        );

        // Encode offer index in a hidden lore line for click detection
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            List<String> lore = meta.getLore() != null ? new ArrayList<>(meta.getLore()) : new ArrayList<>();
            lore.add(GUI_OFFER_PREFIX + index); // hidden marker
            meta.setLore(lore);
            item.setItemMeta(meta);
        }

        return item;
    }

    private ItemStack buildInfoItem() {
        ItemStack item = new ItemStack(Material.CLOCK);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;
        meta.setDisplayName(TextUtil.parse("<gradient:#1a1a1a:#444444>⚫ Black Market Info</gradient>"));
        List<String> lore = new ArrayList<>();
        lore.add(TextUtil.parse("<dark_gray>Restock berikutnya:"));
        lore.add(TextUtil.parse("<gray>" + market.getTimeRemaining()));
        lore.add("");
        lore.add(TextUtil.parse("<dark_gray>Item berganti setiap restock!"));
        lore.add(TextUtil.parse(GUI_INFO_KEY)); // marker
        meta.setLore(lore);
        meta.setDisplayName(TextUtil.parse("<gradient:#1a1a1a:#444444>⚫ Black Market Info</gradient>"));
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack buildCloseItem() {
        ItemStack item = new ItemStack(Material.BARRIER);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;
        meta.setDisplayName(TextUtil.parse("<red>✗ Tutup"));
        List<String> lore = new ArrayList<>();
        lore.add(TextUtil.parse("<dark_gray>Klik untuk menutup market."));
        lore.add(GUI_CLOSE_KEY);
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack buildRarityFilterItem(String rarityId) {
        RarityDefinition rarity = market.getRarity(rarityId);
        if (rarity == null) return new ItemStack(Material.PAPER);
        return rarity.buildIconItem();
    }

    private ItemStack buildFiller(String materialName) {
        Material mat;
        try { mat = Material.valueOf(materialName.toUpperCase()); } catch (Exception e) { mat = Material.BLACK_STAINED_GLASS_PANE; }
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(" ");
            item.setItemMeta(meta);
        }
        return item;
    }

    private List<Integer> getSlotList(FileConfiguration cfg, String path, List<Integer> def) {
        List<?> raw = cfg.getList(path);
        if (raw == null) return def;
        List<Integer> result = new ArrayList<>();
        for (Object o : raw) {
            if (o instanceof Number) result.add(((Number) o).intValue());
        }
        return result.isEmpty() ? def : result;
    }

    /** Refresh the inventory for a player (called after purchase) */
    public void refreshInventory(Player player) {
        if (player.getOpenInventory() != null) {
            Inventory newInv = buildInventory(player);
            // Update each slot without closing
            for (int i = 0; i < newInv.getSize() && i < player.getOpenInventory().getTopInventory().getSize(); i++) {
                player.getOpenInventory().getTopInventory().setItem(i, newInv.getItem(i));
            }
            player.updateInventory();
        }
    }
}
