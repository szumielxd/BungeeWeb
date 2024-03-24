package me.szumielxd.proxyserverlist.velocity.commands;

import java.util.List;
import java.util.Objects;

import org.jetbrains.annotations.NotNull;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;

import me.szumielxd.proxyserverlist.velocity.ProxyServerListVelocity;
import me.szumielxd.proxyyweb.common.commands.CommonCommand;

public class VelocityCommandWrapper implements SimpleCommand {
	
	
	private final @NotNull ProxyServerListVelocity plugin;
	private final @NotNull CommonCommand<CommandSource> command;
	
	
	public VelocityCommandWrapper(@NotNull ProxyServerListVelocity plugin, @NotNull CommonCommand<CommandSource> command) {
		this.plugin = Objects.requireNonNull(plugin, "plugin cannot be null");
		this.command = Objects.requireNonNull(command, "command cannot be null");
	}
	

	@Override
	public void execute(@NotNull Invocation invocation) {
		this.command.execute(invocation.source(), invocation.arguments());
	}
	
	
	@Override
	public List<String> suggest(@NotNull Invocation invocation) {
		return this.command.onTabComplete(invocation.source(), invocation.arguments());
	}
	
	
	@Override
	public boolean hasPermission(@NotNull Invocation invocation) {
		return this.command.getPermission() == null || invocation.source().hasPermission(this.command.getPermission());
	}

}
