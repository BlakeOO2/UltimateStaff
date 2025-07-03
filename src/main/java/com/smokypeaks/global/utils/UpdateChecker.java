package com.smokypeaks.global.utils;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import com.smokypeaks.Main;
import org.bukkit.Bukkit;


import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;

public class UpdateChecker {
    private final Main plugin;
    private final String githubRepo;
    private final String currentVersion;
    private static final String GITHUB_API_URL = "https://api.github.com/repos/%s/releases/latest";

    public UpdateChecker(Main plugin, String githubRepo, String currentVersion) {
        this.plugin = plugin;
        this.githubRepo = githubRepo;
        this.currentVersion = currentVersion;
        plugin.getLogger().info("Initialized UpdateChecker for repo: " + githubRepo);
    }

    public void checkForUpdates(boolean force) {
        if (!force && !plugin.getConfig().getBoolean("auto-update", true)) {
            return;
        }

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            HttpURLConnection connection = null;
            try {
                String apiUrl = String.format(GITHUB_API_URL, githubRepo);
                plugin.getLogger().info("Checking for updates at: " + apiUrl);

                // Setup connection
                connection = (HttpURLConnection) new URL(apiUrl).openConnection();
                connection.setRequestMethod("GET");
                connection.setRequestProperty("Accept", "application/vnd.github.v3+json");
                connection.setConnectTimeout(5000);
                connection.setReadTimeout(5000);

                // Check response code
                int responseCode = connection.getResponseCode();
                plugin.getLogger().info("GitHub API Response Code: " + responseCode);

                if (responseCode == 404) {
                    String message = "§6[UltimateStaff] §eNo releases found on GitHub. Please create a release at: §bhttps://github.com/" + githubRepo + "/releases";
                    MessageUtil.notifyAdmins(plugin, message);
                    plugin.getLogger().warning("No releases found. Create one at: https://github.com/" + githubRepo + "/releases");
                    return;
                }

                if (responseCode != 200) {
                    String message = "§c[UltimateStaff] §eUpdate check failed. Status: " + responseCode;
                    MessageUtil.notifyAdmins(plugin, message);
                    plugin.getLogger().warning("Update check failed. HTTP Status: " + responseCode);
                    return;
                }

                // Read response
                StringBuilder response = new StringBuilder();
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(connection.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                }

                // Parse JSON response
                JsonObject jsonResponse = JsonParser.parseString(response.toString()).getAsJsonObject();

                if (!jsonResponse.has("tag_name")) {
                    throw new IllegalStateException("Invalid response format: missing tag_name");
                }

                String latestVersion = jsonResponse.get("tag_name").getAsString();
                plugin.getLogger().info("Found latest version: " + latestVersion);

                if (isNewerVersion(latestVersion)) {
                    String downloadUrl = null;
                    if (jsonResponse.has("assets") && jsonResponse.getAsJsonArray("assets").size() > 0) {
                        downloadUrl = jsonResponse.get("assets").getAsJsonArray()
                                .get(0).getAsJsonObject()
                                .get("browser_download_url").getAsString();
                    }

                    String updateMessage = "§6[UltimateStaff] §eNew version " + latestVersion + " available!";
                    if (downloadUrl != null && plugin.getConfig().getBoolean("auto-update", true)) {
                        updateMessage += "\n§6[UltimateStaff] §eDownloading update...";
                        downloadUpdate(downloadUrl);
                    } else {
                        updateMessage += "\n§6[UltimateStaff] §eDownload manually from: §bhttps://github.com/" + githubRepo + "/releases";
                    }
                    MessageUtil.notifyAdmins(plugin, updateMessage);
                } else if (force) {
                    MessageUtil.notifyAdmins(plugin, "§6[UltimateStaff] §aYou are running the latest version!");
                }

            } catch (IOException e) {
                String errorMessage = "§c[UltimateStaff] §eUpdate check failed: " + e.getMessage();
                MessageUtil.notifyAdmins(plugin, errorMessage);
                plugin.getLogger().warning("Update check failed: " + e.getMessage());
                if (plugin.getConfig().getBoolean("debug", false)) {
                    e.printStackTrace();
                }
            } catch (JsonSyntaxException e) {
                String errorMessage = "§c[UltimateStaff] §eInvalid response from GitHub API";
                MessageUtil.notifyAdmins(plugin, errorMessage);
                plugin.getLogger().warning("Failed to parse GitHub API response: " + e.getMessage());
                if (plugin.getConfig().getBoolean("debug", false)) {
                    e.printStackTrace();
                }
            } catch (Exception e) {
                String errorMessage = "§c[UltimateStaff] §eUnexpected error during update check";
                MessageUtil.notifyAdmins(plugin, errorMessage);
                plugin.getLogger().warning("Unexpected error: " + e.getMessage());
                if (plugin.getConfig().getBoolean("debug", false)) {
                    e.printStackTrace();
                }
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
            }
        });
    }

    private boolean isNewerVersion(String latestVersion) {
        // Remove 'v' prefix if present
        String cleanLatest = latestVersion.startsWith("v") ? latestVersion.substring(1) : latestVersion;
        String cleanCurrent = currentVersion.startsWith("v") ? currentVersion.substring(1) : currentVersion;

        return !cleanCurrent.equals(cleanLatest);
    }

    private void downloadUpdate(String downloadUrl) {
        plugin.getLogger().info("Starting download from: " + downloadUrl);

        try {
            // Get the current jar file
            File currentJar = new File(plugin.getClass()
                    .getProtectionDomain()
                    .getCodeSource()
                    .getLocation()
                    .toURI());

            // Get the plugins folder (parent of the current jar)
            File pluginsFolder = currentJar.getParentFile();
            plugin.getLogger().info("Plugins folder: " + pluginsFolder.getAbsolutePath());

            // Download new version directly to the plugins folder with a temporary name
            URL url = new URL(downloadUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestProperty("User-Agent", "UltimateStaff UpdateChecker");

            // Get the original file name without version suffix for proper loading
            String originalName = currentJar.getName();
            // Use .update.jar suffix so it won't be loaded until renamed
            File downloadFile = new File(pluginsFolder, originalName + ".update");

            plugin.getLogger().info("Downloading update to: " + downloadFile.getAbsolutePath());

            // Download the file
            try (InputStream in = connection.getInputStream();
                 FileOutputStream out = new FileOutputStream(downloadFile)) {

                byte[] buffer = new byte[1024];
                int bytesRead;
                while ((bytesRead = in.read(buffer)) != -1) {
                    out.write(buffer, 0, bytesRead);
                }
            }

            // Set update marker file with update information
            File updateMarker = new File(plugin.getDataFolder(), "update.marker");
            try (PrintWriter writer = new PrintWriter(updateMarker)) {
                writer.println(currentJar.getAbsolutePath());
                writer.println(downloadFile.getAbsolutePath());
            }

            plugin.getLogger().info("Update downloaded successfully! The plugin will be updated on next server restart.");
            MessageUtil.notifyAdmins(plugin, "§6[UltimateStaff] §aUpdate downloaded! Will be applied on next server restart.");

        } catch (Exception e) {
            plugin.getLogger().warning("Failed to download update: " + e.getMessage());
            if (plugin.getConfig().getBoolean("debug", false)) {
                e.printStackTrace();
            }
            MessageUtil.notifyAdmins(plugin, "§c[UltimateStaff] §eFailed to download update: " + e.getMessage());
        }
    }


}
