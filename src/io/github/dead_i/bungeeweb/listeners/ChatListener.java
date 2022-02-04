package io.github.dead_i.bungeeweb.listeners;

import io.github.dead_i.bungeeweb.BungeeWeb;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.ChatEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.event.EventHandler;

public class ChatListener implements Listener {
    private Plugin plugin;

    public ChatListener(Plugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onChat(ChatEvent event) {
    	if (event.getSender() instanceof ProxiedPlayer) {
    		ProxiedPlayer player = (ProxiedPlayer) event.getSender();
    		String msg = event.getMessage();
            int type = 1;
            if (msg.startsWith("/")) {
                type = 2;
                if (BungeeWeb.getConfig().getList("hiddencommands").contains(msg.split(" ")[0].substring(1).toLowerCase())) return;
            }
            BungeeWeb.log(plugin, player, type, msg);
    	}
    }
}
