package io.github.dead_i.bungeeweb;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class PurgeScheduler implements Runnable {
    private String table;
    private long time;
    private long min;

    public PurgeScheduler(String table, int days) {
        this.table = BungeeWeb.getConfig().getString("database.prefix") + table;
        time = days * 86400;

        try (Connection conn = BungeeWeb.getDatabase()) {
        	try (PreparedStatement stm = conn.prepareStatement(String.format("SELECT MIN(`id`) FROM `%s`", this.table))) {
        		ResultSet rs = stm.executeQuery();
                if (rs.next()) min = rs.getLong(1);
        	}
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        // Chunking method courtesy of guidance from http://mysql.rjweb.org/doc.php/deletebig
        try (Connection conn = BungeeWeb.getDatabase()) {
        	long max = -1;
        	try (PreparedStatement stm = conn.prepareStatement(String.format("SELECT `id` FROM `%s` WHERE `id` >= ? ORDER BY `id` LIMIT 1000,1", table))) {
        		stm.setLong(1, min);
        		ResultSet maxquery = stm.executeQuery();
        		if(maxquery.next()) max = maxquery.getLong("id");
        	}
        	if (max > -1) {
        		try (PreparedStatement stm = conn.prepareStatement(String.format("DELETE FROM `%s` WHERE `id` >= ? AND `id` < ? AND `time` < ?", table))) {
        			stm.setLong(1, min);
        			stm.setLong(2, max);
        			stm.setLong(3, (System.currentTimeMillis() / 1000) - time);
        			stm.executeUpdate();
                    min = max;
        		}
        	}
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
