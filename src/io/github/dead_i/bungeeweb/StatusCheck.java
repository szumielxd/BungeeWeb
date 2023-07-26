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

import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.config.Configuration;

public class StatusCheck implements Runnable {
	
	private final @NotNull BungeeWeb plugin;
	private long lastId = 0;

	public StatusCheck(BungeeWeb plugin) {
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
			plugin.getLogger().warning("An error occurred when initialising the statistics.");
			e.printStackTrace();
		}
	}

	@Override
	public void run() {
		Map<Long, Long[]> stats = Stream.concat(Stream.of(""), this.plugin.getProxy().getServers().values().parallelStream()
				.map(ServerInfo::getName))
				.map(plugin.getServerIdManager()::getServerId)
				.distinct()
				.collect(Collectors.toMap(Function.identity(), v -> new Long[] {
							0L, // player count
							0L, // max players
							0L // activity
						}));
		
		try (Connection conn = this.plugin.getDatabaseManager().connect()) {
			fillData(conn, stats);
			Instant now = Instant.ofEpochSecond(System.currentTimeMillis() / 60_000 * 60); // last full minute
			String sql = "INSERT INTO `%1$s` (`time`, `server_id`, `playercount`, `maxplayers`, `activity`) VALUES ".formatted(TABLE_SERVER_STATS)
					+ ", (?, ?, ?, ?, ?)".repeat(stats.size()).substring(2);
			int i = 1;
			try (PreparedStatement stm = conn.prepareStatement(sql)) {
				for (Entry<Long, Long[]> entry : stats.entrySet()) {
					stm.setTimestamp(i++, Timestamp.from(now));
					stm.setLong(i++, entry.getKey());
					stm.setLong(i++, entry.getValue()[0]);
					stm.setLong(i++, entry.getValue()[1]);
					stm.setLong(i++, entry.getValue()[2]);
				}
				stm.executeUpdate();
			}
		} catch (SQLException e) {
			plugin.getLogger().warning("An error occurred when executing the database query to update the statistics.");
			e.printStackTrace();
		}
	}
	
	private void fillData(@NotNull Connection conn, @NotNull Map<Long, Long[]> stats) throws SQLException {
		Configuration config = this.plugin.getConfig();
		if (config.getBoolean("stats.playercount")) {
			fillPlayercountData(stats);
			if (config.getBoolean("stats.maxplayers")) {
				fillMaxplayersData(conn, stats);
			}
		}
		if (config.getBoolean("stats.activity")) {
			fillActivityData(conn, stats);
		}
	}
	
	private void fillPlayercountData(@NotNull Map<Long, Long[]> stats) {
		ServerIdManager srvIdMgr = this.plugin.getServerIdManager();
		this.plugin.getProxy().getServers().values().stream()
				.forEach(info -> {
					long id = srvIdMgr.getServerId(info.getName());
					Optional.ofNullable(stats.get(id)).ifPresent(srv -> srv[0] = (long) info.getPlayers().size());
				});
		stats.get(srvIdMgr.getServerId(""))[0] = (long) this.plugin.getProxy().getOnlineCount();
	}
	
	private void fillMaxplayersData(@NotNull Connection conn, @NotNull Map<Long, Long[]> stats) throws SQLException {
		String sql = "SELECT `server_id`, MAX(`maxplayers`) FROM `%1$s` GROUP BY `server_id`".formatted(TABLE_SERVER_STATS);
		try (Statement stm = conn.createStatement()) {
			try (ResultSet rs = stm.executeQuery(sql)) {
				while (rs.next()) {
					Long[] values = stats.get(rs.getLong(1));
					if (values != null) {
						values[1] = Math.max(values[0], rs.getLong(2));
					}
				}
			}
		}
	}
	
	private void fillActivityData(@NotNull Connection conn, @NotNull Map<Long, Long[]> stats) throws SQLException {
		String sql = "SELECT `server_id`, COUNT(*), MAX(`id`) as `id` FROM `%1$s` WHERE `id` > ? GROUP BY `server_id` ORDER BY `id`".formatted(TABLE_LOGS);
		try (PreparedStatement stm = conn.prepareStatement(sql)) {
			stm.setLong(1, this.lastId);
			try (ResultSet rs = stm.executeQuery()) {
				while (rs.next()) {
					Long[] values = stats.get(rs.getLong(1));
					this.lastId = rs.getLong(3);
					if (values != null) {
						values[1] = rs.getLong(2);
					}
				}
			}
		}
	}
	
}
