package com.smokypeaks.server.commands;

import com.smokypeaks.Main;
import com.smokypeaks.global.permissions.StaffPermissions;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class DeathLocationCommand implements CommandExecutor, TabCompleter {
    private final Main plugin;

    public DeathLocationCommand(Main plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Check if sender is a player or console
        if (!(sender instanceof Player) && args.length == 0) {
            sender.sendMessage("§cConsole must specify a player name!");
            return true;
        }

        // Check permission
        if (sender instanceof Player && !sender.hasPermission(StaffPermissions.Staff.DEATH_LOCATION)) {
            sender.sendMessage("§cYou don't have permission to use this command!");
            return true;
        }

        Player target;

        // If no args, use the sender (if it's a player)
        if (args.length == 0) {
            target = (Player) sender;
        } else {
            // Try to find the target player
            target = Bukkit.getPlayer(args[0]);
            if (target == null) {
                // Try to find by UUID if offline
                try {
                    UUID targetUuid = UUID.fromString(args[0]);
                    Location deathLoc = plugin.getDeathManager().getDeathLocation(targetUuid);
                    if (deathLoc != null) {
                        sendDeathLocationMessage(sender, args[0], deathLoc);
                        return true;
                    }
                } catch (IllegalArgumentException ignored) {
                    // Not a UUID, ignore
                }

                sender.sendMessage("§cPlayer not found or no death location recorded!");
                return true;
            }
        }

        // Get death location for the target
        Location deathLoc = plugin.getDeathManager().getDeathLocation(target.getUniqueId());
        if (deathLoc == null) {
            sender.sendMessage("§cNo death location found for " + target.getName());
            return true;
        }

        // Send information
        sendDeathLocationMessage(sender, target.getName(), deathLoc);

        // If sender is a player and has teleport permission, offer teleport
        if (sender instanceof Player player && player.hasPermission(StaffPermissions.Staff.TELEPORT)) {
            player.sendMessage("§eUse §6/stp pos " + 
                    deathLoc.getX() + " " + 
                    deathLoc.getY() + " " + 
                    deathLoc.getZ() + " " + 
                    deathLoc.getWorld().getName() + 
                    " §eto teleport to this location.");
        }

        return true;
    }

    private void sendDeathLocationMessage(CommandSender sender, String playerName, Location location) {
        sender.sendMessage("§6[Staff] §eDeath location for " + playerName + ":");
        sender.sendMessage(String.format("§7World: §f%s", location.getWorld().getName()));
        sender.sendMessage(String.format("§7X: §f%.1f§7, Y: §f%.1f§7, Z: §f%.1f", 
                location.getX(), location.getY(), location.getZ()));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> playerNames = new ArrayList<>();
            for (Player player : Bukkit.getOnlinePlayers()) {
                playerNames.add(player.getName());
            }
            return playerNames;
        }
        return new ArrayList<>();
    }
}
