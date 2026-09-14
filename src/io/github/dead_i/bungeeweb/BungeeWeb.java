package io.github.dead_i.bungeeweb;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.server.session.DefaultSessionIdManager;
import org.eclipse.jetty.server.session.SessionHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import com.google.inject.Inject;
import com.moandjiezana.toml.Toml;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;

import io.github.dead_i.bungeeweb.commands.ReloadConfig;
import io.github.dead_i.bungeeweb.hikari.HikariDB;
import io.github.dead_i.bungeeweb.hikari.MariaDB;
import io.github.dead_i.bungeeweb.hikari.MysqlDB;
import io.github.dead_i.bungeeweb.listeners.ChannelListener;
import io.github.dead_i.bungeeweb.listeners.ChatListener;
import io.github.dead_i.bungeeweb.listeners.PlayerDisconnectListener;
import io.github.dead_i.bungeeweb.listeners.PostLoginListener;
import io.github.dead_i.bungeeweb.listeners.ServerConnectedListener;
import io.github.dead_i.bungeeweb.listeners.ServerKickListener;
import jakarta.servlet.http.HttpServletRequest;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

@Plugin(
		id = "id----",
		name = "@pluginName@",
		version = "@version@",
		authors = { "@author@" },
		description = "@description@",
		url = "https://github.com/szumielxd/BungeeWeb/"
)
public class BungeeWeb {
	
	@Getter @Setter(AccessLevel.PRIVATE) private static BungeeWeb instance;
	@Getter private Toml config;
	@Getter private ProxyServer proxy;
	@Getter private Logger logger;
	@Getter private Path dataFolder;
	private @Nullable HikariDB databaseManager;
	private @Nullable PlayerInfoManager playerInfoManager;
	private @Nullable ServerIdManager serverIdManager;
	private @Nullable ClientIdManager clientIdManager;
	
	
	@Inject
	public BungeeWeb(ProxyServer proxy, Logger logger, @DataDirectory final Path dataFolder) {
		setInstance(this);
		this.proxy = proxy;
		this.logger = logger;
		this.dataFolder = dataFolder;
	}
	
	
	//Function for loading config
	public void reloadConfig() {
		File file = this.getDataFolder().resolve("config.toml").toFile();
		if (!file.getParentFile().exists()) {
			file.getParentFile().mkdirs();
		}
		if (!file.exists()) {
			try (InputStream input = getClass().getResourceAsStream("/" + file.getName())) {
				if (input != null) {
					Files.copy(input, file.toPath());
				} else {
					file.createNewFile();
				}
			} catch (IOException e) {
				e.printStackTrace();
				return;
			}
		}
		this.config = new Toml().read(file);
	}
	
	@Subscribe
	public void onProxyInitialization(ProxyInitializeEvent event) {

		// Get configuration
		reloadConfig();

		// Setup locales
		setupDirectory("lang");
		setupLocale("en");
		setupLocale("fr");
		setupLocale("es");
		setupLocale("de");
		setupLocale("it");

		// Setup directories
		setupDirectory("themes");

		// Connect to the database
		String hostName = getConfig().getString("database.host") + ":" + getConfig().getLong("database.port");
		String dbName = getConfig().getString("database.db");
		Map<String, String> dbProperties = Map.of("useUnicode", "true", "characterEncoding", "utf8");
		String dbUser = getConfig().getString("database.user");
		String dbPasswd = getConfig().getString("database.pass");
		this.databaseManager = (getConfig().getString("database.mode", "MySQL").equalsIgnoreCase("MySQL")?
				new MysqlDB(this, hostName, dbName, dbProperties, dbUser, dbPasswd)
				: new MariaDB(this, hostName, dbName, dbProperties, dbUser, dbPasswd))
				.setup();
		
		
		// Initial database table setup
		if (!this.databaseManager.initialize()) {
			return;
		}
		// Setup managers
		this.playerInfoManager = new PlayerInfoManager(this);
		this.serverIdManager = new ServerIdManager(databaseManager);
		this.clientIdManager = new ClientIdManager(databaseManager);
		this.playerInfoManager.start();

		// Start automatic chunking
		setupPurging("log");
		setupPurging("stats");

		// Register listeners
		getProxy().getEventManager().register(this, new ChatListener(this));
		getProxy().getEventManager().register(this, new PlayerDisconnectListener(this));
		getProxy().getEventManager().register(this, new PostLoginListener(this));
		getProxy().getEventManager().register(this, new ServerConnectedListener(this));
		getProxy().getEventManager().register(this, new ServerKickListener(this));
		getProxy().getEventManager().register(this, new ChannelListener(this));

		// Register commands
		var commandManager = getProxy().getCommandManager();
		var reloadMeta = commandManager.metaBuilder("bwreload").plugin(this).build();
		commandManager.register(reloadMeta, new ReloadConfig(this));

		// Graph loops
		long inc = getConfig().getLong("server.statscheck");
		if (inc > 0) {
			getProxy().getScheduler().buildTask(this, new ServerStatsCheck(this))
					.delay(inc, TimeUnit.SECONDS)
					.repeat(inc, TimeUnit.SECONDS)
					.schedule();
			getProxy().getScheduler().buildTask(this, new ClientStatsCheck(this))
					.delay(inc, TimeUnit.SECONDS)
					.repeat(inc, TimeUnit.SECONDS)
					.schedule();
		}

		// Setup the context
		ContextHandler context = new ContextHandler("/");
		SessionHandler sessions = new SessionHandler();
		sessions.setHandler(new WebHandler(this));
		context.setHandler(sessions);

		// Setup the server
		final Server server = new Server(getConfig().getLong("server.port").intValue());
		server.setSessionIdManager(new DefaultSessionIdManager(server));
		server.setHandler(sessions);
		server.setStopAtShutdown(true);

		// Start listening
		getProxy().getScheduler().buildTask(this, () -> {
			try {
				server.start();
			} catch(Exception e) {
				getLogger().warn("Unable to bind web server to port.");
				e.printStackTrace();
			}
		}).schedule();
	}

