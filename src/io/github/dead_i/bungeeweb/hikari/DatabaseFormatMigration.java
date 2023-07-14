package io.github.dead_i.bungeeweb.hikari;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import com.zaxxer.hikari.HikariDataSource;

import io.github.dead_i.bungeeweb.BungeeWeb;
import static io.github.dead_i.bungeeweb.hikari.HikariDB.*;

public class DatabaseFormatMigration {
	
	

	private static final Pattern CSV_NUMERIC_PATTERN = Pattern.compile("(\\d+)\\.csv");
	private final AtomicLong lastPlayerId = new AtomicLong(0);
	private final AtomicLong lastSessionId = new AtomicLong(0);
	private final AtomicLong lastServerId = new AtomicLong(0);
	private final Map<UUID, Long> playerIds = new ConcurrentHashMap<>();
	private final Map<UUID, Long> sessionIds = new ConcurrentHashMap<>();
	private final Map<String, Long> serverIds = new ConcurrentHashMap<>();

	private final HikariDataSource hikari;
	private final BungeeWeb plugin;
	private final Path tempDir;
	private final Path sessionDir;
	private final Path playerDir;
	private final Path serverDir;
	private final Path logsDir;
	private final Path cmdsDir;
	private final Path chatDir;
	private final Path srvChDir;
	
	public DatabaseFormatMigration(HikariDataSource hikari, BungeeWeb plugin) {
		this.hikari = hikari;
		this.plugin = plugin;
		this.tempDir = this.plugin.getDataFolder().toPath().resolve("temp");
		this.sessionDir = tempDir.resolve("sessions");
		this.playerDir = tempDir.resolve("players");
		this.serverDir = tempDir.resolve("servers");
		this.logsDir = tempDir.resolve("logs");
		this.cmdsDir = tempDir.resolve("commands");
		this.chatDir = tempDir.resolve("chat");
		this.srvChDir = tempDir.resolve("serverchanges");
	}
	

	private record ServerNameEntry(long id, String name) {}
	private record PlayerIdEntry(long id, UUID uuid, String name) {}
	private record SessionIdEntry(long id, long playerId, String ip) {}
	private record LogsEntry(long id, long time, long playerId, long sessionId, long serverId, int type) {}
	private record ChatEntry(long id, String message) {}
	private record CommandEntry(long id, String command, String args) {}
	private record ServerSwitchEntry(long id, long target, String extra) {}
	
	private void setupTempDir() throws IOException {
		this.plugin.getLogger().info("Cleaning temp directories...");
		long start = System.currentTimeMillis();
		if (Files.exists(tempDir)) {
			Files.walk(tempDir).parallel()
		      .sorted(Comparator.reverseOrder())
		      .map(Path::toFile)
		      .forEach(File::delete);
		}
		this.plugin.getLogger().info("Cleaned up temp directories!");
		for (Path path : List.of(sessionDir, playerDir, serverDir, logsDir, cmdsDir, chatDir, srvChDir)) {
			Files.createDirectories(path);
		}
		this.plugin.getLogger().info(() -> "Database upgrade done in %,dms!".formatted(System.currentTimeMillis() - start));
	}
	
