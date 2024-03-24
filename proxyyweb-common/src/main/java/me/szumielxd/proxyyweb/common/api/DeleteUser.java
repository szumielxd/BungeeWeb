package me.szumielxd.proxyyweb.common.api;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import org.jetbrains.annotations.NotNull;

import io.github.dead_i.bungeeweb.APICommand;
import io.github.dead_i.bungeeweb.BungeeWeb;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import me.szumielxd.proxyyweb.common.hikari.HikariDB;

public class DeleteUser extends APICommand {
    
	public DeleteUser(@NotNull BungeeWeb plugin) {
        super(plugin, "deleteuser", "settings.users.delete");
    }

    @Override
    public void execute(HttpServletRequest req, HttpServletResponse res, String[] args) throws IOException, SQLException {
        String id = req.getParameter("id");
        if (id != null && !id.isEmpty() && BungeeWeb.isNumber(id)) {
        	try (Connection conn = this.plugin.getDatabaseManager().connect()) {
        		try (PreparedStatement stm = conn.prepareStatement(String.format("DELETE FROM `%1$s` WHERE `id` = ? AND `group` < ?", HikariDB.TABLE_USERS))) {
        			stm.setInt(1, Integer.parseInt(id));
                    stm.setInt(2, BungeeWeb.getGroupPower(req));
                    stm.executeUpdate();
                    this.log(req, () -> "success -> " + id);
                    res.getWriter().print("{ \"status\": 1 }");
        		}
        	}
        }else {
            this.log(req, "failure");
            res.getWriter().print("{ \"status\": 0, \"error\": \"Incorrect usage.\" }");
        }
    }
}
