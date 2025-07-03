// com.smokypeaks.server.commands.ServerRestartCommand.java
package com.smokypeaks.server.commands;

import com.smokypeaks.Main;
import com.smokypeaks.global.permissions.StaffPermissions;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class ServerRestartCommand implements CommandExecutor, TabCompleter {
    private final Main plugin;
    private BukkitTask restartTask;
    private static final List<String> TIME_UNITS = Arrays.asList("s", "m", "h");

    public ServerRestartCommand(Main plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission(StaffPermissions.Admin.RESTART)) {
            sender.sendMessage("§cYou don't have permission to restart the server!");
            return true;
        }

        if (args.length < 1) {
            sender.sendMessage("§cUsage: /restart <time> [reason]");
            return true;
        }

        // Cancel existing restart if running
        if (restartTask != null) {
            restartTask.cancel();
            Bukkit.broadcastMessage("§c[Server] §ePrevious restart schedule has been cancelled.");
        }

        // Parse time
        String timeStr = args[0].toLowerCase();
        int seconds = parseTime(timeStr);
        if (seconds <= 0) {
            sender.sendMessage("§cInvalid time format! Use <number><s|m|h>");
            return true;
        }

        // Get reason if provided
        String reason = args.length > 1 ?
                String.join(" ", Arrays.copyOfRange(args, 1, args.length)) :
                "Server Restart";

        // Start restart countdown
        scheduleRestart(seconds, reason);
        sender.sendMessage("§6[Server] §eRestart scheduled in " + formatTime(seconds));

        return true;
    }

    private void scheduleRestart(int seconds, String reason) {
        AtomicInteger timeLeft = new AtomicInteger(seconds);

        restartTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            int current = timeLeft.getAndDecrement();

            // Broadcast at specific intervals
            if (current == seconds || // Start
                    current == 300 || // 5 minutes
                    current == 60 || // 1 minute
                    current == 30 || // 30 seconds
                    current == 10 || // 10 seconds
                    current <= 5) { // Last 5 seconds

                Bukkit.broadcastMessage("§c[Server] §eRestarting in " + formatTime(current) +
                        "\n§eReason: §f" + reason);
            }

            // Restart server
            if (current <= 0) {
                Bukkit.broadcastMessage("§c[Server] §eRestarting now...");
                restartTask.cancel();

                // Check if we have an update to apply on restart
                File updateMarker = new File(plugin.getDataFolder(), "update.marker");
                if (updateMarker.exists()) {
                    Bukkit.broadcastMessage("§6[UltimateStaff] §eApplying plugin update during restart...");
                }

                // Use a slightly longer delay to ensure all messages are sent
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    // Use the stop command instead of restart for better compatibility
                    // This ensures the JVM fully exits and the server script restarts it
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "stop");
                }, 40L); // 2-second delay
            }
        }, 0L, 20L); // Run every second
    }

    private int parseTime(String timeStr) {
        try {
            char unit = timeStr.charAt(timeStr.length() - 1);
            int value = Integer.parseInt(timeStr.substring(0, timeStr.length() - 1));

            return switch (unit) {
                case 's' -> value;
                case 'm' -> value * 60;
                case 'h' -> value * 3600;
                default -> -1;
            };
        } catch (Exception e) {
            return -1;
        }
    }

    private String formatTime(int seconds) {
        if (seconds >= 3600) {
            return (seconds / 3600) + " hour" + (seconds / 3600 == 1 ? "" : "s");
        } else if (seconds >= 60) {
            return (seconds / 60) + " minute" + (seconds / 60 == 1 ? "" : "s");
        } else {
            return seconds + " second" + (seconds == 1 ? "" : "s");
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            String current = args[0].toLowerCase();
            for (String unit : TIME_UNITS) {
                completions.add("5" + unit);
                completions.add("10" + unit);
                completions.add("30" + unit);
                completions.add("60" + unit);
            }
            return completions.stream()
                    .filter(s -> s.startsWith(current))
                    .toList();
        }

        return completions;
    }
}
