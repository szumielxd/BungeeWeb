package io.github.dead_i.bungeeweb.api;

import io.github.dead_i.bungeeweb.APICommand;
import io.github.dead_i.bungeeweb.BungeeWeb;
import net.md_5.bungee.api.plugin.Plugin;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class DeleteUser extends APICommand {
    public DeleteUser() {
        super("deleteuser", "settings.users.delete");
    }

    @Override
    public void execute(Plugin plugin, HttpServletRequest req, HttpServletResponse res, String[] args) throws IOException, SQLException {
        String id = req.getParameter("id");
        if (id != null && !id.isEmpty() && BungeeWeb.isNumber(id)) {
        	try (Connection conn = BungeeWeb.getDatabase()) {
        		try (PreparedStatement stm = conn.prepareStatement(String.format("DELETE FROM `%susers` WHERE `id` = ? AND `group` < ?", BungeeWeb.getConfig().getString("database.prefix")))) {
        			stm.setInt(1, Integer.parseInt(id));
                    stm.setInt(2, BungeeWeb.getGroupPower(req));
                    stm.executeUpdate();
                    res.getWriter().print("{ \"status\": 1 }");
        		}
        	}
        }else{
            res.getWriter().print("{ \"status\": 0, \"error\": \"Incorrect usage.\" }");
        }
    }
}
