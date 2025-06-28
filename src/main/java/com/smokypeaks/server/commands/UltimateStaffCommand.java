package com.smokypeaks.server.commands;

import com.smokypeaks.Main;
import com.smokypeaks.global.permissions.StaffPermissions;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class UltimateStaffCommand implements CommandExecutor, TabCompleter {
    private final Main plugin;
    private final LagCommand lagCommand;

    public UltimateStaffCommand(Main plugin) {
        this.plugin = plugin;
        this.lagCommand = new LagCommand(plugin);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            showPluginInfo(sender);
            return true;
        }

        String subCommand = args[0].toLowerCase();
        String[] subArgs = Arrays.copyOfRange(args, 1, args.length);

        switch (subCommand) {
            case "lag" -> {
                return lagCommand.onCommand(sender, command, label, subArgs);
            }
            case "reload" -> {
                if (!sender.hasPermission(StaffPermissions.Admin.RELOAD)) {
                    sender.sendMessage("§cYou don't have permission to use this command!");
                    return true;
                }
                plugin.reloadPlugin();
                sender.sendMessage("§a[UltimateStaff] Plugin configuration reloaded!");
            }
            case "help" -> showHelp(sender);
            case "version" -> showVersion(sender);
            default -> sender.sendMessage("§c[UltimateStaff] Unknown subcommand. Use /ultimatestaff help for available commands.");
        }

        return true;
    }

    private void showVersion(CommandSender sender) {
        sender.sendMessage("§6[UltimateStaff] §eVersion: §f" + plugin.getDescription().getVersion());
    }

    private void showPluginInfo(CommandSender sender) {
        sender.sendMessage("§6§l===== UltimateStaff =====\n" +
                "§eVersion: §f" + plugin.getDescription().getVersion() + "\n" +
                "§eAuthor: §fHydrantz\n" +
                "§7Use /ultimatestaff help for available commands.");
    }

    private void showHelp(CommandSender sender) {
        sender.sendMessage("§6§l===== UltimateStaff Commands =====\n" +
                "§e/ultimatestaff lag §7- Diagnose and manage server lag\n" +
                "§e/ultimatestaff reload §7- Reload the plugin configuration\n" +
                "§e/ultimatestaff version §7- Show plugin version\n" +
                "§e/ultimatestaff help §7- Show this help message");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return Arrays.asList("lag", "reload", "help", "version")
                    .stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        } else if (args.length > 1 && args[0].equalsIgnoreCase("lag")) {
            return lagCommand.onTabComplete(sender, command, alias, Arrays.copyOfRange(args, 1, args.length));
        }
        return new ArrayList<>();
    }
}
