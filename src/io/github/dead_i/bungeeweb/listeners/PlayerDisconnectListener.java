package io.github.dead_i.bungeeweb.listeners;

import java.util.Optional;

import org.jetbrains.annotations.NotNull;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.proxy.server.ServerInfo;

import io.github.dead_i.bungeeweb.BungeeWeb;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class PlayerDisconnectListener {

	@NonNull private final @NotNull BungeeWeb plugin;

	@Subscribe
	public void onPlayerDisconnect(@NotNull DisconnectEvent event) {
		this.plugin.getDatabaseManager().logPlayerDisconnect(event.getPlayer());
		
		this.plugin.getPlayerInfoManager().getActiveSession(event.getPlayer())
				.ifPresent(session -> {
					// mark session as ready to unload
					session.setExpiration(Optional.of(System.currentTimeMillis() + 1_800_000));
					// update activity
					event.getPlayer().getCurrentServer()
							.map(ServerConnection::getServerInfo)
							.map(ServerInfo::getName)
							.ifPresent(session.getActivity()::updateActivity);
				});
	}
}
