package id.multiverse.blackmarket.market;

import id.multiverse.blackmarket.economy.EconomyManager.CurrencyType;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.Material;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RewardDefinition {

    private final String id;
    private final String name;
    private final String rarity;
    private final double chance;
    private final int stock;
    private final double priceBase;
    private final double priceMin;
    private final double priceMax;
    private final CurrencyType currency;
    private final ConfigurationSection iconSection;
    private final ConfigurationSection rewardItemSection;
    private final List<String> commands;

    public RewardDefinition(String id, ConfigurationSection section, CurrencyType defaultCurrency) {
        this.id = id;
        this.name = section.getString("name", id);
        this.rarity = section.getString("rarity", "common");
        this.chance = section.getDouble("chance", 50.0);
        this.stock = section.getInt("stock", 10);
        this.priceBase = section.getDouble("price-base", 100.0);
        this.priceMin = section.contains("price-min") ? section.getDouble("price-min") : -1;
        this.priceMax = section.contains("price-max") ? section.getDouble("price-max") : -1;
        this.currency = parseCurrency(section.getString("currency"), defaultCurrency);
        this.iconSection = section.getConfigurationSection("icon");
        this.rewardItemSection = section.getConfigurationSection("reward-item");
        this.commands = section.getStringList("commands");
    }

    private CurrencyType parseCurrency(String s, CurrencyType def) {
        if (s == null) return def;
        return s.equalsIgnoreCase("PLAYERPOINTS") ? CurrencyType.PLAYERPOINTS : CurrencyType.VAULT;
    }

    public ItemStack buildIconItem(double currentPrice, String currencyFormatted, int currentStock, RarityDefinition rarityDef) {
        Material mat = Material.CHEST;
        int amount = 1;
        String displayName = name;
        List<String> lore = new ArrayList<>();

        if (iconSection != null && iconSection.isString("material")) {
            try { mat = Material.valueOf(iconSection.getString("material", "CHEST").toUpperCase()); } catch (Exception ignored) {}
            amount = Math.max(1, Math.min(64, iconSection.getInt("amount", 1)));
            displayName = iconSection.getString("name", name);
            lore = new ArrayList<>(iconSection.getStringList("lore"));
        }

        // Add standard info lines
        lore.add("");
        lore.add(rarityDef.getColorCode() + "Rarity: " + rarityDef.getName());
        lore.add("<gray>Stok: <white>" + (currentStock <= 0 ? "∞" : currentStock));
        lore.add("<gray>Harga: <white>" + currencyFormatted);
        lore.add("<gray>Chance: <white>" + String.format("%.1f", chance) + "%");
        lore.add("");

        if (currentStock == 0) {
            lore.add("<red>✗ HABIS TERJUAL");
        } else {
            lore.add("<green>✔ Klik untuk membeli");
        }

        return buildItem(mat, amount, displayName, lore, rarityDef.isGlow() && currentStock > 0);
    }

    public ItemStack buildRewardItem() {
        if (rewardItemSection == null || rewardItemSection.getString("material") == null) return null;
        Material mat;
        try { mat = Material.valueOf(rewardItemSection.getString("material", "AIR").toUpperCase()); }
        catch (Exception e) { return null; }
        if (mat == Material.AIR) return null;

        int amount = Math.max(1, Math.min(64, rewardItemSection.getInt("amount", 1)));
        String displayName = rewardItemSection.getString("name", name);
        List<String> lore = new ArrayList<>(rewardItemSection.getStringList("lore"));

        ItemStack item = buildItem(mat, amount, displayName, lore, false);

        // Apply enchantments
        ConfigurationSection enchSection = rewardItemSection.getConfigurationSection("enchantments");
        if (enchSection != null && item.getItemMeta() != null) {
            ItemMeta meta = item.getItemMeta();
            for (String enchKey : enchSection.getKeys(false)) {
                try {
                    Enchantment ench = Enchantment.getByName(enchKey.toUpperCase());
                    if (ench == null) ench = Enchantment.getByKey(org.bukkit.NamespacedKey.minecraft(enchKey.toLowerCase()));
                    if (ench != null) meta.addEnchant(ench, enchSection.getInt(enchKey), true);
                } catch (Exception ignored) {}
            }
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack buildItem(Material mat, int amount, String name, List<String> lore, boolean glow) {
        ItemStack item = new ItemStack(mat, amount);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        // Use MiniMessage / legacy color parsing
        meta.setDisplayName(colorize(name));
        List<String> coloredLore = new ArrayList<>();
        for (String line : lore) coloredLore.add(colorize(line));
        meta.setLore(coloredLore);

        if (glow) {
            meta.addEnchant(Enchantment.UNBREAKING, 1, true);
            meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ENCHANTS);
        }

        item.setItemMeta(meta);
        return item;
    }

    /** Basic colorizer: handle hex #RRGGBB and legacy &codes */
    public static String colorize(String text) {
        if (text == null) return "";
        // hex colors
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("#([A-Fa-f0-9]{6})").matcher(text);
        StringBuffer sb = new StringBuffer();
        while (m.find()) {
            String hex = m.group(1);
            char[] chars = hex.toCharArray();
            String replacement = "\u00A7x" +
                "\u00A7" + chars[0] + "\u00A7" + chars[1] +
                "\u00A7" + chars[2] + "\u00A7" + chars[3] +
                "\u00A7" + chars[4] + "\u00A7" + chars[5];
            m.appendReplacement(sb, java.util.regex.Matcher.quoteReplacement(replacement));
        }
        m.appendTail(sb);
        return net.md_5.bungee.api.ChatColor.translateAlternateColorCodes('&', sb.toString());
    }

    // ── Getters ──────────────────────────────────────────
    public String getId() { return id; }
    public String getName() { return name; }
    public String getRarity() { return rarity; }
    public double getChance() { return chance; }
    public int getStock() { return stock; }
    public double getPriceBase() { return priceBase; }
    public double getPriceMin() { return priceMin; }
    public double getPriceMax() { return priceMax; }
    public CurrencyType getCurrency() { return currency; }
    public List<String> getCommands() { return commands; }
}
