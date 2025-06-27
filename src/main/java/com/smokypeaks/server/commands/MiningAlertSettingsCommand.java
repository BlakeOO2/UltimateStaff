package com.smokypeaks.server.commands;

import com.smokypeaks.Main;
import com.smokypeaks.global.permissions.StaffPermissions;
import com.smokypeaks.server.settings.AlertSettings; // Make sure this import is correct
import com.smokypeaks.server.settings.GlobalSettings;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.util.StringUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class MiningAlertSettingsCommand implements CommandExecutor, TabCompleter {
    private final Main plugin;
    private final List<String> MONITORED_ORES = Arrays.asList(
            "DIAMOND_ORE",
            "DEEPSLATE_DIAMOND_ORE",
            "ANCIENT_DEBRIS"
    );

    public MiningAlertSettingsCommand(Main plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission(StaffPermissions.Admin.ALERTS_CONFIG)) {
            sender.sendMessage(ChatColor.RED + "You don't have permission to configure mining alerts!");
            return true;
        }

        if (args.length < 1) {
            sendHelp(sender);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "list" -> listSettings(sender);
            case "edit" -> handleEdit(sender, args);
            case "toggle" -> handleToggle(sender, args);
            case "preview" -> handlePreview(sender, args);
            case "reload" -> handleReload(sender);
            default -> sendHelp(sender);
        }

        return true;
    }

    private void handleEdit(CommandSender sender, String[] args) {
        if (args.length < 4) {
            sender.sendMessage(ChatColor.RED + "Usage: /miningconfig edit <ore> <setting> <value>");
            return;
        }

        Material material;
        try {
            material = Material.valueOf(args[1].toUpperCase());
            if (!MONITORED_ORES.contains(material.name())) {
                sender.sendMessage(ChatColor.RED + "Invalid ore type!");
                return;
            }
        } catch (IllegalArgumentException e) {
            sender.sendMessage(ChatColor.RED + "Invalid ore type!");
            return;
        }

        AlertSettings settings = plugin.getMiningAlertManager().getSettings(material);
        String setting = args[2].toLowerCase();
        String value = String.join(" ", Arrays.copyOfRange(args, 3, args.length));

        switch (setting) {
            case "message" -> {
                settings.setMessage(value);
                sender.sendMessage(ChatColor.GREEN + "Updated message format!");
            }
            case "sound" -> {
                try {
                    Sound.valueOf(value.toUpperCase());
                    settings.setSoundType(value.toUpperCase());
                    sender.sendMessage(ChatColor.GREEN + "Updated alert sound!");
                } catch (IllegalArgumentException e) {
                    sender.sendMessage(ChatColor.RED + "Invalid sound type!");
                }
            }
            case "volume" -> {
                try {
                    float volume = Float.parseFloat(value);
                    settings.setSoundVolume(volume);
                    sender.sendMessage(ChatColor.GREEN + "Updated sound volume!");
                } catch (NumberFormatException e) {
                    sender.sendMessage(ChatColor.RED + "Invalid volume value!");
                }
            }
            case "pitch" -> {
                try {
                    float pitch = Float.parseFloat(value);
                    settings.setSoundPitch(pitch);
                    sender.sendMessage(ChatColor.GREEN + "Updated sound pitch!");
                } catch (NumberFormatException e) {
                    sender.sendMessage(ChatColor.RED + "Invalid pitch value!");
                }
            }
            case "veinradius" -> {
                try {
                    int radius = Integer.parseInt(value);
                    settings.setVeinRadius(radius);
                    sender.sendMessage(ChatColor.GREEN + "Updated vein detection radius!");
                } catch (NumberFormatException e) {
                    sender.sendMessage(ChatColor.RED + "Invalid radius value!");
                }
            }
            case "veinthreshold" -> {
                try {
                    int threshold = Integer.parseInt(value);
                    settings.setVeinThreshold(threshold);
                    sender.sendMessage(ChatColor.GREEN + "Updated vein detection threshold!");
                } catch (NumberFormatException e) {
                    sender.sendMessage(ChatColor.RED + "Invalid threshold value!");
                }
            }
            default -> sender.sendMessage(ChatColor.RED + "Unknown setting!");
        }

        plugin.getMiningAlertManager().saveSettings();
    }

    private void handleToggle(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /miningconfig toggle <ore>");
            return;
        }

        try {
            Material material = Material.valueOf(args[1].toUpperCase());
            if (!MONITORED_ORES.contains(material.name())) {
                sender.sendMessage(ChatColor.RED + "Invalid ore type!");
                return;
            }

            AlertSettings settings = plugin.getMiningAlertManager().getSettings(material);
            settings.setEnabled(!settings.isEnabled());
            plugin.getMiningAlertManager().saveSettings();

            sender.sendMessage(ChatColor.GREEN + "Mining alerts for " + material.name() + " are now " +
                    (settings.isEnabled() ? "enabled" : "disabled") + "!");
        } catch (IllegalArgumentException e) {
            sender.sendMessage(ChatColor.RED + "Invalid ore type!");
        }
    }

    private void handlePreview(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /miningconfig preview <ore>");
            return;
        }

        try {
            Material material = Material.valueOf(args[1].toUpperCase());
            if (!MONITORED_ORES.contains(material.name())) {
                sender.sendMessage(ChatColor.RED + "Invalid ore type!");
                return;
            }

            AlertSettings settings = plugin.getMiningAlertManager().getSettings(material);
            sender.sendMessage(ChatColor.YELLOW + "Preview for " + material.name() + ":");
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', settings.getMessage()
                    .replace("%player%", sender.getName())
                    .replace("%ore%", material.name())
                    .replace("%x%", "100")
                    .replace("%y%", "12")
                    .replace("%z%", "100")
                    .replace("%count%", "3")
                    .replace("%time%", "5")));
        } catch (IllegalArgumentException e) {
            sender.sendMessage(ChatColor.RED + "Invalid ore type!");
        }
    }

    private void handleReload(CommandSender sender) {
        plugin.getMiningAlertManager().loadSettings();
        sender.sendMessage(ChatColor.GREEN + "Mining alert settings reloaded!");
    }

    private void listSettings(CommandSender sender) {
        sender.sendMessage(ChatColor.GOLD + "=== Mining Alert Settings ===");
        for (String oreName : MONITORED_ORES) {
            Material material = Material.valueOf(oreName);
            AlertSettings settings = plugin.getMiningAlertManager().getSettings(material);
            sender.sendMessage(ChatColor.YELLOW + oreName + ": " +
                    (settings.isEnabled() ? ChatColor.GREEN + "Enabled" : ChatColor.RED + "Disabled"));
        }
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(ChatColor.GOLD + "=== Mining Alert Commands ===");
        sender.sendMessage(ChatColor.YELLOW + "/miningconfig list " + ChatColor.GRAY + "- List all settings");
        sender.sendMessage(ChatColor.YELLOW + "/miningconfig toggle <ore> " + ChatColor.GRAY + "- Toggle alerts for ore");
        sender.sendMessage(ChatColor.YELLOW + "/miningconfig edit <ore> <setting> <value> " + ChatColor.GRAY + "- Edit settings");
        sender.sendMessage(ChatColor.YELLOW + "/miningconfig preview <ore> " + ChatColor.GRAY + "- Preview alerts");
        sender.sendMessage(ChatColor.YELLOW + "/miningconfig reload " + ChatColor.GRAY + "- Reload settings");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            return StringUtil.copyPartialMatches(args[0],
                    Arrays.asList("list", "edit", "toggle", "preview", "reload"),
                    new ArrayList<>());
        }

        if (args.length == 2 && (args[0].equalsIgnoreCase("edit") ||
                args[0].equalsIgnoreCase("toggle") ||
                args[0].equalsIgnoreCase("preview"))) {
            return StringUtil.copyPartialMatches(args[1], MONITORED_ORES, new ArrayList<>());
        }

        if (args.length == 3 && args[0].equalsIgnoreCase("edit")) {
            return StringUtil.copyPartialMatches(args[2],
                    Arrays.asList("message", "sound", "volume", "pitch", "veinradius", "veinthreshold"),
                    new ArrayList<>());
        }

        if (args.length == 4 && args[0].equalsIgnoreCase("edit") && args[2].equalsIgnoreCase("sound")) {
            return StringUtil.copyPartialMatches(args[3],
                    Arrays.stream(Sound.values()).map(Sound::name).collect(Collectors.toList()),
                    new ArrayList<>());
        }

        return completions;
    }
}
