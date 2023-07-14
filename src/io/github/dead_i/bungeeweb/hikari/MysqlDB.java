package io.github.dead_i.bungeeweb.hikari;

import java.util.HashMap;
import java.util.Map;

import com.zaxxer.hikari.HikariConfig;

import io.github.dead_i.bungeeweb.BungeeWeb;

public class MysqlDB extends HikariDB {

	public MysqlDB(BungeeWeb plugin, String host, String database, Map<String, String> properties, String user, String pass) {
		super(plugin, host, database, properties, user, pass);
	}

	/**
	 * Get default port for this implementation of HikariCP.
	 * 
	 * @return default port
	 */
	@Override
	protected int getDefaultPort() {
		return 3306;
	}

	/**
	 * Modify and setup connection properties.
	 * 
	 * @param properties default properties map
	 */
	@Override
	protected void setupProperties(HikariConfig config, Map<String, String> properties) {
		properties = new HashMap<>(properties);
		properties.putIfAbsent("socketTimeout", "30000");
		properties.putIfAbsent("cachePrepStmts", "true");
		properties.putIfAbsent("prepStmtCacheSize", "250");
		properties.putIfAbsent("prepStmtCacheSqlLimit", "2048");
		properties.putIfAbsent("useServerPrepStmts", "true");
		properties.putIfAbsent("useLocalSessionState", "true");
		properties.putIfAbsent("rewriteBatchedStatements", "true");
		properties.putIfAbsent("cacheResultSetMetadata", "true");
		properties.putIfAbsent("cacheServerConfiguration", "true");
		properties.putIfAbsent("elideSetAutoCommits", "true");
		properties.putIfAbsent("maintainTimeStats", "false");
		properties.putIfAbsent("alwaysSendSetIsolation", "false");
		properties.putIfAbsent("cacheCallableStmts", "true");
		properties.putIfAbsent("serverTimezone", "UTC");
		properties.forEach(config::addDataSourceProperty);
	}

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
	@Override
	public void setupDatabase(HikariConfig config, String address, int port, String database, String user, String password) {
		try {
			Class.forName("com.mysql.cj.jdbc.Driver");
			config.setDriverClassName("com.mysql.cj.jdbc.Driver");
		} catch (ClassNotFoundException e) {
			config.setDriverClassName("com.mysql.jdbc.Driver");
		}
		config.setJdbcUrl("jdbc:mysql://" + address + ":" + port + "/" + database);
		config.setUsername(user);
		config.setPassword(password);
	}

}
