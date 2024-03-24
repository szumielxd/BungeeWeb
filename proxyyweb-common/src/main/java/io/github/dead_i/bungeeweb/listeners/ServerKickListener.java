package io.github.dead_i.bungeeweb.listeners;

import java.util.Optional;

import org.jetbrains.annotations.NotNull;

import io.github.dead_i.bungeeweb.BungeeWeb;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.event.ServerKickEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.chat.ComponentSerializer;
import net.md_5.bungee.event.EventHandler;

@RequiredArgsConstructor
public class ServerKickListener implements Listener {
	
	@NonNull private final @NotNull BungeeWeb plugin;

	@EventHandler
	public void onServerKick(ServerKickEvent event) {
		this.plugin.getDatabaseManager().logPlayerKick(event.getPlayer(), Optional.ofNullable(event.getCancelServer()).map(ServerInfo::getName).orElse(""), ComponentSerializer.toString(event.getKickReasonComponent()));
	}
}
