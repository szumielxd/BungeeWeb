package io.github.dead_i.bungeeweb.listeners;

import org.jetbrains.annotations.NotNull;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.PostLoginEvent;

import io.github.dead_i.bungeeweb.BungeeWeb;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class PostLoginListener {

	@NonNull private final @NotNull BungeeWeb plugin;

	@Subscribe
	public void onPostLogin(@NotNull PostLoginEvent event) {
		this.plugin.getDatabaseManager().logPlayerConnect(event.getPlayer());
	}
}
