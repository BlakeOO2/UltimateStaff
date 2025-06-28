package com.smokypeaks.server.managers;

import com.smokypeaks.Main;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

public class ChatFilterManager {
    private final Main plugin;
    private final Set<String> filteredWords = new HashSet<>();
    private final List<Pattern> wordPatterns = new ArrayList<>();
    private File filterConfigFile;
    private FileConfiguration filterConfig;
    private boolean filterEnabled = true;
    private String replacementString = "****";

    public ChatFilterManager(Main plugin) {
        this.plugin = plugin;
        loadFilterConfig();
    }

    /**
     * Load the filter configuration
     */
    public void loadFilterConfig() {
        // Create config file if it doesn't exist
        if (filterConfigFile == null) {
            filterConfigFile = new File(plugin.getDataFolder(), "chatfilter.yml");
        }

        if (!filterConfigFile.exists()) {
            plugin.saveResource("chatfilter.yml", false);
        }

        filterConfig = YamlConfiguration.loadConfiguration(filterConfigFile);

        // Load settings
        filterEnabled = filterConfig.getBoolean("enabled", true);
        replacementString = filterConfig.getString("replacement-string", "****");

        // Load filtered words
        filteredWords.clear();
        wordPatterns.clear();

        List<String> words = filterConfig.getStringList("filtered-words");
        for (String word : words) {
            addWordToFilter(word, false);
        }

        plugin.getLogger().info("Loaded " + filteredWords.size() + " filtered words");
    }

    /**
     * Save the filter configuration
     */
    public void saveFilterConfig() {
        if (filterConfig == null || filterConfigFile == null) {
            return;
        }

        // Save settings
        filterConfig.set("enabled", filterEnabled);
        filterConfig.set("replacement-string", replacementString);

        // Save filtered words
        filterConfig.set("filtered-words", new ArrayList<>(filteredWords));

        try {
            filterConfig.save(filterConfigFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save chat filter config: " + e.getMessage());
        }
    }

    /**
     * Add a word to the filter
     * @param word The word to add
     * @param save Whether to save the config after adding
     * @return True if the word was added, false if it already exists
     */
    public boolean addWordToFilter(String word, boolean save) {
        word = word.toLowerCase();
        if (filteredWords.contains(word)) {
            return false;
        }

        filteredWords.add(word);

        // Create regex pattern for this word
        // Match the word with word boundaries to avoid filtering parts of other words
        wordPatterns.add(Pattern.compile("\\b" + Pattern.quote(word) + "\\b", Pattern.CASE_INSENSITIVE));

        if (save) {
            saveFilterConfig();
        }

        return true;
    }

    /**
     * Remove a word from the filter
     * @param word The word to remove
     * @return True if the word was removed, false if it wasn't in the filter
     */
    public boolean removeWordFromFilter(String word) {
        word = word.toLowerCase();
        if (!filteredWords.contains(word)) {
            return false;
        }

        filteredWords.remove(word);

        // Rebuild the patterns list
        wordPatterns.clear();
        for (String filteredWord : filteredWords) {
            wordPatterns.add(Pattern.compile("\\b" + Pattern.quote(filteredWord) + "\\b", Pattern.CASE_INSENSITIVE));
        }

        saveFilterConfig();
        return true;
    }

    /**
     * Filter a chat message
     * @param message The message to filter
     * @return The filtered message
     */
    public String filterMessage(String message) {
        if (!filterEnabled) {
            return message;
        }

        String filteredMessage = message;
        for (Pattern pattern : wordPatterns) {
            filteredMessage = pattern.matcher(filteredMessage).replaceAll(replacementString);
        }

        return filteredMessage;
    }

    /**
     * Check if a message contains filtered words
     * @param message The message to check
     * @return True if the message contains filtered words
     */
    public boolean containsFilteredWords(String message) {
        if (!filterEnabled) {
            return false;
        }

        for (Pattern pattern : wordPatterns) {
            if (pattern.matcher(message).find()) {
                return true;
            }
        }

        return false;
    }

    /**
     * Get a list of all filtered words
     * @return A list of filtered words
     */
    public List<String> getFilteredWords() {
        return new ArrayList<>(filteredWords);
    }

    /**
     * Set whether the filter is enabled
     * @param enabled Whether the filter should be enabled
     */
    public void setFilterEnabled(boolean enabled) {
        this.filterEnabled = enabled;
        saveFilterConfig();
    }

    /**
     * Check if the filter is enabled
     * @return True if the filter is enabled
     */
    public boolean isFilterEnabled() {
        return filterEnabled;
    }

    /**
     * Set the replacement string
     * @param replacementString The string to replace filtered words with
     */
    public void setReplacementString(String replacementString) {
        this.replacementString = replacementString;
        saveFilterConfig();
    }

    /**
     * Get the replacement string
     * @return The replacement string
     */
    public String getReplacementString() {
        return replacementString;
    }
}
