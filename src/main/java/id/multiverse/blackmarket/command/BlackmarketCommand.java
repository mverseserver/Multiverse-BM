package id.multiverse.blackmarket.command;

import id.multiverse.blackmarket.MultiverseBlackmarket;
import id.multiverse.blackmarket.util.TextUtil;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;

public class BlackmarketCommand implements CommandExecutor, TabCompleter {

    private final MultiverseBlackmarket plugin;

    public BlackmarketCommand(MultiverseBlackmarket plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(TextUtil.parse(plugin.getConfigManager().getMessage("player-only")));
            return true;
        }

        if (!player.hasPermission("multiversebm.use")) {
            player.sendMessage(TextUtil.parse(plugin.getConfigManager().getMessage("no-permission")));
            return true;
        }

        if (plugin.getConfigManager().isCommandOpensGui()) {
            plugin.getGuiManager().openMarket(player);
        } else {
            String msg = plugin.getConfigManager().getMessage("blackmarket-info")
                    .replace("{time_remaining}", plugin.getMarketManager().getTimeRemaining());
            player.sendMessage(TextUtil.parse(msg));
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender s, Command c, String label, String[] args) {
        return Collections.emptyList();
    }
}
