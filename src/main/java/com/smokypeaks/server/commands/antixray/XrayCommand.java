package com.smokypeaks.server.commands.antixray;

import com.smokypeaks.Main;
import com.smokypeaks.global.permissions.StaffPermissions;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class XrayCommand implements CommandExecutor, TabCompleter {
    private final Main plugin;

    public XrayCommand(Main plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission(StaffPermissions.Staff.XRAY_COMMANDS)) {
            sender.sendMessage(ChatColor.RED + "You don't have permission to use this command.");
            return true;
        }

        if (args.length == 0) {
            sendHelpMessage(sender);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "help" -> sendHelpMessage(sender);
            case "info" -> {
                if (args.length < 2) {
                    sender.sendMessage(ChatColor.RED + "Usage: /xray info <player>");
                    return true;
                }
                showPlayerInfo(sender, args[1]);
            }
            case "inspect" -> {
                if (!(sender instanceof Player)) {
                    sender.sendMessage(ChatColor.RED + "This command can only be used by players.");
                    return true;
                }
                // TODO: Implement inspection mode
                sender.sendMessage(ChatColor.YELLOW + "Xray inspection mode will be implemented in a future update.");
            }
            default -> sendHelpMessage(sender);
        }

        return true;
    }

    private void sendHelpMessage(CommandSender sender) {
        sender.sendMessage(ChatColor.GOLD + "===== AntiXray Commands =====\n" +
                ChatColor.YELLOW + "/xray help" + ChatColor.WHITE + " - Show this help message\n" +
                ChatColor.YELLOW + "/xray info <player>" + ChatColor.WHITE + " - Show a player's mining statistics\n" +
                ChatColor.YELLOW + "/xray inspect" + ChatColor.WHITE + " - Enter inspection mode to analyze a player's mining");
    }

    private void showPlayerInfo(CommandSender sender, String playerName) {
        // TODO: Implement player mining info display
        sender.sendMessage(ChatColor.YELLOW + "Mining statistics for " + playerName + ":");
        sender.sendMessage(ChatColor.YELLOW + "This feature will be fully implemented in a future update.");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission(StaffPermissions.Staff.XRAY_COMMANDS)) {
            return List.of();
        }

        if (args.length == 1) {
            return Arrays.asList("help", "info", "inspect").stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("info")) {
            return plugin.getServer().getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(name -> name.toLowerCase().startsWith(args[1].toLowerCase()))
                    .collect(Collectors.toList());
        }

        return List.of();
    }
}
