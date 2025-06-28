package com.smokypeaks.server.listeners;

import com.smokypeaks.Main;
import com.smokypeaks.global.automod.ViolationType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChatEvent;

import java.util.*;
import java.util.regex.Pattern;

public class AutoModChatListener implements Listener {
    private final Main plugin;

    // Chat filters
    private final Pattern harassmentPattern = Pattern.compile(
            "(?i).*(retard|kys|kill yourself|suicide|idiot|moron|dumb|trash|garbage|noob|loser|suck|kys|trash player).*"
    );

    private final Pattern spamPattern = Pattern.compile(
            "(?i).*(\\b(\\w+)\\s+\\2\\b|.*(.)\\3{3,}.*)"
    );

    private final Pattern threatPattern = Pattern.compile(
            "(?i).*(ddos|dox|hack|swat|find you|ip|address|leak|download this).*"
    );

    private final Pattern impersonationPattern = Pattern.compile(
            "(?i).*(staff|admin|mod|owner|developer|dev).*(am|i am|i'm).*"
    );

    private final Map<UUID, LinkedList<Long>> chatTimes = new HashMap<>();
    private static final int SPAM_THRESHOLD = 5; // messages
    private static final int SPAM_WINDOW = 5000; // 5 seconds

    public AutoModChatListener(Main plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerChat(PlayerChatEvent event) {
        Player player = event.getPlayer();
        String message = event.getMessage();

        // Skip if player has bypass permission
        if (player.hasPermission("ultimatestaff.automod.bypass")) return;

        // Check for spam (message frequency)
        if (isSpamming(player)) {
            plugin.getAutoModManager().processViolation(
                    player.getUniqueId(),
                    ViolationType.CHAT_ABUSE,
                    "Message rate too high: " + message
            );
        }

        // Check for harassment/disrespect
        if (harassmentPattern.matcher(message).matches()) {
            plugin.getAutoModManager().processViolation(
                    player.getUniqueId(),
                    ViolationType.HARASSMENT_DISRESPECT,
                    "Detected disrespectful terms: " + message
            );
        }

        // Check for repeated words/characters (spam)
        if (spamPattern.matcher(message).matches()) {
            plugin.getAutoModManager().processViolation(
                    player.getUniqueId(),
                    ViolationType.CHAT_ABUSE,
                    "Repeated text pattern: " + message
            );
        }

        // Check for threats
        if (threatPattern.matcher(message).matches()) {
            plugin.getAutoModManager().processViolation(
                    player.getUniqueId(),
                    ViolationType.THREATS,
                    "Potential threat detected: " + message
            );
        }

        // Check for impersonation
        if (impersonationPattern.matcher(message).matches() && !player.hasPermission("ultimatestaff.staff")) {
            plugin.getAutoModManager().processViolation(
                    player.getUniqueId(),
                    ViolationType.IMPERSONATION,
                    "Potential staff impersonation: " + message
            );
        }
    }

    /**
     * Check if a player is sending messages too quickly
     * @param player The player to check
     * @return True if the player is spamming, false otherwise
     */
    private boolean isSpamming(Player player) {
        UUID playerUUID = player.getUniqueId();
        long currentTime = System.currentTimeMillis();

        // Get or create message history for this player
        LinkedList<Long> messageTimes = chatTimes.computeIfAbsent(playerUUID, k -> new LinkedList<>());

        // Add current message time
        messageTimes.add(currentTime);

        // Remove messages outside the time window
        while (!messageTimes.isEmpty() && currentTime - messageTimes.peekFirst() > SPAM_WINDOW) {
            messageTimes.removeFirst();
        }

        // Check if message count exceeds threshold
        return messageTimes.size() >= SPAM_THRESHOLD;
    }
}
