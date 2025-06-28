package com.smokypeaks.server.commands;

import com.smokypeaks.Main;
import com.smokypeaks.global.permissions.StaffPermissions;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class ChatFilterCommand implements CommandExecutor, TabCompleter {
    private final Main plugin;

    public ChatFilterCommand(Main plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Check permission
        if (!sender.hasPermission(StaffPermissions.Admin.CHAT_FILTER)) {
            sender.sendMessage("§cYou don't have permission to use this command!");
            return true;
        }

        // Handle subcommands
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "add" -> handleAdd(sender, args);
            case "remove" -> handleRemove(sender, args);
            case "list" -> handleList(sender, args);
            case "toggle" -> handleToggle(sender, args);
            case "replacement" -> handleReplacement(sender, args);
            case "reload" -> handleReload(sender);
            default -> sendHelp(sender);
        }

        return true;
    }

    private void handleAdd(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage("§cUsage: /chatfilter add <word>");
            return;
        }

        String word = args[1].toLowerCase();
        boolean added = plugin.getChatFilterManager().addWordToFilter(word, true);

        if (added) {
            sender.sendMessage("§6[Chat Filter] §eAdded '" + word + "' to the filter.");
        } else {
            sender.sendMessage("§6[Chat Filter] §e'" + word + "' is already in the filter.");
        }
    }

    private void handleRemove(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage("§cUsage: /chatfilter remove <word>");
            return;
        }

        String word = args[1].toLowerCase();
        boolean removed = plugin.getChatFilterManager().removeWordFromFilter(word);

        if (removed) {
            sender.sendMessage("§6[Chat Filter] §eRemoved '" + word + "' from the filter.");
        } else {
            sender.sendMessage("§6[Chat Filter] §e'" + word + "' is not in the filter.");
        }
    }

    private void handleList(CommandSender sender, String[] args) {
        int page = 1;
        if (args.length > 1) {
            try {
                page = Integer.parseInt(args[1]);
            } catch (NumberFormatException e) {
                sender.sendMessage("§cInvalid page number.");
                return;
            }
        }

        List<String> words = plugin.getChatFilterManager().getFilteredWords();
        int totalPages = (int) Math.ceil(words.size() / 10.0);
        page = Math.max(1, Math.min(page, totalPages));

        sender.sendMessage("§6[Chat Filter] §eFiltered Words (Page " + page + "/" + totalPages + "):");

        int start = (page - 1) * 10;
        int end = Math.min(start + 10, words.size());

        for (int i = start; i < end; i++) {
            sender.sendMessage("§7- §f" + words.get(i));
        }

        if (page < totalPages) {
            sender.sendMessage("§7Use /chatfilter list " + (page + 1) + " to see the next page.");
        }

        sender.sendMessage("§7Filter is currently " + 
                (plugin.getChatFilterManager().isFilterEnabled() ? "§aenabled" : "§cdisabled") + 
                "§7. Replacement: '" + plugin.getChatFilterManager().getReplacementString() + "'");
    }

    private void handleToggle(CommandSender sender, String[] args) {
        boolean currentState = plugin.getChatFilterManager().isFilterEnabled();
        plugin.getChatFilterManager().setFilterEnabled(!currentState);

        sender.sendMessage("§6[Chat Filter] §eFilter is now " + 
                (plugin.getChatFilterManager().isFilterEnabled() ? "§aenabled" : "§cdisabled") + "§e.");
    }

    private void handleReplacement(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage("§cUsage: /chatfilter replacement <text>");
            sender.sendMessage("§7Current replacement: '" + plugin.getChatFilterManager().getReplacementString() + "'");
            return;
        }

        String replacement = args[1];
        plugin.getChatFilterManager().setReplacementString(replacement);

        sender.sendMessage("§6[Chat Filter] §eReplacement string set to '" + replacement + "'.");
    }

    private void handleReload(CommandSender sender) {
        plugin.getChatFilterManager().loadFilterConfig();
        sender.sendMessage("§6[Chat Filter] §eChat filter reloaded.");
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage("§6===== Chat Filter Commands =====§r");
        sender.sendMessage("§e/chatfilter add <word> §7- Add a word to the filter");
        sender.sendMessage("§e/chatfilter remove <word> §7- Remove a word from the filter");
        sender.sendMessage("§e/chatfilter list [page] §7- List filtered words");
        sender.sendMessage("§e/chatfilter toggle §7- Toggle filter on/off");
        sender.sendMessage("§e/chatfilter replacement <text> §7- Set replacement text");
        sender.sendMessage("§e/chatfilter reload §7- Reload chat filter config");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission(StaffPermissions.Admin.CHAT_FILTER)) {
            return new ArrayList<>();
        }

        if (args.length == 1) {
            List<String> completions = Arrays.asList("add", "remove", "list", "toggle", "replacement", "reload");
            return completions.stream()
                    .filter(s -> s.toLowerCase().startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }

        if (args.length == 2) {
            if (args[0].equalsIgnoreCase("remove")) {
                List<String> words = plugin.getChatFilterManager().getFilteredWords();
                return words.stream()
                        .filter(s -> s.toLowerCase().startsWith(args[1].toLowerCase()))
                        .collect(Collectors.toList());
            }
        }

        return new ArrayList<>();
    }
}
