package com.smokypeaks.server.commands.debug;

import com.smokypeaks.Main;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * Debug command for the punishment system
 */
public class PunishmentDebugCommand implements CommandExecutor, TabCompleter {
    private final Main plugin;

    public PunishmentDebugCommand(Main plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage("§6[Staff] §eAvailable debug commands:§r");
            sender.sendMessage("§e/punishdebug session §7- View your current session data");
            sender.sendMessage("§e/punishdebug categories §7- List all punishment categories");
            sender.sendMessage("§e/punishdebug violations <category> §7- List violations in a category");
            sender.sendMessage("§e/punishdebug clear §7- Clear your session data");
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "session":
                if (sender instanceof Player player) {
                    UUID uuid = player.getUniqueId();
                    String category = plugin.getPunishmentManager().getSessionCategory(uuid);
                    String violation = plugin.getPunishmentManager().getSessionViolation(uuid);

                    sender.sendMessage("§6[Staff] §eCurrent punishment session data:§r");
                    sender.sendMessage("§eCategory: §f" + (category != null ? category : "None"));
                    sender.sendMessage("§eViolation: §f" + (violation != null ? violation : "None"));
                } else {
                    sender.sendMessage("§cThis command can only be used by players.");
                }
                break;

            case "categories":
                sender.sendMessage("§6[Staff] §eAvailable punishment categories:§r");
                List<String> categoryDisplayNames = plugin.getPunishmentManager().getCategoryDisplayNames();
                for (String categoryInfo : categoryDisplayNames) {
                    sender.sendMessage("§e- " + categoryInfo);
                }
                break;

            case "violations":
                if (args.length < 2) {
                    sender.sendMessage("§cUsage: /punishdebug violations <category>");
                    return true;
                }

                String categoryKey = args[1].toLowerCase();
                sender.sendMessage("§6[Staff] §eViolations in category '" + categoryKey + "':§r");

                List<String> violationNames = plugin.getPunishmentManager().getViolationNames(categoryKey);
                if (violationNames.isEmpty()) {
                    sender.sendMessage("§c- No violations found or category doesn't exist");
                } else {
                    for (String violationInfo : violationNames) {
                        sender.sendMessage("§e- " + violationInfo);
                    }
                }
                break;

            case "clear":
                if (sender instanceof Player player) {
                    // This would need to be added to PunishmentManager
                    plugin.getPunishmentManager().clearSession(player.getUniqueId());
                    sender.sendMessage("§6[Staff] §eYour punishment session has been cleared.");
                } else {
                    sender.sendMessage("§cThis command can only be used by players.");
                }
                break;

            default:
                sender.sendMessage("§cUnknown debug command. Use /punishdebug for help.");
                break;
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return Arrays.asList("session", "categories", "violations", "clear");
        } else if (args.length == 2 && args[0].equalsIgnoreCase("violations")) {
            // Use the new simplified method instead
            return plugin.getPunishmentManager().getCategoryNames();
        }
        return null;
    }
}
