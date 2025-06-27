// com.smokypeaks.server.managers.FreezeManager.java
package com.smokypeaks.server.managers;

import com.smokypeaks.Main;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.HashSet;
import java.util.UUID;

public class FreezeManager {
    private final Main plugin;
    private final HashSet<UUID> frozenPlayers = new HashSet<>();

    public FreezeManager(Main plugin) {
        this.plugin = plugin;
    }

    public void toggleFreeze(Player target, Player staff) {
        UUID uuid = target.getUniqueId();
        if (frozenPlayers.contains(uuid)) {
            unfreezePlayer(target, staff);
        } else {
            freezePlayer(target, staff);
        }
    }

    public void freezePlayer(Player target, Player staff) {
        UUID uuid = target.getUniqueId();
        if (frozenPlayers.contains(uuid)) return; // Prevent double freeze

        frozenPlayers.add(uuid);
        target.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, Integer.MAX_VALUE, 100, false, false));
        target.addPotionEffect(new PotionEffect(PotionEffectType.JUMP, Integer.MAX_VALUE, 128, false, false));

        target.sendMessage("§c[Staff] §eYou have been frozen by a staff member!");
        staff.sendMessage("§6[Staff] §eYou have frozen §f" + target.getName());
    }

    public void unfreezePlayer(Player target, Player staff) {
        UUID uuid = target.getUniqueId();
        if (!frozenPlayers.contains(uuid)) return; // Prevent double unfreeze

        frozenPlayers.remove(uuid);
        target.removePotionEffect(PotionEffectType.SLOW);
        target.removePotionEffect(PotionEffectType.JUMP);

        target.sendMessage("§a[Staff] §eYou have been unfrozen!");
        if (staff != null) {
            staff.sendMessage("§6[Staff] §eYou have unfrozen §f" + target.getName());
        }
    }

    public void cleanup() {
        // Unfreeze all players
        for (UUID uuid : new HashSet<>(frozenPlayers)) {
            Player player = plugin.getServer().getPlayer(uuid);
            if (player != null) {
                unfreezePlayer(player, null);
            }
        }
        frozenPlayers.clear();
    }

    public boolean isFrozen(Player player) {
        return frozenPlayers.contains(player.getUniqueId());
    }
}
