package com.smokypeaks.server.listeners;

import com.smokypeaks.Main;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public class DeathListener implements Listener {
    private final Main plugin;

    public DeathListener(Main plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        List<ItemStack> drops = event.getDrops();

        // Create a copy of the drops to store
        ItemStack[] deathItems = drops.toArray(new ItemStack[0]);

        // Record the death location and items
        plugin.getDeathManager().recordDeath(player, deathItems);
    }
}
