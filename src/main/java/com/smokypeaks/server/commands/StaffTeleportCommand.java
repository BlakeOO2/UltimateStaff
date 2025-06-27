// com.smokypeaks.server.commands.StaffTeleportCommand.java
package com.smokypeaks.server.commands;

import com.smokypeaks.Main;
import com.smokypeaks.global.permissions.StaffPermissions;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class StaffTeleportCommand implements CommandExecutor {
    private final Main plugin;

    public StaffTeleportCommand(Main plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player staff)) {
            sender.sendMessage("§cThis command can only be used by players!");
            return true;
        }

        if (!staff.hasPermission(StaffPermissions.Staff.TELEPORT)) {
            staff.sendMessage("§cYou don't have permission to use this command!");
            return true;
        }

        if (args.length == 0) {
            sendHelp(staff);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "player", "p" -> {
                if (args.length < 2) {
                    staff.sendMessage("§cUsage: /" + label + " player <name>");
                    return true;
                }
                Player target = Bukkit.getPlayer(args[1]);
                if (target != null) {
                    plugin.getTeleportManager().teleportToPlayer(staff, target);
                } else {
                    staff.sendMessage("§c[Staff] §ePlayer not found!");
                }
            }
            case "random", "r" -> plugin.getTeleportManager().teleportToRandomPlayer(staff);
            case "back", "b" -> plugin.getTeleportManager().returnToPreviousLocation(staff);
            case "pos", "position" -> {
                if (args.length < 4) {
                    staff.sendMessage("§cUsage: /" + label + " pos <x> <y> <z>");
                    return true;
                }
                try {
                    double x = Double.parseDouble(args[1]);
                    double y = Double.parseDouble(args[2]);
                    double z = Double.parseDouble(args[3]);
                    Location loc = new Location(staff.getWorld(), x, y, z);
                    plugin.getTeleportManager().teleportToLocation(staff, loc);
                } catch (NumberFormatException e) {
                    staff.sendMessage("§c[Staff] §eInvalid coordinates!");
                }
            }
            default -> sendHelp(staff);
        }

        return true;
    }

    private void sendHelp(Player player) {
        player.sendMessage("§6[Staff] §eTeleport Commands:");
        player.sendMessage("§e/stp player <name> §7- Teleport to a player");
        player.sendMessage("§e/stp random §7- Teleport to a random player");
        player.sendMessage("§e/stp back §7- Return to previous location");
        player.sendMessage("§e/stp pos <x> <y> <z> §7- Teleport to coordinates");
    }
}
