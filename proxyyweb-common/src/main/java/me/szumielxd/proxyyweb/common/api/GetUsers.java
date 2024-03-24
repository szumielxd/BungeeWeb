package me.szumielxd.proxyyweb.common.api;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;

import org.jetbrains.annotations.NotNull;

import io.github.dead_i.bungeeweb.APICommand;
import io.github.dead_i.bungeeweb.BungeeWeb;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import me.szumielxd.proxyyweb.common.hikari.HikariDB;

public class GetUsers extends APICommand {

    public GetUsers(@NotNull BungeeWeb plugin) {
        super(plugin, "getusers", "settings.users.list");
    }

    @Override
    public void execute(HttpServletRequest req, HttpServletResponse res, String[] args) throws IOException, SQLException {
        HashMap<Integer, Object> out = new HashMap<>();
        try (Connection conn = this.plugin.getDatabaseManager().connect()) {
        	try (PreparedStatement stm = conn.prepareStatement("SELECT * FROM `%1$s`".formatted(HikariDB.TABLE_USERS))) {
        		ResultSet rs = stm.executeQuery();
        		while (rs.next()) {
                    HashMap<String, Object> users = new HashMap<>();
                    users.put("user", rs.getString("user"));
                    users.put("group", rs.getInt("group"));
                    out.put(rs.getInt("id"), users);
                }
                this.log(req, String::new);
                res.getWriter().print(GSON_PARSER.toJson(out));
        	}
        }
    }
}
