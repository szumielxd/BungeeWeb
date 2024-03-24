package me.szumielxd.proxyyweb.common.api;

import static me.szumielxd.proxyyweb.common.hikari.HikariDB.TABLE_SERVERS;
import static me.szumielxd.proxyyweb.common.hikari.HikariDB.TABLE_SERVER_STATS;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.jetbrains.annotations.NotNull;

import io.github.dead_i.bungeeweb.APICommand;
import io.github.dead_i.bungeeweb.BungeeWeb;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class GetStats extends APICommand {
	
	private static final String[] TYPES = { "playercount", "maxplayers", "activity" };

	public GetStats(@NotNull BungeeWeb plugin) {
		super(plugin, "getstats", "stats");
	}


	@Override
	public void execute(HttpServletRequest req, HttpServletResponse res, String[] args) throws IOException, SQLException {
		long current = System.currentTimeMillis() / 1000;
		long month = current - 2628_000;
		
		// Since
		long time = Optional.ofNullable(req.getParameter("since"))
				.filter(BungeeWeb::isNumber)
				.map(Long::parseLong).orElse(month);
		
		// Servers
		List<String> servers = Optional.ofNullable(req.getParameter("servers"))
				.map(s -> s.split(","))
				.stream()
				.flatMap(Stream::of)
				.map(String::toLowerCase)
				.distinct()
				.toList();

		if (time < month) {
			res.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            this.log(req, "failure");
			res.getWriter().print("{ \"error\": \"Attempted to fetch too many records. The number of records you request is capped at 1 month for security reasons.\" }");
			return;
		}

		try (Connection conn = this.plugin.getDatabaseManager().connect()) {
			String sql = """
					SELECT `name`, `time`, `playercount`, `maxplayers`, `activity` FROM `%1$s` as `st`
					    LEFT JOIN `%2$s` as `s` ON `st`.`server_id` = `s`.`id`
					    WHERE `time` > ? %3$s
					"""
					.formatted(TABLE_SERVER_STATS, TABLE_SERVERS,
							servers.isEmpty() ? "" : "AND `name` IN (%s)"
								.formatted(", ?".repeat(servers.size()).substring(2)));
			try (PreparedStatement stm = conn.prepareStatement(sql)) {
				Map<String, Map<String, List<Long[]>>> records = new HashMap<>(); // <Server, Type, <[time, value]>>
				int pos = 1;
				stm.setTimestamp(pos++, Timestamp.from(Instant.ofEpochSecond(time)));
				if (!servers.isEmpty()) {
					for (String srv : servers) {
						stm.setString(pos++, srv);
					}
				}
				try (ResultSet rs = stm.executeQuery()) {
					while (rs.next()) {
						Map<String, List<Long[]>> values = records.computeIfAbsent(rs.getString(1),
								k -> Stream.of(TYPES).collect(Collectors.toMap(Function.identity(), e -> new LinkedList<>())));
						long epochSeconds = rs.getTimestamp(2).toInstant().toEpochMilli();
						for (int i = 0; i < 3; i++) {
							values.get(TYPES[i]).add(new Long[] { epochSeconds, (long) rs.getInt(3 + i) });
						}
					}
				}
				HashMap<String, Object> out = new HashMap<>();
				out.put("increment", this.plugin.getConfig().getInt("server.statscheck"));
				out.put("data", records);
                this.log(req, () -> "time: %d, servers: ".formatted(time, servers));
				res.getWriter().print(GSON_PARSER.toJson(out));
			}
		}
	}
}
