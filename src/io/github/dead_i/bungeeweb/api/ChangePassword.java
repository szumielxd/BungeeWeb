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

public class ChangePassword extends APICommand {
	
    public ChangePassword(@NotNull BungeeWeb plugin) {
        super(plugin, "changepassword", "settings.password");
    }

    @Override
    public void execute(HttpServletRequest req, HttpServletResponse res, String[] args) throws IOException, SQLException {
        String current = req.getParameter("currentpass");
        String pass = req.getParameter("newpass");
        String confirm = req.getParameter("confirmpass");
        HikariDB database = this.plugin.getDatabaseManager();
        if (current != null && pass != null && confirm != null && pass.equals(confirm) && database.getLogin((String) req.getSession().getAttribute("user"), current).isPresent()) {
        	try (Connection conn = database.connect()) {
        		try (PreparedStatement stm = conn.prepareStatement(String.format("UPDATE `%1$s` SET `pass`= ?, `salt`= ? WHERE `id`= ?", HikariDB.TABLE_USERS))) {
        			String salt = SecureUtils.salt();
                    stm.setString(1, SecureUtils.encrypt(req.getParameter("newpass"), salt));
                    stm.setString(2, salt);
                    stm.setInt(3, (int) req.getSession().getAttribute("id"));
                    stm.executeUpdate();
                    res.getWriter().print("{ \"status\": 1 }");
        		}
        	}
        }else{
            res.getWriter().print("{ \"status\": 0 }");
        }
    }
}
