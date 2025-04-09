package io.github.dead_i.bungeeweb.listeners;

import org.jetbrains.annotations.NotNull;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.command.CommandExecuteEvent;
import com.velocitypowered.api.event.player.PlayerChatEvent;
import com.velocitypowered.api.proxy.Player;

import io.github.dead_i.bungeeweb.BungeeWeb;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class ChatListener {
	
	@NonNull private final @NotNull BungeeWeb plugin;

	@Subscribe
	public void onChat(@NotNull PlayerChatEvent event) {
		String msg = event.getMessage();
		if (msg.startsWith("/")) {
			logCommand(event.getPlayer(), msg.substring(1));
		} else {
			this.plugin.getDatabaseManager().logPlayerChat(event.getPlayer(), msg);
		}
	}
	
	@Subscribe
	public void onCommand(@NotNull CommandExecuteEvent event) {
		if (event.getCommandSource() instanceof Player player) {
			logCommand(player, event.getCommand());
		}
	}
	
	private void logCommand(@NotNull Player player, @NotNull String command) {
		command = removeLeadingSpaces(command);
		if (!this.plugin.getConfig().getList("command-security.hiddencommands").contains(command.split(" ")[0].toLowerCase())) {
			this.plugin.getDatabaseManager().logPlayerCommand(player, "/" + command);
		}
	}
	
	private @NotNull String removeLeadingSpaces(@NotNull String text) {
		int index = 0;
		while (index < text.length() && text.charAt(index) == ' ') {
			index++;
		}
		return text.substring(index);
	}
	
}
