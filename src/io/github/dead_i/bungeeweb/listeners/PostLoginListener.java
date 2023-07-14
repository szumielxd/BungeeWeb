package io.github.dead_i.bungeeweb.listeners;

import org.jetbrains.annotations.NotNull;

import io.github.dead_i.bungeeweb.BungeeWeb;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import net.md_5.bungee.api.event.PostLoginEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

@RequiredArgsConstructor
public class PostLoginListener implements Listener {

	@NonNull private final @NotNull BungeeWeb plugin;

    @EventHandler
    public void onPostLogin(@NotNull PostLoginEvent event) {
        this.plugin.getDatabaseManager().logPlayerConnect(event.getPlayer());
    }
}
