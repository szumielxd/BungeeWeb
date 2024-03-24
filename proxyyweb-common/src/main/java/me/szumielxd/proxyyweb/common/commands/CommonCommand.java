package me.szumielxd.proxyyweb.common.commands;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class CommonCommand<T> {
	
	
	private final @NotNull String name;
	private final @Nullable String permission;
	private final @NotNull String[] aliases;
	
	
	protected CommonCommand(@NotNull String name, @Nullable String permission, @NotNull String... aliases) {
		this.name = Objects.requireNonNull(name, "name cannot be null");
		this.permission = permission;
		this.aliases = Arrays.copyOf(Objects.requireNonNull(aliases, "aliases cannot be null"), aliases.length);
	}
	
	
	public abstract void execute(@NotNull T sender, @NotNull String[] args);
	
	
	public @NotNull List<String> onTabComplete(@NotNull T sender, @NotNull String[] args) {
		return new ArrayList<>();
	}
	
	
	public @NotNull String getName() {
		return this.name;
	}
	
	public @Nullable String getPermission() {
		return this.permission;
	}
	
	public @NotNull String[] getAliases() {
		return this.aliases;
	}
	

}
