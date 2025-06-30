package com.smokypeaks.server.commands.antixray;

import com.smokypeaks.Main;
import com.smokypeaks.global.permissions.StaffPermissions;
import com.smokypeaks.server.managers.AntiXrayManager;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class XrayLearningCommand implements CommandExecutor, TabCompleter {
    private final Main plugin;

    public XrayLearningCommand(Main plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission(StaffPermissions.Alerts.XRAY_LEARNING)) {
            sender.sendMessage(ChatColor.RED + "You don't have permission to use this command.");
            return true;
        }

        if (args.length == 0) {
            sendHelpMessage(sender);
            return true;
        }

        AntiXrayManager antiXrayManager = plugin.getAntiXrayManager();

        switch (args[0].toLowerCase()) {
            case "help" -> sendHelpMessage(sender);
            case "enable" -> {
                antiXrayManager.setLearningMode(true);
                sender.sendMessage(ChatColor.GREEN + "Xray learning mode has been enabled. The system will learn from mining patterns.");
            }
            case "disable" -> {
                antiXrayManager.setLearningMode(false);
                sender.sendMessage(ChatColor.YELLOW + "Xray learning mode has been disabled.");
            }
            case "status" -> {
                boolean isEnabled = antiXrayManager.isLearningMode();
                double threshold = antiXrayManager.getDetectionThreshold();
                sender.sendMessage(ChatColor.GOLD + "===== AntiXray Learning Status =====");
                sender.sendMessage(ChatColor.YELLOW + "Learning Mode: " + 
                        (isEnabled ? ChatColor.GREEN + "Enabled" : ChatColor.RED + "Disabled"));
                sender.sendMessage(ChatColor.YELLOW + "Detection Threshold: " + ChatColor.AQUA + threshold + "%");
            }
            case "threshold" -> {
                if (args.length < 2) {
                    sender.sendMessage(ChatColor.RED + "Usage: /xraylearn threshold <value>");
                    return true;
                }
                try {
                    double threshold = Double.parseDouble(args[1]);
                    if (threshold < 0 || threshold > 100) {
                        sender.sendMessage(ChatColor.RED + "Threshold must be between 0 and 100.");
                        return true;
                    }
                    antiXrayManager.setDetectionThreshold(threshold);
                    sender.sendMessage(ChatColor.GREEN + "Detection threshold set to " + threshold + "%");
                } catch (NumberFormatException e) {
                    sender.sendMessage(ChatColor.RED + "Invalid number format. Please provide a valid number.");
                }
            }
            case "mark" -> {
                if (args.length < 3) {
                    sender.sendMessage(ChatColor.RED + "Usage: /xraylearn mark <player> <xray|legitimate>");
                    return true;
                }

                // Find the target player
                Player target = plugin.getServer().getPlayer(args[1]);
                if (target == null) {
                    sender.sendMessage(ChatColor.RED + "Player not found.");
                    return true;
                }

                // Determine if marking as xray or legitimate
                if (!args[2].equalsIgnoreCase("xray") && !args[2].equalsIgnoreCase("legitimate")) {
                    sender.sendMessage(ChatColor.RED + "You must specify 'xray' or 'legitimate'.");
                    return true;
                }

                boolean isXray = args[2].equalsIgnoreCase("xray");
                boolean success = antiXrayManager.markPattern(target.getUniqueId(), isXray);

                if (success) {
                    sender.sendMessage(ChatColor.GREEN + "Marked " + target.getName() + "'s recent mining pattern as " 
                        + (isXray ? "X-ray" : "legitimate") + " mining.");
                } else {
                    sender.sendMessage(ChatColor.RED + "No recent mining data available for " + target.getName() + ".");
                }
            }
            case "stats" -> {
                AntiXrayManager.LearningStats stats = antiXrayManager.getLearningStats();
                sender.sendMessage(ChatColor.GOLD + "===== Learning System Statistics =====");
                sender.sendMessage(ChatColor.YELLOW + "Patterns analyzed: " + ChatColor.WHITE + stats.getTotalPatterns());
                sender.sendMessage(ChatColor.YELLOW + "Known X-ray patterns: " + ChatColor.WHITE + stats.getXrayPatterns());
                sender.sendMessage(ChatColor.YELLOW + "Known legitimate patterns: " + ChatColor.WHITE + stats.getLegitimatePatterns());
                sender.sendMessage(ChatColor.YELLOW + "Current accuracy: " + ChatColor.WHITE + String.format("%.1f%%", stats.getAccuracy()));
            }
            case "clear" -> {
                if (args.length < 2) {
                    sender.sendMessage(ChatColor.RED + "Usage: /xraylearn clear <player>");
                    return true;
                }

                // Find the target player
                Player target = plugin.getServer().getPlayer(args[1]);
                if (target == null) {
                    sender.sendMessage(ChatColor.RED + "Player not found.");
                    return true;
                }

                antiXrayManager.clearLearningData(target.getUniqueId());
                sender.sendMessage(ChatColor.GREEN + "Cleared learning data for " + target.getName() + ".");
            }
            default -> sendHelpMessage(sender);
        }

        return true;
    }

    private void sendHelpMessage(CommandSender sender) {
        sender.sendMessage(ChatColor.GOLD + "===== AntiXray Learning Commands =====");
        sender.sendMessage(ChatColor.YELLOW + "/xraylearn help" + ChatColor.WHITE + " - Show this help message");
        sender.sendMessage(ChatColor.YELLOW + "/xraylearn enable" + ChatColor.WHITE + " - Enable learning mode");
        sender.sendMessage(ChatColor.YELLOW + "/xraylearn disable" + ChatColor.WHITE + " - Disable learning mode");
        sender.sendMessage(ChatColor.YELLOW + "/xraylearn status" + ChatColor.WHITE + " - Check learning mode status");
        sender.sendMessage(ChatColor.YELLOW + "/xraylearn threshold <value>" + ChatColor.WHITE + " - Set detection threshold (0-100)");
        sender.sendMessage(ChatColor.YELLOW + "/xraylearn mark <player> <xray|legitimate>" + ChatColor.WHITE + " - Mark player's recent mining as X-ray or legitimate");
        sender.sendMessage(ChatColor.YELLOW + "/xraylearn stats" + ChatColor.WHITE + " - View learning system statistics");
        sender.sendMessage(ChatColor.YELLOW + "/xraylearn clear <player>" + ChatColor.WHITE + " - Clear learning data for a player");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission(StaffPermissions.Alerts.XRAY_LEARNING)) {
            return List.of();
        }

        if (args.length == 1) {
            return Arrays.asList("help", "enable", "disable", "status", "threshold", "mark", "stats", "clear").stream()
                    .filter(s -> s.toLowerCase().startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }

        // Tab completion for second argument
        if (args.length == 2) {
            if (args[0].equalsIgnoreCase("mark") || args[0].equalsIgnoreCase("clear")) {
                // Return online player names
                return plugin.getServer().getOnlinePlayers().stream()
                        .map(Player::getName)
                        .filter(name -> name.toLowerCase().startsWith(args[1].toLowerCase()))
                        .collect(Collectors.toList());
            }
        }

        // Tab completion for third argument
        if (args.length == 3 && args[0].equalsIgnoreCase("mark")) {
            return Arrays.asList("xray", "legitimate").stream()
                    .filter(s -> s.toLowerCase().startsWith(args[2].toLowerCase()))
                    .collect(Collectors.toList());
        }

        return List.of();
    }
}
