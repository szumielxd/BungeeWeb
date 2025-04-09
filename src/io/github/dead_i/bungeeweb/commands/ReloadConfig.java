package io.github.dead_i.bungeeweb.commands;

import org.jetbrains.annotations.NotNull;

import com.velocitypowered.api.command.SimpleCommand;

import io.github.dead_i.bungeeweb.BungeeWeb;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

public class ReloadConfig implements SimpleCommand {
	
	private @NotNull BungeeWeb plugin;

	public ReloadConfig(@NotNull BungeeWeb plugin) {
		this.plugin = plugin;
	}
	
	@Override
	public boolean hasPermission(Invocation invocation) {
		return invocation.source().hasPermission("bungeeweb.reload");
	}

	@Override
	public void execute(Invocation invocation) {
		this.plugin.reloadConfig();
		invocation.source().sendMessage(Component.text("The BungeeWeb configuration has been reloaded. Please note that certain changes may require a proxy restart to take effect.", NamedTextColor.RED));
	}
}
