package me.szumielxd.proxyserverlist.velocity.objects;

import java.net.SocketAddress;
import java.util.Collection;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.BiConsumer;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.google.gson.JsonElement;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.ConnectionRequestBuilder;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.api.proxy.server.ServerInfo;
import com.velocitypowered.api.util.Favicon;

import lombok.Getter;
import me.szumielxd.proxyserverlist.velocity.ProxyServerListVelocity;
import me.szumielxd.proxyyweb.common.objects.SenderWrapper;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;

public class VelocitySenderWrapper implements SenderWrapper<CommandSource, Player, Component> {

	
	private final @Getter @NotNull ProxyServerListVelocity plugin;
	
	
	public VelocitySenderWrapper(@NotNull ProxyServerListVelocity plugin) {
		this.plugin = Objects.requireNonNull(plugin, "plugin cannot be null");
	}
	
	
	@Override
	public @NotNull UUID getUniqueId(@NotNull Player player) {
		return Objects.requireNonNull(player, "player cannot be null").getUniqueId();
	}

	@Override
	public @NotNull String getName(@NotNull CommandSource sender) {
		Objects.requireNonNull(sender, "sender cannot be null");
		if (sender instanceof Player player) return player.getUsername();
		return "Console";
	}

	@Override
	public int getProtocolVersion(@NotNull Player player) {
		Objects.requireNonNull(player, "player cannot be null");
		return player.getProtocolVersion().getProtocol();
	}
	
	@Override
	public void sendMessage(@NotNull CommandSource sender, @NotNull Component message) {
		Objects.requireNonNull(sender, "sender cannot be null");
		Objects.requireNonNull(message, "message cannot be null");
		sender.sendMessage(message);
	}
	
	@Override
	public boolean hasPermission(@NotNull CommandSource sender, @NotNull String permission) {
		return Objects.requireNonNull(sender, "sender cannot be null").hasPermission(permission);
	}

	@Override
	public void connectToServer(@NotNull Player player, @NotNull String server, @Nullable BiConsumer<Player, Boolean> resultConsumer) {
		Objects.requireNonNull(player, "player cannot be null");
		Objects.requireNonNull(server, "server cannot be null");
		plugin.getProxy().getServer(server).map(player::createConnectionRequest).map(ConnectionRequestBuilder::connectWithIndication).ifPresent(future -> {
			if (resultConsumer != null) future.thenAccept(result -> resultConsumer.accept(player, result));
		});
	}

	@Override
	public @NotNull CommandSource getConsole() {
		return this.plugin.getProxy().getConsoleCommandSource();
	}

	@Override
	public boolean isPlayer(@Nullable CommandSource sender) {
		return sender instanceof Player;
	}

	@Override
	public boolean isOnline(@NotNull Player player) {
		return player.isActive();
	}

	@Override
	public @NotNull Optional<Player> getPlayer(@NotNull UUID uuid) {
		return this.plugin.getProxy().getPlayer(uuid);
	}

	@Override
	public @NotNull Optional<Player> getPlayer(@NotNull String playerName) {
		return this.plugin.getProxy().getPlayer(playerName);
	}

	@Override
	public @NotNull Collection<Player> getPlayers() {
		return this.plugin.getProxy().getAllPlayers();
	}
	
	@Override
	public int getPlayerCount() {
		return this.plugin.getProxy().getPlayerCount();
	}

	@Override
	public @NotNull Optional<Collection<Player>> getPlayers(@NotNull String serverName) {
		return this.plugin.getProxy().getServer(serverName).map(RegisteredServer::getPlayersConnected);
	}

	@Override
	public @Nullable Component jsonToBase(@Nullable JsonElement jsonElement) {
		if (jsonElement == null) {
			return null;
		}
		return GsonComponentSerializer.gson().deserializeFromTree(jsonElement);
	}

	@Override
	public @Nullable JsonElement baseToJson(@Nullable Object component) {
		if (component == null) {
			return null;
		}
		return GsonComponentSerializer.gson().serializeToTree((Component) component);
	}

	@Override
	public @Nullable Component componentToBase(@Nullable Component component) {
		return component;
	}

	@Override
	public @Nullable Component baseToComponent(@Nullable Object component) {
		return (Component) component;
	}

	@Override
	public @NotNull Optional<SocketAddress> getServerAddress(@NotNull String serverName) {
		return this.plugin.getProxy().getServer(serverName).map(RegisteredServer::getServerInfo).map(ServerInfo::getAddress);
	}
	
	@Override
	public @NotNull Optional<String> getFavicon() {
		return this.plugin.getProxy().getConfiguration().getFavicon().map(Favicon::getBase64Url);
	}
	
	@Override
	public int getMaxPlayers() {
		return this.plugin.getProxy().getConfiguration().getShowMaxPlayers();
	}

}
