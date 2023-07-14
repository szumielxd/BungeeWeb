package io.github.dead_i.bungeeweb.hikari;

import java.nio.ByteBuffer;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import io.github.dead_i.bungeeweb.BungeeWeb;
import io.github.dead_i.bungeeweb.PlayerInfoManager.PlayerSession;
import io.github.dead_i.bungeeweb.SecureUtils;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.connection.Server;

public abstract class HikariDB {
	
	
	protected HikariDataSource hikari;
	
	private final BungeeWeb plugin;
	private final String dbHost;
	private final String dbName;
	private final String dbUser;
	private final String dbPasswd;
	private final Map<String, String> dbProps;
	
	private static final String PREFIX = BungeeWeb.getInstance().getConfig().getString("database.prefix");
	
	public static final String TABLE_OLD_LOG = PREFIX + "log";

	public static final String TABLE_STATS = PREFIX + "stats";
	public static final String TABLE_USERS = PREFIX + "users";
	public static final String TABLE_SERVERS = PREFIX + "servers";
	public static final String TABLE_PLAYERS = PREFIX + "players";
	public static final String TABLE_SESSIONS = PREFIX + "player_sessions";
	public static final String TABLE_LOGS = PREFIX + "logs";
	public static final String TABLE_CHAT = PREFIX + "chat";
	public static final String TABLE_COMMANDS = PREFIX + "commands";
	public static final String TABLE_SERVERCHANGES = PREFIX + "serverchanges";

	public static final Pattern COMMAND_PATTERN = Pattern.compile("^/([^ ]*)( (.*))?$");

	
	public static String escapeSql(String text) {
		return text.replace("\\", "\\\\").replace("'", "\\'").replace("\"", "\\\"");
	}
	
	public record UserProfile(int id, String username, int groupId) {}
	
	
	protected HikariDB(BungeeWeb plugin, String host, String database, Map<String, String> properties, String user, String pass) {
		
		this.plugin = plugin;
		dbHost = host;
		dbName = database;
		dbUser = user;
		dbPasswd = pass;
		dbProps = new HashMap<>(properties);
		
	}
	
	
	/**
	 * Get default port for this implementation of HikariCP.
	 * 
	 * @return default port
	 */
	protected abstract int getDefaultPort();
	
	
	/**
	 * Setup database connection properties.
	 */
	public HikariDB setup() {
		HikariConfig config = new HikariConfig();
		config.setPoolName("bungeeweb-hikari");
		final String[] host = dbHost.split(":");
		int port = this.getDefaultPort();
		if (host.length > 1) {
			try {
				port = Integer.parseInt(host[1]);
			} catch (NumberFormatException e) {
				// fallback to default port
			}
		}
		this.setupDatabase(config, host[0], port, dbName, dbUser, dbPasswd);
		
		this.setupProperties(config, dbProps);
		config.setMaximumPoolSize(32);
		config.setMinimumIdle(10);
		config.setMaxLifetime(1800000);
		config.setConnectionTimeout(120 * 1000L);
		config.setKeepaliveTime(0);
		config.setConnectionTimeout(5000);
		config.setInitializationFailTimeout(-1);
		
		this.hikari = new HikariDataSource(config);
		return this;
	}
	
	public void tryUpgradeDatabaseSchema() {
		if (checkTableExistence(TABLE_OLD_LOG)) {
			this.plugin.getLogger().info("Detected old database schema. Upgrading to new format...");
			new DatabaseFormatMigration(this.hikari, this.plugin).migrate();
			for (int i = 0; true; i++) {
				String name = "bungeeweb_log_migrated" + (i > 0 ? i : "");
				if (!checkTableExistence(name)) {
					renameTable(TABLE_OLD_LOG, name);
					break;
				}
			}
		}
	}
	
