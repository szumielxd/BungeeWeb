package io.github.dead_i.bungeeweb.api;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import org.jetbrains.annotations.NotNull;

import io.github.dead_i.bungeeweb.APICommand;
import io.github.dead_i.bungeeweb.BungeeWeb;
import io.github.dead_i.bungeeweb.SecureUtils;
import io.github.dead_i.bungeeweb.hikari.HikariDB;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class CreateUser extends APICommand {
    
	public CreateUser(@NotNull BungeeWeb plugin) {
        super(plugin, "createuser", "settings.users.create");
    }

    @Override
    public void execute(HttpServletRequest req, HttpServletResponse res, String[] args) throws IOException, SQLException {
        String user = req.getParameter("user");
        String pass = req.getParameter("pass");
        String group = req.getParameter("group");
        String salt = SecureUtils.salt();

        if (user != null && !user.isEmpty() && pass != null && !pass.isEmpty() && group != null && BungeeWeb.isNumber(group)) {
            if (user.length() <= 16) {
                int groupid = Integer.parseInt(group);
                if (groupid < BungeeWeb.getGroupPower(req)) {
                	try (Connection conn = this.plugin.getDatabaseManager().connect()) {
                		try (PreparedStatement stm = conn.prepareStatement(String.format("INSERT INTO `%1$s` (`user`, `pass`, `salt`, `group`) VALUES(?, ?, ?, ?)", HikariDB.TABLE_USERS))) {
                            stm.setString(1, user);
                            stm.setString(2, SecureUtils.encrypt(pass, salt));
                            stm.setString(3, salt);
                            stm.setInt(4, groupid);
                            stm.executeUpdate();
                            res.getWriter().print("{ \"status\": 1 }");
                		}
                	}
                }else{
                    res.getWriter().print("{ \"status\": 0, \"error\": \"You do not have permission to create a user of this group.\" }");
                }
            }else{
                res.getWriter().print("{ \"status\": 0, \"error\": \"The username provided is too long.\" }");
            }
        }else{
            res.getWriter().print("{ \"status\": 0, \"error\": \"Incorrect usage.\" }");
        }
    }
}
