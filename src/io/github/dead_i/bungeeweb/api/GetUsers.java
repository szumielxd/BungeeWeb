package io.github.dead_i.bungeeweb.api;

import com.google.gson.Gson;
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
import java.util.HashMap;

public class GetUsers extends APICommand {
    private Gson gson = new Gson();

    public GetUsers() {
        super("getusers", "settings.users.list");
    }

    @Override
    public void execute(Plugin plugin, HttpServletRequest req, HttpServletResponse res, String[] args) throws IOException, SQLException {
        HashMap<Integer, Object> out = new HashMap<>();
        try (Connection conn = BungeeWeb.getDatabase()) {
        	try (PreparedStatement stm = conn.prepareStatement(String.format("SELECT * FROM `%susers`", BungeeWeb.getConfig().getString("database.prefix")))) {
        		ResultSet rs = stm.executeQuery();
        		while (rs.next()) {
                    HashMap<String, Object> users = new HashMap<>();
                    users.put("user", rs.getString("user"));
                    users.put("group", rs.getInt("group"));
                    out.put(rs.getInt("id"), users);
                }
                res.getWriter().print(gson.toJson(out));
        	}
        }
    }
}
