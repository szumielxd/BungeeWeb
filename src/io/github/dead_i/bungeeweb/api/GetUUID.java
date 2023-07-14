package io.github.dead_i.bungeeweb.api;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;
import java.util.UUID;

import org.jetbrains.annotations.NotNull;

import io.github.dead_i.bungeeweb.APICommand;
import io.github.dead_i.bungeeweb.BungeeWeb;
import io.github.dead_i.bungeeweb.hikari.HikariDB;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class GetUUID extends APICommand {
    
	public GetUUID(@NotNull BungeeWeb plugin) {
        super(plugin, "getuuid", null);
    }

    @Override
    public void execute(HttpServletRequest req, HttpServletResponse res, String[] args) throws IOException, SQLException {
        String user = req.getParameter("username");
        if (user == null) {
            res.getWriter().print("{ \"error\": \"A username was not provided.\" }");
            return;
        }
        try (Connection conn = this.plugin.getDatabaseManager().connect()) {
        	Optional<UUID> uuid = getByUsername(conn, user, false);
        	if (!uuid.isPresent()) {
        		uuid = getByUsername(conn, user, true);
        	}
        	if (uuid.isPresent()) {
                res.getWriter().print("{ \"uuid\": \"" + uuid.get() + "\" }");
            } else {
                res.getWriter().print("{ \"error\": \"No such username exists in the database.\" }");
            }
        }
    }

    private Optional<UUID> getByUsername(Connection conn, String search, boolean partial) throws SQLException {
    	search = search.replace("%", "");
    	String sql = "SELECT `uuid` FROM `%s` WHERE `username` LIKE ? ORDER BY LENGTH(`username`), `id` ASC LIMIT 1".formatted(HikariDB.TABLE_PLAYERS);
    	try (PreparedStatement stm = conn.prepareStatement(sql)) {
    		String param = partial ? "%" + search.replace("_", "\\_") + "%" : search;
    		stm.setString(1, param);
    		try (ResultSet rs = stm.executeQuery()) {
    			if (rs.next()) {
        			return Optional.of(HikariDB.bytesToUuid(rs.getBytes(1)));
        		}
    		}
    	}
    	return Optional.empty();
    }
}
