package io.github.dead_i.bungeeweb;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;

import org.jetbrains.annotations.NotNull;

import io.github.dead_i.bungeeweb.hikari.HikariDB;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import net.md_5.bungee.api.connection.ProxiedPlayer;

@RequiredArgsConstructor
public class PlayerInfoManager {

	private final @NotNull Map<UUID, PlayerSession> sessions = new ConcurrentHashMap<>();
	private final @NotNull Map<UUID, Long> playerIds = new ConcurrentHashMap<>();
	private final @NotNull Map<UUID, CompletableFuture<Long>> playerIdQueries = new ConcurrentHashMap<>();
	private @NotNull HikariDB hikari;
	
	public @NotNull Optional<PlayerSession> getActiveSession(@NotNull UUID uuid) {
		return Optional.ofNullable(this.sessions.get(uuid));
	}
	
	public @NotNull PlayerSession createNewSession(@NotNull ProxiedPlayer player) {
		long playerId = getPlayerId(player);
		SocketAddress address = player.getPendingConnection().getSocketAddress();
		String ip = address instanceof InetSocketAddress inet ? inet.getHostString() : "0.0.0.0";
		String host = Optional.ofNullable(player.getPendingConnection().getVirtualHost())
				.map(InetSocketAddress::getHostString)
				.orElse("");
		PlayerSession session = new PlayerSession(playerId, player.getPendingConnection().getVersion(), ip, host);
		this.hikari.insertPlayerSession(session);
		this.sessions.put(player.getUniqueId(), session);
		return session;
	}
	
	public long getPlayerId(@NotNull ProxiedPlayer player) {
		Long id = this.playerIds.get(player.getUniqueId());
		if (id == null) {
			try {
				final String name = player.getName();
				return playerIdQueries.computeIfAbsent(player.getUniqueId(),
						uuid -> CompletableFuture.supplyAsync(
								() -> {
									long playerId = hikari.getOrCreatePlayerId(uuid, name);
									playerIds.put(uuid, playerId);
									playerIdQueries.remove(uuid);
									return playerId;
								}))
						.get();
			} catch (ExecutionException e) {
				playerIdQueries.remove(player.getUniqueId());
				throw new RuntimeException(e);
			} catch (InterruptedException e) {
				playerIdQueries.remove(player.getUniqueId());
				Thread.currentThread().interrupt();
				throw new RuntimeException(e);
			}
		}
		return id;
	}
	
	
	@Getter
	@RequiredArgsConstructor
	public static class PlayerSession {
		
		@Setter private long id = -1;
		private final long playerId;
		private final int protocol;
		private final @NotNull String ip;
		private @NotNull String client = "";
		private final @NotNull String hostname;
		@Setter private @NotNull Optional<Long> expiration = Optional.empty();
		
		
		
	}

}
