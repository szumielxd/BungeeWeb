package io.github.dead_i.bungeeweb;

import com.google.common.collect.ImmutableMap;
import com.google.common.io.ByteStreams;
import io.github.dead_i.bungeeweb.commands.*;
import io.github.dead_i.bungeeweb.hikari.HikariDB;
import io.github.dead_i.bungeeweb.hikari.MariaDB;
import io.github.dead_i.bungeeweb.hikari.MysqlDB;
import io.github.dead_i.bungeeweb.listeners.*;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.YamlConfiguration;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.server.session.HashSessionIdManager;
import org.eclipse.jetty.server.session.HashSessionManager;
import org.eclipse.jetty.server.session.SessionHandler;
import org.eclipse.jetty.util.log.StdErrLog;
import org.eclipse.jetty.util.security.Credential;
import javax.servlet.http.HttpServletRequest;
import java.io.*;
import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;

public class BungeeWeb extends Plugin {
    private static Configuration config;
    private static Configuration defaultConfig;
    private static HikariDB hikari;

    @Override
    public void onEnable() {

        // Get configuration
        reloadConfig(this);

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
        hikari = (getConfig().getString("database.mode", "MySQL").equalsIgnoreCase("MySQL")? new MysqlDB(getConfig().getString("database.host") + ":" + getConfig().getInt("database.port"), getConfig().getString("database.db"), ImmutableMap.of("useUnicode", "true", "characterEncoding", "utf8"), getConfig().getString("database.user"), getConfig().getString("database.pass"))
                : new MariaDB(getConfig().getString("database.host") + ":" + getConfig().getInt("database.port"), getConfig().getString("database.db"), ImmutableMap.of("useUnicode", "true", "characterEncoding", "utf8"), getConfig().getString("database.user"), getConfig().getString("database.pass"))).setup();

        // Initial database table setup
        try (Connection db = getDatabase()) {
        	if (db == null) {
                getLogger().severe("BungeeWeb is disabling. Please check your database settings in your config.yml");
                return;
            }
        	String prefix = getConfig().getString("database.prefix");
            try (Statement stm = db.createStatement()) { stm.executeUpdate(String.format("CREATE TABLE IF NOT EXISTS `%slog` (`id` int(11) NOT NULL AUTO_INCREMENT, `time` int(10) NOT NULL, `type` int(2) NOT NULL, `uuid` varchar(32) NOT NULL, `username` varchar(16) NOT NULL, `server` varchar(32) NOT NULL DEFAULT '', `content` varchar(256) NOT NULL DEFAULT '', PRIMARY KEY (`id`)) CHARACTER SET utf8", prefix)); }
            try (Statement stm = db.createStatement()) { stm.executeUpdate(String.format("CREATE TABLE IF NOT EXISTS `%susers` (`id` int(4) NOT NULL AUTO_INCREMENT, `user` varchar(16) NOT NULL, `pass` varchar(32) NOT NULL, `salt` varchar(16) NOT NULL, `group` int(1) NOT NULL DEFAULT '1', PRIMARY KEY (`id`)) CHARACTER SET utf8", prefix)); }
            try (Statement stm = db.createStatement()) { stm.executeUpdate(String.format("CREATE TABLE IF NOT EXISTS `%sstats` (`id` int(11) NOT NULL AUTO_INCREMENT, `time` int(10) NOT NULL, `playercount` int(6) NOT NULL DEFAULT -1, `maxplayers` int(6) NOT NULL DEFAULT -1, `activity` int(12) NOT NULL DEFAULT -1, PRIMARY KEY (`id`)) CHARACTER SET utf8", prefix)); }

            try (ResultSet rs = db.createStatement().executeQuery(String.format("SELECT COUNT(*) FROM `%susers`", prefix))) {
            	while (rs.next()) if (rs.getInt(1) == 0) {
                    String salt = salt();
                    try (PreparedStatement stm = db.prepareStatement(String.format("INSERT INTO `%susers` (`user`, `pass`, `salt`, `group`) VALUES('admin', ?, ?, 3)", prefix))) { 
                    	stm.setString(1, encrypt("admin", salt));
                    	stm.setString(2, salt);
                    	stm.executeUpdate();
                    }
                    getLogger().warning("A new admin account has been created.");
                    getLogger().warning("Both the username and password is 'admin'. Please change the password after first logging in.");
                }
            }
        } catch (SQLException e) {
            getLogger().severe("Unable to connect to the database. Disabling...");
            e.printStackTrace();
            return;
        }

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

        // Setup logging
        org.eclipse.jetty.util.log.Log.setLog(new JettyLogger());
        Properties p = new Properties();
        p.setProperty("org.eclipse.jetty.LEVEL", "WARN");
        StdErrLog.setProperties(p);

        // Setup the context
        ContextHandler context = new ContextHandler("/");
        SessionHandler sessions = new SessionHandler(new HashSessionManager());
        sessions.setHandler(new WebHandler(this));
        context.setHandler(sessions);

        // Setup the server
        final Server server = new Server(getConfig().getInt("server.port"));
        server.setSessionIdManager(new HashSessionIdManager());
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

    public void setupLocale(String lang) {
        try {
            String filename = "lang/" + lang + ".json";
            File file = new File(getDataFolder(), filename);
            if (!file.exists()) file.createNewFile();
            ByteStreams.copy(getResourceAsStream(filename), new FileOutputStream(file));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void setupDirectory(String directory) {
        File dir = new File(getDataFolder(), directory);
        try {
            if (!dir.exists()) {
                dir.mkdir();
                File readme = new File(dir, "REAMDE.md");
                readme.createNewFile();
                ByteStreams.copy(getResourceAsStream(directory + "/README.md"), new FileOutputStream(readme));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void setupPurging(String type) {
        int days = getConfig().getInt("server." + type + "days");
        int purge = getConfig().getInt("server.purge", 10);
        if (purge > 0 && days > 0) {
            getProxy().getScheduler().schedule(this, new PurgeScheduler(type, days), purge, purge, TimeUnit.MINUTES); // NO WAY! -> getProxy().getScheduler().schedule(this, new PurgeScheduler("stats", days), purge, purge, TimeUnit.MINUTES);
        }
    }

    public static Configuration getConfig() {
        return config;
    }

    public static void reloadConfig(Plugin plugin) {
        if (!plugin.getDataFolder().exists()) plugin.getDataFolder().mkdir();
        ConfigurationProvider provider = ConfigurationProvider.getProvider(YamlConfiguration.class);
        InputStream defaultStream = plugin.getResourceAsStream("config.yml");
        File configFile = new File(plugin.getDataFolder(), "config.yml");
        try {
            if (!configFile.exists()) {
                configFile.createNewFile();
                try (FileOutputStream out = new FileOutputStream(configFile)) {
                    ByteStreams.copy(defaultStream, out);
                    plugin.getLogger().warning("A new configuration file has been created. Please edit config.yml and restart BungeeCord.");
                    return;
                }
            }
            config = provider.load(configFile);
        } catch (IOException e) {
            e.printStackTrace();
        }

        try (Scanner scanner = new Scanner(defaultStream, "UTF-8").useDelimiter("\\A")) {
            defaultConfig = provider.load(scanner.next());
        }
    }

    public static Connection getDatabase() throws SQLException {
        return hikari.connect();
    }

    public static void log(Plugin plugin, ProxiedPlayer player, int type) {
        log(plugin, player, type, "");
    }

    public static void log(Plugin plugin, final ProxiedPlayer player, final int type, final String content) {
    	final String server = Optional.ofNullable(player.getServer()).map(srv -> srv.getInfo()).map(ServerInfo::getName).orElse("");
        plugin.getProxy().getScheduler().runAsync(plugin, () -> {
            try (Connection conn = getDatabase()) {
            	try (PreparedStatement stm = conn.prepareStatement(String.format("INSERT INTO `%slog` (`time`, `type`, `uuid`, `username`, `server`, `content`) VALUES(?, ?, ?, ?, ?, ?)", getConfig().getString("database.prefix")))) {
            		stm.setLong(1, System.currentTimeMillis() / 1000);
                    stm.setInt(2, type);
                    stm.setString(3, getUUID(player));
                    stm.setString(4, player.getName());
                    stm.setString(5, server.length() > 32 ? content.substring(0, 31) : server);
                    stm.setString(6, content.length() > 256 ? content.substring(0, 255) : content);
                    stm.executeUpdate();
            	}
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
    }

    public static String getUUID(ProxiedPlayer p) {
        return p.getUniqueId().toString().replace("-", "");
    }

    public static ResultSet getLogin(String user, String pass) {
        if (user == null || pass == null) return null;
        try (Connection conn = getDatabase()) {
        	try (PreparedStatement st = conn.prepareStatement(String.format("SELECT * FROM `%susers` WHERE `user`=?", BungeeWeb.getConfig().getString("database.prefix")))) {
                st.setString(1, user);
                ResultSet rs = st.executeQuery();
                while (rs.next()) if (rs.getString("pass").equals(BungeeWeb.encrypt(pass + rs.getString("salt")))) return rs;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        
        return null;
    }

    public static List<Object> getGroupPermissions(int group) {
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

    public static String encrypt(String pass) {
        return Credential.MD5.digest(pass).split(":")[1];
    }

    public static String encrypt(String pass, String salt) {
        return encrypt(pass + salt);
    }

    public static String salt() {
        byte[] salt = new byte[16];
        new SecureRandom().nextBytes(salt);
        return Base64.getEncoder().encodeToString(salt).substring(0, 16);
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
