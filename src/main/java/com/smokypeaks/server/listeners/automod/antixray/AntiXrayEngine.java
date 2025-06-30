package com.smokypeaks.server.listeners.automod.antixray;

import com.smokypeaks.Main;
import com.smokypeaks.global.automod.ViolationType;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import java.util.*;

public class AntiXrayEngine {
    private final Main plugin;
    private final Map<UUID, PlayerMiningData> playerData;
    private final Set<Material> monitoredBlocks;

    public AntiXrayEngine(Main plugin) {
        this.plugin = plugin;
        this.playerData = new HashMap<>();
        this.monitoredBlocks = new HashSet<>(Arrays.asList(
            Material.DIAMOND_ORE,
            Material.DEEPSLATE_DIAMOND_ORE,
            Material.ANCIENT_DEBRIS,
            Material.EMERALD_ORE,
            Material.DEEPSLATE_EMERALD_ORE
        ));
    }

    public void analyzeMiningPattern(Player player, Block block) {
        if (!monitoredBlocks.contains(block.getType()) || 
            player.hasPermission("ultimatestaff.automod.bypass")) {
            return;
        }

        PlayerMiningData data = playerData.computeIfAbsent(
            player.getUniqueId(), 
            k -> new PlayerMiningData()
        );

        // Update mining data
        data.addBlock(block);

        // Calculate confidence score based on patterns
        double confidence = calculateConfidence(data);

        // Record mining for learning data (always, for potential marking)
        plugin.getAntiXrayManager().getLearningData(player.getUniqueId())
            .recordMining(block.getType(), block.getLocation());

        // Process the confidence score through the AntiXrayManager
        plugin.getAntiXrayManager().processConfidenceScore(
            player.getUniqueId(),
            confidence,
            "Direct to valuable ore"
        );
    }

    private double calculateConfidence(PlayerMiningData data) {
        // Implementation of the confidence calculation

        double confidence = 0.0;

        // Factor 1: Direct tunneling to ore (highest weight)
        if (data.hasDirectTunneling()) {
            confidence += 50.0;
        }

        // Factor 2: Unusual ore discovery rate
        if (data.getOreDiscoveryRate() > 0.7) {
            confidence += 30.0;
        }

        // Factor 3: Mining pattern consistency
        if (data.hasConsistentPattern()) {
            confidence += 10.0;
        }

        return Math.min(confidence, 100.0);
    }

    private static class PlayerMiningData {
        private final List<Location> miningLocations;
        private final Map<Material, Integer> oreCounts;
        private int totalBlocks;

        public PlayerMiningData() {
            this.miningLocations = new ArrayList<>();
            this.oreCounts = new HashMap<>();
            this.totalBlocks = 0;
        }

        public void addBlock(Block block) {
            miningLocations.add(block.getLocation());
            oreCounts.merge(block.getType(), 1, Integer::sum);
            totalBlocks++;
        }

        public boolean hasDirectTunneling() {
            if (miningLocations.size() < 3) return false;

            // Check if the last 3 blocks form a straight line
            Location loc1 = miningLocations.get(miningLocations.size() - 1);
            Location loc2 = miningLocations.get(miningLocations.size() - 2);
            Location loc3 = miningLocations.get(miningLocations.size() - 3);

            return isInLine(loc1, loc2, loc3);
        }

        public double getOreDiscoveryRate() {
            if (totalBlocks == 0) return 0.0;
            int totalOres = oreCounts.values().stream().mapToInt(Integer::intValue).sum();
            return (double) totalOres / totalBlocks;
        }

        public boolean hasConsistentPattern() {
            // Check if recent mining follows a predictable pattern
            return miningLocations.size() >= 5 && 
                   hasRepeatingDirections(miningLocations.subList(
                       miningLocations.size() - 5, 
                       miningLocations.size()
                   ));
        }

        private boolean isInLine(Location loc1, Location loc2, Location loc3) {
            // Check if three points form a straight line
            double dx1 = loc2.getX() - loc1.getX();
            double dy1 = loc2.getY() - loc1.getY();
            double dz1 = loc2.getZ() - loc1.getZ();

            double dx2 = loc3.getX() - loc2.getX();
            double dy2 = loc3.getY() - loc2.getY();
            double dz2 = loc3.getZ() - loc2.getZ();

            // Check if vectors are parallel
            return Math.abs(dx1 * dy2 - dx2 * dy1) < 0.1 &&
                   Math.abs(dy1 * dz2 - dy2 * dz1) < 0.1 &&
                   Math.abs(dz1 * dx2 - dz2 * dx1) < 0.1;
        }

        private boolean hasRepeatingDirections(List<Location> locations) {
            if (locations.size() < 2) return false;

            // Calculate movement vectors between consecutive locations
            List<Vector3> vectors = new ArrayList<>();
            for (int i = 1; i < locations.size(); i++) {
                Location curr = locations.get(i);
                Location prev = locations.get(i - 1);
                vectors.add(new Vector3(
                    curr.getX() - prev.getX(),
                    curr.getY() - prev.getY(),
                    curr.getZ() - prev.getZ()
                ));
            }

            // Check if vectors repeat
            return vectors.stream().distinct().count() <= 2;
        }
    }

    private static class Vector3 {
        final double x, y, z;

        Vector3(double x, double y, double z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Vector3)) return false;
            Vector3 v = (Vector3) o;
            return Math.abs(x - v.x) < 0.1 &&
                   Math.abs(y - v.y) < 0.1 &&
                   Math.abs(z - v.z) < 0.1;
        }

        @Override
        public int hashCode() {
            return Objects.hash(
                Math.round(x * 10),
                Math.round(y * 10),
                Math.round(z * 10)
            );
        }
    }
}
