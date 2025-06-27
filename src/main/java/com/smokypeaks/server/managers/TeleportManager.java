// com.smokypeaks.server.managers.TeleportManager.java
package com.smokypeaks.server.managers;

import com.smokypeaks.Main;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.stream.Collectors;

public class TeleportManager {
    private final Main plugin;
    private final Map<UUID, Location> lastLocations = new HashMap<>();
    private final Map<UUID, Long> lastRandomTp = new HashMap<>();
    private final long randomTpCooldown = 10000; // 10 seconds cooldown

    public TeleportManager(Main plugin) {
        this.plugin = plugin;
    }

    public void teleportToPlayer(Player staff, Player target) {
        lastLocations.put(staff.getUniqueId(), staff.getLocation());
        staff.teleport(target.getLocation());
        staff.sendMessage("§6[Staff] §eTeleported to §f" + target.getName());
    }

    public void teleportToRandomPlayer(Player staff) {
        if (isOnCooldown(staff)) {
            staff.sendMessage("§c[Staff] §ePlease wait before using random teleport again!");
            return;
        }

        List<Player> possibleTargets = plugin.getServer().getOnlinePlayers().stream()
                .filter(p -> p != staff)
                .filter(p -> !plugin.getStaffModeManager().isInStaffMode(p))
                .collect(Collectors.toList());

        if (possibleTargets.isEmpty()) {
            staff.sendMessage("§c[Staff] §eNo suitable players to teleport to!");
            return;
        }

        Player target = possibleTargets.get(new Random().nextInt(possibleTargets.size()));
        lastLocations.put(staff.getUniqueId(), staff.getLocation());
        lastRandomTp.put(staff.getUniqueId(), System.currentTimeMillis());

        staff.teleport(target.getLocation());
        staff.sendMessage("§6[Staff] §eRandomly teleported to §f" + target.getName());
    }

    public void returnToPreviousLocation(Player staff) {
        Location lastLoc = lastLocations.get(staff.getUniqueId());
        if (lastLoc != null) {
            staff.teleport(lastLoc);
            lastLocations.remove(staff.getUniqueId());
            staff.sendMessage("§6[Staff] §eReturned to previous location");
        } else {
            staff.sendMessage("§c[Staff] §eNo previous location found!");
        }
    }

    public void teleportToLocation(Player staff, Location location) {
        lastLocations.put(staff.getUniqueId(), staff.getLocation());
        staff.teleport(location);
        staff.sendMessage("§6[Staff] §eTeleported to location");
    }

    private boolean isOnCooldown(Player staff) {
        Long lastUse = lastRandomTp.get(staff.getUniqueId());
        return lastUse != null &&
                System.currentTimeMillis() - lastUse < randomTpCooldown;
    }

    public void cleanup() {
        lastLocations.clear();
        lastRandomTp.clear();
    }
}