	public void setupLocale(@NotNull String lang) {
		String filename = "lang/" + lang + ".json";
		Path file = getDataFolder().resolve(filename);
		try {
			if (Files.notExists(file)) {
				try (InputStream content = getClass().getResourceAsStream("/" + filename)) {
					Files.write(file, content.readAllBytes(), StandardOpenOption.CREATE);
				}
			}
		} catch (FileAlreadyExistsException e) {
			// file already exists
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void setupDirectory(@NotNull String directory) {
		Path dir = getDataFolder().resolve(directory);
		try {
			if (Files.notExists(dir)) {
				try (InputStream content = getClass().getResourceAsStream("/" + directory + "/README.md")) {
					this.getLogger().info("XXX: `%s`".formatted(directory));
					Files.createDirectory(dir);
					Path readme = dir.resolve("REAMDE.md");
					Files.write(readme, content.readAllBytes(), StandardOpenOption.CREATE);
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void setupPurging(@NotNull String type) {
		/*int days = getConfig().getInt("server." + type + "days");
		int purge = getConfig().getInt("server.purge", 10);
		if (purge > 0 && days > 0) {
			getProxy().getScheduler().schedule(this, new PurgeScheduler(this, type, days), purge, purge, TimeUnit.MINUTES);
		}*/
	}
	
	public @NotNull PlayerInfoManager getPlayerInfoManager() {
		return Optional.ofNullable(this.playerInfoManager)
				.orElseThrow(() -> new IllegalStateException("BungeeWeb is not initialized"));
	}
	
	public @NotNull ServerIdManager getServerIdManager() {
		return Optional.ofNullable(this.serverIdManager)
				.orElseThrow(() -> new IllegalStateException("BungeeWeb is not initialized"));
	}
	
	public @NotNull ClientIdManager getClientIdManager() {
		return Optional.ofNullable(this.clientIdManager)
				.orElseThrow(() -> new IllegalStateException("BungeeWeb is not initialized"));
	}
	
	public @NotNull HikariDB getDatabaseManager() {
		return Optional.ofNullable(this.databaseManager)
				.orElseThrow(() -> new IllegalStateException("BungeeWeb is not initialized"));
	}

	public static String getUUID(Player p) {
		return p.getUniqueId().toString().replace("-", "");
	}

	public List<Object> getGroupPermissions(int group) {
		List<Object> permissions = new ArrayList<>();

		for (int i = group; i > 0; i--) {
			String key = "permissions.group" + i;
			permissions.addAll(config.getList(key));
		}

		return permissions;
	}

	public static int getGroupPower(HttpServletRequest req) {
		int group = (Integer) req.getSession().getAttribute("group");
		if (group >= 3) group++;
		return group;
	}

	public static boolean isNumber(String number) {
		try {
			return Long.parseLong(number) >= 0;
		} catch (NumberFormatException ignored) {
			return false;
		}
	}
}
