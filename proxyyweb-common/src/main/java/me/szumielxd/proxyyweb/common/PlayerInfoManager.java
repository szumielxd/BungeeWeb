package me.szumielxd.proxyyweb.common;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import me.szumielxd.proxyyweb.common.objects.CommonScheduler.ExecutedTask;
import me.szumielxd.proxyyweb.common.objects.SenderWrapper;

@RequiredArgsConstructor
public class PlayerInfoManager<T, U extends T> {

	private final @NotNull Map<UUID, PlayerSession> sessions = new ConcurrentHashMap<>();
	private final @NotNull Map<UUID, Long> playerIds = new ConcurrentHashMap<>();
	private final @NotNull Map<UUID, CompletableFuture<Long>> playerIdQueries = new ConcurrentHashMap<>();
	private final @NotNull ProxyyWeb<T, U, ?> plugin;
	private @Nullable ExecutedTask updateTask = null;
	
	
	public void start() {
		this.updateTask = this.plugin.getScheduler().runTaskTimer(() -> {
			this.updateActivity();
			this.saveActivity();
			this.updateSessionState();
		}, 1, 1, TimeUnit.MINUTES);
	}
	
	public void stop() {
		if (this.updateTask == null) {
			throw new IllegalStateException("PlayerInfoManager is not started");
		}
		this.updateTask.cancel();
		this.updateTask = null;
	}
	
	public @NotNull Optional<PlayerSession> getActiveSession(@NotNull UUID uuid) {
		return Optional.ofNullable(this.sessions.get(uuid));
	}
	
	public @NotNull Optional<PlayerSession> getActiveSession(@NotNull U player) {
		return getActiveSession(this.plugin.getSenderWrapper().getUniqueId(player));
	}
	
	public @NotNull PlayerSession createNewSession(@NotNull U player) {
		long playerId = getPlayerId(player);
		UUID playerUuid = this.plugin.getSenderWrapper().getUniqueId(player);
		SocketAddress address = player.getPendingConnection().getSocketAddress();
		String ip = address instanceof InetSocketAddress inet ? inet.getHostString() : "0.0.0.0";
		String host = Optional.ofNullable(player.getPendingConnection().getVirtualHost())
				.map(InetSocketAddress::getHostString)
				.orElse("");
		PlayerSession session = new PlayerSession(playerId, player.getPendingConnection().getVersion(), ip, host);
		this.plugin.getDatabaseManager().insertPlayerSession(session);
		this.sessions.put(playerUuid, session);
		return session;
	}
	
	public long getPlayerId(@NotNull U player) {
		UUID playerUuid = this.plugin.getSenderWrapper().getUniqueId(player);
		Long id = this.playerIds.get(playerUuid);
		if (id == null) {
			try {
				final String name = this.plugin.getSenderWrapper().getName(player);
				return playerIdQueries.computeIfAbsent(playerUuid,
						uuid -> CompletableFuture.supplyAsync(
								() -> {
									long playerId = this.plugin.getDatabaseManager().getOrCreatePlayerId(uuid, name);
									playerIds.put(uuid, playerId);
									playerIdQueries.remove(uuid);
									return playerId;
								}))
						.get();
			} catch (ExecutionException e) {
				playerIdQueries.remove(playerUuid);
				throw new RuntimeException(e);
			} catch (InterruptedException e) {
				playerIdQueries.remove(playerUuid);
				Thread.currentThread().interrupt();
				throw new RuntimeException(e);
			}
		}
		return id;
	}
	
	private void updateActivity() {
		SenderWrapper<T, U, ?> swrapper = this.plugin.getSenderWrapper();
		this.sessions.forEach((uuid, session) -> {
			swrapper.getPlayer(uuid).filter(swrapper::isPlayer).ifPresent(player -> {
				
			});
			if (player != null && player.isConnected() && player.getServer() != null) {
				String serverName = player.getServer().getInfo().getName();
				session.getActivity().updateActivity(serverName);
			}
		});
	}
	
	private void saveActivity() {
		this.plugin.getDatabaseManager().updateSessionTime(this.sessions.values());
	}
	
	private void updateSessionState() {
		this.sessions.entrySet().removeIf(e -> {
			Optional<Long> expiration = e.getValue().getExpiration();
			if (expiration.filter(val -> System.currentTimeMillis() - val > 0).isPresent()) {
				return true;
			}
			ProxiedPlayer player = this.plugin.getProxy().getPlayer(e.getKey());
			if (player == null || !player.isConnected() && expiration.isEmpty()) {
				e.getValue().setExpiration(Optional.of(System.currentTimeMillis() + 1_800_000));
			}
			return false;
		});
	}
	
	
	@Getter
	@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
	public class PlayerSession {
		
		@Setter private long id = -1;
		private final long playerId;
		private final int protocol;
		private final @NotNull String ip;
		private @NotNull long clientId = plugin.getClientIdManager().getDefaultClientId();
		private final @NotNull String hostname;
		private final @NotNull HourlyActivity activity = new HourlyActivity();
		@Setter private @NotNull Optional<Long> expiration = Optional.empty();
		
		public CompletableFuture<Void> setClient(@NotNull String client) {
			this.clientId = plugin.getClientIdManager().getClientId(trimClientName(client));
			return CompletableFuture.runAsync(() -> plugin.getDatabaseManager().setSessionClientBrand(this));
		}
		
		private String trimClientName(String client) {
			int index;
			if ((index = client.indexOf(':')) > -1) {
				client = client.substring(0, index);
			}
			if ((index = client.indexOf('@')) > -1) {
				client = client.substring(0, index);
			}
			return client;
		}
		
		
		public class HourlyActivity {
			
			private long lastUpdate = System.currentTimeMillis() / 1000; // seconds
			private long lastFullHour = lastUpdate / 3600 * 3600; // last full hour in seconds
			private final Map<Long, Map<Long, Integer>> toUpdate = new HashMap<>(); // activity entries to to update grouped by fullHour
			
			public synchronized void updateActivity(@NotNull String serverName) {
				long serverId = plugin.getServerIdManager().getServerId(serverName);
				long now = System.currentTimeMillis() / 1000;
				long currentHour = now / 3600 * 3600;
				// loop for each full hour since last update
				for (long hour = this.lastFullHour; hour <= currentHour; hour += 3600) {
					long min = Math.max(this.lastUpdate, hour);
					long max = Math.min(hour + 7200, now);
					int toAdd = (int) (max - min);
					if (toAdd > 0) {
						this.toUpdate.computeIfAbsent(this.lastFullHour, k -> new HashMap<>())
								.compute(serverId, (srv, val) -> (val == null ? 0 : val) + toAdd);
					}
				}
				this.lastFullHour = currentHour;
				this.lastUpdate = now;
			}
			
			public List<ActivityUpdateEntry> fetchAndClearActivityEntries() {
				List<ActivityUpdateEntry> entries = new LinkedList<>();
				this.toUpdate.forEach((time, map) -> {
					map.replaceAll((serverId, seconds) -> {
						int minutes = seconds / 60;
						if (minutes > 0) {
							entries.add(new ActivityUpdateEntry(id, serverId, time, minutes));
							seconds -= minutes * 60;
						}
						return seconds;
					});
					map.values().removeIf(s -> s == 0);
				});
				this.toUpdate.keySet().removeIf(time -> time != this.lastFullHour);
				return entries;
			}
			
			public record ActivityUpdateEntry(long sessionId, long serverId, long time, int minutes) {}
			
			
			
		}
		
		
		
	}

}
