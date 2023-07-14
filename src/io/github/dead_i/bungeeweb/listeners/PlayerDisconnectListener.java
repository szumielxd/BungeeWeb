package io.github.dead_i.bungeeweb.listeners;

import org.jetbrains.annotations.NotNull;

import io.github.dead_i.bungeeweb.BungeeWeb;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import net.md_5.bungee.api.event.PlayerDisconnectEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

@RequiredArgsConstructor
public class PlayerDisconnectListener implements Listener {

	@NonNull private final @NotNull BungeeWeb plugin;

    @EventHandler
    public void onPlayerDisconnect(PlayerDisconnectEvent event) {
    	this.plugin.getDatabaseManager().logPlayerDisconnect(event.getPlayer());
    }
}
