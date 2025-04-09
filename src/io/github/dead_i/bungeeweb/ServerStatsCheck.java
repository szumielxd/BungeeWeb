package io.github.dead_i.bungeeweb;

import static io.github.dead_i.bungeeweb.hikari.HikariDB.TABLE_LOGS;
import static io.github.dead_i.bungeeweb.hikari.HikariDB.TABLE_SERVER_STATS;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.jetbrains.annotations.NotNull;

import com.moandjiezana.toml.Toml;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.api.proxy.server.ServerInfo;

import lombok.Data;

public class ServerStatsCheck implements Runnable {
	
	private final @NotNull BungeeWeb plugin;
	private long lastId = 0;

	public ServerStatsCheck(BungeeWeb plugin) {
		this.plugin = plugin;
		try (Connection conn = this.plugin.getDatabaseManager().connect()) {
			try (Statement stm = conn.createStatement()) {
				try (ResultSet rs = stm.executeQuery("SELECT `id` FROM `%1$s` ORDER BY `id` DESC LIMIT 1".formatted(TABLE_LOGS))) {
					if (rs.next()) {
						this.lastId = rs.getLong(1);
					}
				}
			}
		} catch (SQLException e) {
			plugin.getLogger().warn("An error occurred when initialising the statistics.");
			e.printStackTrace();
		}
	}

	@Override
	public void run() {
		Map<Long, ServerStats> stats = Stream.concat(Stream.of(""), this.plugin.getProxy().getAllServers().parallelStream()
				.map(RegisteredServer::getServerInfo)
				.map(ServerInfo::getName))
				.map(plugin.getServerIdManager()::getServerId)
				.distinct()
				.collect(Collectors.toMap(Function.identity(), v -> new ServerStats()));
		
		try (Connection conn = this.plugin.getDatabaseManager().connect()) {
			fillData(conn, stats);
			Instant now = Instant.ofEpochSecond(System.currentTimeMillis() / 60_000 * 60); // last full minute
			String sql = "INSERT INTO `%1$s` (`time`, `server_id`, `playercount`, `maxplayers`, `activity`) VALUES ".formatted(TABLE_SERVER_STATS)
					+ ", (?, ?, ?, ?, ?)".repeat(stats.size()).substring(2);
			int i = 1;
			try (PreparedStatement stm = conn.prepareStatement(sql)) {
				for (Entry<Long, ServerStats> entry : stats.entrySet()) {
					stm.setTimestamp(i++, Timestamp.from(now));
					stm.setLong(i++, entry.getKey());
					stm.setLong(i++, entry.getValue().getPlayerCount());
					stm.setLong(i++, entry.getValue().getMaxPlayers());
					stm.setLong(i++, entry.getValue().getActions());
				}
				stm.executeUpdate();
			}
		} catch (SQLException e) {
			plugin.getLogger().warn("An error occurred when executing the database query to update the statistics.");
			e.printStackTrace();
		}
	}
	
	private void fillData(@NotNull Connection conn, @NotNull Map<Long, ServerStats> stats) throws SQLException {
		Toml config = this.plugin.getConfig();
		if (config.getBoolean("stats.playercount").booleanValue()) {
			fillPlayercountData(stats);
			if (config.getBoolean("stats.maxplayers").booleanValue()) {
				fillMaxplayersData(conn, stats);
			}
		}
		if (config.getBoolean("stats.activity").booleanValue()) {
			fillActivityData(conn, stats);
		}
	}
	
	private void fillPlayercountData(@NotNull Map<Long, ServerStats> stats) {
		ServerIdManager srvIdMgr = this.plugin.getServerIdManager();
		this.plugin.getProxy().getAllServers().stream()
				.forEach(info -> Optional.of(srvIdMgr.getServerId(info.getServerInfo().getName()))
						.map(stats::get)
						.ifPresent(srv -> srv.setPlayerCount(info.getPlayersConnected().stream()
								.filter(Player::isActive)
								.count())));
		stats.get(srvIdMgr.getServerId("")).setPlayerCount((long) this.plugin.getProxy().getPlayerCount());
	}
	
	private void fillMaxplayersData(@NotNull Connection conn, @NotNull Map<Long, ServerStats> stats) throws SQLException {
		String sql = "SELECT `server_id`, MAX(`maxplayers`) FROM `%1$s` GROUP BY `server_id`".formatted(TABLE_SERVER_STATS);
		try (Statement stm = conn.createStatement()) {
			try (ResultSet rs = stm.executeQuery(sql)) {
				while (rs.next()) {
					ServerStats values = stats.get(rs.getLong(1));
					if (values != null) {
						values.setMaxPlayers(Math.max(values.getPlayerCount(), rs.getLong(2)));
					}
				}
			}
		}
	}
	
	private void fillActivityData(@NotNull Connection conn, @NotNull Map<Long, ServerStats> stats) throws SQLException {
		String sql = "SELECT `server_id`, COUNT(*), MAX(`id`) as `id` FROM `%1$s` WHERE `id` > ? GROUP BY `server_id` ORDER BY `id`".formatted(TABLE_LOGS);
		try (PreparedStatement stm = conn.prepareStatement(sql)) {
			stm.setLong(1, this.lastId);
			try (ResultSet rs = stm.executeQuery()) {
				while (rs.next()) {
					ServerStats values = stats.get(rs.getLong(1));
					this.lastId = rs.getLong(3);
					if (values != null) {
						values.setActions(rs.getLong(2));
					}
				}
			}
		}
	}
	
	@Data
	private static class ServerStats {
		
		long playerCount;
		long maxPlayers;
		long actions;
		
	}
	
}
