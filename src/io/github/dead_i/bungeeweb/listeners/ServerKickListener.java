package io.github.dead_i.bungeeweb.listeners;

import java.util.Optional;

import org.jetbrains.annotations.NotNull;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.player.KickedFromServerEvent;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.api.proxy.server.ServerInfo;

import io.github.dead_i.bungeeweb.BungeeWeb;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

@RequiredArgsConstructor
public class ServerKickListener {
	
	@NonNull private final @NotNull BungeeWeb plugin;

	@Subscribe
	public void onServerKick(@NotNull KickedFromServerEvent event) {
		this.plugin.getDatabaseManager().logPlayerKick(
				event.getPlayer(),
				Optional.ofNullable(event.getServer())
						.map(RegisteredServer::getServerInfo)
						.map(ServerInfo::getName)
						.orElse(""),
				event.getServerKickReason()
						.map(LegacyComponentSerializer.legacySection()::serialize)
						.map(s -> s.length() > 255 ? s.substring(0, 255) : s)
						.orElse(""));
	}
}
