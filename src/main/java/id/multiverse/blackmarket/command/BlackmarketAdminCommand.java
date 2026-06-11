package id.multiverse.blackmarket.command;

import id.multiverse.blackmarket.MultiverseBlackmarket;
import id.multiverse.blackmarket.util.TextUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class BlackmarketAdminCommand implements CommandExecutor, TabCompleter {

    private final MultiverseBlackmarket plugin;

    public BlackmarketAdminCommand(MultiverseBlackmarket plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("multiversebm.admin")) {
            sender.sendMessage(TextUtil.parse(plugin.getConfigManager().getMessage("no-permission")));
            return true;
        }

        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "reload" -> {
                plugin.getConfigManager().reloadAll();
                plugin.getMarketManager().loadData();
                sender.sendMessage(TextUtil.parse(plugin.getConfigManager().getMessage("reload-success")));
            }
            case "restock" -> {
                plugin.getMarketManager().forceRestock();
                sender.sendMessage(TextUtil.parse(plugin.getConfigManager().getMessage("restock-success")));
            }
            case "opengui" -> {
                if (args.length < 2) {
                    sender.sendMessage(TextUtil.parse(plugin.getConfigManager().getMessage("invalid-opengui")));
                    return true;
                }
                Player target = Bukkit.getPlayerExact(args[1]);
                if (target == null) {
                    sender.sendMessage(TextUtil.parse(
                            plugin.getConfigManager().getMessage("player-not-found")
                                    .replace("{player}", args[1])));
                    return true;
                }
                plugin.getGuiManager().openMarket(target);
                sender.sendMessage(TextUtil.parse(
                        plugin.getConfigManager().getMessage("opengui-success")
                                .replace("{player}", target.getName())));
            }
            case "help" -> sendHelp(sender);
            default -> sender.sendMessage(TextUtil.parse(plugin.getConfigManager().getMessage("invalid-subcommand")));
        }
        return true;
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(TextUtil.parse(plugin.getConfigManager().getMessage("help-header")));
        sender.sendMessage(TextUtil.parse(plugin.getConfigManager().getMessage("help-title")));
        sender.sendMessage(TextUtil.parse(plugin.getConfigManager().getMessage("help-line-1")));
        sender.sendMessage(TextUtil.parse(plugin.getConfigManager().getMessage("help-line-2")));
        sender.sendMessage(TextUtil.parse(plugin.getConfigManager().getMessage("help-line-3")));
        sender.sendMessage(TextUtil.parse(plugin.getConfigManager().getMessage("help-line-4")));
        sender.sendMessage(TextUtil.parse(plugin.getConfigManager().getMessage("help-footer")));
    }

    @Override
    public List<String> onTabComplete(CommandSender s, Command c, String label, String[] args) {
        if (args.length == 1) {
            return Arrays.asList("reload", "restock", "opengui", "help").stream()
                    .filter(sub -> sub.startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("opengui")) {
            return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(n -> n.toLowerCase().startsWith(args[1].toLowerCase()))
                    .collect(Collectors.toList());
        }
        return Collections.emptyList();
    }
}
