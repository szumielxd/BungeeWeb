package io.github.dead_i.bungeeweb;

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

import io.github.dead_i.bungeeweb.commands.ReloadConfig;
import io.github.dead_i.bungeeweb.hikari.HikariDB;
import io.github.dead_i.bungeeweb.hikari.MariaDB;
import io.github.dead_i.bungeeweb.hikari.MysqlDB;
import io.github.dead_i.bungeeweb.listeners.ChatListener;
import io.github.dead_i.bungeeweb.listeners.PlayerDisconnectListener;
import io.github.dead_i.bungeeweb.listeners.PostLoginListener;
import io.github.dead_i.bungeeweb.listeners.ServerConnectedListener;
import io.github.dead_i.bungeeweb.listeners.ServerKickListener;
import jakarta.servlet.http.HttpServletRequest;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.YamlConfiguration;

public class BungeeWeb extends Plugin {
    
	@Getter @Setter(AccessLevel.PRIVATE) private static BungeeWeb instance;
	@Getter private Configuration config;
	@Getter private Configuration defaultConfig; 
    private @Nullable HikariDB databaseManager;
    private @Nullable PlayerInfoManager playerInfoManager;
    private @Nullable ServerIdManager serverIdManager;

    
    @Override
    public void onLoad() {
    	setInstance(this);
    }
    
    @Override
    public void onEnable() {

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
        String hostName = getConfig().getString("database.host") + ":" + getConfig().getInt("database.port");
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
        this.playerInfoManager = new PlayerInfoManager(databaseManager);
        this.serverIdManager = new ServerIdManager(databaseManager);

        // Start automatic chunking
        setupPurging("log");
        setupPurging("stats");

        // Register listeners
        getProxy().getPluginManager().registerListener(this, new ChatListener(this));
        getProxy().getPluginManager().registerListener(this, new PlayerDisconnectListener(this));
        getProxy().getPluginManager().registerListener(this, new PostLoginListener(this));
        getProxy().getPluginManager().registerListener(this, new ServerConnectedListener(this));
        getProxy().getPluginManager().registerListener(this, new ServerKickListener(this));

        // Register commands
        getProxy().getPluginManager().registerCommand(this, new ReloadConfig(this));

        // Graph loops
        int inc = getConfig().getInt("server.statscheck");
        if (inc > 0) getProxy().getScheduler().schedule(this, new StatusCheck(this, inc), inc, inc, TimeUnit.SECONDS);

        // Setup the context
        ContextHandler context = new ContextHandler("/");
        SessionHandler sessions = new SessionHandler();
        sessions.setHandler(new WebHandler(this));
        context.setHandler(sessions);

        // Setup the server
        final Server server = new Server(getConfig().getInt("server.port"));
        server.setSessionIdManager(new DefaultSessionIdManager(server));
        server.setHandler(sessions);
        server.setStopAtShutdown(true);

        // Start listening
        getProxy().getScheduler().runAsync(this, () -> {
            try {
                server.start();
            } catch(Exception e) {
                getLogger().warning("Unable to bind web server to port.");
                e.printStackTrace();
            }
        });
    }

    public void setupLocale(@NotNull String lang) {
        String filename = "lang/" + lang + ".json";
        Path file = getDataFolder().toPath().resolve(filename);
        try {
            if (Files.notExists(file)) {
                try (InputStream content = getResourceAsStream(filename)) {
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
        Path dir = getDataFolder().toPath().resolve(directory);
        try {
            if (Files.notExists(dir)) {
                try (InputStream content = getResourceAsStream(directory + "/README.md")) {
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
        int days = getConfig().getInt("server." + type + "days");
        int purge = getConfig().getInt("server.purge", 10);
        if (purge > 0 && days > 0) {
            getProxy().getScheduler().schedule(this, new PurgeScheduler(this, type, days), purge, purge, TimeUnit.MINUTES);
        }
    }
    
    public @NotNull PlayerInfoManager getPlayerInfoManager() {
    	return Optional.ofNullable(this.playerInfoManager)
    			.orElseThrow(() -> new IllegalStateException("BungeeWeb is not initialized"));
    }
    
    public @NotNull ServerIdManager getServerIdManager() {
    	return Optional.ofNullable(this.serverIdManager)
    			.orElseThrow(() -> new IllegalStateException("BungeeWeb is not initialized"));
    }
    
    public @NotNull HikariDB getDatabaseManager() {
    	return Optional.ofNullable(this.databaseManager)
    			.orElseThrow(() -> new IllegalStateException("BungeeWeb is not initialized"));
    }
    
    public void reloadConfig() {
        if (!this.getDataFolder().exists()) this.getDataFolder().mkdir();
        String filename = "config.yml";
        Path configFile = this.getDataFolder().toPath().resolve(filename);
        try {
            try (InputStream content = this.getResourceAsStream(filename)) {
                if (Files.notExists(configFile)) {
                    Files.write(configFile, content.readAllBytes(), StandardOpenOption.CREATE);
                    this.getLogger().warning(() -> "A new configuration file has been created. Please edit `%s` and restart BungeeCord.".formatted(filename));
                }
                ConfigurationProvider provider = ConfigurationProvider.getProvider(YamlConfiguration.class);
                config = provider.load(Files.readString(configFile));
                defaultConfig = provider.load(content);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static String getUUID(ProxiedPlayer p) {
        return p.getUniqueId().toString().replace("-", "");
    }

    public List<Object> getGroupPermissions(int group) {
        List<Object> permissions = new ArrayList<>();

        for (int i = group; i > 0; i--) {
            String key = "permissions.group" + i;
            permissions.addAll(config.getList(key, defaultConfig.getList(key)));
        }

        return permissions;
    }

    public static int getGroupPower(HttpServletRequest req) {
        int group = (Integer) req.getSession().getAttribute("group");
        if (group >= 3) group++;
        return group;
    }

    public static boolean isNumber(String number) {
        int o;
        try {
            o = Integer.parseInt(number);
        } catch (NumberFormatException ignored) {
            return false;
        }
        return o >= 0;
    }
}
