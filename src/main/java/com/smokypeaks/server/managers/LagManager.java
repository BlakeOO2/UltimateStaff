package com.smokypeaks.server.managers;

import com.smokypeaks.Main;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.text.DecimalFormat;
import java.util.*;
import java.util.stream.Collectors;

public class LagManager {
    private final Main plugin;
    private final Map<UUID, LagReport> playerReports = new HashMap<>();
    private final DecimalFormat df = new DecimalFormat("#.##");

    // Settings
    private final int HIGH_ENTITY_COUNT = 100; // Entities per chunk to consider high
    private final int TPS_THRESHOLD = 18; // TPS below this is considered laggy

    public LagManager(Main plugin) {
        this.plugin = plugin;
    }

    /**
     * Generate a full server lag report
     * @return The lag report text
     */
    public String generateLagReport() {
        StringBuilder report = new StringBuilder();
        report.append("§6§l===== UltimateStaff Lag Report =====");
        report.append("\n§f");

        // Server info
        double tps = getTPS();
        String tpsColor = tps > TPS_THRESHOLD ? "§a" : (tps > 10 ? "§e" : "§c");
        report.append("\n§7Server TPS: ").append(tpsColor).append(df.format(tps)).append(" §7(20 is perfect)");

        // Memory usage
        MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
        MemoryUsage heapUsage = memoryBean.getHeapMemoryUsage();
        long usedMemory = heapUsage.getUsed() / (1024 * 1024);
        long maxMemory = heapUsage.getMax() / (1024 * 1024);
        double memoryPercentage = (double) usedMemory / maxMemory * 100;
        String memColor = memoryPercentage < 70 ? "§a" : (memoryPercentage < 85 ? "§e" : "§c");

        report.append("\n§7Memory Usage: ").append(memColor)
              .append(usedMemory).append("MB / ").append(maxMemory).append("MB ")
              .append("(").append(df.format(memoryPercentage)).append("%)");

        // Player count
        report.append("\n§7Online Players: §f").append(Bukkit.getOnlinePlayers().size()).append("/").append(Bukkit.getMaxPlayers());

        // World analysis
        report.append("\n\n§e§lWorlds Analysis:");
        Map<World, Integer> worldEntityCounts = new HashMap<>();
        Map<World, Integer> worldChunkCounts = new HashMap<>();

        for (World world : Bukkit.getWorlds()) {
            int entityCount = world.getEntities().size();
            int loadedChunks = world.getLoadedChunks().length;

            worldEntityCounts.put(world, entityCount);
            worldChunkCounts.put(world, loadedChunks);

            report.append("\n§7- ").append(world.getName())
                  .append(":\n  §7Entities: §f").append(entityCount)
                  .append("\n  §7Loaded Chunks: §f").append(loadedChunks);
        }

        // Laggy chunks analysis
        List<LaggyChunk> laggyChunks = findLaggyChunks();
        if (!laggyChunks.isEmpty()) {
            report.append("\n\n§c§lLaggy Chunks (Top 5):");
            laggyChunks.stream()
                .sorted(Comparator.comparing(LaggyChunk::getEntityCount).reversed())
                .limit(5)
                .forEach(chunk -> {
                    report.append("\n§7- World: §f").append(chunk.getWorld().getName())
                          .append(" §7at X: §f").append(chunk.getX())
                          .append(" §7Z: §f").append(chunk.getZ())
                          .append(" §7(§c").append(chunk.getEntityCount()).append(" entities§7)")
                          .append("\n  §7Entity Types: §f").append(chunk.getEntityBreakdown());
                });
        } else {
            report.append("\n\n§a§lNo laggy chunks detected!");
        }

        // Plugin analysis - add later if needed

        return report.toString();
    }

    /**
     * Check TPS (Ticks Per Second) - estimate based on available data
     */
    public double getTPS() {
        try {
            // Try to get TPS from Paper API if available
            return Bukkit.getTPS()[0];
        } catch (Exception e) {
            // Fallback to a conservative estimate if not using Paper
            return 18.5; // Conservative estimate if we can't get real TPS
        }
    }