	public void migrate() {
		try {
			setupTempDir();
			try (BufferedWriter bw = Files.newBufferedWriter(tempDir.resolve("logs.csv"), StandardOpenOption.APPEND, StandardOpenOption.CREATE)) {
				// save logs to temp files
				IntStream.range(0, 16)
						.parallel()
						.mapToObj(Integer::toHexString)
						.forEach(this::prepareToMigration);
				
				// upload servers to database
				uploadProcessedDataInDir(serverDir,
						this::insertServerNames,
						vars -> new ServerNameEntry(Long.parseLong(vars[0]), vars[1]),
						2,
						"serverNames");
				
				// upload players to database
				uploadProcessedDataInDir(playerDir,
						this::insertPlayerIds,
						vars -> new PlayerIdEntry(Long.parseLong(vars[0]), UUID.fromString(vars[1]), vars[2]),
						3,
						"playerIds");
				
				// upload sessions to database
				uploadProcessedDataInDir(sessionDir,
						this::insertSessionIds,
						vars -> new SessionIdEntry(Long.parseLong(vars[0]), Long.parseLong(vars[1]), vars[2]),
						3,
						"sessionIds");

				// upload logs to database
				uploadProcessedDataByThreadFile(logsDir,
						this::insertLogs,
						vars -> new LogsEntry(
										Long.parseLong(vars[0]),
										Long.parseLong(vars[1]),
										Long.parseLong(vars[2]),
										Long.parseLong(vars[3]),
										Long.parseLong(vars[4]),
										Integer.parseInt(vars[5])
								),
						6,
						"logs");

				// upload chats to database
				uploadProcessedDataByThreadFile(chatDir,
						this::insertChats,
						vars -> new ChatEntry(
										Long.parseLong(vars[0]),
										vars[1]
								),
						2,
						"chats");

				// upload commands to database
				uploadProcessedDataByThreadFile(cmdsDir,
						this::insertCommands,
						vars -> new CommandEntry(
										Long.parseLong(vars[0]),
										vars[1],
										vars[2]
								),
						3,
						"commands");

				// upload serverSwitches to database
				uploadProcessedDataByThreadFile(srvChDir,
						this::insertServerSwitches,
						vars -> new ServerSwitchEntry(
										Long.parseLong(vars[0]),
										Long.parseLong(vars[1]),
										vars[2]
								),
						3,
						"serverSwitches");
			}
			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private void prepareToMigration(String index) {
		int size = 500_000;
		try (Connection conn = this.hikari.getConnection();
				BufferedWriter logsWriter = Files.newBufferedWriter(logsDir.resolve(index + ".csv"));
				BufferedWriter chatWriter = Files.newBufferedWriter(chatDir.resolve(index + ".csv"));
				BufferedWriter cmdsWriter = Files.newBufferedWriter(cmdsDir.resolve(index + ".csv"));
				BufferedWriter srvChWriter = Files.newBufferedWriter(srvChDir.resolve(index + ".csv"))) {
			preInsertion(conn);
			try (PreparedStatement stm = conn.prepareStatement("SELECT `id`, `time`, `type`, `uuid`, `username`, `server`, `content` FROM `%1$s` WHERE `uuid` LIKE ? ORDER BY `id` ASC LIMIT ? OFFSET ?".formatted(TABLE_OLD_LOG))) {
				int i = 0;
				stm.setString(1, index + "%");
				stm.setInt(2, size);
				boolean notEmpty;
				long start = System.currentTimeMillis();
				CompletableFuture<ResultSet> future = null;
				do {
					notEmpty = future == null;
					long mysqlStart = System.currentTimeMillis();
					if (!notEmpty) {
						try (ResultSet rs = future.get()) {
							long part1 = System.currentTimeMillis();
							this.plugin.getLogger().info(() -> "[Thread-%s] Completed query in %dms".formatted(index, part1 - mysqlStart));
							int count = 0;
							while (rs.next()) {
								notEmpty = true;
								count++;
								processDownloadedData(rs, logsWriter, chatWriter, cmdsWriter, srvChWriter);
							}
							this.plugin.getLogger().info("[Thread-%s] (%d) Completed loop with %d results in %dms (%dms)".formatted(index, i, count, System.currentTimeMillis() - part1, System.currentTimeMillis() - mysqlStart));
						}
					}
					// safety lock
					if (i > 20) {
						break;
					}
					stm.setInt(3, i++ * size);
					future = CompletableFuture.supplyAsync(() -> {
						try {
							return stm.executeQuery();
						} catch (SQLException e) {
							throw new RuntimeException(e);
						}
					});
				} while (notEmpty);
				this.plugin.getLogger().info(() -> "[Thread-%s] Completed process in %dms".formatted(index, System.currentTimeMillis() - start));
			}
			postInsertion(conn);
		} catch (SQLException | IOException | InterruptedException | ExecutionException e) {
			Thread.currentThread().interrupt();
			throw new RuntimeException(e);
		}
	}
	
	private void processDownloadedData(ResultSet rs, BufferedWriter logsWriter, BufferedWriter chatWriter, BufferedWriter cmdsWriter, BufferedWriter srvChWriter) throws SQLException, IOException {
		
		long id = rs.getLong(1);
		long time = rs.getLong(2);
		int type = rs.getInt(3);
		String uuidString = rs.getString(4);
		UUID uuid = new UUID(Long.parseUnsignedLong(uuidString.substring(0, 16), 16), Long.parseUnsignedLong(uuidString.substring(16), 16));
		String name = rs.getString(5);
		String server = rs.getString(6);
		String content = rs.getString(7);
		
		long playerId = getPlayerId(uuid, name);
		if (type == 3) {
			long sessionId = lastSessionId.incrementAndGet();
			sessionIds.put(uuid, sessionId);
			Files.writeString(sessionDir.resolve(sessionId + ".csv"), escapeCsvValues(sessionId, playerId, content));
		}
		long sessionId = sessionIds.getOrDefault(uuid, 0L);
		long serverId = getServerId(server);
		logsWriter.append("%s%n".formatted(escapeCsvValues(id, time, playerId, sessionId, serverId, type)));
		
		switch (type) {
			case 1 -> chatWriter.append("%s%n".formatted(escapeCsvValues(id, content)));
			case 2 -> {
				Matcher match = COMMAND_PATTERN.matcher(content);
				if (match.matches()) {
					cmdsWriter.append("%s%n".formatted(escapeCsvValues(id, match.group(1), match.group(2))));
				} else {
					throw new IllegalStateException("Cannot match command: `%s`".formatted(content));
				}
			}
			case 5 -> srvChWriter.append("%s%n".formatted(escapeCsvValues(id, 0, content.substring(content.indexOf(':') + 1))));
			case 6 -> srvChWriter.append("%s%n".formatted(escapeCsvValues(id, getServerId(content), "")));
		}
	}
	
	private <T> void uploadProcessedDataInDir(Path directory, BiConsumer<Connection, List<T>> uploader, Function<String[], T> parser, int columns, String processName) {
		Pattern separator = buildCsvSeparator(columns);
		IntStream.range(0, 16)
				.parallel()
				.forEach(index -> {
					long start = System.currentTimeMillis();
					List<T> data = new LinkedList<>();
					try (Connection conn = hikari.getConnection()) {
						preInsertion(conn);
						Files.walk(directory, 1)
								.map(Path::getFileName)
								.map(Path::toString)
								.map(CSV_NUMERIC_PATTERN::matcher)
								.filter(Matcher::matches)
								.map(m -> m.group(1))
								.map(Integer::parseInt)
								.filter(i -> i % 16 == index)
								.forEach(fileIndex -> {
									if (data.size() == 65536 / columns) {
										uploader.accept(conn, data);
										data.clear();
									}
									try {
										String datta = Files.readString(directory.resolve(fileIndex + ".csv"));
										String[] vars = getEscapedCsvValues(separator, datta, columns);
										data.add(parser.apply(vars));
									} catch (IOException e) {
										throw new RuntimeException(e);
									}
								});
						if (!data.isEmpty()) {
							uploader.accept(conn, data);
						}
						postInsertion(conn);
						this.plugin.getLogger().info("[Thread-%s] Completed %s in %dms".formatted(index, processName, System.currentTimeMillis() - start));
					} catch (IOException | SQLException e) {
						throw new RuntimeException(e);
					}
				});
	}
	
	private <T> void uploadProcessedDataByThreadFile(Path directory, BiConsumer<Connection, List<T>> uploader, Function<String[], T> parser, int columns, String processName) {
		Pattern separator = buildCsvSeparator(columns);
		AtomicInteger globalCounter = new AtomicInteger(0);
		IntStream.range(0, 16)
				.parallel()
				.forEach(index -> {
					AtomicInteger threadCounter = new AtomicInteger(0);
					long start = System.currentTimeMillis();
					LinkedList<T> data = new LinkedList<>();
					AtomicInteger counter = new AtomicInteger(0);
					try (Connection conn = hikari.getConnection()) {
						preInsertion(conn);
						try (BufferedReader br = Files.newBufferedReader(directory.resolve(Integer.toHexString(index) + ".csv"))) {
							br.lines()
									.filter(str -> !str.isBlank())
									.map(str -> getEscapedCsvValues(separator, str, columns))
									.forEach(vars -> {
										if (data.size() == 65536 / columns) {
											long queryStart = System.currentTimeMillis();
											uploader.accept(conn, data);
											int threadTotal = threadCounter.addAndGet(data.size());
											int globalTotal = globalCounter.addAndGet(data.size());
											int total = counter.addAndGet(data.size());
											data.clear();
											this.plugin.getLogger().info("[Thread-%s] Inserted %,d %s to db (%,d in thread, %,d in total) %dms".formatted(index, total, processName, threadTotal, globalTotal, System.currentTimeMillis() - queryStart));
										}
										data.add(parser.apply(vars));
									});
							if (!data.isEmpty()) {
								uploader.accept(conn, data);
								data.clear();
							}
						}
						postInsertion(conn);
						this.plugin.getLogger().info("[Thread-%s] Completed %s in %dms".formatted(index, processName, System.currentTimeMillis() - start));
					} catch (IOException | SQLException e) {
						throw new RuntimeException(e);
					}
				});
	}
	
	
	
	
	private void insertServerNames(Connection conn, List<ServerNameEntry> serverNames) {
		String query = "INSERT INTO `%1$s` (`id`, `name`) VALUES ".formatted(TABLE_SERVERS)
				+ serverNames.stream().map(s -> "(?, ?)").collect(Collectors.joining(", "));
		try (PreparedStatement stm = conn.prepareStatement(query)) {
			int i = 1;
			for (ServerNameEntry srv : serverNames) {
				stm.setLong(i++, srv.id());
				stm.setString(i++, srv.name());
			}
			stm.executeUpdate();
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}
	
	
	private void insertPlayerIds(Connection conn, List<PlayerIdEntry> playerIds) {
		String query = "INSERT INTO `%1$s` (`id`, `uuid`, `username`) VALUES ".formatted(TABLE_PLAYERS)
				+ playerIds.stream().map(s -> "(?, ?, ?)").collect(Collectors.joining(", "));
		try (PreparedStatement stm = conn.prepareStatement(query)) {
			int i = 1;
			for (PlayerIdEntry pid : playerIds) {
				stm.setLong(i++, pid.id());
				stm.setBytes(i++, uuidToBytes(pid.uuid()));
				stm.setString(i++, pid.name());
			}
			stm.executeUpdate();
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}
	
	
	private void insertSessionIds(Connection conn, List<SessionIdEntry> sessionIds) {
		String query = "INSERT INTO `%1$s` (`id`, `player_id`, `protocol_id`, `ip_address`, `client`, `hostname`) VALUES ".formatted(TABLE_SESSIONS)
				+ sessionIds.stream().map(s -> "(?, ?, 0, INET6_ATON(?), 'UNKNOWN', '')").collect(Collectors.joining(", "));
		try (PreparedStatement stm = conn.prepareStatement(query)) {
			int i = 1;
			for (SessionIdEntry pid : sessionIds) {
				stm.setLong(i++, pid.id());
				stm.setLong(i++, pid.playerId());
				stm.setString(i++, pid.ip());
			}
			stm.executeUpdate();
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}
	
	
	private void insertLogs(Connection conn, List<LogsEntry> logs) {
		String query = "INSERT INTO `%1$s` (`id`, `time`, `player_id`, `session_id`, `server_id`, `type`) VALUES ".formatted(TABLE_LOGS)
				+ logs.stream().map(s -> "(?, ?, ?, ?, ?, ?)").collect(Collectors.joining(", "));
		try (PreparedStatement stm = conn.prepareStatement(query)) {
			int i = 1;
			for (LogsEntry log : logs) {
				stm.setLong(i++, log.id());
				stm.setTimestamp(i++, Timestamp.from(Instant.ofEpochSecond(log.time())));
				stm.setLong(i++, log.playerId());
				stm.setLong(i++, log.sessionId());
				stm.setLong(i++, log.serverId());
				stm.setInt(i++, log.type());
			}
			stm.executeUpdate();
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}
	
	
	private void insertChats(Connection conn, List<ChatEntry> chats) {
		String query = "INSERT INTO `%1$s` (`id`, `message`) VALUES ".formatted(TABLE_CHAT)
				+ chats.stream().map(s -> "(?, ?)").collect(Collectors.joining(", "));
		try (PreparedStatement stm = conn.prepareStatement(query)) {
			int i = 1;
			for (ChatEntry chat : chats) {
				stm.setLong(i++, chat.id());
				stm.setString(i++, chat.message());
			}
			stm.executeUpdate();
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}
	
	
	private void insertCommands(Connection conn, List<CommandEntry> cmds) {
		String query = "INSERT INTO `%1$s` (`id`, `command`, `arguments`) VALUES ".formatted(TABLE_COMMANDS)
				+ cmds.stream().map(s -> "(?, ?, ?)").collect(Collectors.joining(", "));
		try (PreparedStatement stm = conn.prepareStatement(query)) {
			int i = 1;
			for (CommandEntry cmd : cmds) {
				stm.setLong(i++, cmd.id());
				stm.setString(i++, cmd.command());
				stm.setString(i++, cmd.args());
			}
			stm.executeUpdate();
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}
	
	
	private void insertServerSwitches(Connection conn, List<ServerSwitchEntry> entries) {
		String query = "INSERT INTO `%1$s` (`id`, `target_server`, `extra`) VALUES ".formatted(TABLE_SERVERCHANGES)
				+ entries.stream().map(s -> "(?, ?, ?)").collect(Collectors.joining(", "));
		try (PreparedStatement stm = conn.prepareStatement(query)) {
			int i = 1;
			for (ServerSwitchEntry entry : entries) {
				stm.setLong(i++, entry.id());
				stm.setLong(i++, entry.target());
				stm.setString(i++, entry.extra());
			}
			stm.executeUpdate();
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}
	
	private long getServerId(String serverName) {
		return serverIds.computeIfAbsent(serverName.toLowerCase(), srv -> {
			long sid = lastServerId.incrementAndGet();
			try {
				Files.writeString(serverDir.resolve(sid + ".csv"), escapeCsvValues(sid, serverName));
				return sid;
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		});
	}
	
	public long getPlayerId(UUID uuid, String name) {
		return playerIds.computeIfAbsent(uuid, uid -> {
			long pid = lastPlayerId.incrementAndGet();
			try {
				Files.writeString(playerDir.resolve(pid + ".csv"), escapeCsvValues(pid, uid, name));
				return pid;
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		});
	}
	
	
	private static void preInsertion(Connection conn) throws SQLException {
		try (Statement stm = conn.createStatement()) {
			stm.addBatch("SET UNIQUE_CHECKS=0");
			stm.addBatch("SET FOREIGN_KEY_CHECKS=0");
			stm.addBatch("SET autocommit=0");
			stm.executeBatch();
		}
	}
	
	
	private static void postInsertion(Connection conn) throws SQLException {
		try (Statement stm = conn.createStatement()) {
			stm.addBatch("SET UNIQUE_CHECKS=1");
			stm.addBatch("SET FOREIGN_KEY_CHECKS=1");
			stm.addBatch("COMMIT");
			stm.executeBatch();
		}
	}
	
	private static Pattern buildCsvSeparator(int columns) {
		return Pattern.compile("^"
				+ "((.*?)((?<=[^\\\\]|^)(\\\\\\\\)*));".repeat(columns - 1)
				+ "((.*?))$");
	}
	
	private static String escapeCsvValues(Object... data) {
		return Stream.of(data)
				.map(s -> s == null ? "" : String.valueOf(s))
				.map(s -> s.replace("\\", "\\\\").replace(";", "\\;").replace("\n", "\\n"))
				.collect(Collectors.joining(";"));
	}
	
	private static String[] getEscapedCsvValues(Pattern separator, String data, int columns) {
		Matcher match = separator.matcher(data);
		String[] values = new String[columns];
		if (match.matches()) {
			for (int i = 0; i < columns; i++) {
				int group = 1 + i * 4; // 1, 5, 9, 16, 21, and so on...
				try {
					String val = match.group(group);
					values[i] = val.replace("\\n", "\n").replace("\\;", "\\").replace("\\\\", "\\");
				} catch (Exception e) {
					throw new RuntimeException("Cannot parse group %d for `%s` with pattern `%s`".formatted(group, data, separator.pattern()), e);
				}
			}
			return values;
		} else {
			throw new IllegalArgumentException("Cannot parse text `%s` as valid csv data".formatted(data));
		}
	}

}
