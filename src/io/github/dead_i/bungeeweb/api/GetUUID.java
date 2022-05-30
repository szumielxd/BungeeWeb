package io.github.dead_i.bungeeweb.api;

import io.github.dead_i.bungeeweb.APICommand;
import io.github.dead_i.bungeeweb.BungeeWeb;
import net.md_5.bungee.api.plugin.Plugin;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

public class GetUUID extends APICommand {
    public GetUUID() {
        super("getuuid", null);
    }

    @Override
    public void execute(Plugin plugin, HttpServletRequest req, HttpServletResponse res, String[] args) throws IOException, SQLException {
        String user = req.getParameter("username");
        if (user == null) {
            res.getWriter().print("{ \"error\": \"A username was not provided.\" }");
            return;
        }
        try (Connection conn = BungeeWeb.getDatabase()) {
        	Optional<String> uuid = getByUsername(conn, user, false);
        	if (!uuid.isPresent()) uuid = getByUsername(conn, user, true);
        	if (uuid.isPresent()) {
                res.getWriter().print("{ \"uuid\": \"" + uuid.get() + "\" }");
            } else {
                res.getWriter().print("{ \"error\": \"No such username exists in the database.\" }");
            }
        }
    }

    private Optional<String> getByUsername(Connection conn, String search, boolean partial) throws SQLException {
    	try (PreparedStatement stm = conn.prepareStatement(String.format("SELECT `uuid` FROM `%slog` WHERE `username` LIKE ? ORDER BY `id` DESC LIMIT 1", BungeeWeb.getConfig().getString("database.prefix")))) {
    		stm.setString(1, partial ? "%" + search + "%" : search);
    		ResultSet rs = stm.executeQuery();
    		if (rs.next()) {
    			return Optional.of(rs.getString(1));
    		}
    	}
    	return Optional.empty();
    }
}
