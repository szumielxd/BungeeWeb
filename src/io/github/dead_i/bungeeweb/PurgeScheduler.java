package io.github.dead_i.bungeeweb;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class PurgeScheduler implements Runnable {
    
	private final BungeeWeb plugin;
	private final String table;
    private final long time;
    private long min;

    public PurgeScheduler(BungeeWeb plugin, String table, int days) {
        this.plugin = plugin;
    	this.table = this.plugin.getConfig().getString("database.prefix") + table;
        this.time = days * 86400L;

        try (Connection conn = this.plugin.getDatabaseManager().connect()) {
        	try (PreparedStatement stm = conn.prepareStatement("SELECT MIN(`id`) FROM `%1$s`".formatted(this.table))) {
        		try (ResultSet rs = stm.executeQuery()) {
        			if (rs.next()) {
                    	this.min = rs.getLong(1);
                    }
        		}
        	}
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        // Chunking method courtesy of guidance from http://mysql.rjweb.org/doc.php/deletebig
        try (Connection conn = this.plugin.getDatabaseManager().connect()) {
        	long max = -1;
        	try (PreparedStatement stm = conn.prepareStatement("SELECT `id` FROM `%1$s` WHERE `id` >= ? ORDER BY `id` LIMIT 1000, 1".formatted(table))) {
        		stm.setLong(1, min);
        		try (ResultSet maxquery = stm.executeQuery()) {
        			if(maxquery.next()) {
        				max = maxquery.getLong(1);
        			}
        		}
        		
        	}
        	if (max > -1) {
        		try (PreparedStatement stm = conn.prepareStatement("DELETE FROM `%1$s` WHERE `id` >= ? AND `id` < ? AND `time` < ?".formatted(table))) {
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
