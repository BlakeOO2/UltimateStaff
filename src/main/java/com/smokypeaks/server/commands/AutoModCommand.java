package com.smokypeaks.server.commands;

import com.smokypeaks.Main;
import com.smokypeaks.global.automod.ViolationType;
import com.smokypeaks.global.permissions.StaffPermissions;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class AutoModCommand implements CommandExecutor, TabCompleter {
    private final Main plugin;

    public AutoModCommand(Main plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission(StaffPermissions.Admin.AUTOMOD)) {
            sender.sendMessage("§cYou don't have permission to use this command!");
            return true;
        }

        if (args.length == 0) {
            showHelp(sender);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "enable" -> {
                plugin.getAutoModManager().setEnabled(true);
                sender.sendMessage("§a[AutoMod] System enabled!");
            }
            case "disable" -> {
                plugin.getAutoModManager().setEnabled(false);
                sender.sendMessage("§c[AutoMod] System disabled!");
            }
            case "status" -> {
                boolean enabled = plugin.getAutoModManager().isEnabled();
                String target = plugin.getAutoModManager().getNotificationTarget();
                sender.sendMessage("§6[AutoMod] Status: " + 
                        (enabled ? "§aEnabled" : "§cDisabled") + 
                        "§6, Notification Target: §f" + target);
            }
            case "notify" -> {
                if (args.length < 2) {
                    sender.sendMessage("§c[AutoMod] Please specify a player name!");
                    return true;
                }
                plugin.getAutoModManager().setNotificationTarget(args[1]);
                sender.sendMessage("§a[AutoMod] Notification target set to: §f" + args[1]);
            }
            case "clear" -> {
                if (args.length < 2) {
                    plugin.getAutoModManager().clearAllViolations();
                    sender.sendMessage("§a[AutoMod] Cleared all violation records!");
                } else {
                    OfflinePlayer target = Bukkit.getOfflinePlayerIfCached(args[1]);
                    if (target == null) {
                        sender.sendMessage("§c[AutoMod] Player not found!");
                        return true;
                    }
                    plugin.getAutoModManager().clearViolations(target.getUniqueId());
                    sender.sendMessage("§a[AutoMod] Cleared violations for §f" + target.getName());
                }
            }
            case "test" -> {
                if (args.length < 3) {
                    sender.sendMessage("§c[AutoMod] Usage: /automod test <player> <violation>");
                    return true;
                }
                Player target = Bukkit.getPlayer(args[1]);
                if (target == null) {
                    sender.sendMessage("§c[AutoMod] Player not found or not online!");
                    return true;
                }
                try {
                    ViolationType violationType = ViolationType.valueOf(args[2].toUpperCase());
                    plugin.getAutoModManager().processViolation(
                            target.getUniqueId(),
                            violationType,
                            "Test violation from " + sender.getName()
                    );
                    sender.sendMessage("§a[AutoMod] Test violation processed for §f" + target.getName());
                } catch (IllegalArgumentException e) {
                    sender.sendMessage("§c[AutoMod] Invalid violation type! Available types:\n" +
                            Arrays.stream(ViolationType.values())
                                    .map(Enum::name)
                                    .collect(Collectors.joining(", ")));
                }
            }
            case "help" -> showHelp(sender);
            default -> sender.sendMessage("§c[AutoMod] Unknown subcommand. Use /automod help for available commands.");
        }

        return true;
    }

    private void showHelp(CommandSender sender) {
        sender.sendMessage("§6§l===== AutoMod Commands =====\n" +
                "§e/automod enable §7- Enable the AutoMod system\n" +
                "§e/automod disable §7- Disable the AutoMod system\n" +
                "§e/automod status §7- Check the current status\n" +
                "§e/automod notify <player> §7- Set notification target\n" +
                "§e/automod clear [player] §7- Clear violation records\n" +
                "§e/automod test <player> <violation> §7- Test a violation\n" +
                "§e/automod help §7- Show this help message");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return Arrays.asList("enable", "disable", "status", "notify", "clear", "test", "help")
                    .stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        } else if (args.length == 2) {
            if (args[0].equalsIgnoreCase("notify") || 
                args[0].equalsIgnoreCase("clear") ||
                args[0].equalsIgnoreCase("test")) {

                return Bukkit.getOnlinePlayers().stream()
                        .map(Player::getName)
                        .filter(name -> name.toLowerCase().startsWith(args[1].toLowerCase()))
                        .collect(Collectors.toList());
            }
        } else if (args.length == 3 && args[0].equalsIgnoreCase("test")) {
            return Arrays.stream(ViolationType.values())
                    .map(type -> type.name().toLowerCase())
                    .filter(name -> name.startsWith(args[2].toLowerCase()))
                    .collect(Collectors.toList());
        }
        return new ArrayList<>();
    }
}
