package io.github.dead_i.bungeeweb.listeners;

import java.net.InetSocketAddress;

import io.github.dead_i.bungeeweb.BungeeWeb;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.PostLoginEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.event.EventHandler;

public class PostLoginListener implements Listener {
    private Plugin plugin;

    public PostLoginListener(Plugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPostLogin(PostLoginEvent event) {
        ProxiedPlayer p = event.getPlayer();
        String host = (p.getSocketAddress() instanceof InetSocketAddress) ?
        		((InetSocketAddress) p.getSocketAddress()).getHostString() : p.getSocketAddress().toString();
        BungeeWeb.log(plugin, p, 3, host);
    }
}
