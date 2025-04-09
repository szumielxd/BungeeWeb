package io.github.dead_i.bungeeweb.api;

import static io.github.dead_i.bungeeweb.hikari.HikariDB.TABLE_SERVERS;
import static io.github.dead_i.bungeeweb.hikari.HikariDB.TABLE_SERVER_STATS;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.jetbrains.annotations.NotNull;

import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.api.proxy.server.ServerInfo;

import io.github.dead_i.bungeeweb.APICommand;
import io.github.dead_i.bungeeweb.BungeeWeb;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AllArgsConstructor;
import lombok.Getter;

public class GetStats extends APICommand {
	
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
		
		// Since
		StatType[] fields = Optional.of(
				Optional.ofNullable(req.getParameter("fields"))
						.map(s -> s.split(","))
						.stream()
						.flatMap(Stream::of)
						.map(StatType::tryParse)
						.filter(Optional::isPresent)
						.map(Optional::get)
						.toArray(StatType[]::new))
				.filter(a -> a.length > 0)
				.orElseGet(StatType::values);
		
		// Servers
		List<String> servers = Optional.ofNullable(req.getParameter("servers"))
				.map(s -> s.split(","))
				.stream()
				.flatMap(Stream::of)
				.map(String::toLowerCase)
				.distinct()
				.toList();
		if (servers.isEmpty()) {
			servers = Stream.concat(
					Stream.of(""),
					plugin.getProxy().getAllServers().stream()
							.map(RegisteredServer::getServerInfo)
							.map(ServerInfo::getName))
					.toList();
		}
		
		List<Long> serverIds = servers.stream()
				.map(plugin.getServerIdManager()::getServerId)
				.toList();

		if (time < month) {
			res.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			res.getWriter().print("{ \"error\": \"Attempted to fetch too many records. The number of records you request is capped at 1 month for security reasons.\" }");
			return;
		}

		try (Connection conn = this.plugin.getDatabaseManager().connect()) {
			String sql = """
					SELECT `name`, `time`, %4$s FROM `%1$s` as `st`
					    LEFT JOIN `%2$s` as `s` ON `st`.`server_id` = `s`.`id`
					    WHERE `time` > ? %3$s
					"""
					.formatted(
							TABLE_SERVER_STATS,
							TABLE_SERVERS,
							serverIds.isEmpty() ? "" : "AND `server_id` IN (%s)"
									.formatted(", ?".repeat(serverIds.size()).substring(2)),
							Stream.of(fields)
									.map("`%s`"::formatted)
									.collect(Collectors.joining(", ")));
			try (PreparedStatement stm = conn.prepareStatement(sql)) {
				Map<String, Map<Long, Long[]>> records = new HashMap<>(); // <Server, Type, <time:[values]>>
				int pos = 1;
				stm.setTimestamp(pos++, Timestamp.from(Instant.ofEpochSecond(time)));
				if (!serverIds.isEmpty()) {
					for (long srv : serverIds) {
						stm.setLong(pos++, srv);
					}
				}
				try (ResultSet rs = stm.executeQuery()) {
					while (rs.next()) {
						var valuesEntry = records.computeIfAbsent(rs.getString(1), k -> new LinkedHashMap<>());
						long epochSeconds = rs.getTimestamp(2).getTime();
						Long[] values = new Long[fields.length];
						for (int i = 0; i < fields.length; i++) {
							values[i] = rs.getLong(3 + i);
						}
						valuesEntry.put(epochSeconds, values);
					}
				}
				HashMap<String, Object> out = new HashMap<>();
				out.put("increment", this.plugin.getConfig().getLong("server.statscheck"));
				out.put("data", records);
				out.put("fields", fields);
				GSON_PARSER.toJson(out, Map.class, GSON_PARSER.newJsonWriter(res.getWriter()));
			}
		}
	}
	
	@AllArgsConstructor
	private enum StatType {
		
		PLAYERCOUNT("playercount"),
		MAXPLAYERS("maxplayers"),
		ACTIVITY("activity");
		
		@Getter private @NotNull String collumnName;
		
		public static @NotNull Optional<StatType> tryParse(String str) {
			try {
				return Optional.of(valueOf(str.toUpperCase()));
			} catch (IllegalArgumentException e) {
				return Optional.empty();
			}
		}
		
	}
}
