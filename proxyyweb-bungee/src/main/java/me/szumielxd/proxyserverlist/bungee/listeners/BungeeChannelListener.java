package me.szumielxd.proxyserverlist.bungee.listeners;

import org.jetbrains.annotations.NotNull;

import me.szumielxd.proxyserverlist.bungee.ProxyServerListBungee;
import me.szumielxd.proxyserverlist.common.listeners.ChannelListener;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.PluginMessageEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

public class BungeeChannelListener extends ChannelListener<CommandSender, ProxiedPlayer> implements Listener {
	
	
	public BungeeChannelListener(@NotNull ProxyServerListBungee plugin) {
		super(plugin);
	}
	
	
	@EventHandler
	public void onPluginChannel(PluginMessageEvent event) {
		if (event.getReceiver() instanceof ProxiedPlayer) {
			boolean handled = this.onChannelMessage(event.getTag(), (ProxiedPlayer) event.getReceiver(), event.getData());
			if (handled) event.setCancelled(true);
		}
	}
	

}
