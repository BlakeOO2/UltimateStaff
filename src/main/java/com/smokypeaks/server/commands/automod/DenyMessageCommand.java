package com.smokypeaks.server.commands.automod;

import com.smokypeaks.Main;
import com.smokypeaks.global.permissions.StaffPermissions;
import com.smokypeaks.server.listeners.automod.chat.AutoModChatListener;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.Collections;
import java.util.List;

/**
 * Command to mark a message or phrase as inappropriate/denied for AutoMod learning
 */
public class DenyMessageCommand implements CommandExecutor, TabCompleter {
    private final Main plugin;

    public DenyMessageCommand(Main plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Check permission
        if (!sender.hasPermission(StaffPermissions.Admin.AUTOMOD_DENY)) {
            sender.sendMessage(ChatColor.RED + "You don't have permission to use this command.");
            return true;
        }

        // Check if we have at least one argument (the message to deny)
        if (args.length == 0) {
            sender.sendMessage(ChatColor.RED + "Usage: /denymessage <message>");
            return true;
        }

        // Combine args to get the full message
        StringBuilder message = new StringBuilder();
        for (String arg : args) {
            message.append(arg).append(" ");
        }
        String fullMessage = message.toString().trim();

        // Get the AutoModChatListener and deny the message
        AutoModChatListener chatListener = getChatListener();
        if (chatListener == null) {
            sender.sendMessage(ChatColor.RED + "Error: Unable to access AutoMod chat listener.");
            return true;
        }

        boolean success = chatListener.addDeniedPhrase(fullMessage);
        if (success) {
            sender.sendMessage(ChatColor.GREEN + "Successfully added \"" + fullMessage + "\" to the denied phrases list.");
            sender.sendMessage(ChatColor.YELLOW + "AutoMod will now treat similar messages as inappropriate.");
        } else {
            sender.sendMessage(ChatColor.RED + "Failed to add phrase to denied list. It may already be denied.");
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        // No tab completion for message content
        return Collections.emptyList();
    }

    /**
     * Get the AutoModChatListener instance from the plugin
     * @return The AutoModChatListener instance, or null if not found
     */
    private AutoModChatListener getChatListener() {
        // Access the AutoModChatListener through the plugin's manager
        return plugin.getAutoModManager().getChatListener();
    }
}
