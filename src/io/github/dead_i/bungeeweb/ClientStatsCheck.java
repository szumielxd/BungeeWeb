package io.github.dead_i.bungeeweb;

import static io.github.dead_i.bungeeweb.hikari.HikariDB.TABLE_CLIENT_STATS;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.jetbrains.annotations.NotNull;

import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.config.Configuration;

public class ClientStatsCheck implements Runnable {
	
	private final @NotNull BungeeWeb plugin;

	public ClientStatsCheck(BungeeWeb plugin) {
		this.plugin = plugin;
	}

	@Override
	public void run() {
		Map<Long, Map<Long, Long>> stats = Stream.concat(Stream.of(""), this.plugin.getProxy().getServers().values().parallelStream()
				.map(ServerInfo::getName))
				.map(plugin.getServerIdManager()::getServerId)
				.distinct()
				.collect(Collectors.toMap(Function.identity(), v -> new HashMap<>(
						Map.of(this.plugin.getClientIdManager().getDefaultClientId(), 0L))));
		
		try (Connection conn = this.plugin.getDatabaseManager().connect()) {
			fillData(stats);
			var results = stats.entrySet().stream()
					.flatMap(e -> e.getValue().entrySet().stream()
							.map(e2 -> new Long[] { e.getKey(), e2.getKey(), e2.getValue() }))
					.toArray(Long[][]::new);
			Instant now = Instant.ofEpochSecond(System.currentTimeMillis() / 60_000 * 60); // last full minute
			String sql = "INSERT INTO `%1$s` (`time`, `server_id`, `client_id`, `playercount`) VALUES ".formatted(TABLE_CLIENT_STATS)
					+ ", (?, ?, ?, ?)".repeat(results.length).substring(2);
			int i = 1;
			try (PreparedStatement stm = conn.prepareStatement(sql)) {
				for (var arr : results) {
					stm.setTimestamp(i++, Timestamp.from(now));
					stm.setLong(i++, arr[0]);
					stm.setLong(i++, arr[1]);
					stm.setLong(i++, arr[2]);
				}
				stm.executeUpdate();
			}
		} catch (SQLException e) {
			plugin.getLogger().warning("An error occurred when executing the database query to update the statistics.");
			e.printStackTrace();
		}
	}
	
	private void fillData(@NotNull Map<Long, Map<Long, Long>> stats) {
		Configuration config = this.plugin.getConfig();
		if (config.getBoolean("stats.playercount")) {
			fillPlayercountData(stats);
		}
	}
	
	private void fillPlayercountData(@NotNull Map<Long, Map<Long, Long>> stats) {
		ServerIdManager srvIdMgr = this.plugin.getServerIdManager();
		this.plugin.getProxy().getServers().values().stream()
				.forEach(info -> Optional.of(srvIdMgr.getServerId(info.getName()))
						.map(stats::get)
						.ifPresent(srv -> srv.putAll(this.mapPlayercountByClient(info.getPlayers()))));
		stats.get(srvIdMgr.getServerId("")).putAll(this.mapPlayercountByClient(this.plugin.getProxy().getPlayers()));
	}
	
	private @NotNull Map<Long, Long> mapPlayercountByClient(Collection<ProxiedPlayer> players) {
		var clientMgr = this.plugin.getClientIdManager();
		var clientCount = players.stream()
				.map(this.plugin.getPlayerInfoManager()::getActiveSession)
				.flatMap(Optional::stream)
				.map(s -> s.getClient())
				.collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
		return clientCount.entrySet().stream()
				.collect(Collectors.toMap(e -> clientMgr.getClientId(e.getKey()), Entry::getValue));
	}
	
}
