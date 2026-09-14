package io.github.dead_i.bungeeweb.listeners;

import org.jetbrains.annotations.NotNull;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.player.PlayerClientBrandEvent;

import io.github.dead_i.bungeeweb.BungeeWeb;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class ChannelListener {

	@NonNull private final @NotNull BungeeWeb plugin;
	
	@Subscribe
	public void onPluginMessage(PlayerClientBrandEvent event) {
		this.plugin.getPlayerInfoManager().getActiveSession(event.getPlayer().getUniqueId())
				.ifPresent(session -> session.setClient(event.getBrand()));
	}

}
