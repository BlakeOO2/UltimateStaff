package com.smokypeaks.server.listeners.automod.chat;

import com.smokypeaks.Main;
import com.smokypeaks.global.automod.ViolationType;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChatEvent;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Advanced chat monitoring system with context awareness and learning capabilities
 */
public class AutoModChatListener implements Listener {
    private final Main plugin;

    // Base detection patterns
    private final Pattern harassmentPattern;
    private final Pattern spamPattern;
    private final Pattern threatPattern;
    private final Pattern impersonationPattern;
    private final Pattern capsPattern;

    // Context-aware detection data structures
    private final Map<UUID, MessageContext> playerContexts = new ConcurrentHashMap<>();
    private final Map<String, Integer> approvedPhrases = new ConcurrentHashMap<>();
    private Map<String, Long> deniedPhrases = new ConcurrentHashMap<>();
    private final Set<String> flaggedPhrases = new HashSet<>();

    // Player message history tracking
    private final Map<UUID, LinkedList<ChatMessage>> messageHistory = new ConcurrentHashMap<>();
    private final Map<UUID, LinkedList<Long>> chatTimes = new ConcurrentHashMap<>();

    // Thresholds and settings
    private static final int SPAM_THRESHOLD = 5; // messages
    private static final int SPAM_WINDOW = 5000; // 5 seconds
    private static final int MESSAGE_HISTORY_SIZE = 10;
    private static final int CAPS_THRESHOLD_PERCENT = 70;
    private static final int MIN_CAPS_LENGTH = 8;
    private static final float SIMILARITY_THRESHOLD = 0.85f;
    private static final int MAX_APPROVED_PHRASES = 2000;

    // Machine learning data file paths
    private final String dataFolder;
    private final String approvedPhrasesFile;
    private final String deniedPhrasesFile;
    private final String flaggedPhrasesFile;