    /**
     * Find chunks with high entity counts that might be causing lag
     */
    public List<LaggyChunk> findLaggyChunks() {
        List<LaggyChunk> laggyChunks = new ArrayList<>();

        for (World world : Bukkit.getWorlds()) {
            // Check loaded chunks
            for (Chunk chunk : world.getLoadedChunks()) {
                Entity[] entities = chunk.getEntities();

                if (entities.length > HIGH_ENTITY_COUNT) {
                    // Count entity types
                    Map<String, Integer> entityTypes = new HashMap<>();
                    for (Entity entity : entities) {
                        String type = entity.getType().toString();
                        entityTypes.put(type, entityTypes.getOrDefault(type, 0) + 1);
                    }

                    laggyChunks.add(new LaggyChunk(
                        world,
                        chunk.getX(),
                        chunk.getZ(),
                        entities.length,
                        entityTypes
                    ));
                }
            }
        }

        return laggyChunks;
    }

    /**
     * Find players who might be causing lag
     */
    public List<String> findLaggyPlayers() {
        // Look for players with many entities nearby or in unusual positions
        List<String> suspiciousPlayers = new ArrayList<>();

        for (Player player : Bukkit.getOnlinePlayers()) {
            int nearbyEntities = player.getNearbyEntities(50, 50, 50).size();

            if (nearbyEntities > HIGH_ENTITY_COUNT) {
                suspiciousPlayers.add(player.getName() + " - " + nearbyEntities + " nearby entities");
            }
        }

        return suspiciousPlayers;
    }

    /**
     * Find redstone activity that might be causing lag
     */
    public void scanForRedstoneActivity(Player requester) {
        // Schedule a task to analyze redstone activity over time
        // This is a complex task that would require tracking block updates
        requester.sendMessage("§6[UltimateStaff] §eRedstone analysis is not implemented yet.");
    }

    /**
     * Creates a new lag report for a player
     */
    public void createReportFor(Player player) {
        LagReport report = new LagReport();
        playerReports.put(player.getUniqueId(), report);

        // Schedule a task to teleport the player to laggy chunks if needed
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (report.isActive()) {
                player.sendMessage("§6[UltimateStaff] §eLag report is ready. Use /ultimatestaff lag show to view it.");
            }
        }, 60L); // 3 second delay
    }

    /**
     * Teleport a player to a laggy chunk for investigation
     */
    public void teleportToLaggyChunk(Player player, int index) {
        List<LaggyChunk> laggyChunks = findLaggyChunks();

        if (laggyChunks.isEmpty()) {
            player.sendMessage("§c[UltimateStaff] No laggy chunks found!");
            return;
        }

        if (index < 0 || index >= laggyChunks.size()) {
            player.sendMessage("§c[UltimateStaff] Invalid chunk index. Use 0-" + (laggyChunks.size()-1));
            return;
        }

        LaggyChunk chunk = laggyChunks.get(index);
        player.teleport(chunk.getWorld().getBlockAt(chunk.getX() * 16 + 8, 100, chunk.getZ() * 16 + 8).getLocation());
        player.sendMessage("§6[UltimateStaff] §eTeleported to laggy chunk with " + chunk.getEntityCount() + " entities.");
    }

    /**
     * Class to represent a chunk with high entity count
     */
    public static class LaggyChunk {
        private final World world;
        private final int x;
        private final int z;
        private final int entityCount;
        private final Map<String, Integer> entityTypes;

        public LaggyChunk(World world, int x, int z, int entityCount, Map<String, Integer> entityTypes) {
            this.world = world;
            this.x = x;
            this.z = z;
            this.entityCount = entityCount;
            this.entityTypes = entityTypes;
        }

        public World getWorld() {
            return world;
        }

        public int getX() {
            return x;
        }

        public int getZ() {
            return z;
        }

        public int getEntityCount() {
            return entityCount;
        }

        public String getEntityBreakdown() {
            return entityTypes.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(3) // Top 3 entity types
                .map(e -> e.getKey() + " ×" + e.getValue())
                .collect(Collectors.joining(", "));
        }
    }

    /**
     * Class to store lag report data
     */
    private static class LagReport {
        private final long createdAt;
        private boolean active;

        public LagReport() {
            this.createdAt = System.currentTimeMillis();
            this.active = true;
        }

        public boolean isActive() {
            return active && System.currentTimeMillis() - createdAt < 5 * 60 * 1000; // 5 minutes
        }
    }
}
