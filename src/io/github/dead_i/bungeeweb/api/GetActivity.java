package io.github.dead_i.bungeeweb.api;

import static io.github.dead_i.bungeeweb.hikari.HikariDB.TABLE_PLAYERS;
import static io.github.dead_i.bungeeweb.hikari.HikariDB.TABLE_SERVERS;
import static io.github.dead_i.bungeeweb.hikari.HikariDB.TABLE_SESSIONS;
import static io.github.dead_i.bungeeweb.hikari.HikariDB.TABLE_TIME;
import static io.github.dead_i.bungeeweb.hikari.HikariDB.uuidToBytes;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

import org.jetbrains.annotations.NotNull;

import io.github.dead_i.bungeeweb.APICommand;
import io.github.dead_i.bungeeweb.BungeeWeb;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class GetActivity extends APICommand {
	
	private static final Pattern UUID_PATTERN = Pattern.compile("[0-9a-f]{8}(-[0-9a-f]{4}){3}-[0-9a-f]{12}");

	public GetActivity(@NotNull BungeeWeb plugin) {
		super(plugin, "getactivity", "activity");
	}
	
	@Override
	public void execute(HttpServletRequest req, HttpServletResponse res, String[] args) throws IOException, SQLException {
		Optional<UUID> uuid = Optional.ofNullable(req.getParameter("uuid"))
				.filter(UUID_PATTERN.asMatchPredicate())
				.map(UUID::fromString);
		Optional<Instant> since = Optional.ofNullable(req.getParameter("since"))
				.filter(BungeeWeb::isNumber)
				.map(Long::parseLong)
				.map(Instant::ofEpochSecond);
		Optional<Instant> to = Optional.ofNullable(req.getParameter("to"))
				.filter(BungeeWeb::isNumber)
				.map(Long::parseLong)
				.map(Instant::ofEpochSecond);
		if (uuid.isPresent() && since.isPresent() && to.isPresent()) {
			String sql = """
					SELECT `time`, `s`.`name`, SUM(`minutes`) as `sum` FROM `bungeeweb_time` as `t`
					    LEFT JOIN `bungeeweb_player_sessions` as `ps` ON `t`.`session_id` = `ps`.`id`
					    LEFT JOIN `bungeeweb_players` as `p` ON `ps`.`player_id` = `p`.`id`
					    LEFT JOIN `bungeeweb_servers` as `s` ON `t`.`server_id` = `s`.`id`
					    WHERE `time` BETWEEN ? AND ? AND `p`.`uuid` = ?
					    GROUP BY `time`, `s`.`name`
					""".formatted(TABLE_TIME, TABLE_SESSIONS, TABLE_PLAYERS, TABLE_SERVERS);
			Map<String, Map<Long, Integer>> times = new HashMap<>();
			try (Connection conn = this.plugin.getDatabaseManager().connect()) {
				try (PreparedStatement stm = conn.prepareStatement(sql)) {
					stm.setTimestamp(1, Timestamp.from(since.get()));
					stm.setTimestamp(2, Timestamp.from(to.get()));
					stm.setBytes(3, uuidToBytes(uuid.get()));
					try (ResultSet rs = stm.executeQuery()) {
						while (rs.next()) {
							long time = rs.getTimestamp(1).toInstant().getEpochSecond();
							String server = rs.getString(2);
							int minutes = rs.getInt(3);
							times.computeIfAbsent(server, s -> new HashMap<>()).put(time, minutes);
						}
						
					}
				}
			}
			res.getWriter().write(GSON_PARSER.toJson(times));
		} else {
			res.setStatus(400); // invalid GET arguments
		}
	}

	
	
	
	
}
