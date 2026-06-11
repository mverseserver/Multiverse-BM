package id.multiverse.blackmarket.market;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class RarityDefinition {

    private final String id;
    private final String name;
    private final int weight;
    private final String colorCode;
    private final boolean glow;
    private final ConfigurationSection iconSection;

    public RarityDefinition(String id, ConfigurationSection section) {
        this.id = id;
        this.name = section.getString("name", id);
        this.weight = section.getInt("weight", 50);
        this.colorCode = section.getString("color", "#ffffff");
        this.glow = section.getBoolean("glow", false);
        this.iconSection = section.getConfigurationSection("icon");
    }

    public ItemStack buildIconItem() {
        Material mat = Material.PAPER;
        int amount = 1;
        String displayName = name;
        List<String> lore = new ArrayList<>();

        if (iconSection != null) {
            try { mat = Material.valueOf(iconSection.getString("material", "PAPER").toUpperCase()); } catch (Exception ignored) {}
            amount = Math.max(1, iconSection.getInt("amount", 1));
            displayName = iconSection.getString("name", name);
            lore = new ArrayList<>(iconSection.getStringList("lore"));
        }

        ItemStack item = new ItemStack(mat, amount);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;
        meta.setDisplayName(RewardDefinition.colorize(displayName));
        List<String> colored = new ArrayList<>();
        for (String l : lore) colored.add(RewardDefinition.colorize(l));
        meta.setLore(colored);
        item.setItemMeta(meta);
        return item;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public int getWeight() { return weight; }
    public String getColorCode() { return colorCode; }
    public boolean isGlow() { return glow; }
}
