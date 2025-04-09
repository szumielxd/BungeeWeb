package io.github.dead_i.bungeeweb.listeners;

import org.jetbrains.annotations.NotNull;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.player.ServerConnectedEvent;
import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.proxy.server.ServerInfo;

import io.github.dead_i.bungeeweb.BungeeWeb;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class ServerConnectedListener {
	
	@NonNull private final @NotNull BungeeWeb plugin;

	@Subscribe
	public void onServerConnected(@NotNull ServerConnectedEvent event) {
		this.plugin.getDatabaseManager().logPlayerServerSwitch(event.getPlayer(), event.getServer().getServerInfo().getName());
		this.plugin.getPlayerInfoManager().getActiveSession(event.getPlayer())
		.ifPresent(session -> 
				// update activity
				event.getPlayer().getCurrentServer()
						.map(ServerConnection::getServerInfo)
						.map(ServerInfo::getName)
						.ifPresent(session.getActivity()::updateActivity));
	}
}
