package io.github.dead_i.bungeeweb.hikari;

import java.util.HashMap;
import java.util.Map;

import com.zaxxer.hikari.HikariConfig;

import io.github.dead_i.bungeeweb.BungeeWeb;

public class MariaDB extends HikariDB {

	public MariaDB(BungeeWeb plugin, String host, String database, Map<String, String> properties, String user, String pass) {
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
		config.setDataSourceClassName("org.mariadb.jdbc.MariaDbDataSource");
		config.addDataSourceProperty("serverName", address);
		config.addDataSourceProperty("port", port);
		config.addDataSourceProperty("databaseName", database);
		config.setUsername(user);
		config.setPassword(password);
	}

}
