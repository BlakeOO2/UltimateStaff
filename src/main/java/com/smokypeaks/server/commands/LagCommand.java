package com.smokypeaks.server.commands;

import com.smokypeaks.Main;
import com.smokypeaks.global.permissions.StaffPermissions;
import com.smokypeaks.server.managers.LagManager;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class LagCommand implements CommandExecutor, TabCompleter {
    private final Main plugin;

    public LagCommand(Main plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cThis command can only be used by players!");
            return true;
        }

        if (!player.hasPermission(StaffPermissions.Admin.LAG_DIAGNOSTIC)) {
            player.sendMessage("§cYou don't have permission to use this command!");
            return true;
        }

        if (args.length == 0) {
            showHelp(player);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "report", "analyze" -> {
                player.sendMessage("§6[UltimateStaff] §eGenerating lag report...");
                String report = plugin.getLagManager().generateLagReport();
                player.sendMessage(report);
            }
            case "chunks" -> {
                player.sendMessage("§6[UltimateStaff] §eScanning for laggy chunks...");
                List<LagManager.LaggyChunk> laggyChunks = plugin.getLagManager().findLaggyChunks();
                if (laggyChunks.isEmpty()) {
                    player.sendMessage("§a[UltimateStaff] No laggy chunks detected!");
                } else {
                    player.sendMessage("§6[UltimateStaff] §eFound " + laggyChunks.size() + " potentially laggy chunks:");
                    for (int i = 0; i < Math.min(laggyChunks.size(), 10); i++) {
                        LagManager.LaggyChunk chunk = laggyChunks.get(i);
                        player.sendMessage("§7" + i + ". World: §f" + chunk.getWorld().getName() + 
                                          " §7at X: §f" + chunk.getX() + " §7Z: §f" + chunk.getZ() + 
                                          " §7(§c" + chunk.getEntityCount() + " entities§7)");
                        player.sendMessage("   §7Entity Types: §f" + chunk.getEntityBreakdown());
                    }
                    player.sendMessage("§e[UltimateStaff] Use /ultimatestaff lag tp <number> to teleport to a chunk.");
                }
            }
            case "tp", "teleport" -> {
                if (args.length < 2) {
                    player.sendMessage("§c[UltimateStaff] Please specify a chunk number! Use /ultimatestaff lag chunks to see the list.");
                    return true;
                }
                try {
                    int index = Integer.parseInt(args[1]);
                    plugin.getLagManager().teleportToLaggyChunk(player, index);
                } catch (NumberFormatException e) {
                    player.sendMessage("§c[UltimateStaff] Invalid number format. Please use a valid number.");
                }
            }
            case "tps" -> {
                double tps = plugin.getLagManager().getTPS();
                String tpsColor = tps > 18 ? "§a" : (tps > 10 ? "§e" : "§c");
                player.sendMessage("§6[UltimateStaff] §7Current TPS: " + tpsColor + String.format("%.2f", tps));
            }
            case "players" -> {
                player.sendMessage("§6[UltimateStaff] §eScanning for players who might be causing lag...");
                List<String> laggyPlayers = plugin.getLagManager().findLaggyPlayers();
                if (laggyPlayers.isEmpty()) {
                    player.sendMessage("§a[UltimateStaff] No players appear to be causing lag.");
                } else {
                    player.sendMessage("§6[UltimateStaff] §ePlayers who might be causing lag:");
                    for (String playerInfo : laggyPlayers) {
                        player.sendMessage("§7- §f" + playerInfo);
                    }
                }
            }
            case "redstone" -> {
                player.sendMessage("§6[UltimateStaff] §eScanning for redstone activity...");
                plugin.getLagManager().scanForRedstoneActivity(player);
            }
            case "help" -> showHelp(player);
            default -> {
                player.sendMessage("§c[UltimateStaff] Unknown subcommand. Use /ultimatestaff lag help for available commands.");
            }
        }

        return true;
    }

    private void showHelp(Player player) {
        player.sendMessage("§6§l===== UltimateStaff Lag Command =====\n" +
                "§e/ultimatestaff lag report §7- Generate a complete lag report\n" +
                "§e/ultimatestaff lag chunks §7- Find chunks with high entity counts\n" +
                "§e/ultimatestaff lag tp <number> §7- Teleport to a laggy chunk\n" +
                "§e/ultimatestaff lag tps §7- Show current server TPS\n" +
                "§e/ultimatestaff lag players §7- Find players who might be causing lag\n" +
                "§e/ultimatestaff lag redstone §7- Scan for excessive redstone usage\n" +
                "§e/ultimatestaff lag help §7- Show this help message");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return Arrays.asList("report", "chunks", "tp", "tps", "players", "redstone", "help")
                    .stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        } else if (args.length == 2 && args[0].equalsIgnoreCase("tp")) {
            return Arrays.asList("0", "1", "2", "3", "4", "5", "6", "7", "8", "9");
        }
        return new ArrayList<>();
    }
}
