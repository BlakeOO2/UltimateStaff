// com.smokypeaks.server.settings.GlobalSettings.java
package com.smokypeaks.server.settings;

public class GlobalSettings {
    // General settings
    private boolean defaultEnabled;
    private int alertRadius;

    // Format settings
    private boolean showCoordinates;
    private boolean showWorld;
    private boolean useRelativeCoordinates;

    // Color settings
    private String suspiciousColor;
    private String normalColor;
    private String veinColor;

    // Time restriction settings
    private int cooldown;
    private boolean quietHoursEnabled;
    private String quietHoursStart;
    private String quietHoursEnd;

    public GlobalSettings() {
        // Set defaults
        this.defaultEnabled = true;
        this.alertRadius = 100;

        this.showCoordinates = true;
        this.showWorld = true;
        this.useRelativeCoordinates = false;

        this.suspiciousColor = "&c";
        this.normalColor = "&e";
        this.veinColor = "&a";

        this.cooldown = 30;
        this.quietHoursEnabled = false;
        this.quietHoursStart = "23:00";
        this.quietHoursEnd = "07:00";
    }

    // Getters and Setters
    public boolean isDefaultEnabled() {
        return defaultEnabled;
    }

    public void setDefaultEnabled(boolean defaultEnabled) {
        this.defaultEnabled = defaultEnabled;
    }

    public int getAlertRadius() {
        return alertRadius;
    }

    public void setAlertRadius(int alertRadius) {
        this.alertRadius = alertRadius;
    }

    public boolean isShowCoordinates() {
        return showCoordinates;
    }

    public void setShowCoordinates(boolean showCoordinates) {
        this.showCoordinates = showCoordinates;
    }

    public boolean isShowWorld() {
        return showWorld;
    }

    public void setShowWorld(boolean showWorld) {
        this.showWorld = showWorld;
    }

    public boolean isUseRelativeCoordinates() {
        return useRelativeCoordinates;
    }

    public void setUseRelativeCoordinates(boolean useRelativeCoordinates) {
        this.useRelativeCoordinates = useRelativeCoordinates;
    }

    public String getSuspiciousColor() {
        return suspiciousColor;
    }

    public void setSuspiciousColor(String suspiciousColor) {
        this.suspiciousColor = suspiciousColor;
    }

    public String getNormalColor() {
        return normalColor;
    }

    public void setNormalColor(String normalColor) {
        this.normalColor = normalColor;
    }

    public String getVeinColor() {
        return veinColor;
    }

    public void setVeinColor(String veinColor) {
        this.veinColor = veinColor;
    }

    public int getCooldown() {
        return cooldown;
    }

    public void setCooldown(int cooldown) {
        this.cooldown = cooldown;
    }

    public boolean isQuietHoursEnabled() {
        return quietHoursEnabled;
    }

    public void setQuietHoursEnabled(boolean quietHoursEnabled) {
        this.quietHoursEnabled = quietHoursEnabled;
    }

    public String getQuietHoursStart() {
        return quietHoursStart;
    }

    public void setQuietHoursStart(String quietHoursStart) {
        this.quietHoursStart = quietHoursStart;
    }

    public String getQuietHoursEnd() {
        return quietHoursEnd;
    }

    public void setQuietHoursEnd(String quietHoursEnd) {
        this.quietHoursEnd = quietHoursEnd;
    }

    // Utility methods
    public String formatCoordinates(int x, int y, int z, int playerX, int playerY, int playerZ) {
        if (!showCoordinates) {
            return "";
        }

        if (useRelativeCoordinates) {
            int dx = x - playerX;
            int dy = y - playerY;
            int dz = z - playerZ;
            return String.format("~%d, ~%d, ~%d", dx, dy, dz);
        } else {
            return String.format("%d, %d, %d", x, y, z);
        }
    }

    public String formatLocation(String worldName, int x, int y, int z, int playerX, int playerY, int playerZ) {
        StringBuilder location = new StringBuilder();

        if (showWorld) {
            location.append(worldName).append(" ");
        }

        location.append(formatCoordinates(x, y, z, playerX, playerY, playerZ));
        return location.toString();
    }
}
