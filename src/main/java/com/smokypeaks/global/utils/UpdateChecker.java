// UpdateChecker.java
package com.smokypeaks.global.utils;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.smokypeaks.Main;
import org.bukkit.Bukkit;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public class UpdateChecker {
    private final Main plugin;
    private final String githubRepo;
    private final String currentVersion;
    private static final String GITHUB_API_URL = "https://api.github.com/repos/%s/releases/latest";

    public UpdateChecker(Main plugin, String githubRepo, String currentVersion) {
        this.plugin = plugin;
        this.githubRepo = githubRepo;
        this.currentVersion = currentVersion;
    }
    public void checkForUpdates() {
        checkForUpdates(false);
    }

    public void checkForUpdates(boolean force) {
        if (!force && !plugin.getConfig().getBoolean("auto-update")) {
            plugin.getLogger().info("Auto-update is disabled in config");
            return;
        }

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                String apiUrl = String.format(GITHUB_API_URL, githubRepo);
                HttpURLConnection connection = (HttpURLConnection) new URL(apiUrl).openConnection();
                connection.setRequestMethod("GET");

                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(connection.getInputStream())
                );
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();

                JsonObject jsonResponse = JsonParser.parseString(response.toString()).getAsJsonObject();
                String latestVersion = jsonResponse.get("tag_name").getAsString();
                String downloadUrl = jsonResponse.get("assets").getAsJsonArray()
                        .get(0).getAsJsonObject()
                        .get("browser_download_url").getAsString();

                if (isNewerVersion(latestVersion)) {
                    notifyAdmins("§6[SmokyPeaks] §eNew version " + latestVersion + " available!");
                    if (plugin.getConfig().getBoolean("auto-update")) {
                        notifyAdmins("§6[SmokyPeaks] §eDownloading update...");
                        downloadUpdate(downloadUrl);
                    }
                }

            } catch (Exception e) {
                e.printStackTrace();
                notifyAdmins("§c[SmokyPeaks] Failed to check for updates: " + e.getMessage());
            }
        });
    }

    private boolean isNewerVersion(String latestVersion) {
        // Compare versions logic here
        return !currentVersion.equals(latestVersion);
    }

    private void downloadUpdate(String downloadUrl) {
        try {
            // Download new jar
            URL url = new URL(downloadUrl);
            Path targetPath = plugin.getDataFolder().toPath().resolve("update.jar");
            Files.copy(url.openStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);

            // Replace current jar on next server restart
            File currentJar = new File(plugin.getClass().getProtectionDomain()
                    .getCodeSource().getLocation().toURI());

            // Schedule replacement for server shutdown
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                try {
                    Files.move(targetPath, currentJar.toPath(),
                            StandardCopyOption.REPLACE_EXISTING);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }));

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    private void notifyAdmins(String message) {
        MessageUtil.notifyAdmins(plugin, message);
    }



    private String parseVersionFromResponse(String response) {
        // Add JSON parsing logic here
        return "";
    }

    private String getDownloadUrl(String response) {
        // Add JSON parsing logic here
        return "";
    }
}
