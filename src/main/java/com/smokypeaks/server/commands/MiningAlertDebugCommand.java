package com.smokypeaks.server.commands;

import com.smokypeaks.Main;
import com.smokypeaks.global.permissions.StaffPermissions;
import com.smokypeaks.server.settings.AlertSettings;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class MiningAlertDebugCommand implements CommandExecutor, TabCompleter {
    private final Main plugin;
    private final List<Material> MONITORED_ORES = Arrays.asList(
            Material.DIAMOND_ORE,
            Material.DEEPSLATE_DIAMOND_ORE,
            Material.ANCIENT_DEBRIS
    );

    public MiningAlertDebugCommand(Main plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission(StaffPermissions.Admin.DEBUG)) {
            sender.sendMessage("§cYou don't have permission to use debug commands!");
            return true;
        }

        if (args.length == 0) {
            showDebugInfo(sender);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "info" -> showDebugInfo(sender);
            case "test" -> testAlert(sender, args);
            case "reload" -> reloadSettings(sender);
            default -> {
                sender.sendMessage("§6=== Mining Alert Debug Commands ===");
                sender.sendMessage("§e/" + label + " info §7- Show current settings");
                sender.sendMessage("§e/" + label + " test <ore> §7- Test alert for specific ore");
                sender.sendMessage("§e/" + label + " reload §7- Reload settings from config");
            }
        }

        return true;
    }

    private void showDebugInfo(CommandSender sender) {
        sender.sendMessage("§6=== Mining Alert Debug Info ===");

        // Show global settings
        sender.sendMessage("§eGlobal Settings:");
        sender.sendMessage("§7  Plugin Version: §f" + plugin.getDescription().getVersion());
        sender.sendMessage("§7  Config Version: §f" + plugin.getConfig().getString("version", "Unknown"));

        // Show ore settings
        sender.sendMessage("\n§eOre Settings:");
        for (Material material : MONITORED_ORES) {
            AlertSettings settings = plugin.getMiningAlertManager().getSettings(material);
            sender.sendMessage("§e" + material.name() + ":");
            sender.sendMessage("§7  Enabled: §f" + settings.isEnabled());
            sender.sendMessage("§7  Message: §f" + settings.getMessage());
            sender.sendMessage("§7  Sound Enabled: §f" + settings.isSoundEnabled());
            if (settings.isSoundEnabled()) {
                sender.sendMessage("§7  Sound: §f" + settings.getSoundType());
                sender.sendMessage("§7  Volume: §f" + settings.getSoundVolume());
                sender.sendMessage("§7  Pitch: §f" + settings.getSoundPitch());
            }
            sender.sendMessage("§7  Vein Detection: §f" + settings.isVeinDetectionEnabled());
            if (settings.isVeinDetectionEnabled()) {
                sender.sendMessage("§7  Vein Radius: §f" + settings.getVeinRadius());
                sender.sendMessage("§7  Vein Threshold: §f" + settings.getVeinThreshold());
            }
        }

        // Show active alerts
        sender.sendMessage("\n§eActive Alert Recipients:");
        int count = 0;
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            if (player.hasPermission(StaffPermissions.Staff.MINING_ALERTS)) {
                sender.sendMessage("§7  - §f" + player.getName());
                count++;
            }
        }
        if (count == 0) {
            sender.sendMessage("§7  None online");
        }
    }

    private void testAlert(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage("§cUsage: /miningdebug test <ore>");
            return;
        }

        Material material;
        try {
            material = Material.valueOf(args[1].toUpperCase());
            if (!MONITORED_ORES.contains(material)) {
                sender.sendMessage("§cInvalid ore type! Must be one of: " +
                        MONITORED_ORES.stream()
                                .map(Material::name)
                                .collect(Collectors.joining(", ")));
                return;
            }
        } catch (IllegalArgumentException e) {
            sender.sendMessage("§cInvalid material name!");
            return;
        }

        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cThis command must be run by a player!");
            return;
        }

        // Send test alert
        plugin.getMiningAlertManager().handleBlockMine(player, material, player.getLocation());
        sender.sendMessage("§aTest alert sent for " + material.name());
    }

    private void reloadSettings(CommandSender sender) {
        plugin.reloadConfig();
        plugin.getMiningAlertManager().loadSettings();
        sender.sendMessage("§aMining alert settings reloaded!");
        showDebugInfo(sender);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            return Arrays.asList("info", "test", "reload");
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("test")) {
            return MONITORED_ORES.stream()
                    .map(Material::name)
                    .collect(Collectors.toList());
        }

        return completions;
    }
}
