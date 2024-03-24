package me.szumielxd.proxyserverlist.velocity.listeners;

import org.jetbrains.annotations.NotNull;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.PluginMessageEvent;
import com.velocitypowered.api.event.connection.PluginMessageEvent.ForwardResult;
import com.velocitypowered.api.proxy.Player;

import me.szumielxd.proxyserverlist.common.listeners.ChannelListener;
import me.szumielxd.proxyserverlist.velocity.ProxyServerListVelocity;

public class VelocityChannelListener extends ChannelListener<CommandSource, Player> {

	public VelocityChannelListener(@NotNull ProxyServerListVelocity plugin) {
		super(plugin);
	}
	
	
	@Subscribe
	public void onPluginChannel(PluginMessageEvent event) {
		if (event.getTarget() instanceof Player) {
			boolean handled = this.onChannelMessage(event.getIdentifier().getId(), (Player) event.getTarget(), event.getData());
			if (handled) event.setResult(ForwardResult.handled());
		}
	}
	

}
