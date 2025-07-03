    package com.smokypeaks.server.managers;

    import com.smokypeaks.Main;
    import com.smokypeaks.global.automod.AutoModRule;
    import com.smokypeaks.global.automod.PlayerViolationRecord;
    import com.smokypeaks.global.automod.PunishmentType;
    import com.smokypeaks.global.automod.ViolationType;
    import com.smokypeaks.server.listeners.automod.chat.AutoModChatListener;
    import org.bukkit.Bukkit;
    import org.bukkit.entity.Player;

    import java.util.*;

    public class AutoModManager {
        private final Main plugin;
        private AutoModChatListener chatListener;
        private final Map<UUID, PlayerViolationRecord> violationRecords = new HashMap<>();
        private final List<AutoModRule> rules = new ArrayList<>();
        private boolean enabled = true;
        private String notificationTarget = "BlakeOO2"; // Default notification target

        public AutoModManager(Main plugin) {
            this.plugin = plugin;
            initializeRules();
        }

        /**
         * Initialize the AutoMod rules
         */
        private void initializeRules() {
            // Chat Behavior Rules
            addRule(new AutoModRule(ViolationType.HARASSMENT_DISRESPECT, "Harassment or Disrespect", PunishmentType.WARN, 0));
            addRule(new AutoModRule(ViolationType.HARASSMENT_DISRESPECT, "Harassment or Disrespect", PunishmentType.TEMP_MUTE, 60)); // 1 hour
            addRule(new AutoModRule(ViolationType.HARASSMENT_DISRESPECT, "Harassment or Disrespect", PunishmentType.PERMANENT_BAN, 0));

            addRule(new AutoModRule(ViolationType.CHAT_ABUSE, "Chat Abuse or Spam", PunishmentType.WARN, 0));
            addRule(new AutoModRule(ViolationType.CHAT_ABUSE, "Chat Abuse or Spam", PunishmentType.TEMP_MUTE, 60)); // 1 hour
            addRule(new AutoModRule(ViolationType.CHAT_ABUSE, "Chat Abuse or Spam", PunishmentType.TEMP_MUTE, 1440)); // 24 hours
            addRule(new AutoModRule(ViolationType.CHAT_ABUSE, "Chat Abuse or Spam", PunishmentType.PERMANENT_MUTE, 0));

            addRule(new AutoModRule(ViolationType.THREATS, "Threats, Doxing, or Malicious Links", PunishmentType.PERMANENT_BAN, 0));

            addRule(new AutoModRule(ViolationType.IMPERSONATION, "Impersonation", PunishmentType.WARN, 0));
            addRule(new AutoModRule(ViolationType.IMPERSONATION, "Impersonation", PunishmentType.TEMP_BAN, 1440)); // 24 hours
            addRule(new AutoModRule(ViolationType.IMPERSONATION, "Impersonation", PunishmentType.PERMANENT_BAN, 0));

            // Gameplay Violation Rules
            addRule(new AutoModRule(ViolationType.UNFAIR_ADVANTAGE, "Unfair Mods/Macros/Scripts", PunishmentType.TEMP_BAN, 1440)); // 24 hours
            addRule(new AutoModRule(ViolationType.UNFAIR_ADVANTAGE, "Unfair Mods/Macros/Scripts", PunishmentType.PERMANENT_BAN, 0));

            addRule(new AutoModRule(ViolationType.CHEATING, "X-Ray/Freecam/Cheats", PunishmentType.PERMANENT_BAN, 0));

            addRule(new AutoModRule(ViolationType.BUG_ABUSE, "Bug Abuse/Duping", PunishmentType.PERMANENT_BAN, 0));

            addRule(new AutoModRule(ViolationType.NON_CONSENSUAL_PVP, "Non-Consensual PvP", PunishmentType.WARN, 0));
            addRule(new AutoModRule(ViolationType.NON_CONSENSUAL_PVP, "Non-Consensual PvP", PunishmentType.TEMP_BAN, 1440)); // 24 hours
            addRule(new AutoModRule(ViolationType.NON_CONSENSUAL_PVP, "Non-Consensual PvP", PunishmentType.PERMANENT_BAN, 0));

            addRule(new AutoModRule(ViolationType.GRIEFING, "Griefing or Stealing", PunishmentType.TEMP_BAN, 1440)); // 24 hours
            addRule(new AutoModRule(ViolationType.GRIEFING, "Griefing or Stealing", PunishmentType.PERMANENT_BAN, 0));

            addRule(new AutoModRule(ViolationType.BUILDING_TOO_CLOSE, "Building Too Close", PunishmentType.WARN, 0));
            addRule(new AutoModRule(ViolationType.BUILDING_TOO_CLOSE, "Building Too Close", PunishmentType.TEMP_BAN, 1440)); // 24 hours

            addRule(new AutoModRule(ViolationType.LAG_INDUCING, "Lag-Inducing Builds", PunishmentType.WARN, 0));
            addRule(new AutoModRule(ViolationType.LAG_INDUCING, "Lag-Inducing Builds", PunishmentType.TEMP_BAN, 1440)); // 24 hours

            // Economy & Trading Rules
            addRule(new AutoModRule(ViolationType.SCAMMING, "Scamming", PunishmentType.WARN, 0));
            addRule(new AutoModRule(ViolationType.SCAMMING, "Scamming", PunishmentType.TEMP_BAN, 1440)); // 24 hours
            addRule(new AutoModRule(ViolationType.SCAMMING, "Scamming", PunishmentType.PERMANENT_BAN, 0));

            addRule(new AutoModRule(ViolationType.IRL_TRADING, "IRL Trading", PunishmentType.PERMANENT_BAN, 0));

            addRule(new AutoModRule(ViolationType.THIRD_PARTY_TRADING, "Third-Party Trading", PunishmentType.PERMANENT_BAN, 0));

            // Inappropriate Content Rules
            addRule(new AutoModRule(ViolationType.INAPPROPRIATE_APPEARANCE, "Inappropriate Skin/Username/Cape", PunishmentType.WARN, 0));
            addRule(new AutoModRule(ViolationType.INAPPROPRIATE_APPEARANCE, "Inappropriate Skin/Username/Cape", PunishmentType.TEMP_BAN, 1440)); // 24 hours

            addRule(new AutoModRule(ViolationType.INAPPROPRIATE_CONTENT, "Inappropriate Builds/Messages", PunishmentType.WARN, 0));
            addRule(new AutoModRule(ViolationType.INAPPROPRIATE_CONTENT, "Inappropriate Builds/Messages", PunishmentType.TEMP_BAN, 1440)); // 24 hours
            addRule(new AutoModRule(ViolationType.INAPPROPRIATE_CONTENT, "Inappropriate Builds/Messages", PunishmentType.PERMANENT_BAN, 0));
        }

        /**
         * Add a rule to the AutoMod system
         *
         * @param rule The rule to add
         */
        public void addRule(AutoModRule rule) {
            rules.add(rule);
        }

        /**
         * Process a rule violation
         *
         * @param playerUUID    The UUID of the player who violated the rule
         * @param violationType The type of violation
         * @param details       Additional details about the violation
         */
        public void processViolation(UUID playerUUID, ViolationType violationType, String details) {
            if (!enabled) return;

            // Get or create violation record for this player
            PlayerViolationRecord record = violationRecords.computeIfAbsent(playerUUID, k -> new PlayerViolationRecord());

            // Record this violation
            record.addViolation(violationType);

            // Get player information
            Player player = Bukkit.getPlayer(playerUUID);
            if (player == null) return; // Player is offline

            // Determine appropriate punishment based on violation count
            int violationCount = record.getViolationCount(violationType);
            AutoModRule rule = findRuleForViolation(violationType, violationCount);

            if (rule != null) {
                sendNotification(player, rule, details);
            }
        }

        /**
         * Find the appropriate rule for a violation based on count
         *
         * @param type  Violation type
         * @param count Violation count
         * @return The rule to apply, or null if no rule applies
         */
        private AutoModRule findRuleForViolation(ViolationType type, int count) {
            List<AutoModRule> matchingRules = rules.stream()
                    .filter(rule -> rule.getViolationType() == type)
                    .toList();

            if (matchingRules.isEmpty()) return null;

            // Get the rule corresponding to the violation count (0-indexed)
            int index = Math.min(count - 1, matchingRules.size() - 1);
            if (index < 0) index = 0;

            return matchingRules.get(index);
        }

        /**
         * Send a notification to the target staff member
         *
         * @param violator The player who violated the rule
         * @param rule     The rule that was violated
         * @param details  Additional details about the violation
         */
        private void sendNotification(Player violator, AutoModRule rule, String details) {
            Player notificationPlayer = Bukkit.getPlayer(notificationTarget);
            if (notificationPlayer == null) return; // Target is offline

            String actionStr = getActionString(rule.getPunishmentType());
            String durationStr = getDurationString(rule.getPunishmentType(), rule.getDuration());

            String message = String.format(
                    "§8[§cAutoWatch§8] §7I would %s §f%s §7for §f%s§7%s",
                    actionStr,
                    violator.getName(),
                    rule.getDescription(),
                    durationStr
            );

            if (details != null && !details.isEmpty()) {
                message += "\n§8[§cAutoWatch§8] §7Details: §f" + details;
            }

            notificationPlayer.sendMessage(message);
        }

        /**
         * Get string representation of punishment action
         */
        private String getActionString(PunishmentType type) {
            return switch (type) {
                case WARN -> "§ewarn";
                case KICK -> "§ekick";
                case TEMP_MUTE -> "§6mute";
                case PERMANENT_MUTE -> "§cpermanently mute";
                case TEMP_BAN -> "§6ban";
                case PERMANENT_BAN -> "§cpermanently ban";
            };
        }

        /**
         * Get string representation of punishment duration
         */
        private String getDurationString(PunishmentType type, int minutes) {
            if (type == PunishmentType.PERMANENT_BAN || type == PunishmentType.PERMANENT_MUTE || minutes <= 0) {
                return "";
            }

            if (minutes < 60) {
                return String.format(" for §f%d minutes", minutes);
            } else if (minutes < 1440) { // Less than 24 hours
                return String.format(" for §f%d hour%s", minutes / 60, minutes / 60 == 1 ? "" : "s");
            } else { // Days
                return String.format(" for §f%d day%s", minutes / 1440, minutes / 1440 == 1 ? "" : "s");
            }
        }

        /**
         * Get the AutoModChatListener instance
         *
         * @return The AutoModChatListener instance
         */
        public AutoModChatListener getChatListener() {
            return chatListener;
        }

        /**
         * Set the AutoModChatListener instance
         *
         * @param listener The AutoModChatListener instance
         */
        public void setChatListener(AutoModChatListener listener) {
            this.chatListener = listener;
        }

        /**
         * Set the notification target player name
         *
         * @param playerName The name of the player to receive notifications
         */
        public void setNotificationTarget(String playerName) {
            this.notificationTarget = playerName;
        }

        /**
         * Get the notification target player name
         *
         * @return The name of the player who receives notifications
         */
        public String getNotificationTarget() {
            return notificationTarget;
        }

        /**
         * Enable or disable the AutoMod system
         *
         * @param enabled Whether the system should be enabled
         */
        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        /**
         * Check if the AutoMod system is enabled
         *
         * @return Whether the system is enabled
         */
        public boolean isEnabled() {
            return enabled;
        }

        /**
         * Clear violation records for a player
         *
         * @param playerUUID The UUID of the player
         */
        public void clearViolations(UUID playerUUID) {
            violationRecords.remove(playerUUID);
        }

        /**
         * Clear all violation records
         */
        public void clearAllViolations() {
            violationRecords.clear();
        }

        ;
    }
