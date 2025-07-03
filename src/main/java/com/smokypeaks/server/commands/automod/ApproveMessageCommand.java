package com.smokypeaks.server.commands.automod;

import com.smokypeaks.Main;
import com.smokypeaks.server.listeners.automod.chat.AutoModChatListener;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Command to approve a message that was incorrectly flagged by AutoMod
 */
public class ApproveMessageCommand implements CommandExecutor, TabCompleter {
    private final Main plugin;

    public ApproveMessageCommand(Main plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("ultimatestaff.automod.approve")) {
            sender.sendMessage(ChatColor.RED + "You don't have permission to use this command.");
            return true;
        }

        if (args.length < 1) {
            sender.sendMessage(ChatColor.RED + "Usage: /approveMessage <message>");
            return true;
        }

        // Combine all arguments into a single message
        StringBuilder messageBuilder = new StringBuilder();
        for (int i = 0; i < args.length; i++) {
            messageBuilder.append(args[i]);
            if (i < args.length - 1) messageBuilder.append(" ");
        }
        String message = messageBuilder.toString();

        // Get the AutoModChatListener instance
        AutoModChatListener chatListener = getChatListener();
        if (chatListener == null) {
            sender.sendMessage(ChatColor.RED + "AutoMod chat system is not available.");
            return true;
        }

        // Approve the message
        chatListener.approveMessage(message);

        sender.sendMessage(ChatColor.GREEN + "Message approved and added to the learning system.");
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
