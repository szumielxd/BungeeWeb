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

public class CreateUser extends APICommand {
    public CreateUser() {
        super("createuser", "settings.users.create");
    }

    @Override
    public void execute(Plugin plugin, HttpServletRequest req, HttpServletResponse res, String[] args) throws IOException, SQLException {
        String user = req.getParameter("user");
        String pass = req.getParameter("pass");
        String group = req.getParameter("group");
        String salt = BungeeWeb.salt();

        if (user != null && !user.isEmpty() && pass != null && !pass.isEmpty() && group != null && BungeeWeb.isNumber(group)) {
            if (user.length() <= 16) {
                int groupid = Integer.parseInt(group);
                if (groupid < BungeeWeb.getGroupPower(req)) {
                	try (Connection conn = BungeeWeb.getDatabase()) {
                		try (PreparedStatement stm = conn.prepareStatement(String.format("INSERT INTO `%susers` (`user`, `pass`, `salt`, `group`) VALUES(?, ?, ?, ?)", BungeeWeb.getConfig().getString("database.prefix")))) {
                            stm.setString(1, user);
                            stm.setString(2, BungeeWeb.encrypt(pass, salt));
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
