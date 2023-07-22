package io.github.dead_i.bungeeweb.api;

import static io.github.dead_i.bungeeweb.hikari.HikariDB.TABLE_CHAT;
import static io.github.dead_i.bungeeweb.hikari.HikariDB.TABLE_COMMANDS;
import static io.github.dead_i.bungeeweb.hikari.HikariDB.TABLE_LOGS;
import static io.github.dead_i.bungeeweb.hikari.HikariDB.TABLE_PLAYERS;
import static io.github.dead_i.bungeeweb.hikari.HikariDB.TABLE_SERVERCHANGES;
import static io.github.dead_i.bungeeweb.hikari.HikariDB.TABLE_SERVERS;
import static io.github.dead_i.bungeeweb.hikari.HikariDB.TABLE_SESSIONS;
import static io.github.dead_i.bungeeweb.hikari.HikariDB.bytesToUuid;
import static io.github.dead_i.bungeeweb.hikari.HikariDB.commaStmReplacers;
import static io.github.dead_i.bungeeweb.hikari.HikariDB.uuidToBytes;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.jetbrains.annotations.NotNull;

import io.github.dead_i.bungeeweb.APICommand;
import io.github.dead_i.bungeeweb.BungeeWeb;
import io.github.dead_i.bungeeweb.ProtocolUtils;
import io.github.dead_i.bungeeweb.hikari.HikariDB.LogType;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AllArgsConstructor;
import lombok.Getter;

public class GetLogs extends APICommand {

	private static final Pattern UUID_PATTERN = Pattern.compile("[0-9a-f]{8}(-[0-9a-f]{4}){3}-[0-9a-f]{12}");
	private static final Pattern IPV4_PATTERN = Pattern.compile("((25[0-5]|(2[0-4]|1\\d|[1-9]|)\\d)\\.?\\b){4}");
	
	public GetLogs(@NotNull BungeeWeb plugin) {
		super(plugin, "getlogs", "logs");
	}
	
	@Override
	public void execute(HttpServletRequest req, HttpServletResponse res, String[] args) throws IOException {

		// limit
		int limit = Math.min(Optional.ofNullable(req.getParameter("limit"))
				.filter(BungeeWeb::isNumber)
				.map(Integer::parseInt)
				.orElse(Integer.MAX_VALUE), 100);
		
		// Minimum ID
		int minId = Optional.ofNullable(req.getParameter("minId"))
				.filter(BungeeWeb::isNumber)
				.map(Integer::parseInt)
				.orElse(-1);
		
		// Maximum ID
		int maxId = Optional.ofNullable(req.getParameter("maxId"))
				.filter(BungeeWeb::isNumber)
				.map(Integer::parseInt)
				.orElse(-1);
		
		// UUIDs
		List<UUID> uuids = Optional.ofNullable(req.getParameter("uuids"))
				.map(s -> s.split(","))
				.stream()
				.flatMap(Stream::of)
				.filter(UUID_PATTERN.asMatchPredicate())
				.map(UUID::fromString)
				.toList();
		
		// Servers
		List<String> servers = Optional.ofNullable(req.getParameter("servers"))
				.map(s -> s.split(","))
				.stream()
				.flatMap(Stream::of)
				.toList();
		
		// IPs
		List<String> ips = Optional.ofNullable(req.getParameter("ips"))
				.map(s -> s.split(","))
				.stream()
				.flatMap(Stream::of)
				.filter(IPV4_PATTERN.asMatchPredicate())
				.toList();
		
		// Types
		List<LogType> types = Optional.ofNullable(req.getParameter("types"))
				.map(s -> s.split(","))
				.stream()
				.flatMap(Stream::of)
				.map(LogType::tryParse)
				.filter(Optional::isPresent)
				.map(Optional::get)
				.toList();

		// Chat message
		Optional<String> message = Optional.ofNullable(req.getParameter("message"));
		
		// Command labels (space separated)
		Optional<List<String>> commands = Optional.ofNullable(req.getParameter("command"))
				.map(s -> s.split(" "))
				.map(List::of);
		
		// Command arguments
		Optional<String> arguments = Optional.ofNullable(req.getParameter("arguments"))
				.map(s -> s.isBlank() ? "" : s);
		
		
		Collection<LogEntry> out = this.queryBasic(
				new OptimizingFilter(uuids, servers, ips, types),
				new ContentMatcher(message, commands, arguments),
				limit, minId, maxId);
		String json = GSON_PARSER.toJson(out);
		res.getWriter().print(json);
	}
	
