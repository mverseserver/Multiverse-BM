package id.multiverse.blackmarket.market;

import id.multiverse.blackmarket.MultiverseBlackmarket;
import id.multiverse.blackmarket.config.ConfigManager;
import id.multiverse.blackmarket.economy.EconomyManager;
import id.multiverse.blackmarket.economy.EconomyManager.CurrencyType;
import id.multiverse.blackmarket.util.TextUtil;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

public class MarketManager {

    public enum PurchaseResult {
        SUCCESS, SOLD_OUT, NO_MONEY, INVENTORY_FULL, ECONOMY_ERROR, UNKNOWN
    }

    private final MultiverseBlackmarket plugin;
    private final ConfigManager cfg;
    private final EconomyManager economy;

    private final Map<String, RarityDefinition> rarities = new LinkedHashMap<>();
    private final Map<String, RewardDefinition> rewardPool = new LinkedHashMap<>();
    private final List<ActiveOffer> activeOffers = new ArrayList<>();

    private final Map<UUID, Long> cooldowns = new ConcurrentHashMap<>();

    private BukkitTask restockTask;
    private long nextRestockMillis = 0L;

    public MarketManager(MultiverseBlackmarket plugin) {
        this.plugin = plugin;
        this.cfg = plugin.getConfigManager();
        this.economy = plugin.getEconomyManager();
    }

    public void initialize() {
        loadData();
        scheduleRestock();
        restock(false); // initial stock on startup
    }

    public void shutdown() {
        if (restockTask != null) restockTask.cancel();
    }

    // ── Data Loading ─────────────────────────────────────

    public void loadData() {
        rarities.clear();
        rewardPool.clear();

        FileConfiguration market = cfg.getMarket();

        // Load rarities
        ConfigurationSection rarSection = market.getConfigurationSection("rarities");
        if (rarSection != null) {
            for (String key : rarSection.getKeys(false)) {
                ConfigurationSection s = rarSection.getConfigurationSection(key);
                if (s != null) rarities.put(key, new RarityDefinition(key, s));
            }
        }

        // Load rewards
        String defaultCurrencyStr = cfg.getDefaultCurrency();
        CurrencyType defaultCurrency = economy.parseCurrency(defaultCurrencyStr);

        ConfigurationSection rewSection = market.getConfigurationSection("rewards");
        if (rewSection != null) {
            for (String key : rewSection.getKeys(false)) {
                ConfigurationSection s = rewSection.getConfigurationSection(key);
                if (s != null) rewardPool.put(key, new RewardDefinition(key, s, defaultCurrency));
            }
        }

        plugin.getLogger().info("Loaded " + rarities.size() + " rarities, " + rewardPool.size() + " rewards.");
    }

    // ── Restock Logic ─────────────────────────────────────