	public void setupTables() {
		String createStats = """
                CREATE TABLE IF NOT EXISTS `%1$s` (
                    `id` int(10) unsigned NOT NULL AUTO_INCREMENT,
                    `time` int(10) unsigned NOT NULL,
                    `playercount` int(11) NOT NULL DEFAULT -1,
                    `maxplayers` int(11) NOT NULL DEFAULT -1,
                    `activity` int(11) NOT NULL DEFAULT -1,
                    PRIMARY KEY (`id`)
                )
                """.formatted(TABLE_STATS);
        String createUsers = """
                CREATE TABLE IF NOT EXISTS `%1$s` (
                    `id` int(10) unsigned NOT NULL AUTO_INCREMENT,
                    `user` varchar(16) NOT NULL,
                    `pass` char(32) NOT NULL,
                    `salt` char(16) NOT NULL,
                    `group` tinyint(1) unsigned NOT NULL DEFAULT '1',
                    PRIMARY KEY (`id`)
                )
                """.formatted(TABLE_USERS);
		String createServers = """
                CREATE TABLE IF NOT EXISTS `%1$s` (
                    `id` int(10) unsigned NOT NULL AUTO_INCREMENT,
                    `name` varchar(32) NOT NULL,
                    PRIMARY KEY (`id`),
                    UNIQUE KEY (`name`)
                )
                """.formatted(TABLE_SERVERS);
		String createPlayers = """
                CREATE TABLE IF NOT EXISTS `%1$s` (
                    `id` int(10) unsigned NOT NULL AUTO_INCREMENT,
                    `uuid` binary(16) NOT NULL,
                    `username` varchar(16) NOT NULL,
                    PRIMARY KEY (`id`),
                    UNIQUE KEY (`uuid`)
                )
                """.formatted(TABLE_PLAYERS);
		String createSessions = """
                CREATE TABLE IF NOT EXISTS `%1$s` (
                    `id` int(10) unsigned NOT NULL AUTO_INCREMENT,
                    `player_id` int(10) unsigned NOT NULL,
                    `protocol_id` smallint(5) unsigned NOT NULL,
                    `ip_address` varbinary(16) NOT NULL,
                    `client` varchar(32) NOT NULL DEFAULT 'UNKNOWN',
                    `hostname` varchar(32) NOT NULL,
                    PRIMARY KEY (`id`),
                    KEY `ip_address` (`ip_address`),
                    FOREIGN KEY (`player_id`) REFERENCES `%2$s` (`id`)
                )
                """.formatted(TABLE_SESSIONS, TABLE_PLAYERS);
		String createLogs = """
                CREATE TABLE IF NOT EXISTS `%1$s` (
                    `id` int(10) unsigned NOT NULL AUTO_INCREMENT,
                    `time` datetime NOT NULL,
                    `player_id` int(10) unsigned NOT NULL,
                    `session_id` int(10) unsigned NOT NULL,
                    `server_id` int(10) unsigned NOT NULL,
                    `type` tinyint unsigned NOT NULL,
                    PRIMARY KEY (`id`),
                    FOREIGN KEY (`player_id`) REFERENCES `%2$s` (`id`),
                    FOREIGN KEY (`server_id`) REFERENCES `%3$s` (`id`),
                    FOREIGN KEY (`session_id`) REFERENCES `%4$s` (`id`)
                )
                """.formatted(TABLE_LOGS, TABLE_PLAYERS, TABLE_SERVERS, TABLE_SESSIONS);
		String createChats = """
                CREATE TABLE IF NOT EXISTS `%1$s` (
                    `id` int(10) unsigned NOT NULL,
                    `message` varchar(256) NOT NULL,
                    PRIMARY KEY (`id`),
                    FOREIGN KEY (`id`) REFERENCES `%2$s` (`id`)
                ) DEFAULT CHARSET=utf8mb4
                """.formatted(TABLE_CHAT, TABLE_LOGS);
		String createCommands = """
                CREATE TABLE IF NOT EXISTS `%1$s` (
                    `id` int(10) unsigned NOT NULL,
                    `command` varchar(255) NOT NULL,
                    `arguments` varchar(256) NOT NULL,
                    PRIMARY KEY (`id`),
                    FOREIGN KEY (`id`) REFERENCES `%2$s` (`id`)
                ) DEFAULT CHARSET=utf8mb4
                """.formatted(TABLE_COMMANDS, TABLE_LOGS);
		String createSwitches = """
                CREATE TABLE IF NOT EXISTS `%1$s` (
                    `id` int(10) unsigned NOT NULL,
                    `target_server` int(10) unsigned NOT NULL,
                    `extra` varchar(256) NOT NULL,
                    PRIMARY KEY (`id`),
                    FOREIGN KEY (`id`) REFERENCES `%2$s` (`id`),
                    FOREIGN KEY (`target_server`) REFERENCES `%3$s` (`id`)
                ) DEFAULT CHARSET=utf8mb4
                """.formatted(TABLE_SERVERCHANGES, TABLE_LOGS, TABLE_SERVERS);
		
		try (Connection conn = hikari.getConnection()) {
			try (Statement stm = conn.createStatement()) {
				stm.addBatch(createStats);
				stm.addBatch(createUsers);
				stm.addBatch(createServers);
				stm.addBatch(createPlayers);
				stm.addBatch(createSessions);
				stm.addBatch(createLogs);
				stm.addBatch(createChats);
				stm.addBatch(createCommands);
				stm.addBatch(createSwitches);
				stm.executeBatch();
			}
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}
	
	public boolean initialize() {
		try (Connection db = this.hikari.getConnection()) {
            if (db == null) {
                this.plugin.getLogger().severe("BungeeWeb is disabling. Please check your database settings in your config.yml");
                return false;
            }
            String prefix = this.plugin.getConfig().getString("database.prefix");
            this.setupTables();

            try (ResultSet rs = db.createStatement().executeQuery(String.format("SELECT COUNT(*) FROM `%susers`", prefix))) {
                while (rs.next()) if (rs.getInt(1) == 0) {
                    String salt = SecureUtils.salt();
                    try (PreparedStatement stm = db.prepareStatement(String.format("INSERT INTO `%susers` (`user`, `pass`, `salt`, `group`) VALUES('admin', ?, ?, 3)", prefix))) { 
                        stm.setString(1, SecureUtils.encrypt("admin", salt));
                        stm.setString(2, salt);
                        stm.executeUpdate();
                    }
                    this.plugin.getLogger().warning("A new admin account has been created.");
                    this.plugin.getLogger().warning("Both the username and password is 'admin'. Please change the password after first logging in.");
                }
            }
        } catch (SQLException e) {
        	this.plugin.getLogger().severe("Unable to connect to the database. Disabling...");
            e.printStackTrace();
            return false;
        }
		this.tryUpgradeDatabaseSchema();
		return true;
	}
	
	public void insertPlayerSession(PlayerSession session) {
		try (Connection conn = this.hikari.getConnection()) {
			String sql = "INSERT INTO `%1$s` (`player_id`, `protocol_id`, `ip_address`, `client`, `hostname`) VALUES (?, ?, INET6_ATON(?), ?, ?)".formatted(TABLE_SESSIONS);
			try (PreparedStatement stm = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
				stm.setLong(1, session.getPlayerId());
				stm.setInt(2, session.getProtocol());
				stm.setString(3, session.getIp());
				stm.setString(4, session.getClient());
				stm.setString(5, session.getHostname());
				stm.executeUpdate();
				try (ResultSet rs = stm.getGeneratedKeys()) {
					rs.next();
					session.setId(rs.getLong(1));
				}
			}
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}
	
	public long getOrCreatePlayerId(UUID uuid, String username) {
		try (Connection conn = this.hikari.getConnection()) {
			String select = "SELECT `id` FROM `%1$s` WHERE `uuid` = ?".formatted(TABLE_PLAYERS);
			try (PreparedStatement stm = conn.prepareStatement(select)) {
				stm.setBytes(1, uuidToBytes(uuid));
				try (ResultSet rs = stm.executeQuery()) {
					if (rs.next()) {
						return rs.getLong(1);
					}
				}
			}
			String insert = "INSERT INTO `%1$s` (`uuid`, `username`) VALUES (?, ?)".formatted(TABLE_PLAYERS);
			try (PreparedStatement stm = conn.prepareStatement(insert, Statement.RETURN_GENERATED_KEYS)) {
				stm.setBytes(1, uuidToBytes(uuid));
				stm.setString(2, username);
				stm.executeUpdate();
				try (ResultSet rs = stm.getGeneratedKeys()) {
					rs.next();
					return rs.getLong(1);
				}
			}
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}
	
	public long getOrCreateServerId(String serverName) {
		try (Connection conn = this.hikari.getConnection()) {
			String select = "SELECT `id` FROM `%1$s` WHERE `name` = ?".formatted(TABLE_SERVERS);
			try (PreparedStatement stm = conn.prepareStatement(select)) {
				stm.setString(1, serverName);
				try (ResultSet rs = stm.executeQuery()) {
					if (rs.next()) {
						return rs.getLong(1);
					}
				}
			}
			String insert = "INSERT INTO `%1$s` (`name`) VALUES (?)".formatted(TABLE_SERVERS);
			try (PreparedStatement stm = conn.prepareStatement(insert, Statement.RETURN_GENERATED_KEYS)) {
				stm.setString(1, serverName);
				stm.executeUpdate();
				try (ResultSet rs = stm.getGeneratedKeys()) {
					rs.next();
					return rs.getLong(1);
				}
			}
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}
	


    public @NotNull Optional<UserProfile> getLogin(@Nullable String user, @Nullable String pass) {
        if (user != null && pass != null) {
        	try (Connection conn = this.hikari.getConnection()) {
                try (PreparedStatement st = conn.prepareStatement(String.format("SELECT `id`, `user`, `group`, `pass`, `salt` FROM `%1$s` WHERE `user` = ?", TABLE_USERS))) {
                    st.setString(1, user);
                    try (ResultSet rs = st.executeQuery()) {
                    	if (rs.next() && rs.getString(4).equals(SecureUtils.encrypt(pass, rs.getString(5)))) {
                        	return Optional.of(new UserProfile(rs.getInt(1), rs.getString(2), rs.getInt(3)));
                        }
                    }
                }
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }
        return Optional.empty();
    }
	
	
	public void logPlayerChat(@NotNull ProxiedPlayer player, String message) {
		String sql = "INSERT INTO `%1$s` (`id`, `message`) VALUES (?, ?)".formatted(TABLE_CHAT);
		PlayerSession session = this.plugin.getPlayerInfoManager().getActiveSession(player.getUniqueId())
				.orElseThrow(() -> new IllegalStateException("Could not find any session for player %s (%s)".formatted(player.getName(), player.getUniqueId())));
		long serverId = this.plugin.getServerIdManager().getServerId(Optional.ofNullable(player.getServer()).map(Server::getInfo).map(ServerInfo::getName).orElse(""));
		try (Connection conn = this.hikari.getConnection()) {
			long logId = this.logBaseEntry(conn, session, serverId, LogType.CHAT);
			try (PreparedStatement stm = conn.prepareStatement(sql)) {
				stm.setLong(1, logId);
				stm.setString(2, message);
				stm.executeUpdate();
			}
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}
	
	public void logPlayerCommand(@NotNull ProxiedPlayer player, String command) {
		String sql = "INSERT INTO `%1$s` (`id`, `command`, `arguments`) VALUES (?, ?, ?)".formatted(TABLE_COMMANDS);
		PlayerSession session = this.plugin.getPlayerInfoManager().getActiveSession(player.getUniqueId())
				.orElseThrow(() -> new IllegalStateException("Could not find any session for player %s (%s)".formatted(player.getName(), player.getUniqueId())));
		long serverId = this.plugin.getServerIdManager().getServerId(Optional.ofNullable(player.getServer()).map(Server::getInfo).map(ServerInfo::getName).orElse(""));
		Matcher match = COMMAND_PATTERN.matcher(command);
		if (match.matches()) {
			try (Connection conn = this.hikari.getConnection()) {
				long logId = this.logBaseEntry(conn, session, serverId, LogType.COMMAND);
				try (PreparedStatement stm = conn.prepareStatement(sql)) {
					stm.setLong(1, logId);
					stm.setString(2, match.group(1));
					stm.setString(3, Optional.ofNullable(match.group(2)).orElse(""));
					stm.executeUpdate();
				}
			} catch (SQLException e) {
				throw new RuntimeException(e);
			}
		} else {
			throw new IllegalArgumentException("`%s` is not valid command".formatted(command));
		}
	}
	
	public void logPlayerConnect(@NotNull ProxiedPlayer player) {
		PlayerSession session = this.plugin.getPlayerInfoManager().createNewSession(player);
		long serverId = this.plugin.getServerIdManager().getServerId(Optional.ofNullable(player.getServer()).map(Server::getInfo).map(ServerInfo::getName).orElse(""));
		try (Connection conn = this.hikari.getConnection()) {
			this.logBaseEntry(conn, session, serverId, LogType.JOIN);
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}
	
	public void logPlayerDisconnect(@NotNull ProxiedPlayer player) {
		PlayerSession session = this.plugin.getPlayerInfoManager().getActiveSession(player.getUniqueId())
				.orElseThrow(() -> new IllegalStateException("Could not find any session for player %s (%s)".formatted(player.getName(), player.getUniqueId())));
		long serverId = this.plugin.getServerIdManager().getServerId(Optional.ofNullable(player.getServer()).map(Server::getInfo).map(ServerInfo::getName).orElse(""));
		try (Connection conn = this.hikari.getConnection()) {
			this.logBaseEntry(conn, session, serverId, LogType.QUIT);
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}
	
	public void logPlayerKick(@NotNull ProxiedPlayer player, String fallbackServer, String kickMessage) {
		String sql = "INSERT INTO `%1$s` (`id`, `target_server`, `extra`) VALUES (?, ?, ?)".formatted(TABLE_SERVERCHANGES);
		PlayerSession session = this.plugin.getPlayerInfoManager().getActiveSession(player.getUniqueId())
				.orElseThrow(() -> new IllegalStateException("Could not find any session for player %s (%s)".formatted(player.getName(), player.getUniqueId())));
		long serverId = this.plugin.getServerIdManager().getServerId(Optional.ofNullable(player.getServer()).map(Server::getInfo).map(ServerInfo::getName).orElse(""));
		long fallbackServerId = this.plugin.getServerIdManager().getServerId(fallbackServer);
		try (Connection conn = this.hikari.getConnection()) {
			long logId = this.logBaseEntry(conn, session, serverId, LogType.KICK);
			try (PreparedStatement stm = conn.prepareStatement(sql)) {
				stm.setLong(1, logId);
				stm.setLong(2, fallbackServerId);
				stm.setString(3, kickMessage);
				stm.executeUpdate();
			}
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}
	
	public void logPlayerServerSwitch(@NotNull ProxiedPlayer player, String targetServer) {
		String sql = "INSERT INTO `%1$s` (`id`, `target_server`, `extra`) VALUES (?, ?, '')".formatted(TABLE_SERVERCHANGES);
		PlayerSession session = this.plugin.getPlayerInfoManager().getActiveSession(player.getUniqueId())
				.orElseThrow(() -> new IllegalStateException("Could not find any session for player %s (%s)".formatted(player.getName(), player.getUniqueId())));
		long serverId = this.plugin.getServerIdManager().getServerId(Optional.ofNullable(player.getServer()).map(Server::getInfo).map(ServerInfo::getName).orElse(""));
		long fallbackServerId = this.plugin.getServerIdManager().getServerId(targetServer);
		try (Connection conn = this.hikari.getConnection()) {
			long logId = this.logBaseEntry(conn, session, serverId, LogType.SERVER_CHANGE);
			try (PreparedStatement stm = conn.prepareStatement(sql)) {
				stm.setLong(1, logId);
				stm.setLong(2, fallbackServerId);
				stm.executeUpdate();
			}
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}
	
	private long logBaseEntry(Connection conn, PlayerSession session, long serverId, LogType type) throws SQLException {
		String sql = "INSERT INTO `%1$s` (`time`, `player_id`, `session_id`, `server_id`, `type`) VALUES (?, ?, ?, ?, ?)".formatted(TABLE_LOGS);
		try (PreparedStatement stm = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
			stm.setTimestamp(1, Timestamp.from(Instant.now()));
			stm.setLong(2, session.getPlayerId());
			stm.setLong(3, session.getId());
			stm.setLong(4, serverId);
			stm.setInt(5, type.ordinal() + 1);
			stm.executeUpdate();
			try (ResultSet rs = stm.getGeneratedKeys()) {
				rs.next();
				return rs.getLong(1);
			}
		}
	}
	
	
	
	
	
	private boolean checkTableExistence(String table) {
		try (Connection conn = this.hikari.getConnection()) {
			try (PreparedStatement stm = conn.prepareStatement("SHOW TABLES LIKE ?")) {
				stm.setString(1, table);
				try (ResultSet rs = stm.executeQuery()) {
					if (rs.next()) {
						return true;
					}
				}
			}
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
		return false;
	}
	
	private void renameTable(String table, String newName) {
		try (Connection conn = this.hikari.getConnection()) {
			try (Statement stm = conn.createStatement()) {
				stm.executeUpdate("ALTER TABLE `%s` RENAME `%s`".formatted(table.replace("`", ""), newName.replace("`", "")));
			}
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}
	
	/**
	 * Modify and setup connection properties.
	 * 
	 * @param properties default properties map
	 */
	protected abstract void setupProperties(HikariConfig config, Map<String, String> properties);
	
	/**
	 * Setup database connection.
	 * 
	 * @param config database configuration object
	 * @param address connection's address
	 * @param port connection's port
	 * @param database database name
	 * @param user database user name
	 * @param password database password
	 */
	public abstract void setupDatabase(HikariConfig config, String address, int port, String database, String user, String password);
	
	/**
	 * Get database connection.
	 * 
	 * @return database connection
	 * @throws SQLException when cannot establish database connection
	 */
	public Connection connect() throws SQLException {
		if (this.hikari == null) throw new SQLException("Unable to get a connection from the pool. (hikari is null)");
		Connection conn = this.hikari.getConnection();
		if (conn == null) throw new SQLException("Unable to get a connection from the pool. (connection is null)");
		return conn;
	}
	
	/**
	 * Check if database is connected.
	 * 
	 * @return true if connection to database is opened
	 */
	public boolean isConnected() {
		return this.isValid() && !this.hikari.isClosed();
	}
	
	/**
	 * Check if database connection is valid.
	 * 
	 * @return true if connection to database is valid
	 */
	public boolean isValid() {
		return this.hikari != null;
	}
	
	/**
	 * Shutdown database
	 */
	public void shutdown() {
		if (this.hikari != null) this.hikari.close();
	}
	
	
	/**
	 * Check if connection can be obtained, otherwise creates new one.
	 */
	public void checkConnection() {
		if (!this.isConnected()) this.setup();
	}
	
	public static byte[] uuidToBytes(UUID uuid) {
		ByteBuffer bb = ByteBuffer.wrap(new byte[16]);
		bb.putLong(uuid.getMostSignificantBits());
		bb.putLong(uuid.getLeastSignificantBits());
		return bb.array();
	}
	
	public static UUID bytesToUuid(byte[] bytes) {
		ByteBuffer bb = ByteBuffer.wrap(bytes);
		return new UUID(bb.getLong(), bb.getLong());
	}
	
	public static String commaStmReplacers(int amount) {
		return commaStmReplacers("?", amount);
	}
	
	public static String commaStmReplacers(String toRepeat, int amount) {
		return IntStream.range(0, amount)
				.mapToObj(i -> toRepeat)
				.collect(Collectors.joining(", "));
	}
	
	public enum LogType {
		CHAT,
		COMMAND,
		JOIN,
		QUIT,
		KICK,
		SERVER_CHANGE;
		
		public static @NotNull Optional<LogType> tryParse(@NotNull String name) {
			try {
				return Optional.of(LogType.valueOf(name.toUpperCase()));
			} catch (IllegalArgumentException e) {
				return Optional.empty();
			}
		}
		
	}

}
