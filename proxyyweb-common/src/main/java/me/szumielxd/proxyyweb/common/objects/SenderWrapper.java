package me.szumielxd.proxyyweb.common.objects;

import java.net.SocketAddress;
import java.util.Collection;
import java.util.Optional;
import java.util.UUID;
import java.util.function.BiConsumer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.google.gson.JsonElement;

import me.szumielxd.legacyminiadventure.VersionableObject;
import me.szumielxd.legacyminiadventure.VersionableObject.ChatVersion;
import me.szumielxd.proxyserverlist.common.ProxyServerList;

public interface SenderWrapper<T, U extends T, C> {
	
	
	public @NotNull UUID getUniqueId(@NotNull U player);
	
	public @NotNull String getName(@NotNull T sender);
	
	public int getProtocolVersion(@NotNull U player);
	
	public boolean hasPermission(@NotNull T sender, @NotNull String permission);
	
	public void sendMessage(@NotNull T sender, @NotNull C message);
	
	public default void sendMessage(@NotNull T sender, @NotNull VersionableObject<C> message) {
		sendMessage(sender, getCorrectMessage(sender, message));
	}
	
	public default C getCorrectMessage(@NotNull T sender, @NotNull VersionableObject<C> message) {
		@SuppressWarnings("unchecked")
		Optional<Integer> version = isPlayer(sender) ? Optional.of(getProtocolVersion((U) sender)) : Optional.empty();
		return message.get(ChatVersion.getCorrect(version));
	}

	public default void connectToServer(@NotNull U player, @NotNull String server) {
		this.connectToServer(player, server, null);
	}
	
	public void connectToServer(@NotNull U player, @NotNull String server, @Nullable BiConsumer<U, Boolean> resultConsumer);
	
	public @NotNull T getConsole();
	
	public boolean isPlayer(@Nullable T sender);
	
	public boolean isOnline(@NotNull U sender);

	public @NotNull Optional<U> getPlayer(@NotNull UUID uuid);
	
	public @NotNull Optional<U> getPlayer(@NotNull String playerName);
	
	public @NotNull Collection<U> getPlayers();
	
	public int getPlayerCount();
	
	public @NotNull Optional<Collection<U>> getPlayers(@NotNull String serverName);
	
	public @Nullable Object componentToBase(@Nullable C component);
	
	public @Nullable C baseToComponent(@Nullable Object component);
	
	public @Nullable Object jsonToBase(@Nullable JsonElement component);
	
	public @Nullable JsonElement baseToJson(@Nullable Object component);
	
	public @NotNull Optional<SocketAddress> getServerAddress(@NotNull String serverName);
	
	public @NotNull ProxyServerList<T, U, C> getPlugin();
	
	public @NotNull Optional<String> getFavicon();
	
	public int getMaxPlayers();
	

}
