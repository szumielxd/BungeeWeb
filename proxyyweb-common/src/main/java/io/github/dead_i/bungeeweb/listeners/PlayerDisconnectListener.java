package io.github.dead_i.bungeeweb.listeners;

import java.util.Optional;

import org.jetbrains.annotations.NotNull;

import io.github.dead_i.bungeeweb.BungeeWeb;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.Server;
import net.md_5.bungee.api.event.PlayerDisconnectEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

@RequiredArgsConstructor
public class PlayerDisconnectListener implements Listener {

	@NonNull private final @NotNull BungeeWeb plugin;

	@EventHandler
	public void onPlayerDisconnect(PlayerDisconnectEvent event) {
		this.plugin.getDatabaseManager().logPlayerDisconnect(event.getPlayer());
		
		this.plugin.getPlayerInfoManager().getActiveSession(event.getPlayer())
				.ifPresent(session -> {
					// mark session as ready to unload
					session.setExpiration(Optional.of(System.currentTimeMillis() + 1_800_000));
					// update activity
					Optional.ofNullable(event.getPlayer().getServer())
							.map(Server::getInfo)
							.map(ServerInfo::getName)
							.ifPresent(session.getActivity()::updateActivity);
				});
	}
}
