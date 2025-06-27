// Updated ClearChatCommand.java
package com.smokypeaks.server.commands;

import com.smokypeaks.Main;
import com.smokypeaks.global.permissions.StaffPermissions;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class ClearChatCommand implements CommandExecutor, TabCompleter {
    private final Main plugin;
    private final int LINES_TO_CLEAR = 300; // Increased number of blank lines

    public ClearChatCommand(Main plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission(StaffPermissions.Staff.CLEAR_CHAT)) {
            sender.sendMessage("§cYou don't have permission to clear chat!");
            return true;
        }

        if (args.length > 0 && args[0].equalsIgnoreCase("self")) {
            // Clear only for the sender
            if (sender instanceof Player player) {
                clearChatForPlayer(player);
                player.sendMessage("§6[Staff] §eYour chat has been cleared.");
            } else {
                sender.sendMessage("§cOnly players can clear their own chat!");
            }
            return true;
        } else if (args.length > 0 && args[0].equalsIgnoreCase("player")) {
            // Clear for a specific player
            if (args.length < 2) {
                sender.sendMessage("§cUsage: /" + label + " player <name>");
                return true;
            }

            Player target = Bukkit.getPlayer(args[1]);
            if (target == null) {
                sender.sendMessage("§cPlayer not found!");
                return true;
            }

            clearChatForPlayer(target);
            target.sendMessage("§6[Staff] §eYour chat has been cleared by a staff member.");
            sender.sendMessage("§6[Staff] §eCleared chat for " + target.getName());
            return true;
        }

        // Clear for all players (default behavior)
        String staffName = sender instanceof Player ? ((Player) sender).getName() : "Console";

        // Clear chat for all players
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!player.hasPermission(StaffPermissions.Staff.BYPASS_CLEAR_CHAT) || player.equals(sender)) {
                clearChatForPlayer(player);
            }
        }

        // Send notification after clearing
        Bukkit.broadcastMessage("§6[Staff] §eChat has been cleared by " + staffName);

        // Log to console
        plugin.getLogger().info("Chat cleared by " + staffName);

        return true;
    }

    private void clearChatForPlayer(Player player) {
        // Create a large string of empty lines
        StringBuilder clearMessage = new StringBuilder();
        for (int i = 0; i < LINES_TO_CLEAR; i++) {
            player.sendMessage(" ");
        }

        // Send as one message for efficiency
        player.sendMessage("§8§m----------------------------------------");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return Arrays.asList("self", "player", "all").stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        } else if (args.length == 2 && args[0].equalsIgnoreCase("player")) {
            return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(name -> name.toLowerCase().startsWith(args[1].toLowerCase()))
                    .collect(Collectors.toList());
        }
        return new ArrayList<>();
    }
}
