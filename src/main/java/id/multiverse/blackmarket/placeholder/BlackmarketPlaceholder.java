package id.multiverse.blackmarket.placeholder;

import id.multiverse.blackmarket.MultiverseBlackmarket;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class BlackmarketPlaceholder extends PlaceholderExpansion {

    private final MultiverseBlackmarket plugin;

    public BlackmarketPlaceholder(MultiverseBlackmarket plugin) {
        this.plugin = plugin;
    }

    @Override public @NotNull String getIdentifier() { return "mbm"; }
    @Override public @NotNull String getAuthor() { return "MultiverseTeam"; }
    @Override public @NotNull String getVersion() { return plugin.getDescription().getVersion(); }
    @Override public boolean persist() { return true; }

    @Override
    public String onPlaceholderRequest(Player player, @NotNull String params) {
        return switch (params.toLowerCase()) {
            case "time_remaining" -> plugin.getMarketManager().getTimeRemaining();
            case "offer_count" -> String.valueOf(plugin.getMarketManager().getActiveOffers().size());
            default -> null;
        };
    }
}
