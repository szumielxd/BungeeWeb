package me.szumielxd.proxyyweb.common.api;

import static me.szumielxd.proxyyweb.common.hikari.HikariDB.TABLE_CLIENTS;
import static me.szumielxd.proxyyweb.common.hikari.HikariDB.TABLE_CLIENT_STATS;
import static me.szumielxd.proxyyweb.common.hikari.HikariDB.TABLE_SERVERS;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.jetbrains.annotations.NotNull;

import io.github.dead_i.bungeeweb.APICommand;
import io.github.dead_i.bungeeweb.BungeeWeb;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class GetClients extends APICommand {
	
	public GetClients(@NotNull BungeeWeb plugin) {
		super(plugin, "getclients", "clients");
	}
	
	@Override
	public void execute(HttpServletRequest req, HttpServletResponse res, String[] args) throws IOException, SQLException {
		Optional<String> server = Optional.ofNullable(req.getParameter("server"));
		Optional<Instant> since = Optional.ofNullable(req.getParameter("since"))
				.filter(BungeeWeb::isNumber)
				.map(Long::parseLong)
				.map(Instant::ofEpochSecond);
		Optional<Instant> to = Optional.ofNullable(req.getParameter("to"))
				.filter(BungeeWeb::isNumber)
				.map(Long::parseLong)
				.map(Instant::ofEpochSecond);
		if (since.isPresent() && to.isPresent()) {
			this.log(req, () -> "server: %s, since: %s, to: %s".formatted(server.orElse(null), since.orElse(null), to.orElse(null)));
			res.getWriter().write(GSON_PARSER.toJson(getServerClients(since.get(), to.get(), server.orElse(""))));
		} else {
            this.log(req, "failure");
			res.setStatus(400); // invalid GET arguments
		}
	}
	
	private @NotNull Map<String, Map<Long, Integer>> getServerClients(@NotNull Instant since, @NotNull Instant to, @NotNull String server) throws SQLException {
		if (!Duration.between(since, to).minusDays(30).isNegative()) {
			to = since.plus(Duration.ofDays(30));
		}
		
		String sql = """
				SELECT `c`.`name`, `time`, `playercount` FROM `%1$s` as `st`
				    LEFT JOIN `%2$s` as `s` ON `st`.`server_id` = `s`.`id`
				    LEFT JOIN `%3$s` as `c` ON `st`.`client_id` = `c`.`id`
				    WHERE `time` BETWEEN ? AND ?
				            AND `s`.`name` = ?
				"""
				.formatted(TABLE_CLIENT_STATS, TABLE_SERVERS, TABLE_CLIENTS);
		Map<String, Map<Long, Integer>> times = new HashMap<>();
		try (Connection conn = this.plugin.getDatabaseManager().connect()) {
			try (PreparedStatement stm = conn.prepareStatement(sql)) {
				stm.setTimestamp(1, Timestamp.from(since));
				stm.setTimestamp(2, Timestamp.from(to));
				stm.setString(3, server);
				try (ResultSet rs = stm.executeQuery()) {
					while (rs.next()) {
						String client = rs.getString(1);
						long time = rs.getTimestamp(2).toInstant().getEpochSecond();
						int playercount = rs.getInt(3);
						times.computeIfAbsent(client, c -> new HashMap<>()).put(time, playercount);
					}
					
				}
			}
		}
		return times;
	}
	
	
}
