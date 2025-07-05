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
import java.util.List;
import java.util.stream.Collectors;

public class PunishCommand implements CommandExecutor, TabCompleter {
    private final Main plugin;

    public PunishCommand(Main plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player staff)) {
            sender.sendMessage("§cOnly players can use this command!");
            return true;
        }

        if (!staff.hasPermission("ultimatestaff.staff.punish")) {
            staff.sendMessage("§cYou don't have permission to use this command!");
            return true;
        }

        if (args.length < 1) {
            staff.sendMessage("§cUsage: /punish <player>");
            return true;
        }

        String targetName = args[0];
        Player target = Bukkit.getPlayer(targetName);

        if (target == null) {
            staff.sendMessage("§cPlayer '" + targetName + "' not found or not online!");
            return true;
        }

        // Prevent staff from punishing themselves
        if (target.equals(staff)) {
            staff.sendMessage("§cYou cannot punish yourself!");
            return true;
        }

        // Check if staff can punish the target
        if (target.hasPermission("ultimatestaff.staff.punish.immunity") && 
            !staff.hasPermission("ultimatestaff.admin.override")) {
            staff.sendMessage("§cYou cannot punish this player!");
            return true;
        }

        try {
            // Open the punishment menu for the target player
            plugin.getPunishmentManager().openMenu(staff, target);
        } catch (Exception e) {
            staff.sendMessage("§cAn error occurred while opening the punishment menu: " + e.getMessage());
            plugin.getLogger().severe("Error opening punishment menu: " + e.getMessage());
            e.printStackTrace();
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            String partialName = args[0].toLowerCase();
            List<String> playerNames = new ArrayList<>();

            // Add online player names that match the partial input
            playerNames.addAll(Bukkit.getOnlinePlayers().stream()
                .map(Player::getName)
                .filter(name -> name.toLowerCase().startsWith(partialName))
                .collect(Collectors.toList()));

            return playerNames;
        }

        return new ArrayList<>();
    }
}
