// com.smokypeaks.bungee.listeners.AdminChatListener.java
package com.smokypeaks.bungee.listeners;

import com.smokypeaks.bungee.BungeeMain;
import com.smokypeaks.bungee.commands.AdminChatCommand;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.ChatEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

public class AdminChatListener implements Listener {
    private final BungeeMain plugin;
    private final AdminChatCommand adminChatCommand;

    public AdminChatListener(BungeeMain plugin, AdminChatCommand adminChatCommand) {
        this.plugin = plugin;
        this.adminChatCommand = adminChatCommand;
    }

    @EventHandler
    public void onChat(ChatEvent event) {
        if (event.isCancelled() || event.isCommand()) return;
        if (!(event.getSender() instanceof ProxiedPlayer player)) return;

        if (adminChatCommand.isInAdminChat(player.getUniqueId())) {
            event.setCancelled(true);
            adminChatCommand.broadcastAdminMessage(player, event.getMessage());
        }
    }
}