    public AutoModChatListener(Main plugin) {
        this.plugin = plugin;
        // Register this listener with the AutoModManager
        plugin.getAutoModManager().setChatListener(this);

        // Initialize data folder and files
        this.dataFolder = plugin.getDataFolder() + File.separator + "automod" + File.separator + "chat";
        this.approvedPhrasesFile = dataFolder + File.separator + "approved_phrases.dat";
        this.deniedPhrasesFile = dataFolder + File.separator + "denied_phrases.dat";
        this.flaggedPhrasesFile = dataFolder + File.separator + "flagged_phrases.dat";

        // Initialize data directories
        initializeDataDirectories();

        // Load learned data
        loadApprovedPhrases();
        this.loadDeniedPhrases();
        loadFlaggedPhrases();

        // Initialize regex patterns
        this.harassmentPattern = buildHarassmentPattern();
        this.spamPattern = Pattern.compile("(?i).*(\\b(\\w+)\\s+\\2\\b|.*(.)\\3{3,}.*)");
        this.threatPattern = buildThreatPattern();
        this.impersonationPattern = Pattern.compile("(?i).*(staff|admin|mod|owner|developer|dev).*(am|i am|i'm).*");
        this.capsPattern = Pattern.compile("[A-Z]{5,}");

        // Schedule periodic save of learned data
        Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, this::saveLearnedData, 12000L, 12000L); // Every 10 minutes
    }

    /**
     * Initialize data directories for storing machine learning data
     */
    private void initializeDataDirectories() {
        try {
            Path path = Paths.get(dataFolder);
            if (!Files.exists(path)) {
                Files.createDirectories(path);
                plugin.getLogger().info("Created AutoMod chat data directory");
            }
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to create AutoMod chat data directory: " + e.getMessage());
        }
    }

    /**
     * Build enhanced harassment detection pattern with dynamic severity weighting
     */
    private Pattern buildHarassmentPattern() {
        // Categorize harassment terms by severity
        String highSeverity = "retard|nigger|faggot|kys|kill yourself|suicide|neck yourself";
        String mediumSeverity = "idiot|moron|stupid|dumb|cunt|bitch";
        String lowSeverity = "noob|trash|garbage|suck|loser";

        // Combine patterns with context awareness
        return Pattern.compile("(?i).*(" + highSeverity + "|" + mediumSeverity + "|" + lowSeverity + ").*");
    }

    /**
     * Build enhanced threat detection pattern
     */
    private Pattern buildThreatPattern() {
        String threatTerms = "ddos|dox|hack|swat|find you|ip|address|leak|download this|come to your";
        String technicalThreats = "rat|token|logger|grabber|keylogger|steal|account";

        return Pattern.compile("(?i).*(" + threatTerms + "|" + technicalThreats + ").*");
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerChat(PlayerChatEvent event) {
        Player player = event.getPlayer();
        String message = event.getMessage();
        UUID playerUUID = player.getUniqueId();

        // Skip if player has bypass permission
        if (player.hasPermission("ultimatestaff.automod.bypass")) return;

        // Update player context with new message
        updatePlayerContext(playerUUID, message);

        // Skip checking if the exact message or very similar one has been approved before
        if (isApprovedPhrase(message)) {
            return;
        }

        // Check for spam by message frequency
        if (isSpammingByFrequency(player)) {
            plugin.getAutoModManager().processViolation(
                    playerUUID,
                    ViolationType.CHAT_ABUSE,
                    "Message rate too high: " + message
            );
            return;
        }

        // Check for spam by repetitive content
        if (isSpammingByContent(playerUUID, message)) {
            plugin.getAutoModManager().processViolation(
                    playerUUID,
                    ViolationType.CHAT_ABUSE,
                    "Repetitive message content: " + message
            );
            return;
        }

        // Check for excessive caps
        if (hasExcessiveCaps(message)) {
            plugin.getAutoModManager().processViolation(
                    playerUUID,
                    ViolationType.CHAT_ABUSE,
                    "Excessive use of capital letters: " + message
            );
            return;
        }

        // Context-aware harassment check
        HarassmentResult harassmentResult = checkForHarassment(message, playerContexts.get(playerUUID));
        if (harassmentResult.isHarassment) {
            // Add to flagged phrases for future detection
            flaggedPhrases.add(normalizeMessage(message));

            plugin.getAutoModManager().processViolation(
                    playerUUID,
                    ViolationType.HARASSMENT_DISRESPECT,
                    "Detected disrespectful content: " + message + 
                    " (Matched: " + harassmentResult.matchedTerm + ", Context: " + harassmentResult.context + ")"
            );
            return;
        }

        // Check for threats with context awareness
        if (threatPattern.matcher(message).find() && isContextualThreat(message, playerContexts.get(playerUUID))) {
            // Add to flagged phrases for future detection
            flaggedPhrases.add(normalizeMessage(message));

            plugin.getAutoModManager().processViolation(
                    playerUUID,
                    ViolationType.THREATS,
                    "Potential threat detected: " + message
            );
            return;
        }

        // Check for impersonation with context
        if (impersonationPattern.matcher(message).find() && !player.hasPermission("ultimatestaff.staff")) {
            plugin.getAutoModManager().processViolation(
                    playerUUID,
                    ViolationType.IMPERSONATION,
                    "Potential staff impersonation: " + message
            );
            return;
        }

        // Check against previously flagged phrases with fuzzy matching
        if (isSimilarToFlaggedPhrase(message)) {
            plugin.getAutoModManager().processViolation(
                    playerUUID,
                    ViolationType.CHAT_ABUSE,
                    "Message similar to previously flagged content: " + message
            );
            return;
        }
    }

    /**
     * Update the context for a player based on their latest message
     * @param playerUUID The player's UUID
     * @param message The message the player sent
     */
    private void updatePlayerContext(UUID playerUUID, String message) {
        // Get or create context
        MessageContext context = playerContexts.computeIfAbsent(playerUUID, k -> new MessageContext());

        // Update context with new message
        context.addMessage(message);

        // Update message history
        LinkedList<ChatMessage> history = messageHistory.computeIfAbsent(playerUUID, k -> new LinkedList<>());
        history.add(new ChatMessage(message, System.currentTimeMillis()));

        // Trim history if needed
        while (history.size() > MESSAGE_HISTORY_SIZE) {
            history.removeFirst();
        }

        // Update chat times for spam detection
        LinkedList<Long> times = chatTimes.computeIfAbsent(playerUUID, k -> new LinkedList<>());
        times.add(System.currentTimeMillis());

        // Remove times outside the window
        while (!times.isEmpty() && System.currentTimeMillis() - times.getFirst() > SPAM_WINDOW) {
            times.removeFirst();
        }
    }

    /**
     * Check if a player is sending messages too quickly
     * @param player The player to check
     * @return True if the player is spamming, false otherwise
     */
    private boolean isSpammingByFrequency(Player player) {
        LinkedList<Long> times = chatTimes.get(player.getUniqueId());
        return times != null && times.size() >= SPAM_THRESHOLD;
    }

    /**
     * Check if a player is sending repetitive content
     * @param playerUUID The player's UUID
     * @param currentMessage The current message to check
     * @return True if the player is repeating content, false otherwise
     */
    private boolean isSpammingByContent(UUID playerUUID, String currentMessage) {
        LinkedList<ChatMessage> history = messageHistory.get(playerUUID);
        if (history == null || history.size() < 2) return false;

        // Check for repeated identical messages
        int identicalCount = 0;
        String normalizedCurrent = normalizeMessage(currentMessage);

        for (ChatMessage pastMessage : history) {
            String normalizedPast = normalizeMessage(pastMessage.content);

            // Check for exact matches or high similarity
            if (normalizedPast.equals(normalizedCurrent) || 
                calculateSimilarity(normalizedPast, normalizedCurrent) > SIMILARITY_THRESHOLD) {
                identicalCount++;
                if (identicalCount >= 2) {
                    return true;
                }
            }
        }

        // Check if message matches basic spam pattern
        return spamPattern.matcher(currentMessage).matches();
    }

    /**
     * Check if a message has excessive capital letters
     * @param message The message to check
     * @return True if the message has too many capital letters, false otherwise
     */
    private boolean hasExcessiveCaps(String message) {
        if (message.length() < MIN_CAPS_LENGTH) return false;

        int capsCount = 0;
        for (char c : message.toCharArray()) {
            if (Character.isUpperCase(c)) capsCount++;
        }

        // Calculate percentage of capital letters
        float capsPercentage = (float) capsCount / message.length() * 100;
        return capsPercentage >= CAPS_THRESHOLD_PERCENT;
    }

    /**
     * Check if a message contains harassment with context awareness
     * @param message The message to check
     * @param context The player's message context
     * @return Result containing harassment status, matched term, and context
     */
    private HarassmentResult checkForHarassment(String message, MessageContext context) {
        Matcher matcher = harassmentPattern.matcher(message.toLowerCase());
        if (matcher.find()) {
            String matchedTerm = matcher.group(1);

            // Get surrounding words for context
            String[] words = message.toLowerCase().split("\\s+");
            int matchIndex = -1;

            for (int i = 0; i < words.length; i++) {
                if (words[i].contains(matchedTerm)) {
                    matchIndex = i;
                    break;
                }
            }

            // Extract context (words before and after the matched term)
            StringBuilder contextBuilder = new StringBuilder();
            if (matchIndex > 0) {
                contextBuilder.append(words[matchIndex - 1]).append(" ");
            }
            contextBuilder.append(matchedTerm);
            if (matchIndex < words.length - 1) {
                contextBuilder.append(" ").append(words[matchIndex + 1]);
            }

            String extractedContext = contextBuilder.toString();

            // Check for benign contexts
            if (isBenignContext(matchedTerm, extractedContext, message)) {
                return new HarassmentResult(false, matchedTerm, extractedContext);
            }

            return new HarassmentResult(true, matchedTerm, extractedContext);
        }

        return new HarassmentResult(false, "", "");
    }

    /**
     * Check if a potentially offensive term is being used in a benign context
     * @param term The potentially offensive term
     * @param context The surrounding context
     * @param fullMessage The full message
     * @return True if the context is benign, false otherwise
     */
    private boolean isBenignContext(String term, String context, String fullMessage) {
        // Check if this exact context has been explicitly denied before
        if (deniedPhrases.containsKey(context)) {
            return false;
        }

        // Check if this exact context has been approved before
        if (approvedPhrases.containsKey(context)) {
            return true;
        }

        // Case-specific benign contexts
        switch (term.toLowerCase()) {
            case "stupid":
                return context.contains("not stupid") || 
                       context.contains("stupid idea") || 
                       context.contains("stupid mistake") || 
                       !isDirectedAtPlayer(fullMessage);

            case "idiot":
                return context.contains("not idiot") || 
                       !isDirectedAtPlayer(fullMessage);

            case "kill":
            case "kys":
                return context.contains("kill the") || 
                       context.contains("kill a") || 
                       context.contains("kill some") || 
                       context.contains("kill mob") || 
                       context.contains("monster") || 
                       context.contains("zombie") ||
                       context.contains("enderman") ||
                       context.contains("creeper") ||
                       context.contains("skeleton");

            default:
                return false;
        }
    }

    /**
     * Check if a message appears to be directed at another player
     * @param message The message to check
     * @return True if the message seems directed at a player, false otherwise
     */
    private boolean isDirectedAtPlayer(String message) {
        String lowerMessage = message.toLowerCase();

        // Check for second-person pronouns or player name prefixes
        if (lowerMessage.contains(" you ") || 
            lowerMessage.contains("you're") || 
            lowerMessage.contains("youre") || 
            lowerMessage.contains("your ") || 
            lowerMessage.contains(" u ") || 
            lowerMessage.contains(" ur ")) {
            return true;
        }

        // Check for direct addressing of online players
        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            if (lowerMessage.contains(onlinePlayer.getName().toLowerCase())) {
                return true;
            }
        }

        return false;
    }

    /**
     * Check if a message is a contextual threat
     * @param message The message to check
     * @param context The player's message context
     * @return True if the message is a contextual threat, false otherwise
     */
    private boolean isContextualThreat(String message, MessageContext context) {
        String lowerMessage = message.toLowerCase();

        // Not a threat if referring to in-game activities
        if (lowerMessage.contains("creeper") || 
            lowerMessage.contains("mob") || 
            lowerMessage.contains("minecraft") || 
            lowerMessage.contains("game") || 
            lowerMessage.contains("server ip") || 
            lowerMessage.contains("server address")) {
            return false;
        }

        // Check for more serious context indicating real threat
        return lowerMessage.contains("your") || 
               lowerMessage.contains("you") || 
               lowerMessage.contains("ur") || 
               lowerMessage.contains("u ") ||
               (context != null && context.containsPersonalReferences());
    }

    /**
     * Check if a message is similar to any previously flagged phrase
     * @param message The message to check
     * @return True if the message is similar to a flagged phrase, false otherwise
     */
    private boolean isSimilarToFlaggedPhrase(String message) {
        String normalized = normalizeMessage(message);

        for (String flagged : flaggedPhrases) {
            if (calculateSimilarity(normalized, flagged) > SIMILARITY_THRESHOLD) {
                return true;
            }
        }

        return false;
    }

    /**
     * Check if a message matches any approved phrase
     * @param message The message to check
     * @return True if the message matches an approved phrase, false otherwise
     */
    private boolean isApprovedPhrase(String message) {
        String normalized = normalizeMessage(message);

        // Direct lookup first
        if (approvedPhrases.containsKey(normalized)) {
            // Increment the usage count
            approvedPhrases.put(normalized, approvedPhrases.get(normalized) + 1);
            return true;
        }

        // Check for similarity to approved phrases
        for (String approved : approvedPhrases.keySet()) {
            if (calculateSimilarity(normalized, approved) > SIMILARITY_THRESHOLD) {
                // Increment the usage count of the similar phrase
                approvedPhrases.put(approved, approvedPhrases.get(approved) + 1);
                return true;
            }
        }

        return false;
    }

    /**
     * Calculate the similarity between two strings (Levenshtein distance based)
     * @param s1 First string
     * @param s2 Second string
     * @return Similarity score between 0.0 and 1.0
     */
    private float calculateSimilarity(String s1, String s2) {
        // Guard against very different length strings
        if (Math.abs(s1.length() - s2.length()) > Math.min(s1.length(), s2.length()) / 2) {
            return 0.0f;
        }

        int[] costs = new int[s2.length() + 1];
        for (int i = 0; i <= s1.length(); i++) {
            int lastValue = i;
            for (int j = 0; j <= s2.length(); j++) {
                if (i == 0) {
                    costs[j] = j;
                } else if (j > 0) {
                    int newValue = costs[j - 1];
                    if (s1.charAt(i - 1) != s2.charAt(j - 1)) {
                        newValue = Math.min(Math.min(newValue, lastValue), costs[j]) + 1;
                    }
                    costs[j - 1] = lastValue;
                    lastValue = newValue;
                }
            }
            if (i > 0) costs[s2.length()] = lastValue;
        }

        int levenshteinDistance = costs[s2.length()];
        int maxLength = Math.max(s1.length(), s2.length());

        return (maxLength - levenshteinDistance) / (float) maxLength;
    }

    /**
     * Normalize a message for comparison
     * @param message The message to normalize
     * @return Normalized message
     */
    private String normalizeMessage(String message) {
        // Remove extra spaces, convert to lowercase, and remove basic punctuation
        return message.trim()
                .toLowerCase()
                .replaceAll("\\s+", " ")
                .replaceAll("[.,!?;:]", "");
    }

    /**
     * Approve a message that was incorrectly flagged
     * @param message The message to approve
     */
    public void approveMessage(String message) {
        String normalized = normalizeMessage(message);

        // Remove from flagged phrases if present
        flaggedPhrases.remove(normalized);

        // Add to approved phrases with usage count of 1
        approvedPhrases.put(normalized, 1);

        // If we've exceeded the maximum approved phrases, remove least used ones
        if (approvedPhrases.size() > MAX_APPROVED_PHRASES) {
            // Find least used phrases
            List<Map.Entry<String, Integer>> entries = new ArrayList<>(approvedPhrases.entrySet());
            entries.sort(Map.Entry.comparingByValue());

            // Remove the 10% least used phrases
            int toRemove = Math.max(1, approvedPhrases.size() / 10);
            for (int i = 0; i < toRemove && i < entries.size(); i++) {
                approvedPhrases.remove(entries.get(i).getKey());
            }
        }

        // Save the updated data
        saveLearnedData();
    }

    /**
     * Save learned data to disk
     */
    public void saveLearnedData() {
        saveApprovedPhrases();
        this.saveDeniedPhrases();
        saveFlaggedPhrases();
    }

    /**
     * Save denied phrases to disk
     */
    private void saveDeniedPhrases() {
        try {
            // Create JSON string with Gson
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            String json = gson.toJson(deniedPhrases);

            // Write to file
            Files.write(Paths.get(deniedPhrasesFile), json.getBytes());
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save denied phrases: " + e.getMessage());
        }
    }

    /**
     * Load denied phrases from disk
     */
    private void loadDeniedPhrases() {
        Path path = Paths.get(deniedPhrasesFile);
        if (!Files.exists(path)) {
            // Ensure denied phrases file parent directory exists
            File deniedPhrasesDir = new File(deniedPhrasesFile).getParentFile();
            if (deniedPhrasesDir != null && !deniedPhrasesDir.exists()) {
                deniedPhrasesDir.mkdirs();
            }
            return;
        }

        try {
            String json = new String(Files.readAllBytes(path));
            Gson gson = new Gson();
            Type type = new TypeToken<Map<String, Long>>(){}.getType();
            Map<String, Long> loaded = gson.fromJson(json, type);

            if (loaded != null) {
                deniedPhrases.clear();
                deniedPhrases.putAll(loaded);
                plugin.getLogger().info("Loaded " + deniedPhrases.size() + " denied phrases");
            }
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to load denied phrases: " + e.getMessage());
        } catch (JsonSyntaxException e) {
            plugin.getLogger().severe("Invalid denied phrases data format: " + e.getMessage());
        }
    }

    /**
     * Save approved phrases to disk
     */
    private void saveApprovedPhrases() {
        try (FileOutputStream fos = new FileOutputStream(approvedPhrasesFile)) {
            Properties props = new Properties();

            // Convert ConcurrentHashMap to Properties
            for (Map.Entry<String, Integer> entry : approvedPhrases.entrySet()) {
                props.setProperty(entry.getKey(), String.valueOf(entry.getValue()));
            }

            props.store(fos, "AutoMod Approved Phrases");
            plugin.getLogger().info("Saved " + approvedPhrases.size() + " approved phrases");
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save approved phrases: " + e.getMessage());
        }
    }

    /**
     * Save flagged phrases to disk
     */
    private void saveFlaggedPhrases() {
        try (FileOutputStream fos = new FileOutputStream(flaggedPhrasesFile)) {
            Properties props = new Properties();

            // Convert Set to Properties
            int index = 0;
            for (String phrase : flaggedPhrases) {
                props.setProperty(String.valueOf(index++), phrase);
            }

            props.store(fos, "AutoMod Flagged Phrases");
            plugin.getLogger().info("Saved " + flaggedPhrases.size() + " flagged phrases");
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save flagged phrases: " + e.getMessage());
        }
    }

    /**
     * Load approved phrases from disk
     */
    private void loadApprovedPhrases() {
        File file = new File(approvedPhrasesFile);
        if (!file.exists()) return;

        try (FileInputStream fis = new FileInputStream(file)) {
            Properties props = new Properties();
            props.load(fis);

            // Convert Properties to ConcurrentHashMap
            for (String key : props.stringPropertyNames()) {
                approvedPhrases.put(key, Integer.parseInt(props.getProperty(key)));
            }

            plugin.getLogger().info("Loaded " + approvedPhrases.size() + " approved phrases");
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to load approved phrases: " + e.getMessage());
        }
    }



    /**
     * Load flagged phrases from disk
     */
    private void loadFlaggedPhrases() {
        File file = new File(flaggedPhrasesFile);
        if (!file.exists()) return;

        try (FileInputStream fis = new FileInputStream(file)) {
            Properties props = new Properties();
            props.load(fis);

            // Convert Properties to Set
            for (String key : props.stringPropertyNames()) {
                flaggedPhrases.add(props.getProperty(key));
            }

            plugin.getLogger().info("Loaded " + flaggedPhrases.size() + " flagged phrases");
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to load flagged phrases: " + e.getMessage());
        }
    }

    /**
     * Get the current number of approved phrases
     * @return Number of approved phrases
     */
    public int getApprovedPhraseCount() {
        return approvedPhrases.size();
    }

    /**
     * Get the current number of denied phrases
     * @return Number of denied phrases
     */
    public int getDeniedPhraseCount() {
        return deniedPhrases.size();
    }

    /**
     * Get the current number of flagged phrases
     * @return Number of flagged phrases
     */
    public int getFlaggedPhraseCount() {
        return flaggedPhrases.size();
    }

    /**
     * Add a phrase to the denied phrases list
     * @param message The message to deny
     * @return True if the phrase was added successfully
     */
    public boolean addDeniedPhrase(String message) {
        String normalized = normalizeMessage(message);

        // Remove from approved phrases if present
        approvedPhrases.remove(normalized);

        // Add to denied phrases with current timestamp
        deniedPhrases.put(normalized, System.currentTimeMillis());

        // Save the updated data
        saveLearnedData();

        return true;
    }

    /**
     * Inner class to represent a chat message with timestamp
     */
    private static class ChatMessage {
        final String content;
        final long timestamp;

        ChatMessage(String content, long timestamp) {
            this.content = content;
            this.timestamp = timestamp;
        }
    }

    /**
     * Inner class to track message context for a player
     */
    private static class MessageContext {
        private final LinkedList<String> recentMessages = new LinkedList<>();
        private static final int CONTEXT_SIZE = 5;

        /**
         * Add a message to the context
         * @param message The message to add
         */
        void addMessage(String message) {
            recentMessages.add(message);
            while (recentMessages.size() > CONTEXT_SIZE) {
                recentMessages.removeFirst();
            }
        }

        /**
         * Check if context contains personal references
         * @return True if context contains personal references
         */
        boolean containsPersonalReferences() {
            for (String msg : recentMessages) {
                String lower = msg.toLowerCase();
                if (lower.contains(" you ") || 
                    lower.contains("your") || 
                    lower.contains("youre") || 
                    lower.contains("you're") || 
                    lower.contains(" u ") || 
                    lower.contains(" ur ")) {
                    return true;
                }
            }
            return false;
        }
    }

    /**
     * Inner class for harassment detection results
     */
    private static class HarassmentResult {
        final boolean isHarassment;
        final String matchedTerm;
        final String context;

        HarassmentResult(boolean isHarassment, String matchedTerm, String context) {
            this.isHarassment = isHarassment;
            this.matchedTerm = matchedTerm;
            this.context = context;
        }
    }
}
