package me.szumielxd.proxyyweb.common.api;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;

import org.jetbrains.annotations.NotNull;

import io.github.dead_i.bungeeweb.APICommand;
import io.github.dead_i.bungeeweb.BungeeWeb;
import io.github.dead_i.bungeeweb.SecureUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import me.szumielxd.proxyyweb.common.hikari.HikariDB;

public class EditUser extends APICommand {
    
	public EditUser(@NotNull BungeeWeb plugin) {
        super(plugin, "edituser", "settings.users.edit");
    }

    @Override
    public void execute(HttpServletRequest req, HttpServletResponse res, String[] args) throws IOException, SQLException {
        List<String> conditions = new LinkedList<>();
        List<Object> params = new LinkedList<>();

        String user = req.getParameter("user");
        if (user != null && !user.isEmpty() && user.length() <= 16) {
            conditions.add("user");
            params.add(user);
        }

        String pass = req.getParameter("pass");
        if (pass != null && !pass.isEmpty()) {
            String salt = SecureUtils.salt();
            conditions.add("pass");
            params.add(SecureUtils.encrypt(pass, salt));
            conditions.add("salt");
            params.add(salt);
        }

        String group = req.getParameter("group");
        int groupid = Integer.parseInt(group);
        if (group != null && !group.isEmpty() && BungeeWeb.isNumber(group)) {
            conditions.add("group");
            params.add(groupid);
        }

        String id = req.getParameter("id");
        if (id != null && !id.isEmpty() && BungeeWeb.isNumber(id) && !conditions.isEmpty()) {
            int power = BungeeWeb.getGroupPower(req);
            if (!conditions.contains("group") || groupid < power) {
                String cond = "";
                for (String s : conditions) {
                    cond += "`" + s + "`=?, ";
                }
                cond = cond.substring(0, cond.length() - 2);

                try (Connection conn = this.plugin.getDatabaseManager().connect()) {
                	try (PreparedStatement stm = conn.prepareStatement(String.format("UPDATE `%1$s` SET %2$s WHERE `id` = ? AND `group` < ?", HikariDB.TABLE_USERS, cond))) {
                        int i = 0;
                        for (Object o : params) {
                            stm.setObject(++i, o);
                        }
                        stm.setInt(i + 1, Integer.parseInt(id));
                        stm.setInt(i + 2, power);
                        stm.executeUpdate();
                        this.log(req, () -> "success -> " + conditions);
                        res.getWriter().print("{ \"status\": 1 }");
                	}
                }
            } else {
                this.log(req, "failure");
                res.getWriter().print("{ \"status\": 0, \"error\": \"You do not have permission to edit a user to this group.\" }");
            }
        } else {
            this.log(req, "failure");
            res.getWriter().print("{ \"status\": 0, \"error\": \"Incorrect usage.\" }");
        }
    }
}
