package io.github.dead_i.bungeeweb.listeners;

import java.util.Optional;

import org.jetbrains.annotations.NotNull;

import io.github.dead_i.bungeeweb.BungeeWeb;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.Server;
import net.md_5.bungee.api.event.ServerConnectedEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

@RequiredArgsConstructor
public class ServerConnectedListener implements Listener {
	
	@NonNull private final @NotNull BungeeWeb plugin;

	@EventHandler
	public void onServerConnected(ServerConnectedEvent event) {
		this.plugin.getDatabaseManager().logPlayerServerSwitch(event.getPlayer(), event.getServer().getInfo().getName());
		this.plugin.getPlayerInfoManager().getActiveSession(event.getPlayer())
		.ifPresent(session -> {
			// update activity
			Optional.ofNullable(event.getPlayer().getServer())
					.map(Server::getInfo)
					.map(ServerInfo::getName)
					.ifPresent(session.getActivity()::updateActivity);
		});
	}
}
