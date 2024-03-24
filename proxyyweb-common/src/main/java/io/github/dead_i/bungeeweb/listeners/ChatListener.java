package io.github.dead_i.bungeeweb.listeners;

import org.jetbrains.annotations.NotNull;

import io.github.dead_i.bungeeweb.BungeeWeb;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.ChatEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

@RequiredArgsConstructor
public class ChatListener implements Listener {
	
	@NonNull private final @NotNull BungeeWeb plugin;

	@EventHandler
	public void onChat(@NotNull ChatEvent event) {
		if (event.getSender() instanceof ProxiedPlayer player) {
			String msg = event.getMessage();
			if (msg.startsWith("/")) {
				if (!this.plugin.getConfig().getList("hiddencommands").contains(msg.split(" ")[0].substring(1).toLowerCase())) {
					this.plugin.getDatabaseManager().logPlayerCommand(player, msg);
				}
			} else {
				this.plugin.getDatabaseManager().logPlayerChat(player, msg);
			}
		}
	}
}
