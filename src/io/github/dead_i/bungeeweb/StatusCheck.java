package io.github.dead_i.bungeeweb;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;

import org.jetbrains.annotations.NotNull;

import net.md_5.bungee.config.Configuration;

import static io.github.dead_i.bungeeweb.hikari.HikariDB.*;

public class StatusCheck implements Runnable {
    
	private final @NotNull BungeeWeb plugin;
    private final int inc;

    public StatusCheck(BungeeWeb plugin, int inc) {
        this.plugin = plugin;
        this.inc = inc;
    }

    @Override
    public void run() {
        int cur = (int) (System.currentTimeMillis() / 1000);
        Configuration config = this.plugin.getConfig();
        ArrayList<String> conditions = new ArrayList<>();
        ArrayList<Object> params = new ArrayList<>();

        int players = 0;
        if (config.getBoolean("stats.playercount")) {
            players = plugin.getProxy().getPlayers().size();
            conditions.add("playercount");
            params.add(players);
        }

        try (Connection db = this.plugin.getDatabaseManager().connect()) {

            if (config.getBoolean("stats.activity")) {
            	try (PreparedStatement stm = db.prepareStatement("SELECT COUNT(*) FROM `%1$s` WHERE `time` > ?".formatted(TABLE_LOGS))) {
            		stm.setTimestamp(1, Timestamp.from(Instant.ofEpochSecond((cur - inc))));
            		try (ResultSet rs = stm.executeQuery()) {
            			rs.next();
                        conditions.add("activity");
                        params.add(rs.getInt(1));
            		}
            	}
            }

            if (config.getBoolean("stats.playercount") && config.getBoolean("stats.maxplayers")) {
                try (ResultSet maxplayers = db.createStatement().executeQuery("SELECT * FROM `%1$s` ORDER BY `playercount` DESC LIMIT 1".formatted(TABLE_STATS))) {
                	conditions.add("maxplayers");
                    if (maxplayers.next()) {
                        int max = maxplayers.getInt("playercount");
                        if (players > max) {
                            params.add(players);
                        } else {
                            params.add(max);
                        }
                    } else {
                        params.add(players);
                    }
                }
            }

            if (conditions.isEmpty()) return;

            StringBuilder keys = new StringBuilder("`time`, ");
            StringBuilder values = new StringBuilder(cur + ", ");
            for (String c : conditions) {
                keys.append("`" + c + "`, ");
                values.append("?, ");
            }
            try (PreparedStatement st = db.prepareStatement("INSERT INTO `%1$s` (".formatted(TABLE_STATS) + keys.substring(0, keys.length() - 2) + ") VALUES(" + values.substring(0, values.length() - 2) + ")")) {
            	int i = 0;
                for (Object p : params) {
                    i++;
                    st.setObject(i, p);
                }
                st.executeUpdate();
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("An error occurred when executing the database query to update the statistics.");
            e.printStackTrace();
        }
    }
}
