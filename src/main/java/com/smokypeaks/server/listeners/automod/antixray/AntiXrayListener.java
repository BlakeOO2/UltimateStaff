package com.smokypeaks.server.listeners.automod.antixray;

import com.smokypeaks.Main;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;

public class AntiXrayListener implements Listener {
    private final Main plugin;
    private final AntiXrayEngine engine;

    public AntiXrayListener(Main plugin) {
        this.plugin = plugin;
        this.engine = new AntiXrayEngine(plugin);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlock();

        // Skip if player has bypass permission
        if (player.hasPermission("ultimatestaff.automod.bypass")) return;

        // Use the AntiXray engine to analyze the mining pattern
        engine.analyzeMiningPattern(player, block);
    }
}