    private void scheduleRestock() {
        if (restockTask != null) restockTask.cancel();
        long intervalTicks = (long) cfg.getRestockIntervalMinutes() * 60 * 20;
        restockTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> restock(true), intervalTicks, intervalTicks);
        nextRestockMillis = System.currentTimeMillis() + (cfg.getRestockIntervalMinutes() * 60 * 1000L);
    }

    public void forceRestock() {
        restock(true);
        // Reset timer
        scheduleRestock();
    }

    private void restock(boolean announce) {
        activeOffers.clear();

        int offersNeeded = cfg.getOffersPerRestock();
        boolean dynamicEnabled = cfg.isDynamicPricingEnabled();
        int variancePct = cfg.getDynamicPricingVariancePercent();

        // Build weighted pool per rarity, then pick offers
        List<RewardDefinition> allRewards = new ArrayList<>(rewardPool.values());
        Collections.shuffle(allRewards);

        // Group by rarity
        Map<String, List<RewardDefinition>> byRarity = new LinkedHashMap<>();
        for (RewardDefinition r : allRewards) {
            byRarity.computeIfAbsent(r.getRarity(), k -> new ArrayList<>()).add(r);
        }

        // Pick offers using rarity weight then individual chance
        List<RewardDefinition> chosen = weightedSelect(byRarity, offersNeeded);

        for (RewardDefinition def : chosen) {
            double price = computePrice(def, dynamicEnabled, variancePct);
            int stock = def.getStock();
            activeOffers.add(new ActiveOffer(def, stock, price));
        }

        nextRestockMillis = System.currentTimeMillis() + (cfg.getRestockIntervalMinutes() * 60 * 1000L);

        plugin.getLogger().info("Black Market restocked with " + activeOffers.size() + " offers.");

        if (announce && cfg.isAnnounceRestocks()) {
            broadcastRestock();
        }
    }

    private List<RewardDefinition> weightedSelect(Map<String, List<RewardDefinition>> byRarity, int needed) {
        List<RewardDefinition> result = new ArrayList<>();
        Random rng = new Random();

        // Build list of rarity entries weighted
        List<Map.Entry<String, Integer>> rarityWeights = new ArrayList<>();
        for (Map.Entry<String, RarityDefinition> e : rarities.entrySet()) {
            if (byRarity.containsKey(e.getKey())) {
                rarityWeights.add(Map.entry(e.getKey(), e.getValue().getWeight()));
            }
        }

        int attempts = 0;
        Set<String> usedIds = new HashSet<>();

        while (result.size() < needed && attempts < needed * 10) {
            attempts++;
            // Pick a rarity by weight
            String pickedRarity = pickWeighted(rarityWeights, rng);
            if (pickedRarity == null) break;

            List<RewardDefinition> pool = byRarity.get(pickedRarity);
            if (pool == null || pool.isEmpty()) continue;

            // Pick a reward by chance
            RewardDefinition candidate = null;
            for (RewardDefinition r : pool) {
                if (usedIds.contains(r.getId())) continue;
                if (rng.nextDouble() * 100 <= r.getChance()) {
                    candidate = r;
                    break;
                }
            }
            if (candidate == null) continue;
            usedIds.add(candidate.getId());
            result.add(candidate);
        }

        // Fill up if not enough
        if (result.size() < needed) {
            for (RewardDefinition r : rewardPool.values()) {
                if (result.size() >= needed) break;
                if (!usedIds.contains(r.getId())) {
                    usedIds.add(r.getId());
                    result.add(r);
                }
            }
        }

        return result;
    }

    private String pickWeighted(List<Map.Entry<String, Integer>> weights, Random rng) {
        int total = weights.stream().mapToInt(Map.Entry::getValue).sum();
        if (total == 0) return null;
        int pick = rng.nextInt(total);
        int cumulative = 0;
        for (Map.Entry<String, Integer> e : weights) {
            cumulative += e.getValue();
            if (pick < cumulative) return e.getKey();
        }
        return weights.get(0).getKey();
    }

    private double computePrice(RewardDefinition def, boolean dynamic, int variancePct) {
        double base = def.getPriceBase();
        if (!dynamic || variancePct == 0) return base;

        double minP = def.getPriceMin() > 0 ? def.getPriceMin() : base * (1 - variancePct / 100.0);
        double maxP = def.getPriceMax() > 0 ? def.getPriceMax() : base * (1 + variancePct / 100.0);

        double range = maxP - minP;
        double price = minP + (new Random().nextDouble() * range);
        // Round to nearest 50
        price = Math.round(price / 50.0) * 50.0;
        return Math.max(1, price);
    }

    // ── Purchase Logic ───────────────────────────────────

    public PurchaseResult purchase(Player player, int offerIndex) {
        // Cooldown check
        if (!player.hasPermission("multiversebm.bypass.cooldown")) {
            long lastPurchase = cooldowns.getOrDefault(player.getUniqueId(), 0L);
            if (System.currentTimeMillis() - lastPurchase < cfg.getPurchaseCooldownMillis()) {
                return PurchaseResult.UNKNOWN; // handled as cooldown
            }
        }

        if (offerIndex < 0 || offerIndex >= activeOffers.size()) return PurchaseResult.UNKNOWN;
        ActiveOffer offer = activeOffers.get(offerIndex);

        if (offer.isSoldOut()) return PurchaseResult.SOLD_OUT;

        // Economy check
        double price = offer.getCurrentPrice();
        CurrencyType currency = offer.getCurrency();
        if (!economy.hasBalance(player, currency, price)) {
            return PurchaseResult.NO_MONEY;
        }

        // Inventory check (only if giving item)
        ItemStack rewardItem = offer.getDefinition().buildRewardItem();
        if (rewardItem != null && player.getInventory().firstEmpty() == -1) {
            return PurchaseResult.INVENTORY_FULL;
        }

        // Withdraw
        if (!economy.withdraw(player, currency, price)) {
            return PurchaseResult.ECONOMY_ERROR;
        }

        // Give item
        if (rewardItem != null) {
            HashMap<Integer, ItemStack> leftover = player.getInventory().addItem(rewardItem);
            if (!leftover.isEmpty()) {
                // Drop at player feet if still no space
                leftover.values().forEach(i -> player.getWorld().dropItemNaturally(player.getLocation(), i));
            }
        }

        // Run commands
        for (String cmd : offer.getDefinition().getCommands()) {
            String processed = cmd.replace("{player}", player.getName());
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), processed);
        }

        // Decrement stock
        offer.decrementStock();

        cooldowns.put(player.getUniqueId(), System.currentTimeMillis());

        return PurchaseResult.SUCCESS;
    }

    // ── Broadcast ─────────────────────────────────────────

    private void broadcastRestock() {
        String raw = cfg.getMessage("restock-broadcast")
                .replace("{time_remaining}", getTimeRemaining());

        // Title
        boolean useTitle = plugin.getConfig().getBoolean("broadcast.use-title", true);
        String titleRaw = plugin.getConfig().getString("broadcast.title", "⚠ BLACK MARKET ⚠");
        String subtitleRaw = plugin.getConfig().getString("broadcast.subtitle", "Restock!");

        // Sound
        boolean useSound = plugin.getConfig().getBoolean("broadcast.sound.enabled", true);
        String soundName = plugin.getConfig().getString("broadcast.sound.type", "ENTITY_ENDER_DRAGON_GROWL");
        float vol = (float) plugin.getConfig().getDouble("broadcast.sound.volume", 0.5);
        float pitch = (float) plugin.getConfig().getDouble("broadcast.sound.pitch", 0.8);

        for (Player p : Bukkit.getOnlinePlayers()) {
            // Chat
            p.sendMessage(TextUtil.parse(raw));

            // Title
            if (useTitle) {
                p.sendTitle(
                    TextUtil.parseLegacy(titleRaw),
                    TextUtil.parseLegacy(subtitleRaw),
                    10, 60, 20
                );
            }

            // Sound
            if (useSound) {
                try {
                    Sound s = Sound.valueOf(soundName.toUpperCase());
                    p.playSound(p.getLocation(), s, vol, pitch);
                } catch (Exception e) {
                    plugin.getLogger().log(Level.WARNING, "Invalid broadcast sound: " + soundName);
                }
            }
        }
    }

    // ── Time Remaining ────────────────────────────────────

    public String getTimeRemaining() {
        long diff = nextRestockMillis - System.currentTimeMillis();
        if (diff <= 0) return "0" + cfg.getTimeFormat("second");

        long seconds = diff / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;

        seconds %= 60; minutes %= 60; hours %= 24;

        StringBuilder sb = new StringBuilder();
        if (days > 0) sb.append(days).append(cfg.getTimeFormat("day")).append(" ");
        if (hours > 0) sb.append(hours).append(cfg.getTimeFormat("hour")).append(" ");
        if (minutes > 0) sb.append(minutes).append(cfg.getTimeFormat("minute")).append(" ");
        sb.append(seconds).append(cfg.getTimeFormat("second"));
        return sb.toString().trim();
    }

    // ── Getters ───────────────────────────────────────────

    public List<ActiveOffer> getActiveOffers() { return Collections.unmodifiableList(activeOffers); }
    public Map<String, RarityDefinition> getRarities() { return Collections.unmodifiableMap(rarities); }
    public RarityDefinition getRarity(String id) { return rarities.get(id); }
}