	private Collection<LogEntry> queryBasic(OptimizingFilter optimisingFilters, ContentMatcher content, int limit, long minId, long maxId) {
		List<String> filters = new LinkedList<>();
		List<Object> params = new LinkedList<>();
		prepareQueryArguments(filters, params, optimisingFilters, content, minId, maxId);
		
		String sql = """
				SELECT `l`.`id`, `time`, `type`, `s`.`name` as `server`, `uuid`, `username`, `protocol_id`, INET6_NTOA(`ip_address`) as `ip`, `client`, `hostname`, `message`, `command`, `arguments`, `target_server`, `extra` FROM `%1$s` as `l`
				    LEFT JOIN `%2$s` as `s` ON `l`.`server_id` = `s`.`id`
				    LEFT JOIN `%3$s` as `ps` ON `l`.`session_id` = `ps`.`id`
				    LEFT JOIN `%4$s` as `p` ON `l`.`player_id` = `p`.`id`
				    LEFT JOIN `%5$s` as `ch` ON `l`.`id` = `ch`.`id`
				    LEFT JOIN `%6$s` as `cmd` ON `l`.`id` = `cmd`.`id`
				    LEFT JOIN  (SELECT `sc`.`id`, `name` as `target_server`, `extra` FROM `%7$s` as `sc`
				        LEFT JOIN `%2$s` as `s` ON `sc`.`target_server` = `s`.`id`) as `sc` ON `l`.`id` = `sc`.`id`
				    %8$s
				    ORDER BY `id` DESC LIMIT ?
				""".formatted(TABLE_LOGS, TABLE_SERVERS, TABLE_SESSIONS, TABLE_PLAYERS, TABLE_CHAT, TABLE_COMMANDS, TABLE_SERVERCHANGES,
						filters.isEmpty() ? "" : ("WHERE " + String.join(" AND ", filters)));
		try (Connection conn = this.plugin.getDatabaseManager().connect()) {
			try (PreparedStatement stm = conn.prepareStatement(sql)) {
				int i = 1;
				if (!params.isEmpty()) {
					for (Object param : params) {
						if (param instanceof UUID uuid) {
							stm.setBytes(i++, uuidToBytes(uuid));
						} else if (param instanceof Instant inst) {
							stm.setTimestamp(i++, Timestamp.from(inst));
						} else {
							stm.setObject(i++, param);
						}
					}
				}
				stm.setLong(i++, limit);
				
				try (ResultSet rs = stm.executeQuery()) {
					Map<Long, LogEntry> logs = new HashMap<>();
					while (rs.next()) {
						long id = rs.getLong("id");
						LogEntry log = new LogEntry(id,
								rs.getTimestamp("time").getTime() / 1000,
								new LogEntry.PlayerSession(
										new LogEntry.PlayerInfo(
												bytesToUuid(rs.getBytes("uuid")),
												rs.getString("username")),
										new LogEntry.ProtocolInfo(rs.getInt("protocol_id")),
										rs.getString("ip"),
										rs.getString("client"),
										rs.getString("hostname")),
								rs.getString("server"),
								LogType.values()[rs.getInt("type") - 1]);
						logs.put(id, switch (log.getType()) {
							case CHAT -> log.asChatLog(rs.getString("message"));
							case COMMAND -> log.asCommandLog(rs.getString("command"), rs.getString("arguments"));
							case KICK -> log.asKickLog(rs.getString("target_server"), rs.getString("extra"));
							case SERVER_CHANGE -> log.asServerSwitchLog(rs.getString("target_server"));
							default -> log;
						});
					}
					return logs.values();
				}
			}
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}
	
	private void prepareQueryArguments(List<String> filters, List<Object> params, OptimizingFilter optimisingFilters, ContentMatcher content, long minId, long maxId) {
		/* ID range */
		// Minimum ID
		if (minId >= 0) {
			filters.add("`l`.`id` > ?");
			params.add(minId);
		}
		// Maximum ID
		if (maxId >= 0) {
			filters.add("`l`.`id` < ?");
			params.add(maxId);
		}
		
		/* Optimization filters */
		// UUID filter
		if (!optimisingFilters.uuids().isEmpty()) {
			filters.add("`p`.`uuid` IN (%s)".formatted(commaStmReplacers(optimisingFilters.uuids().size())));
			params.addAll(optimisingFilters.uuids());
		}
		// type filter
		if (!optimisingFilters.ips().isEmpty()) {
			filters.add("`ps`.`ip_address` IN (%s)".formatted(commaStmReplacers("INET6_ATON(?)", optimisingFilters.ips().size())));
			params.addAll(optimisingFilters.ips());
		}
		// serverName filter
		if (!optimisingFilters.servers().isEmpty()) {
			filters.add("`s`.`name` IN (%s)".formatted(commaStmReplacers(optimisingFilters.servers().size())));
			params.addAll(optimisingFilters.servers());
		}
		// type filter
		if (!optimisingFilters.types().isEmpty()) {
			filters.add("`l`.`type` IN (%s)".formatted(commaStmReplacers(optimisingFilters.types().size())));
			params.addAll(optimisingFilters.types().stream().map(t -> t.ordinal() + 1).toList());
		}
		
		/* Content filters */
		if (content.isPresent()) {
			List<String> contentFilters = new LinkedList<>();
			content.message.ifPresent(message -> {
				contentFilters.add("`ch`.`message` REGEXP ?");
				params.add(message);
			});
			if (content.commands().isPresent() || content.arguments().isPresent()) {
				List<String> commandFilters = new LinkedList<>();
				content.commands().ifPresent(commands -> {
					commandFilters.add("`cmd`.`command` IN (%s)".formatted(commaStmReplacers(commands.size())));
					params.addAll(commands);
				});
				content.arguments.ifPresent(arguments -> {
					commandFilters.add("`cmd`.`arguments` REGEXP ?");
					params.add(arguments);
				});
				contentFilters.add("(%s)".formatted(String.join(" AND ", commandFilters)));
			}
			filters.add("(%s)".formatted(String.join(" OR ", contentFilters)));
		}
	}
	
	@AllArgsConstructor
	@Getter
	protected static class LogEntry {
		
		private final long id;
		private final long time;
		private final @NotNull PlayerSession session;
		private final @NotNull String server;
		private final @NotNull LogType type;

		public @NotNull ChatLogEntry asChatLog(@NotNull String message) {
			return new ChatLogEntry(this.id, this.time, this.session, this.server, message);
		}
		
		public @NotNull CommandLogEntry asCommandLog(@NotNull String command, @NotNull String arguments) {
			return new CommandLogEntry(this.id, this.time, this.session, this.server, command, arguments);
		}
		
		public @NotNull KickLogEntry asKickLog(@NotNull String fallbackServer, @NotNull String reason) {
			return new KickLogEntry(this.id, this.time, this.session, this.server, fallbackServer, reason);
		}

		public @NotNull ServerSwitchLogEntry asServerSwitchLog(@NotNull String targetServer) {
			return new ServerSwitchLogEntry(this.id, this.time, this.session, this.server, targetServer);
		}
		
		public record ProtocolInfo(int id, String name) {
			public ProtocolInfo(int protocolId) {
				this(protocolId, ProtocolUtils.getProtocolName(protocolId)
						.orElse("UNKNOWN(%d)".formatted(protocolId)));
			}
		};
		
		public record PlayerSession(@NotNull PlayerInfo player, ProtocolInfo protocol, @NotNull String ip, @NotNull String client, @NotNull String hostname) {}
		
		public record PlayerInfo(@NotNull UUID uuid, @NotNull String name) {}
		
	}
	
	@Getter
	private static class ChatLogEntry extends LogEntry {

		private final @NotNull String message;
		
		protected ChatLogEntry(long id, long time, @NotNull PlayerSession session, @NotNull String server, @NotNull String message) {
			super(id, time, session, server, LogType.CHAT);
			this.message = message;
		}
		
	}
	
	@Getter
	private static class CommandLogEntry extends LogEntry {

		private final @NotNull String command;
		private final @NotNull String arguments;
		
		protected CommandLogEntry(long id, long time, @NotNull PlayerSession session, @NotNull String server, @NotNull String command, @NotNull String arguments) {
			super(id, time, session, server, LogType.COMMAND);
			this.command = command;
			this.arguments = arguments;
		}
		
	}
	
	@Getter
	private static class KickLogEntry extends LogEntry {

		private final @NotNull String fallbackServer;
		private final @NotNull String reason;
		
		protected KickLogEntry(long id, long time, @NotNull PlayerSession session, @NotNull String server, @NotNull String fallbackServer, @NotNull String reason) {
			super(id, time, session, server, LogType.KICK);
			this.fallbackServer = fallbackServer;
			this.reason = reason;
		}
		
	}
	
	@Getter
	private static class ServerSwitchLogEntry extends LogEntry {

		private final @NotNull String targetServer;
		
		protected ServerSwitchLogEntry(long id, long time, @NotNull PlayerSession session, @NotNull String server, @NotNull String targetServer) {
			super(id, time, session, server, LogType.SERVER_CHANGE);
			this.targetServer = targetServer;
		}
		
	}
	
	private record ContentMatcher(@NotNull Optional<String> message, @NotNull Optional<List<String>> commands, @NotNull Optional<String> arguments) {
		public boolean isPresent() {
			return message.isPresent()
					|| commands.isPresent()
					|| arguments.isPresent();
		}
	}
	
	private record OptimizingFilter(List<UUID> uuids, List<String> servers, List<String> ips, List<LogType> types) {}
}
