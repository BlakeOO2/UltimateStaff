package com.smokypeaks.server.automod.listeners;

import com.smokypeaks.Main;
import com.smokypeaks.server.automod.ViolationType;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class AutoModPlayerListener implements Listener {
    private final Main plugin;

    // Track player movements for potential cheating detection
    private final Map<UUID, Location> lastPositions = new HashMap<>();
    private final Map<UUID, Long> lastMoveTime = new HashMap<>();

    // Track mining patterns for X-ray detection
    private final Map<UUID, Map<Material, Integer>> valuablesMined = new HashMap<>();
    private static final Material[] VALUABLE_ORES = {
            Material.DIAMOND_ORE, Material.DEEPSLATE_DIAMOND_ORE,
            Material.ANCIENT_DEBRIS,
            Material.EMERALD_ORE, Material.DEEPSLATE_EMERALD_ORE
    };

    // Track PvP for non-consensual PvP detection
    private final Map<UUID, Map<UUID, Long>> pvpAttacks = new HashMap<>();
    private static final long PVP_TIMEOUT = 300000; // 5 minutes in ms

    public AutoModPlayerListener(Main plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        // Check for inappropriate username
        String username = player.getName().toLowerCase();
        if (username.contains("nigga") || username.contains("negro") || 
            username.contains("hitler") || username.contains("fuck") ||
            username.contains("nazi") || username.contains("sex")) {

            plugin.getAutoModManager().processViolation(
                    player.getUniqueId(),
                    ViolationType.INAPPROPRIATE_APPEARANCE,
                    "Inappropriate username: " + player.getName()
            );
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Material blockType = event.getBlock().getType();

        // Skip if player has bypass permission
        if (player.hasPermission("ultimatestaff.automod.bypass")) return;

        // Check for X-ray (diamond mining without digging nearby blocks)
        for (Material valuableOre : VALUABLE_ORES) {
            if (blockType == valuableOre) {
                // Get or create mining record for this player
                Map<Material, Integer> playerMining = valuablesMined.computeIfAbsent(
                        player.getUniqueId(), k -> new HashMap<>());

                // Increment count for this ore type
                int count = playerMining.getOrDefault(blockType, 0) + 1;
                playerMining.put(blockType, count);

                // Check for suspicious pattern (threshold varies by ore type)
                int threshold = (blockType == Material.ANCIENT_DEBRIS) ? 5 : 10;
                if (count >= threshold) {
                    plugin.getAutoModManager().processViolation(
                            player.getUniqueId(),
                            ViolationType.CHEATING,
                            "Suspicious ore mining pattern: " + count + " " + 
                                    blockType.toString().replace("_", " ").toLowerCase()
                    );

                    // Reset counter
                    playerMining.put(blockType, 0);
                }
                break;
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        // Skip normal movement causes
        if (event.getCause() == PlayerTeleportEvent.TeleportCause.ENDER_PEARL ||
            event.getCause() == PlayerTeleportEvent.TeleportCause.COMMAND ||
            event.getCause() == PlayerTeleportEvent.TeleportCause.PLUGIN) {
            return;
        }

        Player player = event.getPlayer();

        // Skip if player has bypass permission
        if (player.hasPermission("ultimatestaff.automod.bypass")) return;

        // Check for suspicious teleportation
        Location from = event.getFrom();
        Location to = event.getTo();
        double distance = from.distance(to);

        if (distance > 30 && event.getCause() == PlayerTeleportEvent.TeleportCause.UNKNOWN) {
            plugin.getAutoModManager().processViolation(
                    player.getUniqueId(),
                    ViolationType.CHEATING,
                    "Suspicious teleport: " + Math.round(distance) + " blocks"
            );
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player attacker) || !(event.getEntity() instanceof Player victim)) {
            return;
        }

        // Skip if attacker has bypass permission
        if (attacker.hasPermission("ultimatestaff.automod.bypass")) return;

        UUID attackerUUID = attacker.getUniqueId();
        UUID victimUUID = victim.getUniqueId();

        // Record this attack
        Map<UUID, Long> attackerHistory = pvpAttacks.computeIfAbsent(attackerUUID, k -> new HashMap<>());
        long currentTime = System.currentTimeMillis();
        attackerHistory.put(victimUUID, currentTime);

        // Check for potential non-consensual PvP (repeated attacks against same player)
        int attackCount = 0;
        for (Map.Entry<UUID, Long> entry : attackerHistory.entrySet()) {
            if (entry.getKey().equals(victimUUID) && currentTime - entry.getValue() < PVP_TIMEOUT) {
                attackCount++;
            }
        }

        if (attackCount >= 5) { // 5 attacks within timeout period
            plugin.getAutoModManager().processViolation(
                    attackerUUID,
                    ViolationType.NON_CONSENSUAL_PVP,
                    "Repeated attacks against " + victim.getName()
            );

            // Reset counter
            attackerHistory.clear();
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        Material blockType = event.getBlock().getType();

        // Skip if player has bypass permission
        if (player.hasPermission("ultimatestaff.automod.bypass")) return;

        // Check for lag-inducing blocks
        if (blockType == Material.HOPPER || blockType == Material.OBSERVER || 
            blockType == Material.COMPARATOR || blockType == Material.REPEATER) {

            // Count redstone components in a small radius
            int count = 0;
            int radius = 5;
            Location loc = event.getBlock().getLocation();

            for (int x = -radius; x <= radius; x++) {
                for (int y = -radius; y <= radius; y++) {
                    for (int z = -radius; z <= radius; z++) {
                        Material type = loc.clone().add(x, y, z).getBlock().getType();
                        if (type == Material.HOPPER || type == Material.OBSERVER || 
                            type == Material.COMPARATOR || type == Material.REPEATER ||
                            type == Material.REDSTONE_WIRE) {
                            count++;
                        }
                    }
                }
            }

            if (count > 50) { // Arbitrary threshold
                plugin.getAutoModManager().processViolation(
                        player.getUniqueId(),
                        ViolationType.LAG_INDUCING,
                        "High density of redstone components: " + count + " in radius " + radius
                );
            }
        }
    }
}
