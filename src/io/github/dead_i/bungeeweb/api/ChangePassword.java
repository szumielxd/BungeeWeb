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

public class ChangePassword extends APICommand {
    public ChangePassword() {
        super("changepassword", "settings.password");
    }

    @Override
    public void execute(Plugin plugin, HttpServletRequest req, HttpServletResponse res, String[] args) throws IOException, SQLException {
        String current = req.getParameter("currentpass");
        String pass = req.getParameter("newpass");
        String confirm = req.getParameter("confirmpass");
        if (current != null && pass != null && confirm != null && pass.equals(confirm) && BungeeWeb.getLogin((String) req.getSession().getAttribute("user"), current) != null) {
        	try (Connection conn = BungeeWeb.getDatabase()) {
        		try (PreparedStatement stm = conn.prepareStatement(String.format("UPDATE `%susers` SET `pass`= ?, `salt`= ? WHERE `id`= ?", BungeeWeb.getConfig().getString("database.prefix")))) {
        			String salt = BungeeWeb.salt();
                    stm.setString(1, BungeeWeb.encrypt(req.getParameter("newpass"), salt));
                    stm.setString(2, salt);
                    stm.setInt(3, Integer.parseInt((String) req.getSession().getAttribute("id")));
                    stm.executeUpdate();
                    res.getWriter().print("{ \"status\": 1 }");
        		}
        	}
        }else{
            res.getWriter().print("{ \"status\": 0 }");
        }
    }
}
