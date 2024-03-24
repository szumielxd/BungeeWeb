package me.szumielxd.proxyserverlist.bungee.commands;

import java.util.Objects;

import org.jetbrains.annotations.NotNull;

import me.szumielxd.proxyserverlist.bungee.ProxyServerListBungee;
import me.szumielxd.proxyyweb.common.commands.CommonCommand;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.api.plugin.TabExecutor;

public class BungeeCommandWrapper extends Command implements TabExecutor {
	
	
	private final @NotNull ProxyServerListBungee plugin;
	private final @NotNull CommonCommand<CommandSender> command;
	

	public BungeeCommandWrapper(@NotNull ProxyServerListBungee plugin, @NotNull CommonCommand<CommandSender> command) {
		super(command.getName(), command.getPermission(), command.getAliases());
		this.plugin = Objects.requireNonNull(plugin, "plugin cannot be null");
		this.command = Objects.requireNonNull(command, "command cannot be null");
	}


	@Override
	public Iterable<String> onTabComplete(CommandSender sender, String[] args) {
		return this.command.onTabComplete(sender, args);
	}


	@Override
	public void execute(CommandSender sender, String[] args) {
		this.command.execute(sender, args);
	}

}
